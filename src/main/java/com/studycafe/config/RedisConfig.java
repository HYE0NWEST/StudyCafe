/*
Redis DB와 Spring boot가 소통할 수 있도록 연결통로 + 통역사 설정
 */
package com.studycafe.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;

@Configuration
/*
설정파일이므로 프로젝트 시작 시 이 메서드를 읽어서 여기에 정의된 Bean(객체)들을 생성
이 애너테이션이 있어야 스프링이 이 메서드를 실행
 */
public class RedisConfig {
    @Bean
    public RedisTemplate<String,String> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String,String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // Template 생성 및 연결 설정

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        // 직렬화 설정
        // 키와 값을 저장할 때 문자열로 변환

        return template; // 완성된 RedisTemplate 객체 반환
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

    @Bean // 이 메서드가 반환하는 CacheManager 객체를 빈으로 등록
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
/*
이 설정은 Redis에 데이터를 저장할 때 Key는 문자열로, Value는 JSON 문자열로 변환
해서 저장하고 5분이 지나면 자동으로 삭제하며 NULL은 저장하지 말라는 규칙 관리자 생성

1. 메서드 시그니처 설정
RedisConnectionFactory는 Redis 서버와의 연결을 담당하는 객체로 스프링 부트가
application.yml 설정을 읽어서 자동으로 주입

2. 캐시 정책 정의
2-1. 유효 기간 설정(TTL)
.entryTtl()을 사용하여 캐시에 저장된 데이터의 수명을 5분으로 설정
데이터가 너무 오래되어 실제 DB내용과 달라지는 데이터 불일치 문제 방지

2-2. NULL 값 제외
.disableCahingNullValues()로 메서드 리턴 값이 NULL인 경우 캐시에 저장하지 않음

2-3. Key 직렬화(Serialization) 방식
.serializeKeysWith()로 캐시의 Key를 저장할 때 자바 객체를 String으로 변환

2-4. Value 직렬화(Serialization) 방식
.serializeValuesWith()로 캐시의 Value를 저장할 때 자바 객체를 JSON 형식 문자열로 변환
GenericJackson2JsonRedisSerializer()는 JSON 내부에 클래스 타입 정보를 함께 저장하여
캐시를 읽어오는 역직렬화때 자바 객체로 정확히 복원

3. RedisCacheManager 생성 및 반환
.builder(connectionFactory)로 Redis 연결 공장을 이용하여 빌더를 생성
.cacheDefaults(config)으로 위에서 정의한 규칙(config)을 기본 설정으로 적용
.build()로 최종적으로 설정이 완료된 RedisCacheManager를 생성하여 반환

 */
}

