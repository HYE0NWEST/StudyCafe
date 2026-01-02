/*
users,seats 테이블을 외래키로 참조하여 reservations 테이블 생성
 */
package com.studycafe.domain.reservation;

import com.studycafe.domain.seat.Seat;
import com.studycafe.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_seat_time", columnList = "seat_id, start_time"),
        @Index(name = "idx_start_time", columnList = "start_time")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/*
idx_seat_time : seat_id + start_time
특정 좌석(seat_id)이 특정 시간(start_time)에 이미 예약 되어 있는지 확인할 때 사용(중복 예약 방지)

idx_start_time : start_time
시간순으로 조회할 때 속도를 높여 줌
 */
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false)
    private LocalDateTime startTime; // 시작 시간

    @Column(nullable = false)
    private LocalDateTime endTime; // 종료 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;// 상태 표시

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ReservationStatus { // 상태 표시(열거형(enum))
        CONFIRMED, CANCELLED, COMPLETED
        // 예약 확정, 예약 취소, 이용 완료
    }


    public Reservation(User user, Seat seat, LocalDateTime startTime, LocalDateTime endTime, ReservationStatus status) {
        this.user = user;
        this.seat = seat;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public void cancel() {

        this.status = ReservationStatus.CANCELLED;
    }
    // 예약 취소(이용종료) 메서드

}
/*
    @ManyToOne은 N:1의 관계를 표현할 때 사용
    User와는 1명의 회원은 여러 개의 예약을 할 수 있으므로 N:1
    Seat와는 1개의 좌석에는 시간대별로 여러 예약이 걸릴 수 있으므로 N:1

    fetch = FetchType.LAZY : 지연 로딩
    예약을 조회할 때 사용자 정보는 가짜객체(Proxy)만 넣어두고 실제로 user.getName()처럼 데이터를
    사용할 때 DB쿼리를 날림

    <-> fetch = FetchType.EAGER(즉시 로딩) : 예약목록과 함께 관련된 사용자 정보,좌석 정보를
    모두 가져와서 불필요한 쿼리가 계속 발생하는 N+1문제와 조인 쿼리가 발생함

    @JoinColumn : 외래키 설정
    참조하는 테이블(Users,Seats)의 기본키인 user_id,seat_id 컬럼을 생성해서 외래키로 사용

    @Enumberated(EnumType.STRING) : DB에 저장 시 문자열 그대로 저장
    기본값을 사용하면 숫자(0,1,2..)가 들어가지만 중간에 enum 순서가 바뀌거나
    중간에 새로운 상태가 추가되면 데이터가 꼬임
     */
