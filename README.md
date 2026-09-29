# docgen

`docgen`은 HWPX 템플릿의 이름 있는 토큰을 치환하는 작은 Java 8 라이브러리이자 명령줄 도구입니다.

주간보고 자동화 프로젝트와는 별개의 독립 프로젝트입니다. 공개 저장소에는 재사용 가능한 문서 생성 핵심 코드, 테스트, 그리고 민감정보를 제거한 최소 예제만 포함합니다.

## 주요 기능

- HWPX ZIP 압축 파일 읽기 및 생성
- `Contents/section*.xml` 파일의 `{{token}}` 플레이스홀더 치환
- 여러 HWPX 텍스트 런으로 나뉜 플레이스홀더 처리
- 기본적으로 누락 토큰을 오류로 처리하고, `--allow-missing`으로 미치환 상태 허용
- `--force` 없이는 기존 출력 파일을 덮어쓰지 않음
- ZIP Slip 공격 및 XML 외부 엔티티 확장을 방어
- Java API와 실행 가능한 명령줄 JAR 제공

## 요구 사항

- 실행: Java 8 이상
- 빌드: Maven 3.8 이상

## 빌드

```bash
mvn test
mvn package
```

실행 가능한 JAR는 `target/docgen-0.1.0.jar`에 생성됩니다.

## 명령줄 사용법

```bash
java -jar target/docgen-0.1.0.jar \
  --template examples/minimal/template.hwpx \
  --data examples/minimal/tokens.json \
  --output result.hwpx
```

입력 JSON은 값이 모두 문자열인 평면 객체여야 합니다.

```json
{
  "title": "Example document",
  "author": "docgen"
}
```

지원 옵션:

- `--template <file>`: 원본 HWPX 템플릿
- `--data <file>`: 평면 JSON 토큰 맵
- `--output <file>`: 생성할 HWPX 경로
- `--allow-missing`: 알 수 없는 플레이스홀더를 그대로 둠
- `--force`: 기존 출력 파일을 덮어씀
- `--help`: 사용법 출력

종료 코드는 다음과 같습니다. `0`은 성공, `2`는 인자 또는 입력 오류, `3`은 잘못된 HWPX/XML, `4`는 토큰 또는 JSON 검증 오류, `5`는 출력을 안전하게 쓸 수 없는 경우입니다.

## Java API

기본 생명주기는 다음과 같습니다.

```java
HwpxDocument document = HwpxDocument.open(templatePath);
try {
    ReplacementResult result = document.replaceTokens(tokens, false);
    document.write(outputPath, false);
} finally {
    document.close();
}
```

토큰 이름에는 영문자, 숫자, `_`, `-`, `.`를 사용할 수 있습니다. 토큰 값은 문자열이어야 합니다. 이 라이브러리는 HWPX의 섹션 XML 파일을 직접 처리하므로 템플릿을 만든 별도 애플리케이션에 의존하지 않습니다.

## 예제

간단한 HWPX 예제와 토큰 데이터는 [`examples/minimal`](examples/minimal)에서 확인할 수 있습니다. `docgen`의 형식 처리 흐름을 확인하기 위한 예제이며, 실제 사무용 문서 템플릿 전체를 제공하는 것은 아닙니다.

## 보안 참고 사항

템플릿은 신뢰할 수 없는 입력으로 취급합니다. 압축 해제 시 대상 디렉터리 밖으로 벗어나는 ZIP 엔트리는 거부합니다. XML 파싱에서는 DTD, 외부 엔티티, 외부 스키마, XInclude를 비활성화합니다. 생성 결과는 임시 압축 파일을 거쳐 기록하며, 기본적으로 기존 파일을 덮어쓰지 않습니다.

## 라이선스

이 프로젝트는 Apache License 2.0으로 배포합니다. 자세한 내용은 [`LICENSE`](LICENSE)와 [`THIRD-PARTY-NOTICES`](THIRD-PARTY-NOTICES)를 확인하세요.
