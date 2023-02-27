package com.mycz.krpc.stater.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class KrpcReferenceScanner extends ClassPathBeanDefinitionScanner {

    public KrpcReferenceScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annoType) {
        super(registry, false);
        super.addIncludeFilter(new AnnotationTypeFilter(annoType));
    }



    @Override
    protected Set<BeanDefinitionHolder> doScan(@Nonnull String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);
        for (BeanDefinitionHolder holder : beanDefinitionHolders) {
            BeanDefinition beanDefinition = holder.getBeanDefinition();
            // 代理
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(Objects.requireNonNull(beanDefinition.getBeanClassName()));
            beanDefinition.setBeanClassName(KrpcReferenceFactoryBean.class.getName());
        }

        return beanDefinitionHolders;
    }

    /**
     * 对扫描对象的条件限制
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }


}
