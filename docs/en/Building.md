# Building

[Deutsch](../de/Building.md)

| Need | Why |
|---|---|
| **JDK 25** | Project Zomboid 42.21's classes are Java 25 (class version 69); JDK 21 cannot read them. |
| `projectzomboid.jar` | from the game folder |
| `ZombieBuddy.jar` | from its Workshop item (3619862853) |
| `ViewpointTrueBallistics.jar` | only for Vehicle Ballistics, from its Workshop item (3812864848) |

The paths are set at the top of each `build.ps1`. Adjust them to your system.

| What | Command |
|---|---|
| A single Java mod | `powershell -ExecutionPolicy Bypass -File mods/ViewpointFixes/Contents/mods/<Mod>/source/build.ps1` |
| Vehicle Ballistics (both jars) | `…/ViewpointVehicleBallistics/source/build.ps1` (`-OnlyVF` for only the ViewpointFixes jar) |
| Vehicle Ballistics: sandbox, translations, Lua, mod.info | `python mods/ViewpointFixes/tools/vehicle_gen.py` |
| Sandbox page *Viewpoint Fixes* | `python mods/ViewpointFixes/tools/sandbox_gen.py` |
| Option tables in `docs/` | `python mods/ViewpointFixes/tools/docgen.py docs` |
| Mod Options Control translations | `python mods/ModOptionsControl/tools/translations.py` |
| Lua syntax check | `mods/ViewpointFixes/tools/luacheck/check.sh <folder>` |

- The Lua syntax check uses the game's own Kahlua compiler. Compile `LuaCheck.java` once with `javac -cp projectzomboid.jar`.
- Vehicle Ballistics is one source in `source/vpveh`. `build.ps1` builds the ViewpointFixes jar (package `vpveh`) and the copy for Bullet Impacts 2D (package `vbi2d`, other sandbox names), which works only while the ViewpointFixes version is not loaded.
- To test, copy the Workshop folders to `%UserProfile%\Zomboid\Workshop\` (without `tools`).
