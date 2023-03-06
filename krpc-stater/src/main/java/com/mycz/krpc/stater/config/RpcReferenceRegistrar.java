package com.mycz.krpc.stater.config;

import com.mycz.krpc.core.annotation.KrpcReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
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

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        ImportBeanDefinitionRegistrar.super.registerBeanDefinitions(importingClassMetadata, registry, new BeanNameGenerator() {
            @Override
            public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
                return definition.getBeanClassName();
            }
        });
    }

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

}
