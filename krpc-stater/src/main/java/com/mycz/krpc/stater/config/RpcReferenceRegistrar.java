package com.mycz.krpc.stater.config;

import com.mycz.krpc.core.annotation.KrpcReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

import javax.annotation.Nonnull;

/**
 * 自定义扫描@KrpcReference注解, 将代理的接口放置到spring容器
 */
@Slf4j
public class RpcReferenceRegistrar implements ImportBeanDefinitionRegistrar {

    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
        AnnotationAttributes rpcAttr = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableKrpcService.class.getName()));
        if (rpcAttr != null) {
            String[] basePackages = rpcAttr.getStringArray("basePackage");
            if (basePackages.length == 0) {
                basePackages = new String[]{((StandardAnnotationMetadata) importingClassMetadata).getIntrospectedClass().getPackage().getName()};
            }
            KrpcReferenceScanner krpcReferenceScanner = new KrpcReferenceScanner(registry, KrpcReference.class);
            krpcReferenceScanner.scan(basePackages);
        }

    }


    //            for (String packageName : packageNameList) {
//                String path = ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(packageName));
//                System.out.println(path);
//                String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX.concat(path).concat("/**/*.class");
//
//                System.out.println(packageSearchPath);
//
//                Resource[] resources = resourceLoader.getResources(packageSearchPath);
//                System.out.println("resources 长度: " + resources.length);
//                int i = 0;
//                for (Resource resource : resources) {
//                    if (resource.isFile()) {
//                        File file = resource.getFile();
//                        String fileName = file.getName().replace(".class", "");
//                        System.out.println(file.getCanonicalPath());
//                        Class<?> aClass = Class.forName(packageName + "." + fileName);
//                        if (aClass.isInterface() && !aClass.isAnnotation() && aClass.isAnnotationPresent(KrpcReference.class)) {
//                            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
//                            GenericBeanDefinition beanDefinition = (GenericBeanDefinition) builder.getBeanDefinition();
//                            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(packageName + "." + fileName);
//                            beanDefinition.setBeanClass(MyFactoryBean.class);
//                            registry.registerBeanDefinition(fileName, beanDefinition);
//                        }
//                    }
//                }
//            }

}
