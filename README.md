# demo.todo

## Environment Setup

1. `.env.example`를 복사해 `.env`를 만들고 값을 채웁니다.
2. 개발 환경을 기본으로 사용하려면 `.env`에 `spring.profiles.active=dev`를 설정합니다.
3. 애플리케이션 실행 시 Spring은 `application.properties`의 `spring.config.import` 설정을 통해 `.env`와 선택된 프로필 파일을 함께 로드합니다.

## Profiles

- `application-dev.properties`: 로컬 개발 기본 설정
- 추후 스테이징/프로덕션 설정은 `application-staging.properties`, `application-prod.properties` 등으로 추가할 수 있습니다.

프로필 전환은 `spring.profiles.active` 속성이나 `--spring.profiles.active` 파라미터를 사용하세요.
