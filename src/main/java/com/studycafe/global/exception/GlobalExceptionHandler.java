/*
서버 전체의 에러를 책임지는 최종 해결사 역할 수행하는 컨트롤러
에러가 발생하면 이 Handler가 에러를 구분해서 다른 메서드를 호출해서 처리
 */
package com.studycafe.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 모든 컨트롤러를 전역으로 제어가능
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }
    /* 내가 만든 에러(CustomExceptino) 처리
    @ExceptionHandler는 CustomException이 터지면 이 메서드로 부름
    의도적으로 throw new CustomException을 했을 때 실행되고
    내가 정한 메시지가 반환됨

    에러 객체(e)로 받고 이 객체에서 getErrorCode()로 Service가 보낸 에러 딱지를 꺼냄
    ErrorResponse의 toResponseEntity를 호출하여 JSON응답을 리턴
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult().getFieldError().getDefaultMessage();

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("BAD_REQUEST")
                        .code("INVALID_INPUT_VALUE")
                        .message(message)
                        .build()
                );
    }
    /* @Valid 어노테이션을 사용한 유효성 검사가 실패했을 때 에러 처리
    @ExceptionHandler를 통해 @Email 등 DTO 검증에 실패했을 때 이 메서드로 이동

    getBindingResult()를 통해서 검사 결과표를 가져오고
    getFieldError()를 통해서 틀린 필드 1개를 가져오고
    getDefaultMessage()를 통해서 DTO에 적어둔 1번째(ex. message)속성값을 가져옴
    ex. @Size(min = 8, message = "비밀번호는 8자 이상입니다.)라면
    message는 저 문자열이 들어감

    꺼내온 문자열 메시지를 내가 만든 ErrorResponse JSON 문자열 형태에 담아서 프론트엔드로 전송
     */

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        e.printStackTrace();
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    /* 이외의 모든 에러 처리
    NullPointerException, IndexOutOfBoundsException처럼 내가 예상치 못한
    에러 발생시 이 메서드로 부름
    e.printStackTrace()로 콘솔에 에러 로그를 찍음(보안상)
    서버 에러(500)이라고 퉁쳐서 반환(보안상)
     */


}
