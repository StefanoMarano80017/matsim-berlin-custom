package org.springboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
*  Consultabili al link http://localhost:8080/swagger-ui.html 
*/

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(
                new Info().title("MATSim Server API REST + WS")
                        .version("1.0")
                        .description("API per avviare e monitorare scenari MATSim"));
    }
}
