package com.studycafe.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration // 스프링 설정 클래스
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S") // 락의 기본 최대 유지 시간 (30초)
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
/* ShedLock 라이브러리 실행(스케줄러 락)
서버와 서버 사이의 퇴실 처리 순서 정하는 메서드

1. @EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
스케줄러 락 기능을 활성화, 만약 내가 개별 스케줄러 메서드에(@SchedulerLock)
시간을 안적으면 기본 30초를 적용(Period Time 30 Seconds)

2. lockProvider 메서드
ShedLock 라이브러리가 락 데이터를 어디에 저장할지 알려주는 다리 역할 수행
내 Redis 연결 정보(RedisConnectionFactory)는 이거라고 LockProvider 객체에
알려줘야 ShedLock이 작동함
>> ShedLock이 락을 할 때 락 정보를 기록하는 곳을 지정해주는 것
 */
}
