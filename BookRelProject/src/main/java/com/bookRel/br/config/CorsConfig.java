package com.bookRel.br.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
	  registry.addMapping("/**")
	          .allowedOriginPatterns("*")                  // 어떤 Origin이든 허용 (개발용)
	          .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
	          .allowedHeaders("*")
	          .exposedHeaders("Location","Content-Disposition")
	          .allowCredentials(true)
	          .maxAge(3600);
	}
}
