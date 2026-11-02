package com.julia.portabilidade_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CentralConfig {

    @Bean
    public RestClient centralRestClient(
            @Value("${central.base-url}") String baseUrl,
            @Value("${central.connect-timeout:2s}") Duration connectTimeout,
            @Value("${central.read-timeout:5s}") Duration readTimeout
    ) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);


        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

}
