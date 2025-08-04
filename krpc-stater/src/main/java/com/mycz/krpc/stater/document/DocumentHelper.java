package com.mycz.krpc.stater.document;

import com.mycz.arch.common.gateway.RequestMethod;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.arch.common.util.StringKit;
import com.mycz.arch.common.validation.annotation.*;
import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.stater.document.annotation.Api;
import com.mycz.krpc.stater.document.annotation.ApiModelProperty;
import com.mycz.krpc.stater.document.entity.ApiInfo;
import com.mycz.krpc.stater.document.entity.ApiInterfaceInfo;
import com.mycz.krpc.stater.document.entity.ApiUpload;
import com.mycz.krpc.stater.gateway.annotation.AuthorityType;
import com.mycz.krpc.stater.gateway.annotation.RequestMapping;
import com.mycz.krpc.stater.gateway.annotation.ResponseType;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DocumentHelper {

    private final ApplicationContext applicationContext;

    public DocumentHelper(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void report() throws Exception {
        this.getRequestMappingAnnotatedMethods();
    }

    private void getRequestMappingAnnotatedMethods() throws Exception {
        // 获取Spring容器中所有Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            // api接口信息上传对象
            ApiUpload apiUpload = new ApiUpload();
            ApiInfo apiInfo = new ApiInfo();
            apiUpload.setApiInfo(apiInfo);

            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = bean.getClass();
            if (!beanClass.getPackageName().startsWith("com.ttc.service.service")) {
                continue;
            }

            // 获取api信息
            if (this.getApiInfo(beanClass, apiInfo)) {
                continue;
            }

            List<ApiInterfaceInfo> apiInterfaceInfos = new ArrayList<>();
            apiUpload.setApiInterfaceInfos(apiInterfaceInfos);

            // 获取类中所有方法
            this.getApiInterfaceInfo(beanClass, apiInterfaceInfos);

            // TODO 请求api上传接口
            // apiUpload
            System.out.println(JsonKit.toJson(apiUpload));

        }
    }

    private void getApiInterfaceInfo(Class<?> beanClass, List<ApiInterfaceInfo> apiInterfaceInfos) throws Exception {
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            // 检查方法是否包含@RequestMapping注解
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestMapping) {
                    ApiInterfaceInfo apiInterfaceInfo = new ApiInterfaceInfo();
                    apiInterfaceInfos.add(apiInterfaceInfo);

                    Class<? extends Annotation> annotationedClass = annotation.annotationType();

                    apiInterfaceInfo.setName(method.getName());
                    apiInterfaceInfo.setMethod(((RequestMethod) annotationedClass.getMethod("method").invoke(annotation)).name());
                    apiInterfaceInfo.setPath((String) annotationedClass.getMethod("path").invoke(annotation));
                    apiInterfaceInfo.setPrefix((String) annotationedClass.getMethod("prefix").invoke(annotation));

                    Short authority = (short) ((Boolean) annotationedClass.getMethod("authority").invoke(annotation) ? 1 : 0);
                    apiInterfaceInfo.setAuthority(authority);
                    apiInterfaceInfo.setAuthorityType(((AuthorityType) annotationedClass.getMethod("authorityType").invoke(annotation)).name());
                    apiInterfaceInfo.setDescription((String) annotationedClass.getMethod("description").invoke(annotation));
                    apiInterfaceInfo.setResponseType(((ResponseType) annotationedClass.getMethod("responseType").invoke(annotation)).name());

                    Short deliverPayload = (short) ((Boolean) annotationedClass.getMethod("deliverPayload").invoke(annotation) ? 1 : 0);
                    apiInterfaceInfo.setDeliverPayload(deliverPayload);
                    Short deliverParams = (short) ((Boolean) annotationedClass.getMethod("deliverParams").invoke(annotation) ? 1 : 0);
                    apiInterfaceInfo.setDeliverParams(deliverParams);

                    Class<?> parameter = method.getParameterTypes()[0];
                    apiInterfaceInfo.setRequestClass(parameter.getName());

                    // 获取BaseResponse泛型数据
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (!actualTypeArguments[0].getTypeName().equals("?")) {
                            apiInterfaceInfo.setResponseClass(actualTypeArguments[0].getTypeName());
                        }
                    }

                    List<ApiInterfaceInfo.Params> params = new ArrayList<>();
                    apiInterfaceInfo.setParams(params);
                    // 接口参数信息
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    this.getRequestFiled(parameterTypes, params);

                    // 获取返回参数字段
                    Class<?> returnType = method.getReturnType();
                    this.getResponseFiled(params, returnType.getDeclaredFields(), null, apiInterfaceInfo);
                }
            }
        }
    }

    private boolean getApiInfo(Class<?> beanClass, ApiInfo apiInfo) throws Exception {
        Annotation[] classAnnotations = beanClass.getAnnotations();

        String serviceName = "";
        // 获取实现接口的注解
        Class<?>[] interfaces = beanClass.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            Annotation[] interfaceAnnotations = interfaceClass.getAnnotations();
            Optional<Annotation> apiAnnotation = Arrays.stream(interfaceAnnotations)
                    .filter(annotation -> annotation instanceof KrpcReference)
                    .findFirst();
            if (apiAnnotation.isPresent()) {
                Method method = apiAnnotation.get().annotationType().getMethod("serviceName");
                serviceName = (String) method.invoke(apiAnnotation.get());
                break;
            }
        }

        Optional<Annotation> annotation = Arrays.stream(classAnnotations)
                .filter(a -> a instanceof Api)
                .findFirst();
        if (annotation.isEmpty()) {
            return true;
        }
        Annotation api = annotation.get();

        apiInfo.setServiceName(serviceName);
        apiInfo.setApiClass(beanClass.getName());
        apiInfo.setTags((String) api.annotationType().getMethod("tags").invoke(api));
        apiInfo.setDescription((String) api.annotationType().getMethod("description").invoke(api));
        return false;
    }

    private void getRequestFiled(Class<?>[] parameterTypes, List<ApiInterfaceInfo.Params> params) throws Exception {
        for (Class<?> parameterType : parameterTypes) {
            Field[] declaredFields = parameterType.getDeclaredFields();
            for (Field field : declaredFields) {
                ApiInterfaceInfo.Params param = new ApiInterfaceInfo.Params();
                params.add(param);

                String fieldName = StringKit.camelToUnderline(field.getName());
                param.setField(fieldName);
                param.setType("request");
                param.setFieldClass(field.getType().getName());
                Annotation[] fieldAnnotations = field.getAnnotations();
                for (Annotation fieldAnnotation : fieldAnnotations) {
                    Class<? extends Annotation> fieldAnnotationClass = fieldAnnotation.annotationType();
                    if (fieldAnnotation instanceof ApiModelProperty) {
                        param.setDescription((String) fieldAnnotationClass.getMethod("value").invoke(fieldAnnotation));
                    } else if (fieldAnnotation instanceof NotNull) {
                        param.setRequired((short) 1);
                    } else if (fieldAnnotation instanceof NotBlank) {
                        param.setRequired((short) 1);
                    } else if (fieldAnnotation instanceof NotEmpty) {
                        param.setRequired((short) 1);
                    } else if (fieldAnnotation instanceof Range) {
                        Boolean required = (Boolean) fieldAnnotationClass.getMethod("required").invoke(fieldAnnotation);
                        param.setRequired(required ? (short) 1 : 0);
                    } else if (fieldAnnotation instanceof Email) {
                        Boolean required = (Boolean) fieldAnnotationClass.getMethod("required").invoke(fieldAnnotation);
                        param.setRequired(required ? (short) 1 : 0);
                    } else if (fieldAnnotation instanceof Length) {
                        Boolean required = (Boolean) fieldAnnotationClass.getMethod("required").invoke(fieldAnnotation);
                        param.setRequired(required ? (short) 1 : 0);
                    } else if (fieldAnnotation instanceof Mobile) {
                        Boolean required = (Boolean) fieldAnnotationClass.getMethod("required").invoke(fieldAnnotation);
                        param.setRequired(required ? (short) 1 : 0);
                    } else if (fieldAnnotation instanceof SetOf) {
                        Boolean required = (Boolean) fieldAnnotationClass.getMethod("required").invoke(fieldAnnotation);
                        param.setRequired(required ? (short) 1 : 0);
                    } else if (fieldAnnotation instanceof Size) {
                        Boolean required = (Boolean) fieldAnnotationClass.getMethod("required").invoke(fieldAnnotation);
                        param.setRequired(required ? (short) 1 : 0);
                    }
                }
            }
        }
    }

    private void getResponseFiled(List<ApiInterfaceInfo.Params> params, Field[] fields, String fieldPrefix, ApiInterfaceInfo apiInterfaceInfo) throws Exception {
        for (Field field : fields) {
            ApiInterfaceInfo.Params param = new ApiInterfaceInfo.Params();

            String fieldName = field.getName();
            if (StringKit.isNotBlank(fieldPrefix)) {
                fieldName = fieldPrefix + "." + field.getName();
            }
            fieldName = StringKit.camelToUnderline(fieldName);

            Class<?> fieldClass = field.getType();

            String fieldClassName = fieldClass.getName();

            // 判断参数是否是泛型参数
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType pt) {
                // 字段超过两个泛型参数，则跳过
                if (pt.getActualTypeArguments().length > 2) {
                    continue;
                }
                Type actualTypeArgument = pt.getActualTypeArguments()[0]; // 获取第一个泛型参数
                // 获取泛型参数的类型
                fieldClass = Class.forName(actualTypeArgument.getTypeName());
            }

            // 如果参数名称为data，则直接获取BaseResponse中的泛型对象
            if (fieldName.equals("data")) {
                if (StringKit.isNotBlank(apiInterfaceInfo.getResponseClass())) {
                    fieldClass = Class.forName(apiInterfaceInfo.getResponseClass());
                } else {
                    continue;
                }
            }
            params.add(param);

            // 如果参数是实体类，则递归获取参数字段
            if (fieldClass.getPackageName().startsWith("com.ttc.api")
                || fieldClass.getPackageName().startsWith("com.mycz.arch")) {
                this.getResponseFiled(params, fieldClass.getDeclaredFields(), fieldName, apiInterfaceInfo);
            }
            if (!fieldClassName.startsWith("java.util.List") && !fieldClassName.startsWith("java.util.Set")) {
                fieldClassName = fieldClass.getName();
            }

            param.setField(fieldName);
            param.setType("response");
            param.setFieldClass(fieldClassName);
            Annotation[] fieldAnnotations = field.getAnnotations();
            for (Annotation fieldAnnotation : fieldAnnotations) {
                Class<? extends Annotation> fieldAnnotationClass = fieldAnnotation.annotationType();
                if (fieldAnnotation instanceof ApiModelProperty) {
                    param.setDescription((String) fieldAnnotationClass.getMethod("value").invoke(fieldAnnotation));
                }
            }
        }
    }


}
