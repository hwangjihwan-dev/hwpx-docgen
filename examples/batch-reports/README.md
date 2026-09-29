# 여러 문서 일괄 생성 예제

이 예제는 하나의 HWPX 템플릿과 여러 개의 JSON 파일을 사용해 결과 문서를 반복 생성합니다.

## 디렉터리 구조

```text
examples/
├─ minimal/
│  └─ template.hwpx
└─ batch-reports/
   ├─ data/
   │  ├─ team-a.json
   │  ├─ team-b.json
   │  ├─ team-c.json
   │  └─ ...
   └─ generate.ps1
```

이 예제는 민감정보가 없는 [`examples/minimal/template.hwpx`](../minimal/template.hwpx)를 템플릿으로 재사용합니다. 실제 업무에서는 공개해도 되는 별도 템플릿을 준비하세요.

## 실행 방법

프로젝트 루트에서 먼저 빌드합니다.

```powershell
mvn package
```

그 다음 일괄 생성 스크립트를 실행합니다.

```powershell
.\examples\batch-reports\generate.ps1
```

성공하면 `examples/batch-reports/output`에 다음 파일이 생성됩니다.

```text
output/
├─ team-a.hwpx
├─ team-b.hwpx
└─ team-c.hwpx
```

각 JSON의 `title`, `author` 값이 템플릿의 `{{title}}`, `{{author}}` 위치에 들어갑니다.

## 다른 템플릿과 데이터 사용하기

스크립트에 경로를 지정하면 기본 예제 대신 다른 템플릿과 데이터 디렉터리를 사용할 수 있습니다.

```powershell
.\examples\batch-reports\generate.ps1 `
  -Template .\templates\report.hwpx `
  -DataDirectory .\data\reports `
  -OutputDirectory .\output\reports
```

JSON 파일은 `docgen`이 지원하는 평면 문자열 객체 형식이어야 합니다.

```json
{
  "title": "2026년 9월 1주차 보고서",
  "author": "개발팀 A"
}
```
