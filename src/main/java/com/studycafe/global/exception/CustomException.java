/*
ErrorCode를 전송하기 위한 운송 수단
 */
package com.studycafe.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CustomException extends RuntimeException{
    private final ErrorCode errorCode;
}
/*
RuntimeException을 상속받음, RuntimeException은 자바에서 예외 처리를 강제하지않아
Service코드가 깔끔해지게 됨
ErrorCode는 에러 발생 시 그 원인(ErrorCode Enum)을 저장함
 */
