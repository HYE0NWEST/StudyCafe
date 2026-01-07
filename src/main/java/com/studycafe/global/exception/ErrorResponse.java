/*
에러가 발생하면 프론트엔드에게 보내줄 공식 에러 공지문 양식
JSON 문자열 형태로 전송하기 위해서 제작했고 DTO 객체임

GlobalExceptionHandler에서 보낸 에러 딱지를 바탕으로 깔끔한 JSON 문자열 생성
 */
package com.studycafe.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter // Getter가 있어야 JSON으로 변환함
@Builder // 객체 생성시 .을 사용하여 깔끔하게 생성가능함
public class ErrorResponse {
    private int status;
    private String error;
    private String code;
    private String message;
/* 필드 구성
status : HTTP 상태 코드 숫자(404,400)
error : HTTP 상태 코드 이름(BAD_REQUEST,NOT_FOUND)
code : ErrorCode의 이름(INVALID_PASSWORD, DUPLICATE_USERNAME)
message : 사용자에게 보여줄 메시지
프론트엔드는 code를 보고 분기 처리
 */
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .error(errorCode.getStatus().name())
                        .code(errorCode.name())
                        .message(errorCode.getMessage())
                        .build()
                );
    }
    /* 자동 변환기(ErrorCode Enum만 있어도 알아서 ResponseEntity로 변환)
    static인 이유는 어디서든 ErrorResponse.toResponseEntity()로 호출 가능하게 함

    header에는 HTTP 상태코드(404,400)를 넣음
    body에는 앞서 정헀던 필드들 4개를 모두 넣고 .build()하여
    ResponseEntity<> DTO 객체로 생성
     */
}
