#!/usr/bin/env bash
# Aufruf: check.sh <ordner>...  - prueft alle .lua darin mit dem Kahlua-Compiler des Spiels
J="G:/Zomboid Claude/VR-Mod/tools/jdk25/bin"
CP="G:/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid.jar"
HERE="$(cd "$(dirname "$0")" && pwd -W)"
files=()
while IFS= read -r -d '' f; do files+=("$f"); done < <(find "$@" -name "*.lua" -print0)
"$J/java" -cp "$CP;$HERE" LuaCheck "${files[@]}" | grep -v "^OK" ; exit "${PIPESTATUS[0]}"
