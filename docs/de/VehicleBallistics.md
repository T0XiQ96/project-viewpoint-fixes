# Vehicle Ballistics & Penetration (Fahrzeug-Ballistik und Durchschüsse)

Mod-ID `ViewpointVehicleBallistics` (in [Project Viewpoint - Fixes](ViewpointFixes.md)) · Kopie für die 2D-Sicht: `VFA_VehicleBallistics2D` (in [Bullet Impacts 2D](BulletImpacts2D.md)) · [English](../en/VehicleBallistics.md)

## Voraussetzungen und Installation

| | |
|---|---|
| Nötig | ZombieBuddy, auf **Clients und dem Dedicated Server** (der Server macht den Schaden) |
| Empfohlen | Viewpoint True Ballistics: Durchschüsse, Abpraller und Schießen aus Fahrzeugen brauchen es |
| Optional | ViewpointEffects3D (3D-Einschläge und bleibende Löcher an Fahrzeugen), Bullet Impacts 2D (2D-Einschläge an Fahrzeugen) |
| Ladereihenfolge | nach `ViewpointTrueBallistics` und `ViewpointEffects3D` |

- Das Jar liegt in `media/java/` (nicht `client/`), damit ZombieBuddy es auch auf dem Server lädt.
- Ohne den Server-Teil bleibt der Fahrzeugschaden im Mehrspieler wie im Grundspiel, und die Server-Prüfung von True Ballistics lehnt Treffer durch Wände ab.
- Sind `ViewpointVehicleBallistics` und `VFA_VehicleBallistics2D` beide aktiv, übernimmt die ViewpointFixes-Fassung; die Kopie bleibt untätig.

## Warum

Project Zomboid ist es egal, **wo** eine Kugel ein Fahrzeug trifft. Es schaut nur, von welcher Seite du schießt, nimmt ein zufälliges Teil dieser Seite und macht wenig Schaden (`Schaden × 50 × DoorDamage/10 ÷ Haltbarkeit`). Mit einer M4 kannst du 200 Schuss in ein Auto setzen und es steht noch bei 30 %. Windschutzscheibe, Reifen und Scheinwerfer reagieren nicht auf Beschuss.

## So wird ein Treffer bestimmt

1. **Flugbahn**
   - Mit True Ballistics: echter Einschlagpunkt und Geschwindigkeit der simulierten Kugel.
   - Ohne (2D, nur wenn *auch in der 2D-Sicht* an ist): Mündung und Zielrichtung wie im Spiel.
   - Im Mehrspieler schickt der Client des Schützen die Flugbahn an den Server. Der prüft sie (Abstand zu Schütze und Fahrzeug, Energie nicht höher als die der Waffe) und nutzt sie; ohne Angaben nimmt er die Blickrichtung.
2. **Fahrzeugform:** die eigenen **Physik-Boxen** des Fahrzeugs aus seinem Script, auch gedrehte Boxen wie eine schräge Windschutzscheibe. Boxen, die nicht zur Fahrzeuggröße passen (anderer Maßstab oder Versatz), werden automatisch daran ausgerichtet. Gibt es keine, gilt der Quader aus den Extents. Räder sind Zylinder aus Position, Radius und Breite. Das funktioniert mit Mod-Fahrzeugen ohne Kompatibilitäts-Patch.
3. **Welches Teil:** bestimmt aus Treffpunkt, Flächennormale und den Sitzen des Fahrzeugs.
   - **Glas:** oberhalb der Gürtellinie (Sitzhöhe + 0,3 m). Front oder schräge Fläche → `Windshield`, Heck → `WindshieldRear`, Seite → das Seitenfenster mit der nächsten Tür bzw. dem nächsten Sitz.
   - **Reifen:** im Radzylinder.
   - **Licht:** äußere Ecken vorn oder hinten in Lampenhöhe → `HeadlightLeft/Right`, `HeadlightRearLeft/Right` (jedes Teil mit Licht).
   - **Haube oder Motor:** vorn oben → `EngineDoor`; vorn unten (Kühlergrill) → `Radiator` / `Engine`.
   - **Kofferraum, Tank, Auspuff:** hinten → `TrunkDoor` / `DoorRear`, hinten unten → `Muffler` / `GasTank`.
   - **Tür:** die Tür am nächsten Sitz. Im hinteren Drittel unten → manchmal `GasTank`.
   - **Dach und übriges Blech:** nur Karosserie.
4. **Panzerung:** Ein eingebautes Teil, dessen ID *armor / armour / protection / cage / plate / plating / bulletproof* enthält und dessen Lage-Wörter zum Ziel passen, nimmt den Treffer.

   | Ziel | Zählende Panzerung |
   |---|---|
   | Windschutzscheibe | Panzerung mit „Windshield“, hinten/vorn muss passen |
   | Seitenfenster | Front/Middle/Rear/Back + Left/Right |
   | Tür | Door + L/R oder Seitenpanzerung |
   | Reifen | Wheel/Tire |
   | Motor | Engine/Hood/Front/Grill |
   | hintere Teile | Rear |
   | alles außer Lichtern | ein Teil, das nur „Armor“ heißt (Panzer) |

   Das passt zu KI5s `DAMN…Armor`-Teilen, Autotsar und anderen Mods. KI5-Fahrzeuge mit `RFsystem` / `CTIsystem` haben Notlaufreifen.

## Was passiert

| Teil | Verhalten (Standardwerte) |
|---|---|
| **Windschutzscheibe** (Verbundglas) | Behält ihre Löcher und bleibt drin. Der Zustand sinkt pro 9-mm-Treffer um 100/15 (Gewehr etwa doppelt); nach etwa 15 Treffern bricht sie ein (*WindshieldShots*). Die Riss-Überlagerung des Spiels wächst mit dem Schaden. Die Kugel geht mit etwa 85 % Energie durch. |
| **Seiten- und Heckscheiben** (Einscheiben-Sicherheitsglas) | 90 % Chance, beim ersten Treffer komplett zu zerspringen (*SideWindowShatter*); sonst Loch mit Sprüngen. |
| **Panzerglas** (Item-Typ enthält armor/bulletproof/ballistic/reinforced) | Schramme, 25 % Abpraller, nutzt sich langsam ab, die Kugel bleibt stecken. |
| **Offenes, heruntergekurbeltes oder zerbrochenes Fenster** | Kein Glasschaden; die Kugel geht ins Fahrzeug. |
| **Reifen** | Platt nach 1 Treffer (Gewehr, Flintenlaufgeschoss, Magnum, ≥ 1200 J), 2 (Pistole, MP) oder 3 (.22, .38, einzelne Schrotkugel, < 350 J). Jede Schrotkugel zählt einzeln. Alternative *langsamer Luftverlust*: jedes Loch verliert Luft, mehr Löcher schneller, platt nach *TireLeakSeconds*. Notlaufreifen verlieren nur etwas Zustand. |
| **Lichter** | Gehen beim ersten Treffer kaputt (*LightBreak* 100 %). |
| **Türen, Haube, Kofferraum, Blech** | 3 % pro 9-mm-Treffer × Kaliberfaktor (*BodyDamage*). Die Kugel geht durch, wenn ihre Energie über *J_VehicleBody* (250 J) liegt. |
| **Motor / Kühler** | 2 % pro Treffer (*EngineDamage*); der Motorblock hält die Kugel auf. |
| **Tank** | Schaden, und der Tank verliert Sprit (etwa 0,4 l/s × Kaliberfaktor), wenn *FuelLeak* an ist. |
| **Panzerung** | Abnutzung 1,5 × Kaliber × *ArmorWear*; 35 % Abpraller; nur Kugeln über *J_MetalSolid* (6000 J, z. B. .50 BMG) kommen mit halber Energie durch. |
| **Insassen** | Eine Kugel, die durch Glas, Tür oder offenes Fenster kam, kann die Person treffen, deren Oberkörper ihrer Bahn am nächsten ist (≤ 0,6 m). Die Chance hängt von Abstand und Restenergie ab. Das Körperteil richtet sich nach der Höhe (Kopf, Hals, Brust, Bauch, Becken, Oberschenkel); der Treffer gibt eine Schusswunde mit Schaden nach Restenergie. Spieler bekommen die Wunde auf ihrem eigenen Client. |

Der Kaliberfaktor ist `√(Energie / 500 J)`: 9 mm = 1, 5,56 etwa 1,85, .22 etwa 0,55, begrenzt auf 0,3–4.

### Energie je Kaliber

Mit True Ballistics kommt die Energie aus dessen Profil (Masse und echte Mündungsgeschwindigkeit des Kalibers), auch Mod-Munition und Kompatibilitäts-Pakete zählen. Ohne True Ballistics wird sie am Munitionsnamen geschätzt.

| Munition | J (etwa) |
|---|---|
| .22 | 150 |
| Schrotkugel | 180 |
| .38 | 320 |
| 9 mm | 500 |
| .45 | 550 |
| .357 / 10 mm | 750 |
| .44 / Magnum | 1300 |
| 5,56 / .223 / 5,45 | 1700 |
| 7,62x39 / .30-30 | 2000 |
| Flintenlaufgeschoss | 3000 |
| .308 / 7,62x51 / .30-06 | 3400 |
| .50 BMG / .338 | 9000 |

AP-Munition (Name enthält `_ap`, „armorpiercing“) zählt ×1,6, Hohlspitz (JHP, HP) ×0,6. True Ballistics fliegt mit einem Geschwindigkeitsfaktor (Standard 0,4); diese Mod rechnet auf echte Joule zurück.

## Was getroffen wurde

Der Schütze sieht einen kurzen Text über seinem Charakter, z. B. *Reifen hinten links: platt (40%)*, *Windschutzscheibe: Einschussloch (73%)* oder *Tür vorne links: getroffen – Insasse getroffen*. Der Server schaltet das ein oder aus (*HitInfo*), und jeder Spieler kann es unter Optionen > Mods > Fahrzeug-Ballistik ausblenden.

## Einschusslöcher und Effekte

Löcher werden in der ModData des getroffenen Teils gespeichert (`vvbHoles`, bis 24 je Teil, in Fahrzeug-Koordinaten). Sie gehen an alle Spieler, und ViewpointEffects3D zeichnet sie an der aktuellen Fahrzeugposition, sie fahren also mit. Sie verschwinden, wenn die Scheibe zerspringt, das Teil ersetzt oder auf 100 % repariert wird. In der 2D-Sicht zeigt Bullet Impacts 2D Funken, Splitter und Staub im Fahrzeugmaterial, aber keine bleibenden Löcher.

## Aus Fahrzeugen schießen

Voraussetzungen: Schusswaffe in der Haupthand, Sitzplatz im Fahrzeug, True Ballistics installiert, Sandbox *DriveBy* (und für den Fahrer *DriverCanShoot*) an.

**Bedienung:** **Rechte Maustaste halten** zum Zielen. Ein Fadenkreuz erscheint in 2D an der Maus, in 3D in der Bildmitte. **Linksklick** schießt; Dauerfeuer schießt weiter, solange die Taste gedrückt ist.

**Richtung:** In der 3D-Sicht (Viewpoint) geht die Kugel dorthin, wohin du schaust; in 2D vom Auge im Sitz zum Mauszeiger.

**Eigenes Fahrzeug:** Die Kugel trifft zuerst dein Fahrzeug an der Stelle, wo die Linie es verlässt.

| Austrittsstelle | Ergebnis |
|---|---|
| Geschlossenes Fenster | Loch oder zerspringt, mit denselben Regeln wie oben; die Energie sinkt |
| Offenes Fenster | nichts |
| Tür / Dach | Blech |
| Panzerung oder Panzerglas | die Kugel bleibt drin |

Im Mehrspieler macht der Server diesen Schaden.

**Alles andere** läuft wie bei einem normalen Schuss: Klang, Lärm, der Zombies anlockt, Patrone und Munition, Ladehemmung und Durchladen. Die Streuung ist ×1,5 (*DriveBySpread*), der Fahrer bekommt zusätzlich ×2 (*DriverSpread*).

Grenze: Es gibt keine Sitz-Ziel-Animation, in der 3. Person zeigen die Arme also nicht aufs Ziel.

### Fenster herunterkurbeln

Das Grundspiel hat schon *Fenster öffnen / schließen* im Radialmenü des Sitzes, für Seitenfenster, die als öffnenbar markiert sind; die meisten Fahrzeuge (auch KI5) erben das. Türfenster von Mod-Fahrzeugen, die nicht öffnenbar sind, werden kurbelbar gemacht (*OpenableWindows*). Fahrzeuge ohne Öffnungs-Animation sehen weiter geschlossen aus, Schüsse gehen aber durch.

## Durchschüsse und Abpraller (Viewpoint True Ballistics)

**Durchschuss:**
- Trifft eine Kugel eine Wand, Tür, niedrige Mauer, ein Möbel oder einen Baum, braucht sie die Energie des Materials (Sandbox, Joule), bei flachem Winkel geteilt durch `cos(Winkel)` (höchstens ×3).
- Kommt sie durch, gibt es den Eintritts-Einschlag (Loch, Klang), und sie verliert `Bedarf + EnergyLoss %` vom Rest. Dann fliegt sie durch die Dicke des Hindernisses weiter, langsamer (weniger Schaden durch den Damage Falloff von True Ballistics, mehr Abfall) und abgelenkt um bis zum Winkel des Materials × *DeflectionScale*.
- Höchstens *MaxPenetrations* Hindernisse pro Kugel (Standard 3).

**Abpraller:** Kommt die Kugel durch hartes Material (Metall, Stein, Panzerung) bei flachem Winkel (< *RicochetAngle*, 20°) nicht durch, kann sie abprallen und mit etwa 35 % ihrer Energie weiterfliegen.

**Fahrzeuge:** Das Schadensmodell entscheidet (Glas, Blech, Motorblock). Kommt die Kugel durch beide Seiten, fliegt sie hinter dem Fahrzeug weiter.

**Wer getroffen werden kann:** Zombies hinter Wänden werden getroffen (*PenetrationZombies*). **Spieler hinter Wänden standardmäßig nicht** (*PenetrationPlayers* aus): Kugeln, die etwas durchschlagen haben oder abgeprallt sind, fliegen an Spielern vorbei.

**Server-Prüfung:** True Ballistics lehnt Treffer ohne freie Schusslinie ab. Der Server-Teil dieser Mod lässt sie zu, wenn jede Wand dazwischen mit der Energie dieser Waffe wirklich durchschlagbar ist, und nur für die oben erlaubten Ziele.

Das Material kommt aus dem `MaterialType` des getroffenen Feldes, derselben Eigenschaft, die den Einschlag-Sound wählt. Türen zählen als Holz, selbstgebaute Wände je nach Sprite als Holz oder Metall, Bäume als „Tree“. Standard-Widerstände:

| Material | J | | Material | J |
|---|---|---|---|---|
| Glas | 20 | | Metall (Zäune, Geräte) | 600 |
| Stoff / Teppich | 20 | | Massivholz | 900 |
| Kunststoff | 60 | | Schweres Metall (Container) | 1000 |
| Putz / Gipskarton | 120 | | Baumstamm | 2200 |
| Dünnes Blech | 150 | | Panzerglas | 2500 |
| Holz (Wände, Türen) | 180 | | Hohlblockstein | 3000 |
| Keramik | 200 | | Ziegel | 3500 |
| Fahrzeugblech (eine Tür) | 250 | | Gehärtetes Metall / Panzerung | 6000 |
| | | | Stein / Beton | 20000 |

Eine 9 mm geht also durch eine Holztür oder Gipskarton, eine 5,56 durch Massivholz, eine .308 ab und zu durch einen Baumstamm, und nichts Realistisches durch Beton. *PenetrationScale* multipliziert alle Werte.

## Sandbox-Optionen

Alle 50 Optionen mit Standard und Bereich: **[vehicle-ballistics-options.generated.md](vehicle-ballistics-options.generated.md)** (Seite *Viewpoint Fixes*, Namen `ViewpointFixes.VehicleBallistics_*`; 2D-Kopie: `VehicleBallistics2D.*`).

## Fehlersuche

- Zeilen in `console.txt`, die mit `[VehicleBallistics]` beginnen, zeigen die geladene Fassung, ob True Ballistics gefunden wurde und Fehler. Ein Fehler schaltet nur den betroffenen Teil ab, dann gilt das Verhalten des Grundspiels.
- Konsolenbefehl `VehicleBallisticsVF_describe()` (2D-Kopie: `VehicleBallistics2D_describe()`) zeigt, wie die Mod das Fahrzeug sieht, in dem du sitzt oder neben dem du stehst: Anzahl Formen, Größe, Fahrtrichtung, Gürtellinie, Räder.
- Falsche Teile bei einem bestimmten Mod-Fahrzeug: bitte Script-Namen des Fahrzeugs und die `describe()`-Ausgabe melden.

## Dateien

| Datei | Zweck |
|---|---|
| `source/vpveh/Geometry.java` | Fahrzeugform, Strahltests |
| `Model.java` | welches Teil, Schaden |
| `Penetration.java` / `VtbBridge.java` | Materialien und True-Ballistics-Hooks |
| `Shot.java` / `Net.java` | Ablauf, Client ↔ Server |
| `DriveBy*.java` | aus Fahrzeugen schießen |
| `Effects.java` | Schnittstelle für Effekt-Mods |
| `Leaks`, `Holes`, `Occupants` | Luft- und Spritverlust, bleibende Löcher, Insassen |
| `source/build.ps1` | baut beide Jars |
| `tools/vehicle_gen.py` | Sandbox-Optionen, Übersetzungen, Lua und mod.info beider Fassungen |
