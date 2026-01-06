package com.studycafe.controller;

import com.studycafe.domain.user.User;
import com.studycafe.domain.user.UserRepository;
import com.studycafe.dto.LoginDto;
import com.studycafe.dto.SignupRequest;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.studycafe.config.jwt.JwtTokenProvider;

import java.util.HashMap;
import java.util.Map;

@RestController // 컨트롤러임을 선언
@RequestMapping("/api/auth") // 이 컨트롤러의 모든 기능은 /api/auth로 시작
@RequiredArgsConstructor // final이 붙은 필드 생성자 생성
public class UserController {
    private final UserRepository userRepository; // 의존성 주입
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login (@RequestBody LoginDto loginDto) {
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디"));

        if(!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않음");
        }

        String token = jwtTokenProvider.createToken(String.valueOf(user.getId()));

        Map<String, Object> response = new HashMap<>();

        response.put("username", user.getUsername());
        response.put("token", token);

        return ResponseEntity.ok(response);
    }
    /* 로그인 구현(로그인 성공 시 아이디 + 닉네임 형태로 반환)
    사용자가 보낸 정보(ID/PW)인 JSON 문자열을 @RequestBody가 LoginDto로 DTO 객체로 변환

    userRepository의 findByUsername메서드를 DTO 객체의 Username을 매개변수로 하여 DB에서 유저 조회
    만약 유저 없으면 RuntimeException 발생

    loginDto.getPassword()는 사용자가 입력한 PW이고 user.getPassword()는 DB에 저장된 암호화된 PW
    passwordEncoder.matches()는 내부적으로 사용자가 입력한 PW를 암호화해보고
    그 결과가 DB에 있는 암호화된 값과 같은지 확인하고 아니면 RuntimeException 발생

    유저의 기본키(ID)를 가지고 jwtTokenProvider의 createToken메서드를 호출해서 토큰 객체 생성

    응답 데이터를 담을 HashMap객체를 생성하고 response.put()으로 키와 값을 넣음
    key : "username", value : 유저 아이디
    key : "token", value : 방금 만든 JWT 토큰

    200 OK와 함께 Map 객체 반환
     */

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        if(userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail()
        );

        userRepository.save(user);

        return ResponseEntity.ok("회원가입 완료, 로그인하세요");
    }
    /* 사용자가 아이디, 비밀번호, 이메일을 적어서 가입
    프론트엔드에서 보낸 JSON 데이터를 @RequestBody를 통해 SignupRequest라는 자바 객체 DTO로 변환해서 받음

    if문으로 DB에게 userRepository의 findByUsername으로 아이디 가진 사람을 찾음
    결과는 Optional로 받고 만약 Optional에 isPresent로 true라면 누군가 아이디를 쓰고 있음
    그러므로 ResponseEntity.badRequest()로 오류와 함께 에러 메시지 전송

    입력받은 DTO(request)를 DB에 실제 저장할 수 있는 엔티티로 변환하고 user에 저장

    userRepository.save(user)메서드로 실제 DB에 INSERT 쿼리가 들어감

    ResponseEntity.ok()로 Ok와 함께 성공 메시지 전송
     */

}
