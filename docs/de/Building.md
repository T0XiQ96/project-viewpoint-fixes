# Bauen

[English](../en/Building.md)

| Nötig | Warum |
|---|---|
| **JDK 25** | Die Klassen von Project Zomboid 42.21 sind Java 25 (Klassenversion 69); JDK 21 kann sie nicht lesen. |
| `projectzomboid.jar` | aus dem Spielordner |
| `ZombieBuddy.jar` | aus dessen Workshop-Item (3619862853) |
| `ViewpointTrueBallistics.jar` | nur für Vehicle Ballistics, aus dessen Workshop-Item (3812864848) |

Die Pfade stehen oben in jeder `build.ps1`. Passe sie an dein System an.

| Was | Befehl |
|---|---|
| Eine einzelne Java-Mod | `powershell -ExecutionPolicy Bypass -File mods/ViewpointFixes/Contents/mods/<Mod>/source/build.ps1` |
| Vehicle Ballistics (beide Jars) | `…/ViewpointVehicleBallistics/source/build.ps1` (`-OnlyVF` nur für das ViewpointFixes-Jar) |
| Vehicle Ballistics: Sandbox, Übersetzungen, Lua, mod.info | `python mods/ViewpointFixes/tools/vehicle_gen.py` |
| Sandbox-Seite *Viewpoint Fixes* | `python mods/ViewpointFixes/tools/sandbox_gen.py` |
| Optionstabellen in `docs/` | `python mods/ViewpointFixes/tools/docgen.py docs` |
| Übersetzungen Mod Options Control | `python mods/ModOptionsControl/tools/translations.py` |
| Lua-Syntaxprüfung | `mods/ViewpointFixes/tools/luacheck/check.sh <Ordner>` |

- Die Lua-Syntaxprüfung nutzt den Kahlua-Compiler des Spiels. `LuaCheck.java` einmal mit `javac -cp projectzomboid.jar` übersetzen.
- Vehicle Ballistics ist eine Quelle in `source/vpveh`. `build.ps1` baut das ViewpointFixes-Jar (Paket `vpveh`) und die Kopie für Bullet Impacts 2D (Paket `vbi2d`, andere Sandbox-Namen), die nur läuft, solange die ViewpointFixes-Fassung nicht geladen ist.
- Zum Testen die Workshop-Ordner nach `%UserProfile%\Zomboid\Workshop\` kopieren (ohne `tools`).
