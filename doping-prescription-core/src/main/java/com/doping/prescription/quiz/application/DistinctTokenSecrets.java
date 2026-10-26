package com.doping.prescription.quiz.application;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Stops the startup when the quiz token key equals the session key (docs/api.md, quizToken): with one key, a leak of
 * either would expose both, and a signature of one token kind would also be valid for the other. Reads the auth
 * module's key by its property name, so the quiz module needs no access to auth's configuration class.
 */
@Component
class DistinctTokenSecrets implements InitializingBean {

    static final String SESSION_SECRET_PROPERTY = "prescription.auth.jwt-secret";

    private final QuizProperties quiz;
    private final Environment environment;

    DistinctTokenSecrets(QuizProperties quiz, Environment environment) {
        this.quiz = quiz;
        this.environment = environment;
    }

    /**
     * @throws IllegalStateException if both keys are equal
     */
    @Override
    public void afterPropertiesSet() {
        if (quiz.tokenSecret().equals(environment.getProperty(SESSION_SECRET_PROPERTY))) {
            throw new IllegalStateException(
                    "prescription.quiz.token-secret must differ from " + SESSION_SECRET_PROPERTY);
        }
    }
}
