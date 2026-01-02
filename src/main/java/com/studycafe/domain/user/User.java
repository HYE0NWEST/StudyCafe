/*
users 테이블 생성
 */
package com.studycafe.domain.user;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users") // 테이블 이름 지정(예약어인 user를 피해서 지음)
@Getter // 모든 필드에 대한 getId()같은 메서드 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 생성
public class User {
    @Id // 기본 키
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 키 생성을 DB에 위임, MySQL의 AUTO_INCREMENT 기능으로 자동으로 숫자가 1씩 증가
    private Long id;


    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String phone;
    private String email;

    @Column(updatable = false)
    private LocalDateTime createdAt; // 가입일(가입한 순간 고정됨)

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    /* @PrePersist : DB에 INSERT쿼리가 날아가기 직전에 자동으로 실행됨
    수동으로 user.setCreateAt()을 하지 않아도 저장하는 순간 자동으로 현재 시간이 기록됨
     */

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    /*
    사용자 정의 생성자
    기본 생성자를 protected로 막아서 외부에서는 이 생성자를 통해 필요한 데이터를 넣어서 객체 생성
     */
}
