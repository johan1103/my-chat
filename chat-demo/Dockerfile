# 1. Java 17 기반 이미지
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 생성 및 설정
WORKDIR /app

# 3. Gradle Wrapper와 소스 파일을 컨테이너로 복사
COPY . .

# 4. Gradle 빌드 실행
RUN ./gradlew clean shadowJar

# 5. Fat JAR 복사
RUN cp build/libs/*-all.jar app.jar

# 6. 컨테이너에서 애플리케이션 실행
CMD ["java", "-jar", "app.jar"]
