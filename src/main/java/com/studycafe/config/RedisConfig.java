/*
Redis DB와 Spring boot가 소통할 수 있도록 연결통로 + 통역사 설정
 */
package com.studycafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
/*
설정파일이므로 프로젝트 시작 시 이 메서드를 읽어서 여기에 정의된 Bean(객체)들을 생성
이 애너테이션이 있어야 스프링이 이 메서드를 실행
 */
public class RedisConfig {
    @Bean
    public RedisTemplate<String,String>
    redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String,String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // Template 생성 및 연결 설정

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        // 직렬화 설정
        // 키와 값을 저장할 때 문자열로 변환

        return template; // 완성된 RedisTemplate 객체 반환
    }
}
/*
@Bean : 이 메서드가 반환하는 객체(RedisTemplate)을 스프링 컨테이너에 등록
다른 메서드에서 private RedisTemplate redisTemplate;이라고 선언만 하면
스프링이 여기서 만든 객체를 자동으로 주입

RedisTemplate<String,String> : Redis를 조작하는 메서드
이때 key와 value를 모두 문자열로 설정

(RedisConnectionFactory connectionFactory) : 연결 공장
application.yml에 적어둔 host,port,password 정보를 바탕으로
Redis와 실제 연결을 맺어주는 객체를 매개변수로 투입 (스프링이 자동으로 해줌)
 */
/*
new RedisTemplate<>()로 빈 Template를 생성
setConnectionFactory(connectionFactory)로 이 Template는 실제 Redis 서버와 연결됨
 */
/*
직렬화를 하지 않은 기본값은 자바 개체를 그대로 바이너리로 변환해서 저장
>> Redis에서는 디버깅하기가 힘든 외계어로 저장됨

StringRedisSerializer(직렬화)를 설정하면 자바의 String을 Redis의 String으로 텍스트 그대로 변환
 */
