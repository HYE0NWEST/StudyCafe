/*
회원가입 시 사용
 */
package com.studycafe.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JSON 파싱용(기본생성자 생성)
@AllArgsConstructor // 테스트용(모든생성자 생성)
public class SignupRequest {
    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;
}
