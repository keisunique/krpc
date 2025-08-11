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
import com.mycz.krpc.stater.gateway.annotation.RequestMappings;
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
 * <p>
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
     * <p>
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

                for (Annotation annotation : implMethod.getAnnotations()) {
                    RequestMapping[] mappings = new RequestMapping[1];
                    if (annotation instanceof RequestMapping) {
                        mappings[0] = (RequestMapping) annotation;
                    } else if ((annotation instanceof RequestMappings)) {
                        mappings = ((RequestMappings) annotation).value();
                    } else {
                        continue;
                    }


                    for (RequestMapping mapping : mappings) {
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
                    }
                }
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

        // 响应类名（展示）与用于递归的组件类
        Type genericReturnType = method.getGenericReturnType();
        Class<?> responseComponentClass = null;
        if (genericReturnType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                Type arg = actualTypeArguments[0];
                // 构建展示：List<String>/Set<User>/User/Object
                String display = buildTypeArgumentDisplay(arg);
                item.setResponseClass(display);
                // 计算 data 字段用于递归的组件类（元素类或实体类）
                responseComponentClass = resolveComponentClass(arg);
            }
        }

        // 请求参数,响应参数
        List<ApiUploadReq.Item.Params> params = new ArrayList<>();
        populateRequestFields(parameterTypes, params);
        Set<String> visited = new HashSet<>();
        populateResponseFields(params, method.getReturnType().getDeclaredFields(), null, item, visited, responseComponentClass);
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
        // 展示泛型：List<String> / List<Object>
        param.setFieldClass(buildFieldClassDisplay(field, field.getType()));

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
    private void populateResponseFields(List<ApiUploadReq.Item.Params> params, Field[] fields, String fieldPrefix, ApiUploadReq.Item apiInterfaceInfo, Set<String> visited, Class<?> responseComponentClass) {
        Arrays.stream(fields).forEach(field -> processResponseField(field, params, fieldPrefix, apiInterfaceInfo, visited, responseComponentClass));
    }

    private void processResponseField(Field field, List<ApiUploadReq.Item.Params> params, String fieldPrefix, ApiUploadReq.Item apiInterfaceInfo, Set<String> visited, Class<?> responseComponentClass) {
        try {
            String fieldName = buildFieldName(field.getName(), fieldPrefix);
            Class<?> fieldClass = determineFieldClass(field, fieldName, apiInterfaceInfo, responseComponentClass);

            if (fieldClass == null) {
                return;
            }

            ApiUploadReq.Item.Params param = createResponseParam(field, fieldName, fieldClass, apiInterfaceInfo);
            params.add(param);

            // 递归处理实体类字段，使用 visited 防止循环引用
            if (isCustomEntityClass(fieldClass)) {
                String visitKey = fieldClass.getName();
                if (visited.contains(visitKey)) {
                    return;
                }
                visited.add(visitKey);
                try {
                    populateResponseFields(params, fieldClass.getDeclaredFields(), fieldName, apiInterfaceInfo, visited, responseComponentClass);
                } finally {
                    visited.remove(visitKey);
                }
            }
        } catch (Exception e) {
            System.err.println("Fail: " + e.getMessage());
        }
    }

    private String buildFieldName(String fieldName, String fieldPrefix) {
        if (StringKit.isNotBlank(fieldPrefix)) {
            fieldName = fieldPrefix + "." + fieldName;
        }
        return StringKit.camelToUnderline(fieldName);
    }

    private Class<?> determineFieldClass(Field field, String fieldName, ApiUploadReq.Item item, Class<?> responseComponentClass) throws ClassNotFoundException {
        Class<?> fieldClass = field.getType();

        // 处理泛型参数
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            if (pt.getActualTypeArguments().length > 0) {
                Type actualTypeArgument = pt.getActualTypeArguments()[0];
                String typeName = actualTypeArgument.getTypeName();
                String resolved = resolveToLoadableClassName(typeName);
                if (StringKit.isBlank(resolved)) {
                    return null;
                }
                fieldClass = Class.forName(resolved);
            }
        }

        // 特殊处理data字段：优先使用已解析的组件类
        if (DATA_FIELD_NAME.equals(fieldName)) {
            if (responseComponentClass != null) {
                return responseComponentClass;
            }

            String responseClass = item.getResponseClass();
            if (StringKit.isBlank(responseClass)) {
                return null;
            }
            // 支持形如 List<Foo> / Set<Bar> / List<?> 的写法
            if (responseClass.contains("<") && responseClass.endsWith(">")) {
                String genericPart = responseClass.substring(responseClass.indexOf('<') + 1, responseClass.lastIndexOf('>')).trim();
                int commaIdx = genericPart.indexOf(',');
                if (commaIdx > -1) {
                    genericPart = genericPart.substring(0, commaIdx).trim();
                }
                String resolved = resolveToLoadableClassName(genericPart);
                if (StringKit.isBlank(resolved)) return null;
                fieldClass = Class.forName(resolved);
            } else {
                String resolved = resolveToLoadableClassName(responseClass);
                if (StringKit.isBlank(resolved)) return null;
                fieldClass = Class.forName(resolved);
            }
        }

        return fieldClass;
    }

    private String resolveToLoadableClassName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String name = typeName.trim();
        // Strip surrounding generic content if any remains
        if (name.contains("<")) {
            name = name.substring(0, name.indexOf('<')).trim();
        }
        // Handle wildcard ? , ? extends, ? super
        if (name.startsWith("?")) {
            name = name.replaceFirst("^\\?\\s*(extends|super)?\\s*", "").trim();
            if (name.isEmpty()) {
                return null;
            }
        }
        return name;
    }

    private ApiUploadReq.Item.Params createResponseParam(Field field, String fieldName, Class<?> fieldClass, ApiUploadReq.Item item) {
        ApiUploadReq.Item.Params param = new ApiUploadReq.Item.Params();

        // data 字段使用展示的 responseClass；其他字段展示泛型：List<String> / List<Object>
        String fieldClassName;
        if (DATA_FIELD_NAME.equals(fieldName) && StringKit.isNotBlank(item.getResponseClass())) {
            fieldClassName = item.getResponseClass();
        } else {
            fieldClassName = buildFieldClassDisplay(field, fieldClass);
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

    private String buildFieldClassDisplay(Field field, Class<?> resolvedClass) {
        Class<?> raw = field.getType();
        if (List.class.isAssignableFrom(raw) || Set.class.isAssignableFrom(raw)) {
            String container = List.class.isAssignableFrom(raw) ? "List" : "Set";
            String genericDisplay = resolveGenericArgumentDisplay(field.getGenericType());
            return container + "<" + genericDisplay + ">";
        }
        return resolvedClass.getSimpleName();
    }

    private String resolveGenericArgumentDisplay(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                Type arg = args[0];
                if (arg instanceof Class<?> cls) {
                    return cls.getSimpleName();
                }
                if (arg instanceof ParameterizedType p) {
                    Type raw = p.getRawType();
                    if (raw instanceof Class<?> rc) {
                        return rc.getSimpleName();
                    }
                }
                if (arg instanceof WildcardType wt) {
                    Type[] uppers = wt.getUpperBounds();
                    if (uppers != null && uppers.length > 0 && uppers[0] instanceof Class<?> up && up != Object.class) {
                        return up.getSimpleName();
                    }
                    Type[] lowers = wt.getLowerBounds();
                    if (lowers != null && lowers.length > 0 && lowers[0] instanceof Class<?> low && low != Object.class) {
                        return low.getSimpleName();
                    }
                    return "Object";
                }
                if (arg instanceof TypeVariable<?> tv) {
                    Type[] bounds = tv.getBounds();
                    if (bounds != null && bounds.length > 0 && bounds[0] instanceof Class<?> b && b != Object.class) {
                        return b.getSimpleName();
                    }
                    return "Object";
                }
            }
            return "Object";
        }
        // 非参数化集合
        return "Object";
    }

    private String buildTypeArgumentDisplay(Type arg) {
        if (arg instanceof ParameterizedType p) {
            Type raw = p.getRawType();
            String container = (raw instanceof Class<?> rc && List.class.isAssignableFrom(rc)) ? "List" :
                               (raw instanceof Class<?> rc2 && Set.class.isAssignableFrom(rc2)) ? "Set" :
                               (raw instanceof Class<?> rc3 ? rc3.getSimpleName() : "Object");
            Type[] args = p.getActualTypeArguments();
            String inner = (args.length > 0) ? buildTypeArgumentDisplay(args[0]) : "Object";
            return container + "<" + inner + ">";
        }
        if (arg instanceof Class<?> c) {
            return c.getSimpleName();
        }
        if (arg instanceof WildcardType) {
            return "Object";
        }
        if (arg instanceof TypeVariable<?> tv) {
            Type[] bounds = tv.getBounds();
            if (bounds != null && bounds.length > 0 && bounds[0] instanceof Class<?> b && b != Object.class) {
                return ((Class<?>) bounds[0]).getSimpleName();
            }
            return "Object";
        }
        return "Object";
    }

    private Class<?> resolveComponentClass(Type arg) {
        if (arg instanceof ParameterizedType p) {
            Type[] args = p.getActualTypeArguments();
            if (args.length > 0) {
                return resolveComponentClass(args[0]);
            }
            return Object.class;
        }
        if (arg instanceof Class<?> c) {
            return c;
        }
        if (arg instanceof WildcardType) {
            return Object.class;
        }
        if (arg instanceof TypeVariable<?> tv) {
            Type[] bounds = tv.getBounds();
            if (bounds != null && bounds.length > 0 && bounds[0] instanceof Class<?> b) {
                return (Class<?>) bounds[0];
            }
            return Object.class;
        }
        return Object.class;
    }

    private boolean isCustomEntityClass(Class<?> fieldClass) {
        String packageName = fieldClass.getPackageName();
        return packageName.startsWith("com.ttc.api") || packageName.startsWith("com.mycz.arch");
    }

}
