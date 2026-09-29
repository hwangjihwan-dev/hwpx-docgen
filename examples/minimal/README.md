# 최소 HWPX 예제

이 디렉터리에는 플레이스홀더 두 개와 이에 대응하는 JSON 토큰 맵을 포함한, 민감정보를 제거한 작은 HWPX 압축 파일이 있습니다.

## 실행 방법

먼저 프로젝트 루트에서 빌드합니다.

```powershell
mvn package
```

그 다음 PowerShell에서 다음 명령을 실행합니다.

```powershell
java -jar .\target\docgen-0.1.0.jar `
  --template .\examples\minimal\template.hwpx `
  --data .\examples\minimal\tokens.json `
  --output .\output\example-result.hwpx `
  --force
```

macOS, Linux 또는 Git Bash에서는 다음처럼 실행할 수 있습니다.

```bash
java -jar ./target/docgen-0.1.0.jar \
  --template ./examples/minimal/template.hwpx \
  --data ./examples/minimal/tokens.json \
  --output ./output/example-result.hwpx \
  --force
```

`output/example-result.hwpx`가 생성되면 한/글에서 열어 결과를 확인합니다. 이 예제의 템플릿에는 `{{title}}`, `{{author}}` 토큰이 있고, [`tokens.json`](tokens.json)에 있는 값으로 각각 치환됩니다.

이 파일은 한/글에서 열 수 있는 HWPX 패키지 구조를 갖춘 최소 예제입니다. `docgen`의 기본 처리 흐름을 확인하기 위한 용도이며, 실제 업무용 문서 작성용 템플릿 전체를 제공하는 것은 아닙니다.

## 내 템플릿으로 바꾸기

1. 한/글에서 문서를 작성하고 값이 들어갈 자리에 `{{token_name}}`을 입력합니다.
2. 문서를 HWPX 형식으로 저장합니다.
3. `template.hwpx` 대신 저장한 파일 경로를 `--template`에 지정합니다.
4. `tokens.json`에 같은 이름의 문자열 값을 추가합니다.
5. `--output`으로 결과 파일 경로를 지정해 실행합니다.

예를 들어 템플릿에 `{{department}}`를 추가했다면 JSON에 다음 항목을 추가합니다.

```json
{
  "title": "Example document",
  "author": "docgen",
  "department": "개발팀"
}
```

토큰 이름은 영문자, 숫자, `_`, `-`, `.`만 사용할 수 있습니다. JSON에 값이 없는 토큰은 기본적으로 오류가 되며, 그대로 남겨두려면 `--allow-missing`을 사용합니다.
