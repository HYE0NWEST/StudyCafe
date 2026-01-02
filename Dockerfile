# 1. Base Image: Java 23 (안정적인 Eclipse Temurin 버전 사용)
FROM eclipse-temurin:17-jdk

# 2. 작업 폴더 설정
WORKDIR /app

# 3. 빌드된 JAR 파일을 도커 내부로 복사
# (build/libs 폴더 안에 있는 jar 파일을 app.jar라는 이름으로 복사)
COPY build/libs/*.jar app.jar

# 4. 실행 명령어 (스프링 부트 실행)
ENTRYPOINT ["java", "-jar", "app.jar"]
