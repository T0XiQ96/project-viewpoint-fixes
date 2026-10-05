### Fahrzeugtreffer

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.VehicleBallistics_Enabled` | Fahrzeugtreffer: aktiv | an |  | Schüsse treffen das echte Teil eines Fahrzeugs (Glas, Reifen, Lichter, Türen, Haube, Motor, Tank, Panzerung) statt eines zufälligen Teils der Seite. |
| `ViewpointFixes.VehicleBallistics_In2D` | Fahrzeugtreffer: auch in der normalen 2D-Sicht | aus |  | Auch für Schüsse ohne True Ballistics (normales Schießen, isometrische Sicht). Der Schuss fliegt dann gerade in Blickrichtung auf Mündungshöhe. |
| `ViewpointFixes.VehicleBallistics_HitInfo` | Fahrzeugtreffer: Schütze sieht, was getroffen wurde | an |  | Ein kurzer Text über deinem Charakter, z. B. „Reifen hinten links: platt“. Jeder Spieler kann es in den Mod-Optionen abschalten. |
| `ViewpointFixes.VehicleBallistics_DamageScale` | Fahrzeugtreffer: Schadensfaktor | 1 | 0,1 – 5 | Multipliziert jeden Teileschaden durch Kugeln. |
| `ViewpointFixes.VehicleBallistics_WindshieldShots` | Windschutzscheibe: Treffer bis sie einbricht | 15 | 1 – 60 | Verbundglas behält seine Löcher und bleibt drin. Treffer einer 9 mm, bis sie einbricht; stärkere Munition zählt mehr (Gewehr etwa doppelt), schwächere weniger. |
| `ViewpointFixes.VehicleBallistics_SideWindowShatter` | Seiten- und Heckscheiben: Chance zu zerspringen pro Treffer (%) | 90 | 0 – 100 | Einscheiben-Sicherheitsglas zerspringt meist beim ersten Treffer komplett. Sonst ein Loch mit Sprüngen. |
| `ViewpointFixes.VehicleBallistics_LightBreak` | Lichter: Chance kaputtzugehen pro Treffer (%) | 100 | 0 – 100 |  |
| `ViewpointFixes.VehicleBallistics_TireShotsStrong` | Reifen: Treffer bis platt (Gewehre, Flintenlaufgeschosse, Magnum) | 1 | 1 – 10 |  |
| `ViewpointFixes.VehicleBallistics_TireShotsMedium` | Reifen: Treffer bis platt (Pistolen, MPs) | 2 | 1 – 10 |  |
| `ViewpointFixes.VehicleBallistics_TireShotsWeak` | Reifen: Treffer bis platt (.22, .38, einzelne Schrotkugeln) | 3 | 1 – 10 | Jede Schrotkugel zählt einzeln, ein naher Schrotschuss macht einen Reifen also meist platt. |
| `ViewpointFixes.VehicleBallistics_TireMode` | Reifen: wie sie Luft verlieren | 1 | 1 = Sofort platt / 2 = Langsamer Luftverlust | Sofort platt: nach den Treffern oben ist der Reifen platt. Langsamer Luftverlust: jedes Loch verliert Luft, mehr Löcher schneller; platt nach der Zeit unten. |
| `ViewpointFixes.VehicleBallistics_TireLeakSeconds` | Reifen: Sekunden bis platt (langsamer Luftverlust) | 30 | 1 – 600 |  |
| `ViewpointFixes.VehicleBallistics_RunFlat` | Reifen: Notlauf bei gepanzerten Fahrzeugen | an |  | Fahrzeuge mit Notlauf- oder Reifendruck-System (KI5) oder Radpanzerung verlieren nur etwas Zustand. |
| `ViewpointFixes.VehicleBallistics_BodyDamage` | Blech: Schaden pro Treffer (%) | 3 | 0 – 25 | Türen, Haube, Kofferraum, Stoßstangen - für eine 9 mm, stärkere Munition mehr. |
| `ViewpointFixes.VehicleBallistics_EngineDamage` | Motor und Kühler: Schaden pro Treffer (%) | 2 | 0 – 25 |  |
| `ViewpointFixes.VehicleBallistics_FuelLeak` | Getroffener Tank verliert Sprit | an |  |  |
| `ViewpointFixes.VehicleBallistics_Occupants` | Schüsse durch Glas und Türen können Insassen treffen | an |  | Hängt von der Restenergie ab: Gewehre gehen durch Türen, kleine Kaliber bleiben oft im Blech stecken. |
| `ViewpointFixes.VehicleBallistics_ArmorWear` | Fahrzeugpanzerung: Abnutzung pro Treffer | 1 | 0 – 10 | Panzerplatten und Panzerglas (KI5 und andere Mods) halten die meisten Kugeln mit einer Schramme auf und nutzen sich langsam ab. |

### Aus Fahrzeugen schießen und Fenster

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.VehicleBallistics_DriveBy` | Aus Fahrzeugen schießen | an |  | Im Fahrzeug mit Schusswaffe in der Hand: rechte Maustaste halten = zielen, Linksklick = schießen (Dauerfeuer schießt weiter). Ein geschlossenes Fenster, durch das du schießt, bekommt ein Loch oder zerspringt; offene (heruntergekurbelte) Fenster bleiben heil. Braucht Viewpoint True Ballistics. |
| `ViewpointFixes.VehicleBallistics_DriverCanShoot` | Aus Fahrzeugen schießen: Fahrer darf schießen | an |  |  |
| `ViewpointFixes.VehicleBallistics_DriveBySpread` | Aus Fahrzeugen schießen: Faktor Streuung | 1,5 | 0,1 – 5 | Multipliziert die normale Streuung (Können, Moodles, Bewegung). |
| `ViewpointFixes.VehicleBallistics_DriverSpread` | Aus Fahrzeugen schießen: zusätzliche Streuung für den Fahrer | 2 | 1 – 5 |  |
| `ViewpointFixes.VehicleBallistics_OpenableWindows` | Seitenfenster von Mod-Fahrzeugen kurbelbar machen | an |  | Türfenster, die sich nicht herunterkurbeln lassen, bekommen die normale Option Fenster öffnen / schließen. Fahrzeuge ohne Öffnungs-Animation sehen weiter geschlossen aus, Schüsse gehen aber durch. |

### Durchschüsse und Abpraller (braucht Viewpoint True Ballistics)

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.VehicleBallistics_Penetration` | Durchschüsse: aktiv (True Ballistics) | an |  | Kugeln können Wände, Türen, Möbel, Bäume und Fahrzeuge durchschlagen, wenn ihre Energie reicht. Danach fliegen sie langsamer (weniger Schaden) und leicht abgelenkt weiter. Braucht Viewpoint True Ballistics. |
| `ViewpointFixes.VehicleBallistics_PenetrationZombies` | Durchschüsse: Zombies hinter Wänden treffbar | an |  |  |
| `ViewpointFixes.VehicleBallistics_PenetrationPlayers` | Durchschüsse: Spieler hinter Wänden treffbar | aus |  | Aus: Kugeln, die etwas durchschlagen haben, fliegen an Spielern dahinter vorbei. |
| `ViewpointFixes.VehicleBallistics_PenetrationScale` | Durchschüsse: Faktor Materialwiderstand | 1 | 0,1 – 10 | Multipliziert alle Materialwerte unten. Höher = weniger Durchschüsse. |
| `ViewpointFixes.VehicleBallistics_MaxPenetrations` | Durchschüsse: Hindernisse pro Kugel | 3 | 0 – 10 |  |
| `ViewpointFixes.VehicleBallistics_EnergyLoss` | Durchschüsse: zusätzlicher Energieverlust (%) | 25 | 0 – 90 | Von der Energie, die nach einem Hindernis übrig ist (Taumeln, Verformung). |
| `ViewpointFixes.VehicleBallistics_DeflectionScale` | Durchschüsse: Faktor Ablenkung | 1 | 0 – 5 |  |
| `ViewpointFixes.VehicleBallistics_Ricochets` | Abpraller: aktiv | an |  | Kugeln, die hartes Material im flachen Winkel nicht durchschlagen, können abprallen und weiterfliegen. |
| `ViewpointFixes.VehicleBallistics_RicochetScale` | Abpraller: Faktor Wahrscheinlichkeit | 1 | 0 – 5 |  |
| `ViewpointFixes.VehicleBallistics_RicochetAngle` | Abpraller: größter Winkel zur Oberfläche (Grad) | 20 | 1 – 60 |  |
| `ViewpointFixes.VehicleBallistics_J_Glass` | Durchschlagsenergie: Glas (J) | 20 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Plaster` | Durchschlagsenergie: Putz / Gipskarton (J) | 120 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Wood` | Durchschlagsenergie: Holz (Wände, Türen) (J) | 180 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_WoodSolid` | Durchschlagsenergie: Massivholz (Balken, schwere Möbel) (J) | 900 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Tree` | Durchschlagsenergie: Baumstämme (J) | 2200 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_MetalLight` | Durchschlagsenergie: Dünnes Blech (J) | 150 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Metal` | Durchschlagsenergie: Metall (Zäune, Geräte) (J) | 600 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_MetalLarge` | Durchschlagsenergie: Schweres Metall (Container) (J) | 1000 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_MetalSolid` | Durchschlagsenergie: Gehärtetes Metall / Panzerung (J) | 6000 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_GlassArmored` | Durchschlagsenergie: Panzerglas (J) | 2500 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Brick` | Durchschlagsenergie: Ziegel (J) | 3500 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Cinderblock` | Durchschlagsenergie: Hohlblockstein (J) | 3000 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Stone` | Durchschlagsenergie: Stein / Beton (J) | 20000 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Plastic` | Durchschlagsenergie: Kunststoff (J) | 60 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Fabric` | Durchschlagsenergie: Stoff / Teppich (J) | 20 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_Ceramic` | Durchschlagsenergie: Keramik (J) | 200 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |
| `ViewpointFixes.VehicleBallistics_J_VehicleBody` | Durchschlagsenergie: Fahrzeugblech (eine Tür) (J) | 250 | 0 – 50000 | Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180. |

Bullet Impacts 2D (VFA_VehicleBallistics2D) hat dieselben Optionen unter `VehicleBallistics2D.<Name>` (Seite „Fahrzeug-Ballistik 2D“), dort ist „auch in der 2D-Sicht“ standardmäßig an.
