package com.studycafe.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.Objects;

@SpringBootTest
public class PerformanceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Redis 캐싱 성능 비교: 캐시 미적용 vs 적용")
    void compareCachingPerformance() {
        // 0. 준비: 기존 캐시 제거 (공정한 테스트를 위해)
        Objects.requireNonNull(cacheManager.getCache("seatStatus")).clear();

        // ---------------------------------------------------
        // Case 1. 첫 번째 조회 (Cache Miss - DB 조회)
        // ---------------------------------------------------
        long start1 = System.currentTimeMillis();
        reservationService.getAllSeatStatus(); // DB 다녀옴
        long end1 = System.currentTimeMillis();
        long time1 = end1 - start1;

        // ---------------------------------------------------
        // Case 2. 두 번째 조회 (Cache Hit - Redis 조회)
        // ---------------------------------------------------
        long start2 = System.currentTimeMillis();
        reservationService.getAllSeatStatus(); // Redis에서 바로 옴
        long end2 = System.currentTimeMillis();
        long time2 = end2 - start2;

        // ---------------------------------------------------
        // 📊 결과 출력 (콘솔에 그래프 그리기)
        // ---------------------------------------------------
        System.out.println("\n=======================================================");
        System.out.println(" [성능 비교 결과] Redis Caching Performance");
        System.out.println("=======================================================");

        System.out.printf("1. 캐시 미적용 (DB 조회)   : %4d ms  ", time1);
        printBar(time1);

        System.out.printf("2. 캐시 적용 (Redis 조회)  : %4d ms  ", time2);
        printBar(time2); // 훨씬 짧게 그려짐

        System.out.println("=======================================================");
        System.out.printf("성능 개선율: 약 %d배 향상\n", time1 / time2);
        System.out.println("=======================================================\n");
    }

    // 막대그래프 그리는 헬퍼 메서드
    private void printBar(long ms) {
        int length = (int) (ms / 2); // 2ms당 막대 1개
        for (int i = 0; i < length; i++) {
            System.out.print("█");
        }
        System.out.println();
    }
}