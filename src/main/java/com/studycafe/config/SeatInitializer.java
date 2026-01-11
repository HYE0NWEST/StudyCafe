/*
애플리케이션 시작 시 좌석 데이터를 자동으로 초기화하는 클래스
 */
package com.studycafe.config;

import com.studycafe.domain.seat.Seat;
import com.studycafe.domain.seat.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component // 스프링이 이 클래스 관리 및 실행(SpringBean)
@RequiredArgsConstructor
public class SeatInitializer implements CommandLineRunner {

    private final SeatRepository seatRepository;

    @Value("${app.seat.total-count:100}")
    private int totalSeatCount;


    @Override
    public void run(String... args) {
        long existingSeatCount = seatRepository.count();

        if (existingSeatCount == 0) {
            log.info("좌석 데이터가 없습니다. {}개의 좌석을 생성합니다.", totalSeatCount);

            for (int i = 1; i <= totalSeatCount; i++) {
                Seat seat = new Seat(i);
                seatRepository.save(seat);
            }

            log.info("좌석 {}개 생성이 완료되었습니다.", totalSeatCount);
        } else {
            log.info("이미 좌석 데이터가 존재합니다. (현재: {}개)", existingSeatCount);
        }
    }
/* 앱이 실행될 때 DB에 좌석이 비어있다면 자동으로 초기 데이터를 넣어주는 역할 수행
만약 데이터가 이미 있다면 생성하지 않음

1. 설정값 유연성 확보
@Value("${app.seat.total-count:100}")는 application.yml 파일에
app.seat.total-count라는 값이 있으면 그 값을 가져오고 없으면 기본값으로 100을 사용함
현재 내 코드에는 이미 100의 값이 존재하므로 totalSeatCount에는 100이 저장됨

2. 실행
2-1. 좌석 개수 확인
seatRepository의 count메서드를 호출해서 DB에 저장된 좌석 개수를 확인

2-2. 데이터가 하나도 없을 때만 로직 수행(0개일때)
만약 데이터가 없다면 좌석을 생성한다는 로직 출력
for문으로 1부터 100(설정된 수)만큼 반복
seat 엔티티 생성자를 호출해서 좌석 객체(seat)를 생성하고 seatRepository로
DB에 저장(INSERT)

2-3. 데이터가 존재하는 경우 로그만 남기고 종료

 */
}
