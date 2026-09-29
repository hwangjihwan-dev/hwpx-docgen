# 최소 HWPX 예제

이 디렉터리에는 플레이스홀더 두 개와 이에 대응하는 JSON 토큰 맵을 포함한, 민감정보를 제거한 작은 HWPX 압축 파일이 있습니다.

프로젝트 루트에서 빌드한 뒤 다음 명령을 실행하세요.

```bash
java -jar target/docgen-0.1.0.jar \
  --template examples/minimal/template.hwpx \
  --data examples/minimal/tokens.json \
  --output result.hwpx
```

이 파일은 `docgen`의 HWPX 형식 처리 흐름을 확인하기 위한 최소 예제입니다. 실제 사무용 문서 작성용 템플릿 전체를 제공하는 것은 아닙니다.
