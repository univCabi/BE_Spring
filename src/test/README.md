# MySQL JPA 테스트 환경

## 기존 환경

- Spring Boot 3.4.3, Java 17 toolchain, JUnit 5, Spring Boot Test를 사용한다.
- JPA와 MySQL JDBC 드라이버가 이미 있으므로 테스트 의존성을 추가하지 않는다.
- 개발 설정은 `mysql_univcabi`, `ddl-auto=create`, `spring.sql.init.mode=always`이며 개발용 삭제/삽입 SQL을 실행한다.
- 기존 일부 테스트는 `@ActiveProfiles("test")`를 사용하지만 테스트 리소스 설정은 없었다.

## DB 준비

MySQL 관리자 계정으로 다음 SQL을 한 번 실행한다. 비밀번호는 로컬 테스트용 값으로 교체한다.
이 SQL은 자동 실행되지 않는다. 개발 스키마에 권한이 없는 테스트 전용 계정을 사용한다.

```sql
CREATE DATABASE IF NOT EXISTS mysql_univcabi_test CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'univcabi_test'@'localhost' IDENTIFIED BY 'replace-with-test-password';
GRANT ALL PRIVILEGES ON mysql_univcabi_test.* TO 'univcabi_test'@'localhost';
```

컨테이너/원격 서버에서는 접속 위치에 맞춰 계정의 호스트 부분을 지정한다.
프로젝트 루트의 `.env.test`에 테스트 계정을 설정한다. 이 파일은 Git에서 제외된다.
파일이 없는 새 체크아웃에서는 아래 내용으로 직접 생성한다.

```properties
TEST_MYSQL_USERNAME=univcabi_test
TEST_MYSQL_PASSWORD=replace-with-test-password
# 선택: TEST_MYSQL_HOST (기본 localhost), TEST_MYSQL_PORT (기본 3306)
```

`application-test.properties`의 `spring.config.import`가 이 파일을 properties 형식으로 읽는다.
값에 따옴표나 `export`를 붙이지 않는다. properties 형식의 역슬래시는 이스케이프해야 한다.
IntelliJ의 Working directory는 프로젝트 루트로 설정한다.
동일 이름의 터미널/IDE 환경 변수가 있으면 파일 값보다 우선하므로 이전 값을 삭제하거나 맞춘다.
CI에서는 파일 없이 `TEST_MYSQL_USERNAME`, `TEST_MYSQL_PASSWORD` 환경 변수를 직접 제공할 수도 있다.

개발용 `MYSQL_USERNAME`, `MYSQL_PASSWORD`는 재사용하지 않는다.
DB 이름을 `mysql_univcabi_test`로 고정하고 자동 DB 생성 옵션을 제외했다.
테스트 계정에는 이 스키마에만 권한을 부여해야 한다.

## 설정 적용 및 테스트 작성 시 참고

- 테스트 클래스패스의 `application.properties`가 개발 설정을 대신하고 `test` 프로필을 활성화한다. 개발 `.env`를 Spring 설정으로 import하지 않는다.
- `application-test.properties`는 실제 MySQL 연결, `create-drop`, 개발 초기화 SQL 비활성화를 설정한다. 테이블은 컨텍스트 시작 시 재생성하고 종료 시 삭제하므로 이 스키마는 테스트 전용이다.
- `spring.test.database.replace=none`으로 향후 JPA 슬라이스 테스트도 실제 MySQL을 사용한다.
- `removeBookmarkByCabinetId()`는 트랜잭션 안에서 엔티티의 삭제 시각을 변경한다. 향후 `@DataJpaTest`와 `@Import(CabinetBookmarkService.class)`로 서비스와 실제 저장소를 함께 테스트할 수 있다. 필요한 사용자/건물/캐비닛/북마크를 직접 생성하고, 호출 후 flush/clear 및 재조회로 변경 감지에 의한 저장을 검증한다.
- `create-drop`은 컨텍스트 단위이므로 테스트 메서드 사이 격리는 별도 트랜잭션 롤백/정리가 필요하다. 동일 스키마를 사용하는 테스트 프로세스를 동시에 실행하지 않는다.
- 기존 `@SpringBootTest`는 Redis/Kafka 구성과 시작 로직도 로드한다. 제공한 테스트용 JWT 값은 인증 설정 의존성만 해결한다. Redis는 기본 DB 15를 사용하며 `TEST_REDIS_HOST/PORT`, Kafka는 `TEST_KAFKA_BOOTSTRAP_SERVERS`로 테스트 서버를 지정할 수 있다. Kafka optional 설정이 사용자 정의 리스너나 헬스 체크를 끄지는 않는다. JPA만 검증할 때는 위 슬라이스 구성을 권장한다.

DB 준비 후 기존 북마크 테스트의 컨텍스트 로딩 확인 명령:

```sh
./gradlew test --tests 'org.univcabi.univcabi.cabinet.service.CabinetBookmarkServiceIntegrationTest'
```

전체 테스트 중 Redis/Kafka 또는 개발 초기 데이터에 의존하는 테스트는 별도 서버와 테스트 데이터 준비가 필요하다.
