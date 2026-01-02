/*
동시에 2명이 같은 좌석을 예약하려고 할 때 1명만 성공시키고 나머지는 튕겨내는 역할
 */
package com.studycafe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service // Service 계층 코드 명시
@RequiredArgsConstructor // final붙은 필드 생성자 자동 생성
public class RedisLockService {
    private final RedisTemplate<String,String> redisTemplate;

    public boolean lockSeat(String seatNumber, String userId) {
        String key = "seat_lock:" + seatNumber;

        return Boolean.TRUE.equals(
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(key,userId, Duration.ofMinutes(5))
        );
    }

    public void unlockSeat(String seatNumber) {
        String key = "seat_lock:" + seatNumber;
        redisTemplate.delete(key);
    }
    /* 좌석 잠금 해제
    해당 key를 Redis에서 delete 처리하여 다른 사람이 다시 lockSeat을 시도할 때 성공하게 함
     */

    public String getLockOwner(String seatNumber) {
        String key = "seat_lock:" + seatNumber;
        return redisTemplate.opsForValue().get(key);
    }

}
/* 좌석 잠금
String key = "seat_lock: + seatNumber;
Redis에 저장할 키 이름을 생성(10번 좌석 >> key = "seat_lock:10")
앞에 접두사를 붙이는 이유는 다른 데이터와 섞이지 않게 하기 위함

opsForValue()
Redis의 String 타입 데이터를 다루겠다는 의미

setIfAbsent(key, userId, Duration.ofMinutes(5))
Redis 명령어인 SETNX를 실행 : 이 key(seat_lock:10)이 없을 때만 데이터 저장
만약 성공 시 키가 없어서 저장에 성공하면 true를 반환(lock 획득 성공)
만약 실패 시 이미 누군가 키를 만들어놨다는 의미이고 누군가 먼저 찜해뒀다는 의미이므로
저장하지 않고 false를 반환
SETNX : SET if not exists(없으면 저장해라)

Duration.ofMinutes(5)
서버가 락을 걸어놓고 락을 영원히 못 풀면 영구적으로 예약 불가 상태이므로 이를 방지함

Boolean.TRUE.equals()
setIfAbsent는 Boolean을 반환하므로 혹시모를 null 에러를 방지하고 true/false로 바꾸기 위함
 */

/* 예시
사용자 A가 0.001초 빨라서 lockSeat("1", "userA")가 실행되어
Redis는 키가 없었으므로 true를 리턴하여 찜 성공 >> 결제 화면으로 넘어감
사용자 B가 0.001초 느려서 lockSeat("1", "userB")가 실행되지만
Redis는 이미 key seat_lock:1이 이미 있으므로 false를 리턴 >> 오류 메시지 출력
 */

/*
JAVA의 synchronized는 서버가 여러 대로 늘리면 각 서버 안에서만 잠금이 걸려 동시서 제어 불가능
Redis는 외부 DB로 서버가 100대 이상이어도 동시성 제어 가능
 */

  /* 현재 좌석 점유자 확인
    좌석번호(45)가 매개변수로 들어옴
    Redis에서 찾을 정확한 이름을 key(String)으로 저장(seat_lock:45)
    Redis에게 seat_lock:45라는 키에 들어있는 값(Value)을 요청
    만약 값이 있다면(선점중) userId가 반환되고 값이 없으면(빈자리) null이 반환됨
     */