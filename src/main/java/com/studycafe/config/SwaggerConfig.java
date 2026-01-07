/*
내가 만든 API 사용 설명서를 자동으로 생성하고
웹에서 바로 테스트할 수 있게 해주는 설정 파일
http://localhost:8080/swagger-ui/index.html에 들어갔을 떄 보이는 화면 설정
 */
package com.studycafe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))

                .addSecurityItem(new SecurityRequirement().addList("bearer-key"))

                .info(new Info()
                        .title("StudyCafe API")
                        .description("스터디카페 예약 시스템 API 명세서")
                        .version("1.0.0"));
    }
    /*
    1. JWT 인증 버튼 생성
    내 API는 JWT가 있어야 들어갈 수 있으므로 Swagger 화면에 Authorize라는 버튼 생성
    Swagger 화면 우측 상단에 Authorize 버튼이 생겨서

     */
}