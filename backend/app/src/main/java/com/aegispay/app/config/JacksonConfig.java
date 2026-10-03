package com.aegispay.app.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer moneyAsDecimal() {
        return builder -> builder
                .featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .serializerByType(BigDecimal.class, new com.fasterxml.jackson.databind.ser.std.ToStringSerializer());
    }
}
