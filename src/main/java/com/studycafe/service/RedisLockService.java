/*
동시에 2명이 같은 좌석을 예약하려고 할 때 1명만 성공시키고 나머지는 튕겨내는 역할
 */
package com.studycafe.service;

import com.studycafe.global.exception.CustomException;
import com.studycafe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service // Service 계층 코드 명시
@RequiredArgsConstructor // final붙은 필드 생성자 자동 생성
public class RedisLockService {
    private final RedisTemplate<String,String> redisTemplate;

    public boolean lockSeat(String seatNumber, String userId) {
        String key = "seat_lock:" + seatNumber;

        try {
            return Boolean.TRUE.equals(
                    redisTemplate
                            .opsForValue()
                            .setIfAbsent(key, userId, Duration.ofMinutes(5))
            );
        } catch (Exception e) {
            log.error("Redis 락 설정 중 오류 발생 - Seat: {}, User: {}, Error: {}", 
                    seatNumber, userId, e.getMessage());
            // Redis 오류 시 false 반환하여 예약 실패 처리
            return false;
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

    public boolean refreshLock(String seatNumber, String userId) {
        String key = "seat_lock:" + seatNumber;
        try {
            String currentOwner = redisTemplate.opsForValue().get(key);
            if (userId.equals(currentOwner)) {
                return Boolean.TRUE.equals(
                        redisTemplate.expire(key, Duration.ofMinutes(5))
                );
            }
            return false;
        } catch (Exception e) {
            log.error("Redis 락 갱신 중 오류 발생 - Seat: {}, User: {}, Error: {}", 
                    seatNumber, userId, e.getMessage());
            return false;
        }
    }
/* 락 연장
1. 키 생성 및 검문 검색
"seat_lock:" + 좌석번호 로 된 key 변수 생성
Redis에서 key라는 사물함을 열어서 안에 이름표가 어떻게 되어있는지 확인
락의 주인이 누구인지 조회하고 currentOwner에 저장

2. 신원 확인
신원 확인된 락 주인(currentOwner)가 요청한 유저(userId)와 같은지 비교
만약 null(락 만료)이거나 다르면(다른 사람임) false를 반환

3. 시간 연장(중요)
만약 본인이 맞다면 추가로 5분을 더 연장
이로써 DB 저장 직전에 락이 풀리는 상황 방지함

4. Redis 에러
만약 Redis 서버가 갑자기 오류가 나면 에러 메시지와 함께 false를 리턴해서
락 획득 실패라고 컨트롤러에게 알림

! 추가
redisTemplate는 스프링이 제공하는 Redis 리모컨으로 자바 객체를 Redis가 이해할 수
있는 데이터로 변환해서 통신을 대신 제공
.opsForValue()는 단순 문자열에 대한 작업 모드를 의미함
.get(key)는 실제로 데이터를 가져오는 명령어
 */

    public String getLockOwner(String seatNumber) {
        String key = "seat_lock:" + seatNumber;
        return redisTemplate.opsForValue().get(key);
    }
/*
현재 좌석 점유자 확인
        좌석번호(45)가 매개변수로 들어옴
        Redis에서 찾을 정확한 이름을 key(String)으로 저장(seat_lock:45)
        Redis에게 seat_lock:45라는 키에 들어있는 값(Value)을 요청
        만약 값이 있다면(선점중) userId가 반환되고 값이 없으면(빈자리) null이 반환됨
 */

    public void unlockSeat(String seatNumber) {
        String key = "seat_lock:" + seatNumber;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis 락 해제 중 오류 발생 - Seat: {}, Error: {}", 
                    seatNumber, e.getMessage());
            // 예외를 다시 던지지 않고 로그만 남김 (이미 예약이 완료된 경우 락 해제 실패해도 큰 문제 없음)
        }
    }
/*
좌석 잠금 해제
해당 key를 Redis에서 delete 처리하여 다른 사람이 다시 lockSeat을 시도할 때 성공하게 함

만약 Redis락을 풀려고 하는데 락이 응답이 없다면
어차피 락은 5분(TTL)뒤에 알아서 사라지므로 큰 문제가 생기지 않아 로그만 남김
 */

    public Map<Integer, String> getLockOwners(List<Integer> seatNumbers) {
        try {
            List<Object> results = redisTemplate.executePipelined(
                    (RedisCallback<Object>) connection -> {
                        for(Integer seatNum : seatNumbers) {
                            String key = "seat_lock:" + seatNum;
                            connection.get(key.getBytes());
                        }
                        return null;
                    }
            );

            Map<Integer, String> lockMap = new HashMap<>();
            for(int i = 0; i < results.size(); i++) {
                Object result = results.get(i);
                if(result != null) {
                    lockMap.put(seatNumbers.get(i), result.toString());
                }
            }
            return lockMap;
        } catch (Exception e) {
            log.error("Redis 파이프라인 조회 중 오류 발생: {}", e.getMessage());
            // 오류 시 빈 맵 반환
            return new HashMap<>();
        }
    }
/* 좌석 100개의 락 정보를 한번에 가져오는 메서드
Redis에게 100번 질문하는 것이 아닌 1번만 왔다갔다 하는 방식(Redis Pipelining)
1. 파이프라인 실행 시작
Redis에게 지금부터 명령 여러 개를 한꺼번에 줄 예정이고 결과도 한꺼번에 달라고 선언
Redis에게 모아뒀다가 한 번에 처리하라는 기능인 executePipelined를 사용

2. 명령 적재
RedisCallback(Redis호출)형태인 connection에 for문을 돌려서 명령을 차곡차곡
쌓음, 이때 seat_lock: + 좌석번호로 key를 생성하여 key에 대한 get 명령을 생성
이때 connection.get()을 호출해도 즉시 값을 반환하지 않고 조회할 목록에 명령을
추가하라는 의미

- * Redis로 1번 통신 * -

3. 결과 매핑(List -> Map 반환)
results 리스트에는 Redis가 조회한 순서대로 결과가 담겨 있음([null,"userA",null, //])
이때 키,값 형태인 lockMap을 선언(자리 번호를 알면 바로 주인을 알 수 있음)
result의 크기만큼 for문을 돌려서 results의 내용을 result 변수에 저장하고
만약 result가 null이 아니라면 누군가 이 자리에 락을 걸고 있다는 뜻이므로
몇 번 좌석의 주인은 누구의 형태로 맵에 저장(put)

4. 반환
{3 = "userA", 10 = "userB"}의 형태로 변환된 lockMap을 반환

5. Redis 에러
만약 좌석표를 로딩하는데 Redis가 죽었다면
여기서 에러가 터지면 메인 페이지가 모두 죽으므로 새로운 Map을 리턴하여
잠금 정보가 없는 상태로 로직이 진행됨
사용자는 좌석표를 볼 수 있고 적어도 서비스 접속 자체는 가능하게 함
 */


}

