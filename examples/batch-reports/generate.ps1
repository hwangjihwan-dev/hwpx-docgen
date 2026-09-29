param(
    [string]$Jar = (Join-Path $PSScriptRoot '..\..\target\docgen-0.1.0.jar'),
    [string]$Template = (Join-Path $PSScriptRoot '..\minimal\template.hwpx'),
    [string]$DataDirectory = (Join-Path $PSScriptRoot 'data'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'output')
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $Jar -PathType Leaf)) {
    throw "JAR 파일을 찾을 수 없습니다: $Jar`n프로젝트 루트에서 mvn package를 먼저 실행하세요."
}

if (-not (Test-Path -LiteralPath $Template -PathType Leaf)) {
    throw "HWPX 템플릿을 찾을 수 없습니다: $Template"
}

if (-not (Test-Path -LiteralPath $DataDirectory -PathType Container)) {
    throw "JSON 데이터 디렉터리를 찾을 수 없습니다: $DataDirectory"
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$dataFiles = @(Get-ChildItem -LiteralPath $DataDirectory -Filter '*.json' -File | Sort-Object Name)

if ($dataFiles.Count -eq 0) {
    throw "JSON 데이터 파일이 없습니다: $DataDirectory"
}

foreach ($dataFile in $dataFiles) {
    $output = Join-Path $OutputDirectory ($dataFile.BaseName + '.hwpx')
    Write-Host "Generating $output"

    & java -jar $Jar `
        --template $Template `
        --data $dataFile.FullName `
        --output $output `
        --force

    if ($LASTEXITCODE -ne 0) {
        throw "문서 생성에 실패했습니다: $($dataFile.Name)"
    }
}

Write-Host "완료: $($dataFiles.Count)개 문서 생성"
