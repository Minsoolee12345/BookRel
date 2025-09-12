package com.bookRel.br.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  @Bean
  public WebClient nlpClient() {
    return WebClient.builder()
        .baseUrl("http://localhost:8001") // FastAPI 주소
        .build();
  }
}
