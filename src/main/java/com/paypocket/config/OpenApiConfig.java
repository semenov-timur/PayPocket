package com.paypocket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payPocketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayPocket API")
                        .description("REST API сервиса электронного кошелька")
                        .version("1.0"));
    }
}