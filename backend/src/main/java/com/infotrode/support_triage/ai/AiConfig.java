package com.infotrode.support_triage.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class AiConfig {

    @Bean
    public RestClient ollamaRestClient(OllamaProperties props) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Duration.ofSeconds(props.getTimeoutSeconds()).toMillis();
        rf.setConnectTimeout(timeoutMs);
        rf.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(rf)
                .build();
    }

    @Bean
    public OllamaClient ollamaClient(RestClient ollamaRestClient, ObjectMapper om) {
        return new OllamaClient(ollamaRestClient, om);
    }
}
