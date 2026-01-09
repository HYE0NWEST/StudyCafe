package com.studycafe.service;

import com.studycafe.domain.reservation.ReservationRepository;
import com.studycafe.domain.seat.Seat;
import com.studycafe.domain.seat.SeatRepository;
import com.studycafe.domain.user.User;
import com.studycafe.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest // 스프링 컨테이너(서버)를 실제로 띄워서 테스트
public class ConcurrencyTest {

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private RedisLockService redisLockService;

    @BeforeEach // 청소
    void setUp() {
        // 테스트 돌릴 때마다 데이터 초기화
        reservationRepository.deleteAll();
        // Redis 락도 초기화
        redisLockService.unlockSeat("1");
    }

    @Test
    @DisplayName("Redis 락 적용 시: 100명이 동시에 1번 좌석을 눌러도 딱 1명만 성공해야 한다.")
    void redisLockTest() throws InterruptedException {

        int threadCount = 100;
        // 사용자 수

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 32개 스레드로 병렬 처리

        CountDownLatch latch = new CountDownLatch(threadCount);
        // 100명이 다 끝날 때까지 기다리는 장치

        AtomicInteger successCount = new AtomicInteger(0);
        // 성공한 사람 수
        AtomicInteger failCount = new AtomicInteger(0);
        // 실패한 사람 수
/*
ExecuterService는 new Thread(100)을 하는 대신에 미리 32개의 스레드를 만들고
작업을 재사용함(스레드 풀)

CountDownLatch는 100을 들고 작업이 1개 끝날 때마다 1씩 감소시킴(countDown)
숫자가 0이 될 때까지 메인 테스트 스레드를 잠시 멈춰 세우는 역할(await) 수행하여
테스트가 작업 시작만 시켜놓고 바로 성공했다고 끝내는 일 없게 함

AtomicInteger은 CPU 레벨에서 순차적인 계산을 보장함
여러 스레드가 동시에 successCount++을 하면 10명이 눌러도 숫자가 1만 올라가는
데이터 씹힘 현상 방지
 */

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    // 우리가 만든 '안전한' 메서드 호출
                    reservationService.preOccupySeat(userId, 1);
                    successCount.incrementAndGet(); // 에러 안 나면 성공 +1
                } catch (Exception e) {
                    failCount.incrementAndGet(); // 에러 나면 실패 +1
                } finally {
                    latch.countDown(); // 한 명 끝남
                }
            });
        }
        latch.await(); // 100명 다 끝날 때까지 대기
/* 테스트 실행 로직
0부터 99까지 for문을 돌리고 userId는 1~100으로 설정

submit()으로 작업 지시서로 executorService를 제시

redisService의 preOccupySeat로 핵심 로직을 실행

만약 예외가 안나면 successCount를 1개씩 증가
만약 예외가 나면 failCount를 1개씩 증가

성공하든 실패하든(finally) 무조건 1명이 끝났으므로 latch를 1 감소

100명이 다 끝날때 까지 대기여야 하므로 메인 스레드는 여기서 기다리고
만약 다 끝나서 latch가 0이되면 다음으로 넘어감
 */




        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());

        // 검증: 오직 1명만 성공해야 함!
        assertEquals(1, successCount.get());
        // 나머지 99명은 실패해야 함!
        assertEquals(99, failCount.get());
    }
}