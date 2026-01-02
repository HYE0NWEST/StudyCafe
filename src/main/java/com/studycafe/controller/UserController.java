package com.studycafe.controller;

import com.studycafe.domain.user.User;
import com.studycafe.domain.user.UserRepository;
import com.studycafe.dto.LoginDto;
import com.studycafe.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController // 컨트롤러임을 선언
@RequestMapping("/api/auth") // 이 컨트롤러의 모든 기능은 /api/auth로 시작
@RequiredArgsConstructor // final이 붙은 필드 생성자 생성
public class UserController {
    private final UserRepository userRepository; // 의존성 주입

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login (@RequestBody LoginDto loginDto) {
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디"));

        if(!user.getPassword().equals(loginDto.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않음");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());

        return ResponseEntity.ok(response);
    }
    /* 로그인 구현(로그인 성공 시 아이디 + 닉네임 형태로 반환)
    Key,value형태로된 Map을 사용하여 userId가 key, 닉네임(username)이 value로 설정

    ResponseEntity<Map<String,Object>>이 반환타입이 되고
    사용자로부터 받은 데이터를 @RequestBody를 통해 LoginDto 객체로 반환하고 user에 저장

    userRepository.findByUsername으로 DB에서 사람을 찾고 비밀번호가 맞는지 확인

    새로운 HashMap response를 생성하여 user에서 userId,username을 받아서
    Map인 response에 저장하고 이를 ResponseEntity.ok상태와 함께 반환

     */

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        if(userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다");
        }

        User user = new User(
                request.getUsername(),
                request.getPassword(),
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
