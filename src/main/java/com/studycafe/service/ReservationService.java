/*
사용자가 좌석을 선택하는 순가 -> 예약 확정 사이의 과정을 처리
 */
package com.studycafe.service;

import com.studycafe.domain.reservation.Reservation;
import com.studycafe.domain.reservation.ReservationRepository;
import com.studycafe.domain.seat.Seat;
import com.studycafe.domain.seat.SeatRepository;
import com.studycafe.domain.user.User;
import com.studycafe.domain.user.UserRepository;
import com.studycafe.dto.SeatStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final붙은 필드 생성자 생성
public class ReservationService {
    private final ReservationRepository reservationRepository; // 예약 저장
    private final SeatRepository seatRepository; // 좌석 조회
    private final UserRepository userRepository; // 유저 조회
    private final RedisLockService redisLockService; // 분산 락 관리

    public String preOccupySeat(Long userId, Integer seatNumber) {
        boolean hasActive = reservationRepository
                .existsActiveReservation(userId,LocalDateTime.now());
        if(hasActive) {
            throw new RuntimeException("이미 이용중인 좌석이 있습니다");
        }

        boolean isLocked = redisLockService
                .lockSeat(
                        String.valueOf(seatNumber), String.valueOf(userId));

        if(!isLocked) {
            throw new RuntimeException("이미 선택된 좌석입니다");
        }

        return "좌석 " + seatNumber + "번을 5분간 선점했습니다";
    }
    /* 좌석 선점 메서드(좌석 클릭 시 실행됨, Redis에 찜만 해두는 단계)
    DB에 INSERT 하기 전에 Redis를 먼저 거치는 과정 수행
    reservationRepository의 existsActiveReservation을 호출하여
    만약 hasActive가 true이면 1인 1좌석을 어기므로 에러 메시지 발생


    RedisLockService를 호출해서 lock 시도 >> true,false 반환
    호출 시 Integer형의 SeatNumber와 Long형의 userId를 String으로 변환함
    그 이유는 Redis는 기본적으로 String 기반의 저장소이고 RedisConfig에서
    StringRedisSerializer를 쓰겠다고 설정함

    만약 false(이미 다른 사람이 선점함)면 에러 메시지 전송
    만약 true면 좌석번호와 함께 성공 메시지 전송
     */

    @Transactional // 에러 발생 시 없던 일로 롤백하여 데이터 안전하게 보관
    public Long confirmReservation(Long userId, Integer seatNumber, int hours) {
        String lockOwner = redisLockService.getLockOwner(String.valueOf(seatNumber));

        if(lockOwner == null || !lockOwner.equals(String.valueOf(userId))) {
            throw new RuntimeException("좌석 선점 시간이 만료되었거나 본인의 선점이 아닙니다");
        }

        User user = userRepository.findById(userId) // 사용자 정보 조회
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        Seat seat = seatRepository.findBySeatNumber(seatNumber) // 좌석 정보 조회
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        Reservation reservation = new Reservation( // 예약 엔티티 생성(JAVA객체)
                user, // 유저
                seat, // 좌석
                LocalDateTime.now(), // 시작 시간
                LocalDateTime.now().plusHours(hours), // 종료 시간(현재 + 이용 시간)
                Reservation.ReservationStatus.CONFIRMED // 상태 : 확정
        );

        reservationRepository.save(reservation); // DB에 INSERT하여 저장

        redisLockService.unlockSeat(String.valueOf(seatNumber)); // Redis 청소하여 메모리 비움

        return reservation.getId(); // 예약 번호 반환
    }
    /*
String lockOwner : Redis에서 이 자리의 주인을 검사하기 위해 lockOwner에 저장

if(lockOwner == null) : 5분이 지나서 선점이 풀린 경우
if(!lockOwner.equals(String.valueOf(userId)) :
이미 선점한 자리를 선점한 사람이 아니라 다른 사람이 결제 요청을 보낸 경우(해킹 등)
>> 선점할 때 이미 했지만 예약 확정할 때 한 번 더 확인해서 이중검증 구현

unlockSeat : Redis의 역할이  종료되었으므로 Redis를 청소하고 락을 지워서 메모리를 비움
 */

    public List<SeatStatusDto> getAllSeatStatus() {
        List<SeatStatusDto> statusList = new ArrayList<>();
        // 결과를 넣을 리스트(statusList) 생성

        List<Reservation> activeReservations =
                reservationRepository.findActiveReservations(LocalDateTime.now());
        // DB에서 사용중(퇴실안했고 입실중인)인 좌석을 가져오기

        Set<Integer> occupiedSeats = activeReservations
                .stream()
                .map(r -> r.getSeat().getSeatNumber())
                .collect(Collectors.toSet());

        for(int i = 1; i <= 100; i++) { // 100번 반복
            String status = "AVAILABLE"; // 사용 가능 상태(빈자리)를 기본값으로 지정
            if(occupiedSeats.contains(i)) { // DB확인
                status = "OCCUPIED";
            }
            else { // Redis 확인(DB에는 없지만 Redis에 잠금키가 있는지 확인
                if(redisLockService.getLockOwner(String.valueOf(i)) != null) {
                    status = "LOCKED";
                }
            } // 최종 결정된 상태를 .add()로 리스트에 추가
            statusList.add(new SeatStatusDto(i,status));
        }
        return statusList; // 상태가 저장된 리스트 리턴
    }
    /* 스터디카페의 전체 좌석 현황판 생성
    Set ...
    리스트를 stream으로 펼치고 예약 객체(정보)에서 좌석 번호만 뽑아냄
    그 후 다시 Set(집합)으로 모으기

    가져온 예약 정보(List<Reservation>)은 너무 무거워서 검색하기 불편함
    List를 100번 반복문을 돌 때마다 activeReservations.contains()를 하면 리스트를 다 뒤져야 함
    그러나 Set으로 accupiedSeats.contains()을 하면 Hash 알고리즘을 이용하여 즉시 찾음
    Set = 내 JAVA 메모리에 잠깐 생기는 객체로서 DB에 있는 데이터를 가져와서 Set에 담은 것
    >> 성능적으로 우수함

    가져온 예약정보에서 좌석번호(int)만 Set에 담음(DB -> JAVA 메모리)
    Set(occupiedSeats)에는 {1,5,7}처럼 숫자만 들어있음(현재 입실 좌석 번호)



    일단 빈 자리라고 기본값으로 설정하고 시작
    Set에 현재 좌석 번호 i가 들어있는지 확인(있다면 누군가 이용중인 것)
    만약 i(seatNumber)가 contains라면 상태(status)를 OCCUPIED로 선언

    DB에 없으므로 이번에는 Redis를 확인
    5분 내에 누군가 자리를 선점(prOccupySeat)을 했는지 확인(좌석번호를 String으로 변경해서 조회)
    만약 userId가 조회된다면 누군가 결제중인 것 >> 상태(status)를 LOCKED로 선언

    DB(Set)에 없고 Redis에도 없다면 진짜 아무도 없는 자리이므로 AVAILABLE 사용 가능함
     */

    public void cancelPreOccupy(Integer seatNumber) {
        redisLockService.unlockSeat(String.valueOf(seatNumber)); // Redis 락 해제
    }
    /* Redis의 잠금을 즉시 해제하는 로직
    프론트엔드에서 넘어온 좌석 번호를 문자열로 변경 후 redisLockService의 메서드 호출
     */

    @Transactional
    public void endUse(Long userId) {
        Reservation reservation = reservationRepository
                .findActiveReservation(userId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("현재 이용중인 예약이 없습니다"));

            reservation.cancel();
    }
    /* 퇴실처리
    @Transactional로 변경사항을 저장하여 자동으로 UPDATE쿼리를 날려주고 에러시 롤백
    reservationRepository의 쿼리를 사용하여 이용중인 예약을 찾고 없으면 에러 메시지 전송

    reservation의 cancel메서드로 객체의 상태를 변경
    reservation.cancel()은 reservation의 cancel메서드를 호출하여 상태를 CANCELLED로 변경
    >> 현재는 자바만 변경된거고 DB는 변경되지 않음

    변경되었으면 @Transcational 어노테이션이 이를 감지하여 현재 상태가 변경된 객체와
    처음의 객체를 비교히여 DB를 자동으로 쿼리를 날려서 업데이트
    ex.
    UPDATE reservation
    SET status = 'CANCELLED'
    WHERE id = 1;
     */

    @Transactional(readOnly = true)
    public Integer getCurrentSeatNumber(Long userId) {
        return reservationRepository.findActiveReservation(userId, LocalDateTime.now())
                .map(reservation -> reservation.getSeat().getSeatNumber())
                .orElse(null);
    }
    /* userId를 가지고 있는 사람의 현재 이용 좌석 번호 조회
       readOnly = true이므로 DB내용을 바꾸지 않고 눈으로 보기만 함
       reservationRepository의 findActiveReservation메서드를 호출
       findActiveReservation 메서드는 Optional상자로 예약 정보를 반환함

       이떄 .map()을 통해서 상자를 열지 않고 상자 안의 내용물을 변경함
       원래의 양 많은 Reservation객체를 좌석번호(Integer)만 남게하고 반환함
       .orElse()는 만약 상자가 비어있으면 그냥 null을 반환함
     */
}
