package com.julia.portabilidade_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CentralConfig {

    @Bean
    public RestClient centralRestClient() {

        return RestClient.builder()
                .baseUrl("http://localhost:4000")
                .build();
    }

}
