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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studycafe.global.exception.CustomException;
import com.studycafe.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor // final붙은 필드 생성자 생성
public class ReservationService {
    private final ReservationRepository reservationRepository; // 예약 저장
    private final SeatRepository seatRepository; // 좌석 조회
    private final UserRepository userRepository; // 유저 조회
    private final RedisLockService redisLockService; // 분산 락 관리

    public String preOccupySeat(Long userId, Integer seatNumber) {
       log.info("좌석 선점 요청 - User: {}, Seat: {}", userId, seatNumber);
        boolean hasActive = reservationRepository
                .existsActiveReservation(userId,LocalDateTime.now());
        if(hasActive) {
            log.warn("선점 실패 : 이미 사용중 - User {}", userId);
            throw new CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        boolean isLocked = redisLockService
                .lockSeat(
                        String.valueOf(seatNumber), String.valueOf(userId));

        if(!isLocked) {
            throw new CustomException(ErrorCode.SEAT_ALREADY_LOCKED);
        }

        return "좌석 " + seatNumber + "번을 5분간 선점했습니다";
    }
    //region
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
    //endregion

    @Transactional // 에러 발생 시 없던 일로 롤백하여 데이터 안전하게 보관
    public Long confirmReservation(Long userId, Integer seatNumber, int hours) {
        String lockOwner = redisLockService.getLockOwner(String.valueOf(seatNumber));

        if(lockOwner == null || !lockOwner.equals(String.valueOf(userId))) {
            throw new CustomException(ErrorCode.INVALID_LOCK);
        }

        User user = userRepository.findById(userId) // 사용자 정보 조회
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Seat seat = seatRepository.findBySeatNumber(seatNumber) // 좌석 정보 조회
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

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

        List<Seat> allSeats = seatRepository.findAll();

        List<Reservation> activeReservations =
               reservationRepository.findActiveReservations(LocalDateTime.now());

       Set<Integer> occupiedSeats = activeReservations
               .stream()
               .map(r -> r.getSeat().getSeatNumber())
               .collect(Collectors.toSet());

       List<SeatStatusDto> statusList = new ArrayList<>();

       for(Seat seat : allSeats) {
           int seatNum = seat.getSeatNumber();
           String status = "AVAILABLE";

           if(occupiedSeats.contains(seatNum)) {
               status = "OCCUPIED";
           }
           else if(redisLockService.getLockOwner(String.valueOf(seatNum)) != null) {
               status = "LOCKED";
           }
           statusList.add(new SeatStatusDto(seatNum,status));
        }
        return statusList; // 상태가 저장된 리스트 리턴
    }
    /* 현재 전체 좌석 현황판, DB와 Redis를 모두 확인해서 각 좌석의 상태 종합
    1. DB에서 정보를 가져오기
    Seat 테이블에서 모든 좌석 정보를 가져와서 allSeats 리스트에 저장
    Reservation 테이블에서 시작 시간과 종료 시간 사이에 지금 시간이 포함된 예약들을
    가져와서(OCCUPIED) activeReservations 리스트에 저장

    2. 사용중인 좌석 번호가 포함된 Set 생성
    리스트를 stream(흐름)으로 바꾸고(검색 속도 향상)
    예약 객체(r)에서 좌석번호만 뺴와서 집합(Set)으로 collect()를 사용하여 생성

    3. 결과를 담을 빈 리스트 생성
    statusList라는 빈 리스트 생성

    4. 모든 좌석을 1좌석씩 검사(반복문)
    seat에서 SeatNumber를 seatNum으로 저장(좌석번호)
    일단 기본 상태를 전 좌석 빈자리(AVAILABLE)으로 설정

    만약 사용중인 좌석 Set(occupiedSeats)에 이 좌석 번호가 있는지 확인
    만약 사용중이라면 상태를 OCCUPIED로 변경

    만약 사용중이 아니라면 누군가 결제하려고 선점해둔 상태인지 확인
    이때 redisLockService의 getLockOwner를 호출해서 이 좌석번호로 된 키가 있는지
    확인하고 값이 null이 아니라면 누군가 가지고 있으므로 상태가 LOCKED로 변경됨

    판별된 좌석 번호와 상태를 (seatNum,status)로 포장(DTO객체로 변환)해서 결과 리스트에 담음

    5. 완성된 전체 좌석 현황표를 프론트엔드로 전송
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
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

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
