package com.mycz.krpcsamplecustomer.test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Arrays;
import java.util.Set;

public class MyMapperScanner extends ClassPathBeanDefinitionScanner {

    public MyMapperScanner(BeanDefinitionRegistry registry) {
        super(registry, true);
        addIncludeFilter(new AnnotationTypeFilter(MyMapper.class));

    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        System.out.println("-----1111 , " + Arrays.toString(basePackages));
        Set<BeanDefinitionHolder> definitionHolders = super.doScan(basePackages);
        System.out.println("definitionHolders size = " + definitionHolders.size());
        for (BeanDefinitionHolder holder : definitionHolders) {
            System.out.println(holder.getBeanName());
        }
        System.out.println("-----2222");
        return definitionHolders;
    }

    /**
     * 对扫描对象的条件限制
     * @param beanDefinition
     * @return
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

}
