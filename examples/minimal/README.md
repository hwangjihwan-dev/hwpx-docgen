# Minimal HWPX example

This directory contains a small sanitized HWPX archive with two placeholders and a matching JSON token map.

Run it from the repository root after building:

```bash
java -jar target/docgen-0.1.0.jar \
  --template examples/minimal/template.hwpx \
  --data examples/minimal/tokens.json \
  --output result.hwpx
```

The fixture is intentionally minimal and is meant to exercise the `docgen` format flow. It is not a complete office-authoring template.
