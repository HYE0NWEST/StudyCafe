/*
시간이 되면 시스템이 알아서 퇴실 처리를 해주는 자동화 시스템
 */
package com.studycafe.service;

import com.studycafe.domain.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

@Slf4j // log. 사용
@Component // 스케줄러 작성시 필수
@RequiredArgsConstructor // final 필드에 생성자 자동 추가
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;
    // 의존성 주입
    
    @Scheduled(fixedDelay = 60000)
    @Transactional // 이전에 에러가 나면 취소(롤백)하거나 안전하게 Commit하기 위해 트랜잭션을 검
    public void autoCheckOut() {
        LocalDateTime now = LocalDateTime.now(); // 현재 시간 불러오고 now에 저장

        int updatedCount = reservationRepository.updateExpiredReservations(now);
        // DB에 업데이트 쿼리 실행

        if(updatedCount > 0) {
            log.info("시간 종료된 예약 {}건을 자동 퇴실 처리했습니다.(기준시간 : {})",
                    updatedCount,now);
        }
        // 변경된 건이 있는 로그 기록이 있으면 실행(updatedCount가 1 이상이면)
    }
}
/*
@Scheduled(fixedDelay = 60000) : 60000ms(1분)마다 자동으로 실행
fixedDelay는 앞의 작업이 다 끝나고 나서 1분을 쉬게 됨
만약 퇴실 처리가 10초 걸리면 (작업10초)->(휴식60초)->(다음작업) 순으로 진행

ReservationRepository의 updateExpiredReservation메서드에 현재 시간을 매개변수로 하여 호출
현재 시간을 기준으로 종료 시간이 지난 예약들을 찾고 CONFIRMED->COMPLETED로 상태 변경
상태 변경한 예약들을 총 몇 명이 바뀌었는지 숫자를 반환하여 updatedCount에 저장

스케줄러는 1분마다 돌으므로 만료된 사람만 로그를 찍도록 하여 쓸데없는 내용 로그 방지
>> Log Spamming, 중요한 기록은 못볼 수 있게 됨

 */
