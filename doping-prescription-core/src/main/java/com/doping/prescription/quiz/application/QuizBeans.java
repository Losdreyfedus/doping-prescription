package com.doping.prescription.quiz.application;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The quiz module's injected collaborators that are not services of their own. */
@Configuration(proxyBeanMethods = false)
class QuizBeans {

    /** Shuffles the options of a micro-question; unpredictable, so the shown letter says nothing about the answer. */
    @Bean
    RandomGenerator optionShuffleRandom() {
        return new SecureRandom();
    }
}
