/*
서버에서 발생할 수 있는 모든 에러 상황을 미리 정의해 둔 에러 코드들
 */
package com.studycafe.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor // 생성자 자동 생성
@Getter // 안에 있는 내용을 꺼내 쓸 수 있게 함
public enum ErrorCode {
    /*
    일반적인 클래스(class)가 아닌 열거형(enum)으로 구현하여 이 안에 적힌
    INVALID_PASSWORD 등은 변수가 아니라 고정된 상수(상태값)들임
     */

    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_LOCK(HttpStatus.BAD_REQUEST, "좌석 선점 시간이 만료되었거나 본인의 선점이 아닙니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"사용자를 찾을 수 없습니다"),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND,"좌석 정보를 찾을 수 없습니다"),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND,"현재 이용중인 예약이 없습니다"),

    DUPLICATE_USERNAME(HttpStatus.CONFLICT,"이미 존재하는 아이디입니다"),
    SEAT_ALREADY_OCCUPIED(HttpStatus.CONFLICT,"이미 이용중인 좌석입니다"),
    SEAT_ALREADY_LOCKED(HttpStatus.CONFLICT,"다른 사용자가 결제 중인 좌석입니다"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"서버에 오류가 발생했습니다");

    private final HttpStatus status;
    private final String message;
}
/* 에러 목록
잘못된 요청(400) : INVALID_PASSWORD, INVALID_INPUT_VALUE, INVALID_LOCK
찾을 수 없음(404) : USER_NOT_FOUND, SEAT_NOT_FOUND, RESERVATION_NOT_FOUND
충돌 및 중복(409) : DUPLICATE_USERNAME, SEAT_ALEADY_OCCUPIED, SEAT_ALREADY_LOCKED
>> 아이디 중복, 누군가 내 자리를 예약하려고 할 떄
>> + 2명의 사용자가 동시에 한 자리를 예약하려고 할 때
서버 에러(500) : INTERVAL_SERVER_ERROR


필드(저장하는 값)
status는 컴퓨터(브라우저)가 알아들을 수 있는 HTTP 상태코드(ex. 404,400)
message는 사용자(사람)에게 보여줄 설명
! 이때 열거형(enum)은 일반 클래스와 달리 상수 목록(상태값들)이 먼저 나와야 한다는 규칙이 존재
 */

