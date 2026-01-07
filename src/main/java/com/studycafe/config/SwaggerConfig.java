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

@Configuration // 설정 파일
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
    1. JWT 인증 버튼(열쇠) 생성
    내 API는 JWT가 있어야 들어갈 수 있으므로 Swagger 화면에 Authorize라는 버튼 생성
    >> 내 사이트는 JWT라는 방식의 열쇠를 쓴다고 정의하고 열쇠를 생성

   "bearer-key"라는 이름의 새로운 보안 설정을 추가(.addSecuritySchemes)
   이때 보안 설정에는 HTTP 인증방식, 그 방식에서도 Bearer Token 방식
   그리고 토큰의 형식은 JWT임을 보안설정에 추가함

    2. 모든 API에 잠금을 설정
    .addList("bearer-key")로 위에서 만든 보안 설정을 가져오고
    이 설정을 .addSecurityItem()을 통해서 문서 전체에 적용

    3. 표지 생성
    info를 새로 생성
    문서의 큰 제목을 .title으로 "StudyCafe API"
    상세 설명을 .description으로 "스터디카페 예약 시스템 API 명세서"
    버전을 .version으로 "1.0.0"
     */
}