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
import java.util.Map;
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

    @Transactional // 트랜잭션으로 선언
    public Long confirmReservation(Long userId, Integer seatNumber, int hours) {
        boolean refreshed = redisLockService.refreshLock(
                String.valueOf(seatNumber),
                String.valueOf(userId)
        );

        if(!refreshed) {
            throw new CustomException(ErrorCode.INVALID_LOCK);
        }

        try{
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            Seat seat = seatRepository.findBySeatNumber(seatNumber)
                    .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

            boolean alreadyReserved = reservationRepository.existsBySeatAndStatus(
                    seat,
                    Reservation.ReservationStatus.CONFIRMED
            );

            if(alreadyReserved) {
                throw new CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED);
            }

            boolean userHasReservation = reservationRepository.existsActiveReservation(
                    userId,
                    LocalDateTime.now()
            );

            if(userHasReservation) {
                throw  new CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED);
            }

            Reservation reservation = new Reservation(
                    user,
                    seat,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(hours),
                    Reservation.ReservationStatus.CONFIRMED
            );

            reservationRepository.save(reservation);

            return reservation.getId();
        }
        finally {
            redisLockService.unlockSeat(String.valueOf(seatNumber));
        }
    }
/*
1. 락 갱신
redisLockService의 refreshLock 메서드를 좌석번호, ID를 넣고 호출하여
현재 좌석 주인과 지금 들어온 유저와 같은지 refreshed에 저장

2. 검사 결과
검사 통과하면 다음으로 통과
그러나 락이 이미 만료되었거나 그 사이에 다른 사람이 채갔으면 false가 반환
그 즉시 CustomException을 던져서 DB 저장 로직 자체를 실행 불가하게 함

3. 핵심 로직
userRepository의 findById와 seatRepository의 findBySeatNumber를 호출해서
각각 user,seat에 유저와 좌석 정보를 저장
(3-1 과정)
(3-2 과정)
유저정보, 좌석정보, 현재 시간, 현재 시간, 만료 시간, 예약 상태(CONFIRMED)를
모두 포함해서 reservation 예약 객체를 생성하고 이를 save 메서드로 DB에 저장(INSERT)
예약 객체에서 userId만 빼와서 리턴

3-1. 좌석 중복 체크(Redis 장애 대비)
만약 Redis가 순간적으로 재부팅되어서 락 정보가 다 날아가는 경우가 발생할 수 있음
그러면 락이 해제된 상태이므로 다른 사람이 들어올 수 있어 DB에 물어봐서 차단

reservationRepository의 existsBySeatAndStatus에게 좌석정보와 CONFIRMED 상태를
매개변수에 넣어서 좌석이 A이고 상태가 CONFIRMED인 데이터가 존재하는지 DB에 물어봄

만약 데이터가 존재하면(true) 이미 좌석이 있다는 CustomException을 발생
만약 데이터가 존재하지 않으면(false) 유저 중복 체크로 넘어감

3-2. 유저 중복 체크(1인 1좌석 원칙 유지)
만약 한 유저가 여러 자리를 가지는 것을 방지하기 위해 DB에서 최종 차단

reservationRepository의 existsActiveReservation 메서드를 호출하여
해당 유저가 이미 예약된 내역이 있으면 CustomException을 발생
해당 유저가 이미 예약된 내역이 없으면 예약 객체 생성 및 DB저장(INSERT)로 넘어감

4. 뒷정리
이 뒷정리는 예약을 성공(자리 사용 완료)했거나 문제가 발생하면 락을 반납하라는 역할
finally를 통해서 에러가 나도 락을 반납하여 다른 사용자가 락을 걸 수 있게 함
만약 없다면 사용자는 튕겨져 나가도 Redis 락은 그대로 유지하게 됨
 */


    public List<SeatStatusDto> getAllSeatStatus() {

        List<Seat> allSeats = seatRepository.findAll();

        List<Reservation> activeReservations =
               reservationRepository.findActiveReservations(LocalDateTime.now());

       Set<Integer> occupiedSeats = activeReservations
               .stream()
               .map(r -> r.getSeat().getSeatNumber())
               .collect(Collectors.toSet());

       List<Integer> seatNums = allSeats
               .stream()
               .map(Seat::getSeatNumber)
               .toList();

       Map<Integer,String> lockedSeats = redisLockService.getLockOwners(seatNums);

       List<SeatStatusDto> statusList = new ArrayList<>();

       for(Seat seat : allSeats) {
           int seatNum = seat.getSeatNumber();
           String status = "AVAILABLE";

           if(occupiedSeats.contains(seatNum)) {
               status = "OCCUPIED";
           }
           else if(lockedSeats.containsKey(seatNum)) {
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

    2. 사용중인 좌석 번호가 포함된 Set 생성(자료구조 최적화)
    리스트를 stream(흐름)으로 바꾸고(검색 속도 향상)
    예약 객체(r)에서 좌석번호만 뺴와서 집합(Set)으로 collect()를 사용하여 생성

    3. 락 조회를 위한 seatNums 리스트 생성
    좌석 정보에서 좌석 번호만 빼서 stream형태의 리스트인 seatNums를 생성

    4. redis 락 정보 받아오기
    redisLockService의 getLockOwners를 호출하여 현재 결제 진행중(락이 걸린)
    좌석 정보를 redis와 딱 1번 통신으로 받아오고 Map형태의 lockedSeats에 저장

    5. 결과를 담을 빈 리스트 생성
    statusList라는 빈 리스트 생성

    6. 모든 좌석을 1좌석씩 검사(반복문)
    seat에서 SeatNumber를 seatNum으로 저장(좌석번호)
    일단 기본 상태를 전 좌석 빈자리(AVAILABLE)으로 설정

    만약 사용중인 좌석 Set(occupiedSeats)에 이 좌석 번호가 있는지 확인
    만약 사용중이라면 상태를 OCCUPIED로 변경

    만약 사용중이 아니라면 누군가 결제하려고 선점해둔 상태인지 확인
    이떄 lockedSeats(Map)을 뒤져서 현재 좌석번호가 존재하면 락이 걸려있으므로
    상태를 LOCKED로 변경

    판별된 좌석 번호와 상태를 (seatNum,status)로 포장(DTO객체로 변환)해서 결과 리스트에 담음

    7. 완성된 전체 좌석 현황표를 프론트엔드로 전송
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

    // 테스트 용
    // [포트폴리오용] Redis 락 없이 예약하는 위험한 메서드 (동시성 이슈 재현용)
    @Transactional
    public void unsafePreOccupySeat(Long userId, Integer seatNumber) {
        // 1. DB에서 예약 확인 (락 없이 단순 조회)
        boolean hasActive = reservationRepository
                .existsActiveReservation(userId, LocalDateTime.now());

        // 2. 이미 예약이 있어도 동시 요청이 들어오면 여기서 다 통과해버림 (Race Condition)
        if (hasActive) {
            throw new CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        // 3. 검증 없이 바로 저장 (원래는 이러면 안 됨!)
        // (테스트를 위해 억지로 예약을 만드는 로직)
        // 실제로는 Reservation 객체를 만들어서 save 해야 하지만,
        // 여기서는 흐름만 보여주기 위해 로그만 찍거나 간단히 처리해도 됨.
        // 하지만 테스트 증명을 위해 실제 저장을 시도하는 로직을 가정함.
    }
}
