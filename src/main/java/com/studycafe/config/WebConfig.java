/*
백엔드 서버와 프로트엔드의 접속을 허락해주는 CORS 설정
 */
package com.studycafe.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 설정파일로 스프링이 켜질때 실행
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
/*
.addMapping("/**")
우리 서버의 모든 URL에 대해 이 규칙 적용(/**은 모든 경로를 포함)

.allowedOrigins("http://localhost:5173", "http://localhost:3000")
이 URL에서 오는 요청만 받기
5173은 vue.js에서 3000은 react에서 주로 사용하는 기본 포트

.allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
이 방식의 요청만 허용함(조회,저장,수정,삭제)
OPTIONS는 브라우저가 실제 요청을 보내기 전에 보내도 되는지 찔러보는 요청

.allowedHeaders("*")
어떤 헤더 정보를 담아 보내든 모두 허용함
JWT 토큰 같은 인증 정보를 헤더에 넣어서 보내는 경우같은 것을 위해 모두 허용(*)

.allowCredentials(true)
쿠키(신분증)나 인증정보를 포함한 요청 허용
로그인 유지 기능에서 쿠키나 세션을 주고받을 때를 위해 이 설정을 true로 설정

 */
/*
CORS : 브라우저 보안 정책
SOP : 동일 출처 정책
출처는 프로토콜(http,https), 호스트(localhost,naver.com), 포트번호가
모두 같아야 같은 출처라고 인정함

현재 프론트엔드와 백엔드의 포트번호가 다르므로 >> 다른 출처이므로 통신 차단

그러나 출처가 달라도 데이터를 가져와야 하는 일이 발생하여 CORS 예외 허용이 발생
백엔드 서버가 5173포트에서 오는 것은 모두 받아들이라고 명찰(header)를 달아줌
그러면 브라우저가 명찰을 보고 통과 >> 이 코드가 명찰을 다는 작업

OPTIONS로 브라우저가 서버에게 데이터 전송 가능 유무를 전송
서버는 WebConfig를 확읺고 명단에 있으므로 허락
허락받은 브라우저는 이때 진짜 데이터(JSON)을 전송
 */

















