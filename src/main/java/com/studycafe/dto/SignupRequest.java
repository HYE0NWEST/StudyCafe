/*
회원가입 시 사용
 */
package com.studycafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JSON 파싱용(기본생성자 생성)
@AllArgsConstructor // 테스트용(모든생성자 생성)
public class SignupRequest {
    private String username;
    private String password;
    private String email;
}
