/*
회원 정보를 DB에서 넣고 빼는 역할을 담당하는 저장소
 */
package com.studycafe.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
}
/*
SELECT *
FROM users
WHERE username = ?
아이디를 보고 이 회원이 존재하는 지 검색
반환타입을 Optional<User>로 정한 이유는 있을 수도 있고 없을 수도 있음을 나타냄

*과정
메서드 이름 : find
데이터를 조회(SELECT)하는 것으로 파악하야 SELECT * FROM까지 쿼리 생성

제네릭타입<User,Long>
대상 테이블은 users테이블 >> SELECT * FROM users까지 쿼리 생성

By
이제부터 조건(WHERE)이 나옴

Username
user 엔티티 안에 username이라는 필드가 존재하여 조건 컬럼은 username
SELECT * FROM users WHERE username = ?;

String username
이 매개변수에 들어온 값을 쿼리의 ? 에 넣음

 */
