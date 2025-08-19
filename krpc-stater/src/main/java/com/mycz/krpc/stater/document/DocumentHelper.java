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

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        ApiUploadReq uploadReq = new ApiUploadReq();
        List<ApiUploadReq.Item> apis = new ArrayList<>();
        uploadReq.setApiInfos(apis);

        for (String name : beanNames) {
            Object bean = applicationContext.getBean(name);
            if (!bean.getClass().getPackageName().startsWith("com.ttc.service.service")) {
                continue;
            }
            List<ApiUploadReq.Item> list = processBean(bean);
            if (list != null && !list.isEmpty()) {
                apis.addAll(list);
            }
        }

        String result = HttpKit.post(rpcProperties.getDocument().getUrl(), JsonKit.toJson(uploadReq, PropertyNamingStrategies.SNAKE_CASE));
        log.info("*** api接口上报 - 完成, 结果: {}", result);
    }

    /**
     * 处理单个 Bean
     */
    private List<ApiUploadReq.Item> processBean(Object bean) {
        try {
            Class<?> clazz = bean.getClass();

            String serviceName = getServiceNameFromInterfaces(clazz);
            if (StringKit.isBlank(serviceName)) {
                return null;
            }

            ApiTag tagAnno = clazz.getAnnotation(ApiTag.class);
            String tag = tagAnno == null ? clazz.getSimpleName() : tagAnno.value();

            List<ApiUploadReq.Item> list = new ArrayList<>();
            for (Method method : clazz.getMethods()) {
                Annotation[] annos = method.getAnnotations();
                for (Annotation anno : annos) {
                    RequestMapping[] mappings = new RequestMapping[1];
                    if (anno instanceof RequestMapping) {
                        mappings[0] = (RequestMapping) anno;
                    } else if (anno instanceof RequestMappings) {
                        mappings = ((RequestMappings) anno).value();
                    } else {
                        continue;
                    }

                    for (RequestMapping mapping : mappings) {
                        ApiUploadReq.Item api = new ApiUploadReq.Item();
                        api.setServiceName(serviceName);
                        api.setClassName(clazz.getName());
                        api.setTag(tag);
                        try {
                            api.setName(method.getName());
                            api.setMethod(((RequestMethod) getAnnotationValue(mapping, "method")).name());
                            api.setPath((String) getAnnotationValue(mapping, "path"));
                            api.setPrefix((String) getAnnotationValue(mapping, "prefix"));
                            api.setAuthority(((Boolean) getAnnotationValue(mapping, "authority")) ? Bool.YES : Bool.NO);
                            api.setAuthorityType(((AuthorityType) getAnnotationValue(mapping, "authorityType")).name());
                            api.setDescription((String) getAnnotationValue(mapping, "description"));
                            api.setResponseType(((ResponseType) getAnnotationValue(mapping, "responseType")).name());
                            api.setDeliverPayload(((Boolean) getAnnotationValue(mapping, "deliverPayload")) ? Bool.YES : Bool.NO);
                            api.setDeliverParams(((Boolean) getAnnotationValue(mapping, "deliverParams")) ? Bool.YES : Bool.NO);

                            fillParamsAndResponse(method, api);
                            list.add(api);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to process RequestMapping annotation for method: " + method.getName(), e);
                        }
                    }
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process bean: " + bean.getClass().getName(), e);
        }
    }

    /**
     * 读取注解方法值
     */
    private Object getAnnotationValue(Annotation annotation, String methodName) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            return m.invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get annotation value for method: " + methodName, e);
        }
    }

    /**
     * 填充请求/响应信息
     */
    private void fillParamsAndResponse(Method method, ApiUploadReq.Item api) {
        // 请求类名
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length < 1) {
            return;
        }
        Type[] genericParamTypes = method.getGenericParameterTypes();
        if (genericParamTypes.length > 0) {
            Type gp = genericParamTypes[0];
            if (gp instanceof ParameterizedType) {
                api.setRequestClass(buildTypeDisplay(gp));
            } else if (gp instanceof Class<?>) {
                api.setRequestClass(((Class<?>) gp).getName());
            } else {
                api.setRequestClass(paramTypes[0].getName());
            }
        } else {
            api.setRequestClass(paramTypes[0].getName());
        }

        // 响应类展示与组件类
        Type retGeneric = method.getGenericReturnType();
        Class<?> componentClass = null;
        if (retGeneric instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                Type arg = args[0];
                api.setResponseClass(buildTypeDisplay(arg));
                componentClass = resolveComponentClass(arg);
            }
        }

        List<ApiUploadReq.Item.Params> params = new ArrayList<>();

        // 请求参数字段
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> pType = paramTypes[i];
            Type gpt = (genericParamTypes.length > i) ? genericParamTypes[i] : null;

            // 若参数本身为泛型容器，且泛型为自定义类，则直接解析该自定义类的字段
            if (gpt instanceof ParameterizedType pt0 && pt0.getActualTypeArguments().length > 0) {
                Class<?> comp = resolveComponentClass(pt0.getActualTypeArguments()[0]);
                if (comp != null && isCustomEntityClass(comp)) {
                    Set<String> seenReq = new HashSet<>();
                    collectRequestFields(params, comp.getDeclaredFields(), null, seenReq);
                    continue;
                }
            }

            // 新增：若参数本身为自定义类型，递归展开其字段
            if (isCustomEntityClass(pType)) {
                Set<String> seenReq = new HashSet<>();
                collectRequestFields(params, pType.getDeclaredFields(), null, seenReq);
                continue;
            }

            // 否则按原始请求对象的字段解析
            Field[] fields = pType.getDeclaredFields();
            for (Field f : fields) {
                ApiUploadReq.Item.Params p = new ApiUploadReq.Item.Params();
                p.setField(StringKit.camelToUnderline(f.getName()));
                p.setType("request");
                p.setFieldClass(buildFieldClassDisplay(f, f.getType()));
                // 注解处理：描述与必填
                Annotation[] fAnnos = f.getAnnotations();
                for (Annotation a : fAnnos) {
                    if (a instanceof ApiModelProperty) {
                        p.setDescription((String) getAnnotationValue(a, "value"));
                    } else if (a instanceof NotNull || a instanceof NotBlank || a instanceof NotEmpty) {
                        p.setRequired(Bool.YES);
                    } else if (a instanceof Range || a instanceof Email || a instanceof Length || a instanceof Mobile || a instanceof SetOf || a instanceof Size) {
                        Boolean required = null;
                        try {
                            required = (Boolean) getAnnotationValue(a, "required");
                        } catch (Exception ignore) {
                            // 某些注解无 required 属性
                        }
                        if (required != null) {
                            p.setRequired(required ? Bool.YES : Bool.NO);
                        }
                    }
                }
                params.add(p);

                // 若字段为泛型容器，且泛型为自定义类，则递归解析其字段
                Type gt = f.getGenericType();
                if (gt instanceof ParameterizedType pt2 && pt2.getActualTypeArguments().length > 0) {
                    Type arg = pt2.getActualTypeArguments()[0];
                    Class<?> comp = resolveComponentClass(arg);
                    if (comp != null && isCustomEntityClass(comp)) {
                        Set<String> seenReq = new HashSet<>();
                        collectRequestFields(params, comp.getDeclaredFields(), StringKit.camelToUnderline(f.getName()), seenReq);
                    }
                } else {
                    // 新增：若字段本身为自定义类型（非泛型），递归展开其字段
                    Class<?> fTypeCls = f.getType();
                    if (isCustomEntityClass(fTypeCls)) {
                        Set<String> seenReq = new HashSet<>();
                        collectRequestFields(params, fTypeCls.getDeclaredFields(), StringKit.camelToUnderline(f.getName()), seenReq);
                    }
                }
            }
        }

        // 响应参数字段
        Set<String> seen = new HashSet<>();
        collectResponseFields(params, method.getReturnType().getDeclaredFields(), null, api, seen, componentClass);
        api.setParams(params);
    }

    /**
     * 遍历并递归收集响应字段
     */
    private void collectResponseFields(List<ApiUploadReq.Item.Params> params, Field[] fields, String prefix, ApiUploadReq.Item api, Set<String> seen, Class<?> componentClass) {
        for (Field f : fields) {
            try {
                String name = buildFieldName(f.getName(), prefix);
                Class<?> cls = determineFieldClass(f, name, api, componentClass);
                if (cls == null) {
                    continue;
                }

                ApiUploadReq.Item.Params p = createResponseParam(f, name, cls, api);
                params.add(p);

                if (isCustomEntityClass(cls)) {
                    String key = cls.getName();
                    if (seen.contains(key)) {
                        continue;
                    }
                    seen.add(key);
                    try {
                        collectResponseFields(params, cls.getDeclaredFields(), name, api, seen, componentClass);
                    } finally {
                        seen.remove(key);
                    }
                }
            } catch (Exception e) {
                System.err.println("Fail: " + e.getMessage());
            }
        }
    }

    /**
     * 遍历并递归收集请求字段（用于泛型自定义类型）
     */
    private void collectRequestFields(List<ApiUploadReq.Item.Params> params, Field[] fields, String prefix, Set<String> seen) {
        for (Field f : fields) {
            try {
                String name = buildFieldName(f.getName(), prefix);
                Class<?> cls = determineFieldClassForRequest(f);
                if (cls == null) {
                    continue;
                }

                ApiUploadReq.Item.Params p = new ApiUploadReq.Item.Params();
                p.setField(name);
                p.setType("request");
                p.setFieldClass(buildFieldClassDisplay(f, cls));

                Annotation[] anns = f.getAnnotations();
                for (Annotation a : anns) {
                    if (a instanceof ApiModelProperty) {
                        p.setDescription((String) getAnnotationValue(a, "value"));
                    } else if (a instanceof NotNull || a instanceof NotBlank || a instanceof NotEmpty) {
                        p.setRequired(Bool.YES);
                    } else if (a instanceof Range || a instanceof Email || a instanceof Length || a instanceof Mobile || a instanceof SetOf || a instanceof Size) {
                        Boolean required = null;
                        try {
                            required = (Boolean) getAnnotationValue(a, "required");
                        } catch (Exception ignore) {
                        }
                        if (required != null) {
                            p.setRequired(required ? Bool.YES : Bool.NO);
                        }
                    }
                }

                params.add(p);

                if (isCustomEntityClass(cls)) {
                    String key = cls.getName();
                    if (seen.contains(key)) {
                        continue;
                    }
                    seen.add(key);
                    try {
                        collectRequestFields(params, cls.getDeclaredFields(), name, seen);
                    } finally {
                        seen.remove(key);
                    }
                }
            } catch (Exception e) {
                System.err.println("Fail: " + e.getMessage());
            }
        }
    }

    /**
     * 从接口类上提取服务名（扫描其实现接口上的 `@KrpcReference`）
     */
    private String getServiceNameFromInterfaces(Class<?> beanClass) {
        Class<?>[] interfaces = beanClass.getInterfaces();
        for (Class<?> itf : interfaces) {
            Annotation[] anns = itf.getAnnotations();
            for (Annotation ann : anns) {
                if (ann instanceof KrpcReference) {
                    return (String) getAnnotationValue(ann, "serviceName");
                }
            }
        }
        return "";
    }

    private String buildFieldName(String name, String prefix) {
        if (StringKit.isNotBlank(prefix)) {
            name = prefix + "." + name;
        }
        return StringKit.camelToUnderline(name);
    }

    private Class<?> determineFieldClass(Field field, String fieldName, ApiUploadReq.Item api, Class<?> componentClass) throws ClassNotFoundException {
        Class<?> cls = field.getType();

        // 处理泛型参数
        Type gt = field.getGenericType();
        if (gt instanceof ParameterizedType pt) {
            if (pt.getActualTypeArguments().length > 0) {
                Type arg = pt.getActualTypeArguments()[0];
                String typeName = arg.getTypeName();
                String resolved = resolveToLoadableClassName(typeName);
                if (StringKit.isBlank(resolved)) {
                    return null;
                }
                cls = Class.forName(resolved);
            }
        }

        // 特殊处理 data 字段：优先使用已解析的组件类
        if (DATA_FIELD_NAME.equals(fieldName)) {
            if (componentClass != null) {
                return componentClass;
            }
            String resp = api.getResponseClass();
            if (StringKit.isBlank(resp)) {
                return null;
            }
            if (resp.contains("<") && resp.endsWith(">")) {
                String generic = resp.substring(resp.indexOf('<') + 1, resp.lastIndexOf('>')).trim();
                int comma = generic.indexOf(',');
                if (comma > -1) {
                    generic = generic.substring(0, comma).trim();
                }
                String resolved = resolveToLoadableClassName(generic);
                if (StringKit.isBlank(resolved)) {
                    return null;
                }
                cls = Class.forName(resolved);
            } else {
                String resolved = resolveToLoadableClassName(resp);
                if (StringKit.isBlank(resolved)) {
                    return null;
                }
                cls = Class.forName(resolved);
            }
        }

        return cls;
    }

    private Class<?> determineFieldClassForRequest(Field field) throws ClassNotFoundException {
        Class<?> cls = field.getType();
        Type gt = field.getGenericType();
        if (gt instanceof ParameterizedType pt) {
            if (pt.getActualTypeArguments().length > 0) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?>) {
                    return (Class<?>) arg;
                }
                String typeName = arg.getTypeName();
                String resolved = resolveToLoadableClassName(typeName);
                if (!StringKit.isBlank(resolved)) {
                    return Class.forName(resolved);
                }
            }
        }
        return cls;
    }

    private String resolveToLoadableClassName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String name = typeName.trim();
        if (name.contains("<")) {
            name = name.substring(0, name.indexOf('<')).trim();
        }
        if (name.startsWith("?")) {
            name = name.replaceFirst("^\\?\\s*(extends|super)?\\s*", "").trim();
            if (name.isEmpty()) {
                return null;
            }
        }
        return name;
    }

    private ApiUploadReq.Item.Params createResponseParam(Field field, String name, Class<?> cls, ApiUploadReq.Item api) {
        ApiUploadReq.Item.Params p = new ApiUploadReq.Item.Params();
        String display;
        if (DATA_FIELD_NAME.equals(name) && StringKit.isNotBlank(api.getResponseClass())) {
            display = api.getResponseClass();
        } else {
            display = buildFieldClassDisplay(field, cls);
        }
        p.setField(name);
        p.setType("response");
        p.setFieldClass(display);

        Annotation[] anns = field.getAnnotations();
        for (Annotation a : anns) {
            if (a instanceof ApiModelProperty) {
                p.setDescription((String) getAnnotationValue(a, "value"));
                break;
            }
        }
        return p;
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
        return "Object";
    }

    private String buildTypeDisplay(Type arg) {
        if (arg instanceof ParameterizedType p) {
            Type raw = p.getRawType();
            String container = (raw instanceof Class<?> rc && List.class.isAssignableFrom(rc)) ? "List" :
                    (raw instanceof Class<?> rc2 && Set.class.isAssignableFrom(rc2)) ? "Set" :
                            (raw instanceof Class<?> rc3 ? rc3.getSimpleName() : "Object");
            Type[] args = p.getActualTypeArguments();
            String inner = (args.length > 0) ? buildTypeDisplay(args[0]) : "Object";
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

    private boolean isCustomEntityClass(Class<?> cls) {
        String pkg = cls.getPackageName();
        return pkg.startsWith("com.ttc.api") || pkg.startsWith("com.mycz.arch");
    }
}
