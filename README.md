# docgen

`docgen` is a small Java 8 library and command-line tool for replacing named tokens in HWPX templates.

It is intentionally independent from the weekly-report automation project. The public repository contains only the reusable document-generation core, tests, and a sanitized minimal example.

## Features

- Read and write HWPX ZIP archives.
- Replace `{{token}}` placeholders in `Contents/section*.xml` files.
- Handle placeholders split across adjacent HWPX text runs.
- Preserve missing placeholders with `--allow-missing`, or fail fast by default.
- Refuse to overwrite an existing output unless `--force` is supplied.
- Protect archive extraction from ZIP Slip and XML parsing from external-entity expansion.
- Provide both a Java API and a shaded command-line JAR.

## Requirements

- Java 8 or newer to run the shaded JAR.
- Maven 3.8 or newer to build the project.

## Build

```bash
mvn test
mvn package
```

The executable JAR is written to `target/docgen-0.1.0.jar`.

## Command-line usage

```bash
java -jar target/docgen-0.1.0.jar \
  --template examples/minimal/template.hwpx \
  --data examples/minimal/tokens.json \
  --output result.hwpx
```

The input JSON must be a flat object whose values are strings:

```json
{
  "title": "Example document",
  "author": "docgen"
}
```

Supported options:

- `--template <file>`: source HWPX template.
- `--data <file>`: flat JSON token map.
- `--output <file>`: generated HWPX path.
- `--allow-missing`: leave unknown placeholders unchanged.
- `--force`: replace an existing output file.
- `--help`: print usage.

Exit codes are `0` for success, `2` for argument/input errors, `3` for invalid HWPX/XML, `4` for token/JSON validation errors, and `5` when the output cannot be written safely.

## Java API

The main lifecycle is:

```java
HwpxDocument document = HwpxDocument.open(templatePath);
try {
    ReplacementResult result = document.replaceTokens(tokens, false);
    document.write(outputPath, false);
} finally {
    document.close();
}
```

Token names may contain letters, digits, `_`, `-`, and `.`. Values are strings. The library operates on section XML files and does not require the original private application that produced a template.

## Example

See [`examples/minimal`](examples/minimal) for a small sanitized HWPX fixture and token data. It is a format-level example for exercising `docgen`; it is not a complete office-authoring template.

## Security notes

Templates are treated as untrusted input. Archive extraction rejects entries that escape the destination directory. XML parsing disables DTDs, external entities, external schemas, and XInclude. Generated output is written through a temporary archive and is not overwritten by default.

## License

This project is released under the Apache License 2.0. See [`LICENSE`](LICENSE) and [`THIRD-PARTY-NOTICES`](THIRD-PARTY-NOTICES).
