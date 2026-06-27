package com.rental.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1 MB
                .build();
    }
}
