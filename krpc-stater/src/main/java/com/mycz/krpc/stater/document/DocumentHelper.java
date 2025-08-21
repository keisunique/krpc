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
 * 文档上报辅助类。
 *
 * 设计要点：
 * - 扫描 Spring 容器中的业务 Service（包前缀限定），根据类及方法上的 `@RequestMapping`/`@RequestMappings`
 *   组合出 API 元信息，打包为 `ApiUploadReq` 上报。
 * - 通过实现接口上的 `@KrpcReference` 获取服务名，避免直接依赖实现类命名。
 * - 对请求参数与响应类型做“结构化展开”：
 *   - 请求侧：当参数为集合/泛型容器，且泛型为自定义实体时，直接展开该自定义实体的字段；
 *     当参数本身或字段本身为自定义实体（非泛型）时，同样递归展开。
 *   - 响应侧：优先识别返回体中 data 字段对应的组件类，按自定义实体进行递归展开，并使用 `seen` 集合防止循环依赖导致无限递归。
 * - 泛型处理：以“能展示即可”为目标，尽量给出可读的 `SimpleName` 或 `Container<Inner>` 展示字符串。
 */
@Slf4j
public class DocumentHelper {

    private static final String DATA_FIELD_NAME = "data";

    private final ApplicationContext applicationContext;
    private final RpcProperties rpcProperties;

    /**
     * 构造函数。
     *
     * @param applicationContext Spring 上下文，用于发现并获取目标 Bean
     * @param rpcProperties      RPC 配置，其中包含文档上报相关配置
     */
    public DocumentHelper(ApplicationContext applicationContext, RpcProperties rpcProperties) {
        this.applicationContext = applicationContext;
        this.rpcProperties = rpcProperties;
    }

    /**
     * 触发文档上报。
     *
     * 流程：
     * 1. 校验文档上报开关与 URL 配置。
     * 2. 扫描容器中目标包前缀下的 Bean，提取其 API 元信息。
     * 3. 通过 HTTP 以 Snake Case 序列化结构上报至文档中心。
     *
     * 异常策略：
     * - 若配置不完整则直接返回，不抛异常。
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
     * 处理单个 Bean，解析其公开方法上的网关映射注解，构造上报条目。
     *
     * 核心逻辑：
     * - 通过实现接口扫描 `@KrpcReference` 得到 serviceName。
     * - 识别方法上的 `@RequestMapping` 或 `@RequestMappings`，生成多个 API 条目。
     * - 填充请求与响应结构（含递归解析）。
     *
     * @param bean Spring 容器中的目标 Bean 实例
     * @return 该 Bean 对应的上报条目集合；若无法确定服务名则返回 null
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
                        List<String> paths = new ArrayList<>(Arrays.asList(mapping.paths()));
                        if (mapping.path() != null) {
                            paths.add(mapping.path());
                        }

                        for (String path : paths) {
                            ApiUploadReq.Item api = new ApiUploadReq.Item();
                            api.setServiceName(serviceName);
                            api.setClassName(clazz.getName());
                            api.setTag(tag);
                            try {
                                api.setName(method.getName());
                                api.setMethod(((RequestMethod) getAnnotationValue(mapping, "method")).name());
                                api.setPath(path);
                                api.setPrefix((String) getAnnotationValue(mapping, "prefix"));
                                api.setAuthority(((Boolean) getAnnotationValue(mapping, "authority")) ? Bool.YES : Bool.NO);
                                api.setAuthorityType(((AuthorityType) getAnnotationValue(mapping, "authorityType")).name());
                                api.setDescription((String) getAnnotationValue(mapping, "description"));
                                api.setResponseType(((ResponseType) getAnnotationValue(mapping, "responseType")).name());
                                api.setDeliverPayload(((Boolean) getAnnotationValue(mapping, "deliverPayload")) ? Bool.YES : Bool.NO);
                                api.setDeliverParams(((Boolean) getAnnotationValue(mapping, "deliverParams")) ? Bool.YES : Bool.NO);

                                // 解析请求/响应结构并填充
                                fillParamsAndResponse(method, api);
                                list.add(api);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to process RequestMapping annotation for method: " + method.getName(), e);
                            }
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
     * 读取注解方法值的通用反射工具。
     *
     * 注意：
     * - 仅按方法名进行反射调用，不做额外的类型/可访问性检查。
     * - 找不到方法或调用异常时，包装为运行时异常抛出。
     *
     * @param annotation 注解实例
     * @param methodName 注解方法名
     * @return 注解方法返回值
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
     * 填充请求与响应信息。
     *
     * 请求侧：
     * - 若首个参数为泛型容器（List/Set/…），且泛型为自定义类型，直接展开该自定义类型字段。
     * - 若参数本身就是自定义类型（非泛型），同样递归展开其字段。
     * - 否则，按原始参数类型的字段进行解析；其中若字段为泛型容器且泛型为自定义类型，则在该字段名下继续展开。
     *
     * 响应侧：
     * - 尝试从返回泛型（如 Result<T>）中解析组件类 T，并在处理名为 data 的字段时优先使用。
     * - 对自定义实体类型字段递归展开，并使用 `seen` 集合避免循环嵌套。
     *
     * @param method 目标方法
     * @param api    将被填充的上报条目
     */
    private void fillParamsAndResponse(Method method, ApiUploadReq.Item api) {
        // 请求类名（展示用途）
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

        // 响应类展示与组件类解析（用于 data 字段展开）
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

        // 请求参数字段解析
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
                    // 若字段本身为自定义类型（非泛型），递归展开其字段
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
     * 遍历并递归收集响应字段。
     *
     * 规则：
     * - 优先用 `determineFieldClass` 判定字段的实际类型（处理 data 字段与泛型场景）。
     * - 对自定义实体类型进行递归展开。
     * - 使用 `seen` 防止循环依赖（如 A 含有 B，B 又含有 A）。
     *
     * @param params         累积写入的参数列表
     * @param fields         当前类的字段集合
     * @param prefix         递归前缀，用于生成类似 `parent.child` 的层级字段名
     * @param api            当前 API 上报条目（提供响应展示串等）
     * @param seen           循环检测集合
     * @param componentClass 若返回类型为容器，此处为其组件类（用于 data 字段）
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
     * 遍历并递归收集请求字段（用于泛型或自定义实体类型）。
     *
     * 注意：
     * - 与响应侧类似，也通过 `seen` 规避循环嵌套。
     * - 仅提取字段上的展示信息与校验注解（必填等）。
     *
     * @param params 累积写入的参数列表
     * @param fields 当前类的字段集合
     * @param prefix 字段名前缀
     * @param seen   循环检测集合
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
     * 从实现的接口中提取服务名：扫描接口上的 `@KrpcReference` 注解。
     *
     * 约定：
     * - 一个实现类可能实现多个接口；此处遇到第一个带 `@KrpcReference` 的接口即返回。
     *
     * @param beanClass 目标实现类
     * @return 服务名；若未找到则返回空串
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

    /**
     * 生成带前缀的字段名，并统一转为下划线风格。
     *
     * @param name   字段原始名
     * @param prefix 可选的前缀（用于嵌套展开）
     * @return 形如 `a.b` 的层级名再做 camel->underline 转换后的结果
     */
    private String buildFieldName(String name, String prefix) {
        if (StringKit.isNotBlank(prefix)) {
            name = prefix + "." + name;
        }
        return StringKit.camelToUnderline(name);
    }

    /**
     * 判定响应字段应展示/展开的实际类型。
     *
     * 处理策略：
     * - 泛型容器字段：尝试取第一个泛型参数的可加载类名，再 `Class.forName`。
     * - data 字段：优先使用外部已解析的 `componentClass`；否则从 `api.getResponseClass()` 的泛型串做回推。
     *
     * @param field          字段反射对象
     * @param fieldName      已带前缀的字段名（用于识别 data）
     * @param api            当前 API 条目
     * @param componentClass 返回体的组件类（若已解析）
     * @return 用于展开/递归的 Class；若无法解析则返回 null
     */
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

    /**
     * 判定请求字段的实际类型（更偏展示用途）。
     *
     * 优先处理泛型容器的第一个泛型参数；若解析失败，则退回原始字段类型。
     *
     * @param field 字段反射对象
     * @return 字段类型或解析得到的泛型参数类型
     */
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

    /**
     * 将类型名规整为可 Class.forName 的类名。
     *
     * 处理：
     * - 去掉外层泛型（截断 `<` 后缀）。
     * - 处理通配 `?` 及 `extends/super` 边界。
     *
     * @param typeName 原始类型名（可能包含泛型/通配）
     * @return 可加载的类名；若无法确定，返回 null
     */
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

    /**
     * 创建响应侧的参数条目，并确定展示字符串。
     *
     * 对 `data` 字段采用返回类展示串，以保留 `Container<Inner>` 的可读性；其他字段使用精简展示。
     *
     * @param field 字段反射对象
     * @param name  字段名（可能包含前缀）
     * @param cls   决定展开/递归的类型
     * @param api   当前 API 条目
     * @return 参数条目
     */
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

    /**
     * 构建字段类型展示串。
     *
     * 规则：
     * - List/Set 展示为 `Container<Inner>` 的形式；内层尽量给出简单类名。
     * - 其他直接使用解析后的 `resolvedClass.getSimpleName()`。
     *
     * @param field          字段反射对象
     * @param resolvedClass  已判定用于展示/展开的类型
     * @return 展示字符串
     */
    private String buildFieldClassDisplay(Field field, Class<?> resolvedClass) {
        Class<?> raw = field.getType();
        if (List.class.isAssignableFrom(raw) || Set.class.isAssignableFrom(raw)) {
            String container = List.class.isAssignableFrom(raw) ? "List" : "Set";
            String genericDisplay = resolveGenericArgumentDisplay(field.getGenericType());
            return container + "<" + genericDisplay + ">";
        }
        return resolvedClass.getSimpleName();
    }

    /**
     * 提取并展示容器类型的泛型实参。
     *
     * 策略：
     * - 若为 Class，使用简单名；若为 ParameterizedType，取其原始类型简单名；
     * - 对通配符与类型变量，若存在上界/下界且不为 Object，则展示该边界的简单名；否则回退为 "Object"。
     *
     * @param genericType 字段的通用类型
     * @return 泛型参数的展示字符串
     */
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

    /**
     * 将 `Type` 构造成友好的展示串。
     *
     * - 支持容器类型的递归展示，如 `Result<List<Foo>>` 将递归地仅展示第一层参数。
     * - 对通配符/类型变量回退为 "Object" 或其上界简单名。
     *
     * @param arg 目标类型
     * @return 展示字符串
     */
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

    /**
     * 从 `Type` 推断“组件类”（常用于容器的第一个类型参数）。
     *
     * @param arg 目标类型
     * @return 组件类；若无法确定则返回 Object.class
     */
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

    /**
     * 判定是否为“自定义实体类”。
     *
     * 约定：
     * - 通过包名前缀来做启发式判断（`com.ttc.api` 或 `com.mycz.arch`）。
     * - 该规则可根据实际业务包结构扩展/收敛。
     *
     * @param cls 目标类型
     * @return 是否认为是自定义实体
     */
    private boolean isCustomEntityClass(Class<?> cls) {
        String pkg = cls.getPackageName();
        return pkg.startsWith("com.ttc.api") || pkg.startsWith("com.mycz.arch");
    }
}
