#!/usr/bin/env python3
"""Prepare the generic MPGram source for inclusion in the MIDPlay JAR."""

import json
import os
import shutil
import sys
from pathlib import Path


SYMBOLS = {
    "NO_NOKIAUI",
}


def enabled(directive):
    parts = directive.split()
    if parts[0] == "ifdef":
        return parts[1] in SYMBOLS
    if parts[0] == "ifndef":
        return parts[1] not in SYMBOLS
    if parts[0] == "if":
        return directive[3:].strip() == '""==""'
    raise ValueError("Unsupported MPGram preprocessor directive: " + directive)


def preprocess(source):
    output = []
    active = [True]
    for line in source.splitlines(True):
        stripped = line.strip()
        if not stripped.startswith("//#"):
            if active[-1]:
                output.append(line)
            continue
        directive = stripped[3:].strip()
        if directive.startswith(("ifdef ", "ifndef ", "if ")):
            active.append(active[-1] and enabled(directive))
        elif directive == "else":
            if len(active) == 1:
                raise ValueError("Unexpected #else")
            parent = active[-2]
            active[-1] = parent and not active[-1]
        elif directive == "endif":
            if len(active) == 1:
                raise ValueError("Unexpected #endif")
            active.pop()
        elif active[-1]:
            marker = line.index("//#")
            output.append(line[:marker] + line[marker + 3:])
    if len(active) != 1:
        raise ValueError("Unclosed MPGram preprocessor directive")
    return "".join(output)


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


def language_constants(mpgram, destination, resources):
    messages = load_jsonc(mpgram / "langs" / "en.jsonc")
    constants = ["package mpgram;", "", "public interface LangConstants {"]
    for index, key in enumerate(messages, 1):
        constants.append("  int L%s = %d;" % (key, index))
    constants.append("  int LLocaleStrings = %d;" % len(messages))
    constants.append("  int Lmpgram = 0;")
    constants.append("}")
    (destination / "LangConstants.java").write_text("\n".join(constants) + "\n", encoding="utf-8")
    locale_dir = resources / "l"
    locale_dir.mkdir(parents=True, exist_ok=True)
    for locale_file in (mpgram / "langs").glob("*.jsonc"):
        if os.environ.get("MPGRAM_FORCE_ENGLISH") == "1" and locale_file.stem != "en":
            continue
        locale = load_jsonc(locale_file)
        output = []
        base = locale.get("LocaleBase")
        if base:
            output.append("/l/" + base)
            for index, key in enumerate(messages, 1):
                if key in locale:
                    output.append(str(index) + ":" + escape_locale(locale[key]))
        else:
            for key, value in messages.items():
                output.append(escape_locale(locale.get(key, value)))
        locale_dir.joinpath(locale_file.stem).write_text("\n".join(output) + "\n", encoding="utf-8")


def main():
    if len(sys.argv) != 3:
        raise SystemExit("Usage: prepare_mpgram.py <mpgram-dir> <output-dir>")
    mpgram = Path(sys.argv[1])
    destination = Path(sys.argv[2])
    resources = destination.parent / "mpgram-res"
    source_dir = mpgram / "src"
    if not (source_dir / "MP.java").is_file():
        raise SystemExit("MPGram source is missing. Initialize third_party/mpgram first.")
    shutil.rmtree(destination, ignore_errors=True)
    shutil.rmtree(resources, ignore_errors=True)
    destination.mkdir(parents=True)
    shutil.copytree(mpgram / "res", resources)
    for source_file in source_dir.rglob("*.java"):
        prepared = preprocess(source_file.read_text(encoding="utf-8"))
        target = destination / source_file.relative_to(source_dir)
        target.parent.mkdir(parents=True, exist_ok=True)
        package = "" if source_file.parent.name == "zip" else "package mpgram;\n\n"
        target.write_text(package + prepared, encoding="utf-8")
    language_constants(mpgram, destination, resources)


if __name__ == "__main__":
    main()
