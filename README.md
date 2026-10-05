# Project Viewpoint - Fixes · Vehicle Ballistics · Mod Options Control

Mods for **Project Zomboid Build 42** (42.21), mostly for [Project Viewpoint](https://steamcommunity.com/sharedfiles/filedetails/?id=3809306528) (3D view). Source code and full documentation. · [Deutsch ↓](#deutsch)

| Workshop item | Contents | Docs |
|---|---|---|
| [Project Viewpoint - Fixes](https://steamcommunity.com/sharedfiles/filedetails/?id=3812510377) | 15 mods: 3D markers, 3D gun effects, controls, aim spread, crosshair, zoom, **Vehicle Ballistics & Penetration**; all options on the sandbox page *Viewpoint Fixes* | [EN](docs/en/ViewpointFixes.md) · [DE](docs/de/ViewpointFixes.md) |
| ↳ Vehicle Ballistics & Penetration | realistic vehicle hits (glass, tyres, lights, armour, occupants), penetration through walls and vehicles, ricochets, shooting from vehicles | [EN](docs/en/VehicleBallistics.md) · [DE](docs/de/VehicleBallistics.md) |
| [Bullet Impacts 2D](https://steamcommunity.com/sharedfiles/filedetails/?id=3813696499) | bullet holes, sparks, dust and ricochets in the 2D view + Vehicle Ballistics 2D | [EN](docs/en/BulletImpacts2D.md) · [DE](docs/de/BulletImpacts2D.md) |
| Mod Options Control (Server) | admins lock Mod Options for players and set their values; locked options can be hidden | [EN](docs/en/ModOptionsControl.md) · [DE](docs/de/ModOptionsControl.md) |

**Option tables** (generated from the mods themselves):

| | EN | DE |
|---|---|---|
| Viewpoint Fixes sandbox | [EN](docs/en/viewpointfixes-sandbox.generated.md) | [DE](docs/de/viewpointfixes-sandbox.generated.md) |
| Vehicle Ballistics | [EN](docs/en/vehicle-ballistics-options.generated.md) | [DE](docs/de/vehicle-ballistics-options.generated.md) |
| Mod Options Control | [EN](docs/en/mod-options-control-options.generated.md) | [DE](docs/de/mod-options-control-options.generated.md) |

Building: [EN](docs/en/Building.md) · [DE](docs/de/Building.md). Bugs and questions: [Issues](https://github.com/T0XiQ96/project-viewpoint-fixes/issues) (mod name + lines from `console.txt`).

## Layout

```
mods/ViewpointFixes/      Workshop item (Contents/mods/<Mod>/42 …, source/ per mod), tools/ = generators
mods/BulletImpacts2D/     Workshop item (VFA_BulletImpacts2D, VFA_VehicleBallistics2D)
mods/ModOptionsControl/   Workshop item
docs/en, docs/de          documentation
```

## Notes

- Unofficial. Not affiliated with the authors of Project Viewpoint, Viewpoint True Ballistics, ZombieBuddy or the other mods these patch.
- Viewpoint and True Ballistics are only used at runtime through ZombieBuddy patches and reflection. **None of their code is included**, and no Viewpoint class stubs are in this repository.
- Please do not re-upload these mods to the Workshop; link to the originals instead.

---

## Deutsch

Mods für **Project Zomboid Build 42** (42.21), überwiegend für [Project Viewpoint](https://steamcommunity.com/sharedfiles/filedetails/?id=3809306528) (3D-Sicht). Quellcode und vollständige Dokumentation.

| Workshop-Item | Inhalt | Doku |
|---|---|---|
| Project Viewpoint - Fixes | 15 Mods: 3D-Marker, 3D-Waffeneffekte, Steuerung, Streuung, Fadenkreuz, Zoom, **Fahrzeug-Ballistik und Durchschüsse**; alle Optionen auf der Sandbox-Seite *Viewpoint Fixes* | [DE](docs/de/ViewpointFixes.md) |
| ↳ Vehicle Ballistics & Penetration | realistische Fahrzeugtreffer (Glas, Reifen, Lichter, Panzerung, Insassen), Durchschüsse durch Wände und Fahrzeuge, Abpraller, Schießen aus Fahrzeugen | [DE](docs/de/VehicleBallistics.md) |
| Bullet Impacts 2D | Einschusslöcher, Funken, Staub und Abpraller in der 2D-Sicht + Fahrzeug-Ballistik 2D | [DE](docs/de/BulletImpacts2D.md) |
| Mod Options Control (Server) | Admins sperren Mod-Optionen für Spieler und legen die Werte fest; gesperrte lassen sich ausblenden | [DE](docs/de/ModOptionsControl.md) |

Inoffiziell. Code von Viewpoint und True Ballistics ist nicht enthalten, beide werden nur zur Laufzeit angesprochen. Bitte die Mods nicht erneut im Workshop hochladen, sondern auf die Originale verlinken.
