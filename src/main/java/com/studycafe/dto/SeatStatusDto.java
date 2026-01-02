/*
프론트에서 좌석에 누가 앉아있고 누가 선점했는지 알아야 함
좌석 정보를 내려주는 역할
 */
package com.studycafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class SeatStatusDto {
    private Integer seatNumber;
    private String status; // "AVAILABLE", "LOCKED", "OCCUPIED"
}
