package com.studycafe.domain.reservation;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Modifying(clearAutomatically = true)
   @Query("UPDATE Reservation r " + // 예약 상태 변경
           "SET r.status = 'COMPLETED' " + // 상태를 COMPLETED로 변경
           "WHERE r.endTime <= :now AND r.status = 'CONFIRMED'")
    int updateExpiredReservations(
            @Param("now") LocalDateTime now
    );
    /* updateExpiredReservations : 시간이 다 된 예약들을 찾아서 상태를 이용완료(COMPLETED)로 변경
1분마다 실행하는 스케줄러가 이 메서드 호출

@Modifying : @Query 어노테이션으로 SELECT가 아닌 UPDATE, DELETE 쿼리를 실행할 때는
반드시 이 어노테이션을 붙이기(수정관련)
clearAutomatically = true : 이 쿼리를 실행한 직후에 1차 캐시(영속성 컨텍스트)를 비우기
이런 쿼리는 Bulk Operation(벌크 연산)으로 JPA를 거치지 않고 DB에 직접 쿼리를 전송
ex.
자바 메모리(영속성 컨택스트,1차 캐시)는 여전히 45번예약상태가 CONFIRMED라고 되어있음
이 쿼리로 DB에서는 COMPLETED로 변경됨
이후 로직에서 45번을 조회하면 DB가 아닌 자바메모리로 가져오기 때문에 여전히 CONFIRMED로 착각함(데이터 불일치)
>> 강제로 다음 조회때 DB에서 새 데이터를 가져오게 함

종료 시간이 현재 시간보다 과거이고(지났고) 아직 이용중(확정)인 상태만 골라서
이용완료(COMPLETED) 상태로 예약 상태를 변경 >> 업데이트된 행의 개수 반환(몇 건 처리 완료)
 */

    @Query("SELECT r " +
            "FROM Reservation r " +
            "JOIN FETCH r.seat" +
            " WHERE r.startTime <= :now " +
            "AND r.endTime > :now " +
            "AND r.status = 'CONFIRMED'")
    List<Reservation> findActiveReservations(
            @Param("now") LocalDateTime now
    );
    /* findActiveReservations : 현재 이용중인 예약 조회(현재 앉아있는 사람 조회)

시작 시간이 현재 시간보다 과거이고(입실했고) 종료 시간은 현재 시간보다 미래인(퇴실 전)
현재 시간 범위 안에 있는 예약만 찾음 >> 현재 사용하고 있는 것을 리스트로 반환

JOIN FETCH를 사용하여 DB에게 예약 정보와 함께 Seat 테이블이랑 JOIN하여 1번의
쿼리로 예약정보(Reservation)과 좌석정보(Seat)를 모두 가져옴

만약 JOIN FETCH를 사용하지 않으면 목록 1번 조회당 100번의 좌석 조회가 필요
 */

    @Query("SELECT COUNT(r) > 0 " +
            "FROM Reservation r " +
            "WHERE r.user.id = :userId " +
            "AND r.status = 'CONFIRMED' " +
            "AND r.endTime > :now")
    boolean existsActiveReservation(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
    /* existsActiveReservation : 중복 예약 방지(이 유저가 현재 시점에서 이용중인 예약 찾기)
Reservation테이블에서 조건에 맞는 예약 데이터가 1개 이상이면 true >> 에러 발생
조건 1 : 지금 예약을 시도하는 사람(userId)
조건 2 : 유효한(CONFIRMED)예약
조건 2 : 종료 시간이 현재 시간보다 미래(아직 안끝난 사람)

만약 자리를 예약한 이용자가 또 다른 자리를 이용하려고 한다면
유저 일치(조건1), CONFIRMED상태(조건2), 종료 시간남음(조건3)이
모두 충족하므로 이미 좌석을 이용중이라고 에러 발생
 */

    @Query("SELECT r " +
            "FROM Reservation r " +
            "JOIN FETCH r.seat "+
            "WHERE r.user.id = :userId " +
            "AND r.status = 'CONFIRMED' " +
            "AND r.endTime > :now")
    Optional<Reservation> findActiveReservation(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
    /* 특정 사용자(userId)가 현재 이용중인 예약 정보를 가져오는 쿼리
    Reservation 테이블의 모든정보(ID,좌석번호,시작시간,종료시간)에서
    특정 사용자(userId) + 현재 이용중 + 현재 상태(CONFIRMED)인 것만 가져옴

    없을 수도 있으므로 Optional로 반환받음
    Optional<Reservation>은 상자로 상자 안에 예약 정보가 있을 수도 없을 수도
     */
}






