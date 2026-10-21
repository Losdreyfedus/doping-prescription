package com.doping.prescription.content.application;

import com.doping.prescription.content.domain.ContentTagger;
import com.doping.prescription.content.domain.KeywordTagger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the {@link ContentTagger} implementation; replacing it is a configuration change only. */
@Configuration(proxyBeanMethods = false)
class ContentTaggingConfiguration {

    @Bean
    ContentTagger contentTagger(ContentProperties properties) {
        return new KeywordTagger(properties.tagging().minConfidence());
    }
}
