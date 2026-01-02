/*
사용자가 보내는 요청을 제일 먼저 받는 역할
 */
package com.studycafe.controller;

import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import com.studycafe.dto.ReservationDto;
import com.studycafe.dto.SeatStatusDto;
import com.studycafe.service.ReservationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 데이터(JSON)을 주는 컨트롤러임을 선언
@RequestMapping("/api/reservations") // 이 컨트롤러안의 모든 기능은 /api/reservations로 시작
@RequiredArgsConstructor // final이 붙은 필드 생성자 자동 생성
public class ReservationController {
    private final ReservationService reservationService; // 서비스 객체 의존성 주입
    
    // 좌석 선점(임시 점유) API
    @PostMapping("/pre-occupy") // POST /api/reservations/pre-occupy
    public ResponseEntity<String> preOccupySeat(
            @RequestBody ReservationDto.PreOccupyRequest request) {

        try {
            String result = reservationService
                    .preOccupySeat(request.getUserId(), request.getSeatNumber());
            return ResponseEntity.ok(result);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    /* 좌석 선점 요청(/pre-occupy) : 사용자가 좌석을 클릭했을 때 호출되는 API
    ResponseEntity<String> : 글자와 함께 HTTP 상태코드를 같이 조절해서 보내주는 포장지 역할
    @RequestBody : 사용자가 보낸 JSON데이터 객체를 JAVA 객체(PreOccupyRequest)로 변환

    try : Service에게 Redis로 가서 선점 요청을 시킴
    Redis에서 좌석 선점 요청을 한 결과를 result에 저장
    이때 변환된 JAVA 객체인 request에서 UserId, SeatNumber를 가져와서 매개변수에 넣음

    만약 성공하면 200 OK라는 Http 상태코드와 함께 결과 메시지 전송
    만약 실패하면 400 Bad Request라는 Http 상태코드와 함께 에러 전송
    e.getMessage()에는 "이미 선택된 좌석입니다"라는 같은 문구가 들어감
     */


    // 예약 확정(결제 후 DB 저장) API
    @PostMapping("/confirm") // POST /api/reservations/confirm
    public ResponseEntity<String> confirmReservation(
            @RequestBody ReservationDto.ReserveRequest request) {

        try {
            Long reservationId = reservationService.confirmReservation(
                    request.getUserId(),
                    request.getSeatNumber(),
                    request.getHours()
            );
            return ResponseEntity.ok("예약이 확정되었습니다. 예약 ID : " + reservationId);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    /* 예약 확정 요청(/confirm) : 사용자가 결제까지 마치고 최종 확인 버튼을 눌렀을 때 호출되는 API
ResponseEntity<String> : 글자와 함께 HTTP 상태코드를 같이 조절해서 보내주는 포장지 역할
사용자로부터 데이터를 받고 @RequestBody로 JAVA 객체(ReserveRequest)로 변환

try : Service에게 진짜 저장을 시킴(DB 저장 + Redis 락 해제)
Service에게 데이터를 진짜 DB에 INSERT하도록 지시

만약 성공하면 예약 ID를 포함해서 응답 메시지 전송
만약 실패(시간 초과, 본인이 아님)하면 에러 전송
e.getMessage()에는 "시간이 초과되었거나 다른 사람입니다"라는 같은 문구가 들어감
 */



    // 좌석 현황판 요청 API
    @GetMapping("/seats")
    public ResponseEntity<List<SeatStatusDto>> getSeatStatus() {
        return ResponseEntity.ok(reservationService.getAllSeatStatus());
    }
    /* 좌석 현황판 요청(/seats, get(데이터조회))
reservationService의 메서드를 호출해서 DB(이용중), Redis(결제 중)인 사람들을
뒤져서 100개의 좌석 리스트를 생성받고 이를 ResponseEntity.ok()로
상태코드 200 성공 도장을 찍어서 전송

ResponseEntity는 HTTP응답을 감싸는 포장지 역할로 데이터와 함께 상태코드를 전송
 */



    // 취소 시 즉시 락 해제 요청 API
    @PostMapping("/cancel") // POST /api/reservations/cancel
    public ResponseEntity<String> cancelPreOccupy(
            @RequestBody ReservationDto.PreOccupyRequest request) {

        reservationService.cancelPreOccupy(request.getSeatNumber());
        return ResponseEntity.ok("선점 취소되었습니다.");
    }
    /*
reservationService가 Redis에게 현재 좌석 lock(선점)을 즉시 풀으라고 요청
Redis에서 해당 좌석의 키가 즉시 삭제되어 다른 사용자가 즉시 선택 가능
 */

    @PostMapping("/end-use")
    public ResponseEntity<String> endUse(
            @RequestBody ReservationDto.PreOccupyRequest request) {
        reservationService.endUse(request.getUserId());
        return ResponseEntity.ok("이용이 종료되었습니다");
    }
    /* 사용자로부터 퇴실 요청을 받아서 reservationService에게 넘겨주는 역할
    @RequestBody를 사용하여 퇴실 요청 데이터를 DTO객체로 변환하고 이때
    좌석선점시 사용한 DTO(PreOccupyRequest)를 사용(userId, seatNumber)

     */

    @GetMapping("/my-seat")
    public ResponseEntity<Integer> getMySeat(@RequestParam Long userId) {
        Integer seatNumber = reservationService.getCurrentSeatNumber(userId);
        return ResponseEntity.ok(seatNumber);
    }
    /* 이 유저의 좌석 번호를 알아보게 하는 역할
        reservationService의 getCurrentSeatNumber 메서드를 호출
        getCurrentSeatNumber 메서드는 현재 이용자의 좌석 번호를 반환
        현재 좌석 번호와 함께 200OK를 반환
     */
}

