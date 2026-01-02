# 1. Java 17 버전 환경 사용
FROM openjdk:17-jdk-slim

# 2. 작업 폴더 설정
WORKDIR /app

# 3. 빌드된 jar 파일 복사 (build/libs 안에 생성될 jar 파일을 app.jar로 복사)
COPY build/libs/*SNAPSHOT.jar app.jar

# 4. 실행 명령어 (환경 변수는 실행 시점에 주입받음)
ENTRYPOINT ["java", "-jar", "app.jar"]