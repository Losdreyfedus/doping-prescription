package com.doping.prescription.learner.application;

import com.doping.prescription.learner.domain.MasteryCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free mastery model to its configuration. */
@Configuration(proxyBeanMethods = false)
class LearnerBeans {

    @Bean
    MasteryCalculator masteryCalculator(LearnerProperties properties) {
        return new MasteryCalculator(properties.mastery().parameters());
    }
}
