/*
보안 규칙을 적은 매뉴얼
 */
package com.studycafe.config;

import com.studycafe.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration // 설정(Configuration)파일
@EnableWebSecurity // 스프링 시큐리티 기능 활성화(내가 정한 규칙 적용)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    /* 비밀번호 암호화 장치
       PasswordEncoder : 스프링 시큐리티가 제공하는 비밀번호 암호화 인터페이스
       BCryptPasswordEncoder : 강력한 해싱 알고리즘인 BCrypt를 사용한 암호화 인트페이스

       현재 이 코드가 없다면 DB에 비밀번호가 그대로 저장되어 위험성이 존재함
       이 코드가 존재한다면 알 수 없는 언어로 비밀번호가 암호화됨

       SecurityConfig는 가장 먼저 설정되는 곳이므로 암호화 도구를 미리 저장함
       @Bean으로 설정하여 어디서든 생성자 주입 또는 @Autowired로 사용 가능
     */

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 로그인 비활성화


                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth ->
                        auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    /* 보안 필터 체인(규칙 묶음)을 Bean으로 등록하여 적용(http 객체에 설정 적용)
       1. 기억 삭제(원래 규칙 삭제)
        CSRF 보호, 폼 로그인, 기본 로그인을 모두 비활성화(웹사이트가 아닌 API 서버를 생성하기 위함)

       2. 세션 설정(세션을 사용하지 않겠다고 선언, JWT 사용)
       sessionManagement는 스프링 시큐리티가 사용자의 로그인 상태를 어떻게 저장할지 설정하는 곳
       이 곳에서 session의 sessionCreationiPolicy(세션생성정책)을 STATELESS로 설정
       >> 원래는 STATEFUL이지만 무상태인 STATELESS로 설정하여 서버에 아무것도 저장하지 말라고 명령
       >> JWT가 있으므로 서버가 메모리를 써가면서 사용자를 기억할 필요 없음(서버 부하 감소)

       3. 경로별로 접근 권한 설정
       authorizeHttpRequests는 URL 주소별로 누가 들어갈 수 있는지 규칙을 정하는 것
       auth의 requestMatchers("/api/auth/**")은 /api/auth/로 시작하는 URL의 모든 요청은
       검사하지 말고 모두 통과(permitAll)
       그러나 이 이외의 URL의 요청(anyRequest())은 인증 도장이 있는 사람만 통과(authenticated)
       >> 허락없이 /api/auth/에 접근하면 에러를 내보냄(403 forbidden)

       4. 내가 만든 필터 끼워 넣기
       이미 UsernamePasswordAuthenticationFilter라는 기본 검사관이 스프링 시큐리티 안에 내장되어 있음
       .addFilterBefore(A,B)는 B가 일하기 전에 A를 먼저 투입하라는 의미
       그러므로 기존 검사관이 일하기 전에 내가 만든 jwtAuthenticationFilter가 먼저 실행됨
       >> 이미 인증 도장이 찍힌 상태로 기존 검사관이 오므로 검사관은 바로 통과함

       5. 설정 마무리(확정)
       지금까지 http라는 객체에 설정을 입력했고 .build()를 통해 이 모든 설정을 묶어서 최종적으로
       작동 가능한 SecurityFilterChain객체로 완성해서 스프링에게 넘겨주고 스프링은 이대로 움직임
     */


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CorsConfiguration의 configuration 객체 생성
        CorsConfiguration configuration = new CorsConfiguration();

        // 모든 요청을 허용
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 허용할 메서드 (GET, POST 등)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명 허용
        configuration.setAllowCredentials(true);

        // 브라우저가 응답 헤더를 읽을 수 있게 허용
        configuration.setExposedHeaders(List.of("Authorization"));

        // 적용 범위 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용

        return source;
    }
/*
CORS : 브라우저가 다른 출처의 리소스 접근을 막는 브라우저 보안 정책
SOP : 동일 출처 정책(보안관)
출처는 프로토콜(http,https), 호스트(localhost,naver.com), 포트번호가
모두 같아야 같은 출처라고 인정함, 현재는 프론트(5173)와 백엔드(8080)이 달라서 브라우저가 차단

그러나 출처가 달라도 데이터를 가져와야 하는 일이 발생하여 CORS 예외 허용이 발생
백엔드 서버가 5173포트에서 오는 것은 모두 받아들이라고 명찰(header)를 달아줌
그러면 브라우저가 명찰을 보고 통과 >> 이 코드가 명찰을 다는 작업

자격 증명 허용
setAllowCredentials()가 true여야 프론트엔드가 헤더에 Authorization 토큰을 실어서 보내도
브라우저가 이를 서버로 향하는 것을 통과시켜 줌

헤더 노출 허용
setExposedHeaders(List.of("Authorization")는 헤더에 'Authorization: Bearer 토큰'을
담아줘도 이 설정이 없으면 브라우저(Javascript)는 보안상 그 헤더를 읽을 수 없으므로 이 헤더는 가능하다고 선언

적용 범위 등록
UrlBasedCorsConfiguration의 source 객체를 생성하고 source의 registerCorsConfiguration을
통해서 서버의 모든 주소에 대해 이 규칙을 적용하라고 선언

규칙 리턴
 */
}