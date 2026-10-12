#!/usr/bin/env python3
"""Package GH2ME source and locale assets in MIDPlay-private namespaces."""

import json
import shutil
import sys
from pathlib import Path


def load_jsonc(path):
    lines = []
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped.startswith("//"):
            continue
        lines.append(line.split(" // ", 1)[0])
    return json.loads("\n".join(lines))


def escape_locale(value):
    return str(value).replace("\\", "\\\\").replace("\n", "\\n")


def main():
    if len(sys.argv) != 3:
        raise SystemExit("Usage: prepare_github_j2me.py <source-dir> <output-dir>")
    root = Path(sys.argv[1])
    output = Path(sys.argv[2])
    source = root / "src"
    shutil.rmtree(output, ignore_errors=True)
    output.mkdir(parents=True)

    for java_file in source.rglob("*.java"):
        relative = java_file.relative_to(source)
        text = java_file.read_text(encoding="utf-8")
        if relative.parts[:3] == ("cc", "nnproject", "json"):
            # Reuse MIDPlay's JSON object model. GH2ME contributes only its
            # streaming reader, adapted to the shared parser's sentinels/API.
            if java_file.name != "JSONStream.java":
                continue
            text = text.replace("JSONObject.json_null", "JSON.json_null")
            text = text.replace("JSONObject.TRUE", "JSON.TRUE")
            text = text.replace("JSONObject.FALSE", "JSON.FALSE")
            target = output / "JSONStream.java"
        else:
            text = text.replace("/file.png", "/gh2me-file.png")
            text = text.replace("/folder.png", "/gh2me-folder.png")
            text = text.replace('getResourceAsStream("/l/"', 'getResourceAsStream("/gh2me/l/"')
            text = "package githubj2me;\n\n" + text
            target = output / java_file.name
        target.write_text(text, encoding="utf-8")

    messages = load_jsonc(root / "langs" / "en.jsonc")
    constants = ["package githubj2me;", "", "public interface LangConstants {"]
    for index, key in enumerate(messages, 1):
        constants.append("  int L%s = %d;" % (key, index))
    constants.extend(["  int LLocaleStrings = %d;" % len(messages), "  int Lgh2me = 0;", "}"])
    output.joinpath("LangConstants.java").write_text("\n".join(constants) + "\n", encoding="utf-8")

    locale_dir = output / "gh2me" / "l"
    locale_dir.mkdir(parents=True)
    for locale_file in (root / "langs").glob("*.jsonc"):
        locale = load_jsonc(locale_file)
        values = [escape_locale(locale.get(key, value)) for key, value in messages.items()]
        locale_dir.joinpath(locale_file.stem).write_text("\n".join(values) + "\n", encoding="utf-8")

    for asset, target in (
        ("g.png", "g.png"),
        ("g.svg", "g.svg"),
        ("file.png", "gh2me-file.png"),
        ("folder.png", "gh2me-folder.png"),
    ):
        shutil.copy2(root / "res" / asset, output / target)


if __name__ == "__main__":
    main()
