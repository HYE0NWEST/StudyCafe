/*
JWT를 생성하고 검증하고 정보를 추출하는 클래스
 */
package com.studycafe.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

   @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-in-seconds}")
    private long tokenValidTime;

    private Key key;
    /* 필드 변수 및 초기화
    @Value는 application.yml에 있는 설정된 값을 가져옴
    jwt.secret은 토큰을 암호화하고 복호화할 때 쓰는 비밀키 문자열로 secretKey에 저장
    jwt.access-token ...은 토큰의 유효시간으로 tokenValidTime에 저장
    key는 실제로 암호화 알고리즘에 사용될 Key 객체를 담을 변수
     */

    @PostConstruct
    protected void init() {
        // JWT Secret Key 검증
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("JWT Secret Key가 설정되지 않았습니다. application.yml 또는 환경변수 JWT_SECRET을 확인하세요.");
        }
        
        // HS256 알고리즘은 최소 256비트(32바이트)의 키가 필요합니다
        if (secretKey.length() < 32) {
            log.warn("JWT Secret Key가 너무 짧습니다. 최소 32자 이상을 권장합니다. (현재: {}자)", secretKey.length());
        }

        this.tokenValidTime = this.tokenValidTime * 1000L;

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    /* 의존성 주입 완료된 후 실행되는 메서드(필드 변수 수정 과정)
       @PostContruct는 스프링이 JwtTokenProvider 객체를 생성하고 @Value에 있는 변수들을
       다 채워 넣은 직후에 자동으로 실행 >> 안전하게 초기화 작업 수행 가능

        application.yml에서 가져온 tokenValidTime에 1000L을 곱하여 밀리초 단위를 사용하게 하고
        L은 Long타입을 의미하여 int 범위를 벗어나도 오버플로우가 발생하지 않게 함

        문자열(secretKey)를 .getBytes()로 바이트 배열로 변환하고 StandardCharsets.UTF_8로
        리눅스 서버에서는 토큰 오류를 방지하고 한글/특수문자를 바이트로 바꾸는 방식을 UTF_8로 고정

        Keys.hmacShaKeyFor()는 JWT라이브러리에게 이 바이트 배열을 HS256같은 알고리즘을 위한 비밀키
        로 사용함을 알리면서 전용 객체인 Key로 만드는 과정을 수행
        이때 들어온 바이트 배열의 길이를 체크하고 암호화 알고리즘에 맞게 세팅된 SecretKey 객체를 반환하여
        this.key에 저장 >> this.key는 이제 서명처럼 사용됨
     */


    public String createToken(String userId) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(userId) // 토큰의 주제 설정(유저의 ID인 기본키)
                .setIssuedAt(now) // 발급 시간(현재 시간)
                .setExpiration(new Date(now.getTime() + tokenValidTime)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 암호화 알고리즘
                .compact();
    }
    /* 토큰 생성
    JWT를 생성하기 위한 빌더 객체인 Jwts.builder()를 열고 주제와 발급 시간을 설정
    만료 시간은 현재 시간 + 유효 시간(tokenValidTime)으로 설정
    서명은 key 객체와 암호화 알고리즘(HS256)으로 설정하여 위조되지 않음을 보장
    .compact()로 위 설정들을 바탕으로 최종적인 JWT 문자열을 반환

    토큰 구조 : Header, Payload(sub:userId, iat:IssuedAt, exp:Expiration), Signature
     */



    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    /* 유저 ID 추출(토큰의 주인 찾는 과정)
    토큰을 해석(파싱)하기 위한 빌더인 Jwts.parserBuilder()를 열고
    .setSigningKey(key)로 토큰을 만들 때 사용했던 비밀키(key)를 설정 >> 다르면 파싱 불가
    .build()로 JwtParser 객체를 생성
    .parseClaimsJws(token)은 JwtParser로 토큰을 검증하고 파싱 >> 서명이 잘못되면 여기서 에러 발생
    .getBody()는 토큰의 내용(Claims,페이로드)를 가져옴
    .getSubject()는 내용 중에서 Payload에 넣었던 값(userId)를 꺼내서 반환
     */



    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다."); // 위조 또는 깨진 토큰
        } catch (ExpiredJwtException e) {
            log.info("유효기간이 만료된 JWT 토큰입니다."); // 유효기간(exp)가 지난 토큰
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 형식의 JWT 토큰입니다."); // 형식이 잘못된 토큰
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.(비어있음)"); // 값이 비어있는 토큰
        }
        return false;
        /* 토큰의 유효성 검사(토큰이 사용 가능한지 검사하는 메서드)
        getUserId와 같이 key를 장착하고 .build()하여 JwtParser 겍체를 생성하고
        .parseClaimsJws(token)으로 서명을 검증하고 일치하는 지 확인 >> true면 메서드 통과
        만약 false라면 각 상황별로 log 출력
         */
    }
}