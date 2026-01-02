/*
데이터를 주고받을 바구니 역할
 */
package com.studycafe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;


public class ReservationDto {
    // 좌석 선점(Lock) 요청용 Dto
    @Getter
    @NoArgsConstructor
    public static class PreOccupyRequest{
        private Long userId; // 사용자 ID
        private Integer seatNumber; // 좌석 번호
    }

    // 예약 확정 요청용 Dto
    @Getter
    @NoArgsConstructor
    public static class ReserveRequest{
        private Long userId; // 사용자 ID
        private Integer seatNumber; // 좌석 번호
        private int hours; // 이용 시간
    }
}
