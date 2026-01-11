package com.studycafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableCaching // 캐시(Cache)관련 어노테이션 기능 동작
@EnableScheduling // 스케줄러 기능 활성화 (ReservationScheduler 동작을 위해 필요)
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class,args);
    }
}