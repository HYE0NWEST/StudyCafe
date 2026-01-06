/*
JWT 토큰 필터 코드
 */
package com.studycafe.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    /*
    OncePerRequestFilter는 사용자의 요청 1번당 딱 1번만 실행하는 것을 보장하는 필터
    검사를 1번 시행 후 검사 완료 딱지를 붙여서 다른 메서드로 포워딩해도 검사 완료함을 증명하여 검사를 하지 않음
    JWT 토큰을 열어서 검증, DB 조호, SecurityContext에 넣는 작업이 너무 무겁기 때문
     */

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 토큰 꺼내기
        String token = resolveToken(request);

        // 2. 토큰이 있고, 유효하다면
        if (token != null && jwtTokenProvider.validateToken(token)) {

            // 3. 토큰에서 유저 ID(숫자) 꺼내기
            String userId = jwtTokenProvider.getUserId(token);

            // 4. "이 사람은 인증된 사람입니다"라고 도장을 찍어서 서버 메모리(Context)에 저장
            // (권한은 없으므로 빈 리스트 new ArrayList<>() 전달)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 다음 단계로 통과
        filterChain.doFilter(request, response);
    }
    /* 핵심 로직
    1. resolveToken 메서드를 사용자의 요청(request)를 매개변수로 하여 토큰 문자열을 token에 저장
    2. token이 null이 아니고 JwtTokenProvider의 validateToken메서드로 정상적인지 판단하고 도장을 찍음
    이 if문 안으로 들어온 코인은 정상코인임
    3. JwtTokenProvider의 getUserId메서드로 토큰 주인의 ID를 꺼내서 userId에 저장
    4. 출입증 발급 및 등록
    스프링 시큐리티가 인정하는 Authentication 객체인 UsernamePasswordAuthenticationToken 객체 생성
    (Principal, Credentials, Authorities) 총 3개의 인자가 들어감
    1번째 인자는 보통 UserDetails 객체를 넣지만 여기서는 userId가 들어감
    2번째 인자는 비밀번호지만 이미 토큰으로 인증을 완료해서 보안상 null 처리
    3번째 인자는 권한으로 빈 ArrayList를 넣어서 일반 인증 유저로 처리

    객체의 .setDetails() 메서드로 이 토큰의 IP 주소등의 부가 정보를 적음(로그 남길 시 유용)
    SecurityContext는 서버의 임시 보안 금고(저장소)로 여기에 객체를 넣어서 스프링 부트가
    이 요청이 끝날 때까지 이 사람은 로그인한 상태라고 인식

    5. 통과
    다음 필터로 넘어가거나 더 이상 필터가 없으면 실제 Controller(API)가 실행됨
    만약 Exception이 발생해도 이 줄은 일단 실행됨, 인증이 필요한 페이지면 뒤에서 막히고
    로그인 페이지면 통과해야 함
     */

    // Request Header에서 "Bearer 토큰값" 형태의 문자열 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 글자 떼고 토큰만 반환
        }
        return null;
    }
    /*
       .getHeader()를 통해서 HTTP 요청의 헤더 중에 Authorization 칸을 보고 bearerToken에 저장
       보통 Authorization칸에 토큰이 존재함
       만약 토큰이 null이 아니고 "Bearer "로 토큰이 시작하면 토큰의 암호문을 반환
       JWT 토큰을 보낼 때는 앞에 Bearer + " " 이란 단어를 보냄, Bearer는 토큰을 지참한 사람이란 뜻
       또한 우리가 필요한건 진짜 암호문으로 Bearer + " "을 없애고 암호문만 리턴(7글자)
     */
}
/*
HttpServletRequest는 클라이언트가 보낸 요청을 자바 읽을 수 있도록 번역한 주문서임
사용자가 서버로 요청을 보내는 순간 웹 애플리케이션(Tomcat)이 자동으로 이 객체를 생성
 */