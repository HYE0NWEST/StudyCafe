/*
데이터를 주고받을 바구니 역할
 */
package com.studycafe.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;


public class ReservationDto {
    // 좌석 선점(Lock) 요청용 Dto
    @Getter
    @NoArgsConstructor
    public static class PreOccupyRequest{
        @NotNull(message = "사용자 ID는 필수입니다")
        private Long userId; // 사용자 ID
        // 빈 문자열과 공백은 통과되고 null만 막음
        
        @NotNull(message = "좌석 번호는 필수입니다")
        @Positive(message = "좌석 번호는 양수여야 합니다")
        private Integer seatNumber; // 좌석 번호
        // 숫자가 0보다 커야만 통과
    }

    // 예약 확정 요청용 Dto
    @Getter
    @NoArgsConstructor
    public static class ReserveRequest{
        @NotNull(message = "사용자 ID는 필수입니다")
        private Long userId; // 사용자 ID
        
        @NotNull(message = "좌석 번호는 필수입니다")
        @Positive(message = "좌석 번호는 양수여야 합니다")
        private Integer seatNumber; // 좌석 번호
        
        @NotNull(message = "이용 시간은 필수입니다")
        @Min(value = 1, message = "이용 시간은 최소 1시간 이상이어야 합니다")
        private Integer hours; // 이용 시간
        // 숫자가 지정한 값인 1보다 크거나 같아야 함
    }
}
