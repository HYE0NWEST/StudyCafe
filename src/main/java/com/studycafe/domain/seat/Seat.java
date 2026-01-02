/*
seats 테이블 생성
 */
package com.studycafe.domain.seat;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "seats") // seats 테이블 이름 지정
@Getter // 모든 필드에 get 메서드 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 생성
public class Seat {
    @Id // 기본키
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동으로 1 증가
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer seatNumber;
    // 겹치면 안되고 null일 수 없음

    public Seat(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    // PROTECTED이기 때문에 외부에서는 이 생성자로 객체 생성
}
