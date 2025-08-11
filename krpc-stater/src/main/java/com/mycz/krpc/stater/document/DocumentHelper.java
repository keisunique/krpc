package com.mycz.krpc.stater.document;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.mycz.arch.common.config.Bool;
import com.mycz.arch.common.gateway.RequestMethod;
import com.mycz.arch.common.util.HttpKit;
import com.mycz.arch.common.util.JsonKit;

import com.mycz.arch.common.util.StringKit;
import com.mycz.arch.common.validation.annotation.*;
import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.stater.config.RpcProperties;
import com.mycz.krpc.stater.document.annotation.ApiModelProperty;
import com.mycz.krpc.stater.document.annotation.ApiTag;
import com.mycz.krpc.stater.document.entity.ApiUploadReq;
import com.mycz.krpc.stater.gateway.annotation.AuthorityType;
import com.mycz.krpc.stater.gateway.annotation.RequestMapping;
import com.mycz.krpc.stater.gateway.annotation.ResponseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档上报辅助类
 *
 * 职责：
 * 1. 扫描指定包前缀下的 Spring Bean
 * 2. 解析方法上的网关注解 `RequestMapping` 及字段上的文档/校验注解
 * 3. 组装为 `ApiUploadReq` 结构并通过 HTTP 上报
 */
@Slf4j
public class DocumentHelper {

    private static final String DATA_FIELD_NAME = "data";

    private final ApplicationContext applicationContext;
    private final RpcProperties rpcProperties;

    public DocumentHelper(ApplicationContext applicationContext, RpcProperties rpcProperties) {
        this.applicationContext = applicationContext;
        this.rpcProperties = rpcProperties;
    }

    /**
     * 触发文档上报
     *
     * 流程：校验开关和 URL -> 扫描 Bean -> 解析并收集接口信息 -> 发起 HTTP 上报
     */
    public void report() throws Exception {
        if (rpcProperties.getDocument() == null || !rpcProperties.getDocument().getEnable()) {
            return;
        }

        if (StringKit.isBlank(rpcProperties.getDocument().getUrl())) {
            log.warn("*** api接口上报url为空, 取消上报");
            return;
        }

        log.info("*** api接口上报 - 开始");

        final String[] allBeanNames = applicationContext.getBeanDefinitionNames();

        final ApiUploadReq uploadReq = new ApiUploadReq();
        final List<ApiUploadReq.Item> allApis = new ArrayList<>();
        uploadReq.setApiInfos(allApis);

        for (String name : allBeanNames) {
            Object impl = applicationContext.getBean(name);
            if (!impl.getClass().getPackageName().startsWith("com.ttc.service.service")) {
                continue;
            }

            List<ApiUploadReq.Item> apis = this.processImplApi(impl);
            if (apis != null && !apis.isEmpty()) {
                allApis.addAll(apis);
            }
        }

        String result = HttpKit.post(rpcProperties.getDocument().getUrl(), JsonKit.toJson(uploadReq, PropertyNamingStrategies.SNAKE_CASE));
        log.info("*** api接口上报 - 完成, 结果: {}", result);
    }

    /**
     * 处理单个 Bean，提取其对外暴露的接口信息
     */
    private List<ApiUploadReq.Item> processImplApi(Object impl) {
        try {
            final Class<?> implClass = impl.getClass();

            // 获取服务名
            final String serviceName = getImplServiceName(implClass);
            if (StringKit.isBlank(serviceName)) {
                return null;
            }

            // 标签优先取 `@ApiTag`，否则用类名
            final ApiTag apiTagAnno = implClass.getAnnotation(ApiTag.class);
            String tag = apiTagAnno == null ? implClass.getSimpleName() : apiTagAnno.value();

            // 单个接口
            final List<ApiUploadReq.Item> itemList = new ArrayList<>();
            for (Method implMethod : implClass.getMethods()) {
                // 遍历该方法上所有的 RequestMapping 注解
                Arrays.stream(implMethod.getAnnotations())
                        .filter(a -> a instanceof RequestMapping)
                        .forEach(mapping -> {
                            ApiUploadReq.Item item = new ApiUploadReq.Item();
                            item.setServiceName(serviceName);
                            item.setClassName(implClass.getName());
                            item.setTag(tag);

                            try {
                                item.setName(implMethod.getName());
                                item.setMethod(((RequestMethod) getAnnotationValue(mapping, "method")).name());
                                item.setPath(getStringFromAnnotation(mapping, "path"));
                                item.setPrefix(getStringFromAnnotation(mapping, "prefix"));

                                item.setAuthority(getBooleanFromAnnotation(mapping, "authority") ? Bool.YES : Bool.NO);
                                item.setAuthorityType(((AuthorityType) getAnnotationValue(mapping, "authorityType")).name());
                                item.setDescription(getStringFromAnnotation(mapping, "description"));
                                item.setResponseType(((ResponseType) getAnnotationValue(mapping, "responseType")).name());

                                item.setDeliverPayload(getBooleanFromAnnotation(mapping, "deliverPayload") ? Bool.YES : Bool.NO);
                                item.setDeliverParams(getBooleanFromAnnotation(mapping, "deliverParams") ? Bool.YES : Bool.NO);

                                // 接口参数
                                populateParameterInfo(implMethod, item);

                                itemList.add(item);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to process RequestMapping annotation for method: " + implMethod.getName(), e);
                            }
                        });
            }

            return itemList;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process bean: " + impl.getClass().getName(), e);
        }
    }

    /**
     * 反射读取注解方法值
     */
    private Object getAnnotationValue(Annotation annotation, String methodName) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            return method.invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get annotation value for method: " + methodName, e);
        }
    }

    private boolean getBooleanFromAnnotation(Annotation annotation, String methodName) {
        return (Boolean) getAnnotationValue(annotation, methodName);
    }

    private String getStringFromAnnotation(Annotation annotation, String methodName) {
        return (String) getAnnotationValue(annotation, methodName);
    }

    /**
     * 填充接口的参数与返回值相关信息
     */
    private void populateParameterInfo(Method method, ApiUploadReq.Item item) {
        // 请求类名
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length < 1) {
            return;
        }

        item.setRequestClass(parameterTypes[0].getName());

        // 响应类名
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0 && !actualTypeArguments[0].getTypeName().equals("?")) {
                item.setResponseClass(actualTypeArguments[0].getTypeName());
            }
        }

        // 请求参数,响应参数
        List<ApiUploadReq.Item.Params> params = new ArrayList<>();
        populateRequestFields(parameterTypes, params);
        Set<String> visited = new HashSet<>();
        populateResponseFields(params, method.getReturnType().getDeclaredFields(), null, item, visited);
        item.setParams(params);
    }

    /**
     * 从接口类上提取服务名（扫描其实现接口上的 `@KrpcReference`）
     */
    private String getImplServiceName(Class<?> beanClass) {
        return Arrays.stream(beanClass.getInterfaces())
                .flatMap(interfaceClass -> Arrays.stream(interfaceClass.getAnnotations()))
                .filter(annotation -> annotation instanceof KrpcReference)
                .findFirst()
                .map(annotation -> getStringFromAnnotation(annotation, "serviceName"))
                .orElse("");
    }

    /**
     * 遍历请求参数类型并提取其字段信息
     */
    private void populateRequestFields(Class<?>[] parameterTypes, List<ApiUploadReq.Item.Params> params) {
        Arrays.stream(parameterTypes)
                .flatMap(parameterType -> Arrays.stream(parameterType.getDeclaredFields()))
                .forEach(field -> processRequestField(field, params));
    }

    /**
     * 处理单个请求字段
     */
    private void processRequestField(Field field, List<ApiUploadReq.Item.Params> params) {
        ApiUploadReq.Item.Params param = new ApiUploadReq.Item.Params();
        params.add(param);

        String fieldName = StringKit.camelToUnderline(field.getName());
        param.setField(fieldName);
        param.setType("request");
        param.setFieldClass(field.getType().getSimpleName());

        processFieldAnnotations(field, param);
    }

    /**
     * 解析字段上的注解以设置描述与必填信息
     */
    private void processFieldAnnotations(Field field, ApiUploadReq.Item.Params param) {
        Arrays.stream(field.getAnnotations())
                .forEach(annotation -> processFieldAnnotation(annotation, param));
    }

    private void processFieldAnnotation(Annotation annotation, ApiUploadReq.Item.Params param) {
        try {
            if (annotation instanceof ApiModelProperty) {
                param.setDescription(getStringFromAnnotation(annotation, "value"));
            } else if (annotation instanceof NotNull || annotation instanceof NotBlank || annotation instanceof NotEmpty) {
                param.setRequired(Bool.YES);
            } else if (isValidationAnnotationWithRequired(annotation)) {
                boolean required = getBooleanFromAnnotation(annotation, "required");
                param.setRequired(required ? Bool.YES : Bool.NO);
            }
        } catch (Exception e) {
            // 保持原有行为：打印错误但不中断整体流程
            System.err.println("Failed to process annotation: " + annotation.annotationType().getSimpleName());
        }
    }

    private boolean isValidationAnnotationWithRequired(Annotation annotation) {
        return annotation instanceof Range || annotation instanceof Email ||
               annotation instanceof Length || annotation instanceof Mobile ||
               annotation instanceof SetOf || annotation instanceof Size;
    }

    /**
     * 递归填充响应字段信息
     */
    private void populateResponseFields(List<ApiUploadReq.Item.Params> params, Field[] fields, String fieldPrefix, ApiUploadReq.Item apiInterfaceInfo, Set<String> visited) {
        Arrays.stream(fields).forEach(field -> processResponseField(field, params, fieldPrefix, apiInterfaceInfo, visited));
    }

    private void processResponseField(Field field, List<ApiUploadReq.Item.Params> params, String fieldPrefix, ApiUploadReq.Item apiInterfaceInfo, Set<String> visited) {
        try {
            String fieldName = buildFieldName(field.getName(), fieldPrefix);
            Class<?> fieldClass = determineFieldClass(field, fieldName, apiInterfaceInfo);

            if (fieldClass == null) {
                return;
            }

            ApiUploadReq.Item.Params param = createResponseParam(field, fieldName, fieldClass);
            params.add(param);

            // 递归处理实体类字段，使用 visited 防止循环引用
            if (isCustomEntityClass(fieldClass)) {
                String visitKey = fieldClass.getName();
                if (visited.contains(visitKey)) {
                    return;
                }
                visited.add(visitKey);
                try {
                    populateResponseFields(params, fieldClass.getDeclaredFields(), fieldName, apiInterfaceInfo, visited);
                } finally {
                    visited.remove(visitKey);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process response field: " + field.getName() + ", error: " + e.getMessage());
        }
    }

    private String buildFieldName(String fieldName, String fieldPrefix) {
        if (StringKit.isNotBlank(fieldPrefix)) {
            fieldName = fieldPrefix + "." + fieldName;
        }
        return StringKit.camelToUnderline(fieldName);
    }

    private Class<?> determineFieldClass(Field field, String fieldName, ApiUploadReq.Item apiInterfaceInfo) throws ClassNotFoundException {
        Class<?> fieldClass = field.getType();

        // 处理泛型参数
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            if (pt.getActualTypeArguments().length > 2) {
                return null; // 跳过超过两个泛型参数的字段
            }
            Type actualTypeArgument = pt.getActualTypeArguments()[0];
            fieldClass = Class.forName(actualTypeArgument.getTypeName());
        }

        // 特殊处理data字段
        if (DATA_FIELD_NAME.equals(fieldName)) {
            if (StringKit.isNotBlank(apiInterfaceInfo.getResponseClass())) {
                fieldClass = Class.forName(apiInterfaceInfo.getResponseClass());
            } else {
                return null; // 跳过没有响应类的data字段
            }
        }

        return fieldClass;
    }

    private ApiUploadReq.Item.Params createResponseParam(Field field, String fieldName, Class<?> fieldClass) {
        ApiUploadReq.Item.Params param = new ApiUploadReq.Item.Params();

        String fieldClassName = field.getType().getName();
        if (!fieldClassName.startsWith("java.util.List") && !fieldClassName.startsWith("java.util.Set")) {
            fieldClassName = fieldClass.getSimpleName();
        }

        param.setField(fieldName);
        param.setType("response");
        param.setFieldClass(fieldClassName);

        // 处理ApiModelProperty注解
        Arrays.stream(field.getAnnotations())
                .filter(annotation -> annotation instanceof ApiModelProperty)
                .findFirst()
                .ifPresent(annotation -> param.setDescription(getStringFromAnnotation(annotation, "value")));

        return param;
    }

    private boolean isCustomEntityClass(Class<?> fieldClass) {
        String packageName = fieldClass.getPackageName();
        return packageName.startsWith("com.ttc.api") || packageName.startsWith("com.mycz.arch");
    }

}
