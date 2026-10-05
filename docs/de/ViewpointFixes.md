# Project Viewpoint - Fixes

Steam Workshop: [3812510377](https://steamcommunity.com/sharedfiles/filedetails/?id=3812510377) · Build 42.21 · [English](../en/ViewpointFixes.md)

Fünfzehn kleine, unabhängige Mods für **Project Viewpoint** (3D-Sicht). Aktiviere nur die, die du willst.

- Alle brauchen **ZombieBuddy** und werden **nach Viewpoint** geladen.
- Mods 1–14 laufen nur beim Client. Mod 12 ändert, wohin deine eigenen Schüsse gehen. Mod 15 ändert das Spielgeschehen und muss **auch auf dem Server** laufen (ZombieBuddy auf dem Dedicated Server).
- Den Java-Quellcode jeder Mod findest du in ihrem `source/`-Ordner ([mods/ViewpointFixes](../../mods/ViewpointFixes/Contents/mods)).
- Inoffiziell, nicht verbunden mit den Autoren von Viewpoint oder der gepatchten Mods. Auf Viewpoint wird nur zur Laufzeit zugegriffen (ZombieBuddy-Patches, Reflection); es ist kein Code daraus enthalten.

| # | Mod-ID | Name | Seite |
|---|---|---|---|
| 1 | `Viewpoint2Dto3D` | Viewpoint 2D to 3D | Client |
| 2 | `Viewpoint2Dto3D_ExtendedPatch` | 2D to 3D – Extended Patch | Client |
| 3 | `ViewpointOpenContainers` | Open All Containers Fix | Client |
| 4 | `ViewpointHideCamera` | Hide Camera | Client |
| 5 | `ViewpointPlaceItems` | Item Placement 3D | Client |
| 6 | `ViewpointMouseRightClick` | Right Click in Mouse Mode | Client |
| 7 | `ViewpointCrosshairUse` | Crosshair Use | Client |
| 8 | `ViewpointProneKey` | Prone on X | Client |
| 9 | `ViewpointInteractList` | Interaction List Cleanup | Client |
| 10 | `ViewpointEffects3D` | 3D Gun Effects, Tracers and Fireflies | Client |
| 11 | `ViewpointShoulderLook3D` | Over the Shoulder in 3D | Client |
| 12 | `ViewpointAimSpread` | Aim Spread | Client (Spielgeschehen) |
| 13 | `ViewpointReticle` | Crosshair Options | Client |
| 14 | `ViewpointZoomHold` | Hold to Zoom | Client |
| 15 | `ViewpointVehicleBallistics` | Vehicle Ballistics & Penetration | **Server + Client** |

---

## Kartenmarker und Beschriftungen

### 1) Viewpoint 2D to 3D (`Viewpoint2Dto3D`)
Viele Mods setzen Hinweise mit der alten isometrischen Projektion (Lua `isoToScreenX/Y`). Unter Viewpoint schweben diese Hinweise an der falschen Stelle.
- Solange die 3D-Sicht an ist, liefern `isoToScreenX/Y` die echte Bildschirmposition aus Viewpoints Kamera (Ich-Perspektive, 3. Person, freie Kamera). Punkte hinter der Kamera werden weit aus dem Bild geschoben, damit Mods sie ausblenden.
- Real Vehicle VFX zeichnet flachen 2D-Rauch (Auspuff, Schadensrauch, Kühlerdampf, Reifenrauch). Der ist in 3D aus; deine Einstellungen kommen danach zurück.
- Mit 3D-Sicht aus oder ohne Viewpoint verhält sich alles wie im Grundspiel. Marker können ein Bild nachhängen.
- Lua: `VFA3D.active()` ist true, solange Viewpoint 3D rendert.

### 2) Viewpoint 2D to 3D – Extended Patch (`Viewpoint2Dto3D_ExtendedPatch`)
Braucht Mod 1. Macht dasselbe für Mods, die `IsoUtils.XToScreen/YToScreen` nutzen.
- Geprüft mit: Leave A Message Note, Shotgun Trajectory, Home Inventory, Cold Breath, Hide Anywhere.
- Nicht abgedeckt: `renderPoly` (z. B. OffGrid) und Mods mit eigenem Java.

## Aussehen

### 3) Open All Containers Fix (`ViewpointOpenContainers`)
Open All Containers tauscht einen Behälter gegen ein „offenes“ Sprite, für das Viewpoint keine 3D-Form hat. Geöffnete Behälter verschwanden deshalb. Jetzt bekommen sie die Geometrie von OAC oder, falls es keine gibt, die Form des geschlossenen Originals.

### 4) Hide Camera (`ViewpointHideCamera`)
Wer sich unter einem Bett versteckte (Hide Anywhere), hatte die Kamera auf stehender Augenhöhe über dem Bett. Jetzt sinkt sie zum Boden, solange du versteckt bist. Die Höhe ist eine Konstante in der Lua-Datei (`EYE_HEIGHT`).

### 10) 3D Gun Effects, Tracers and Fireflies (`ViewpointEffects3D`)
Effekte, die in 3D flach oder unsichtbar waren, werden jetzt in Viewpoints Szene gezeichnet und hinter Wänden verdeckt. Jeder Teil hat eine Option.
- **Leuchtspuren:** die Tracer des Spiels in 3D, deine und die anderer Spieler. Farbe, Länge und Tempo kommen je Munitionstyp, Waffen-Mods wirken also mit. Mit Viewpoint True Ballistics wird jede Kugel dort gezeichnet, wo sie wirklich fliegt, ohne doppelte Tracer.
- **Mündungsfeuer:** ein heller Blitz, der die Umgebung kurz beleuchtet, immer wenn das Spiel selbst sein Mündungsfeuer zeigt (hängt von Waffe und Zufall ab; Schalldämpfer wirken).
- **Pulverrauch:** Wolken entstehen an der echten Mündung und bleiben in der Welt, in der Ich-Perspektive größer (einstellbar). Die flachen 2D-Wolken von Muzzle Smoke sind in 3D aus.
- **Einschläge nach Material** (das `MaterialType` des getroffenen Feldes, dasselbe, nach dem das Spiel den Einschlag-Sound wählt):

  | Material | Effekt |
  |---|---|
  | Holz | dunkles Loch, Splitter |
  | Metall | kleines Loch, Funken |
  | Gehärtetes Metall / Panzerglas | nur Schramme, oft Abpraller |
  | Stein, Beton, Ziegel | Loch, Staub, einige Funken |
  | Erde, Gras, Sand, Schnee | Spritzer, kurzlebige Spur |

  Abpraller sind auf harten Flächen bei flachem Winkel wahrscheinlicher. Die Lochgröße hängt von der Munition ab (auch Mod-Munition). Löcher verblassen nach etwa 2 Minuten, einstellbar bis etwa 10.
- **Fahrzeuge** (mit Mod 15): Einschläge zeigen das wirklich getroffene Teil: Glas, Blech, Reifen, Panzerung oder Lampe, mit Funken, Glassplittern und Abprallern. Einschusslöcher bleiben am Fahrzeug und fahren mit:

  | Teil | Loch |
  |---|---|
  | Glas | Loch mit Sprung-Hof |
  | Blech | Loch mit blankem Rand |
  | Panzerung | Schramme |

  Ein Loch verschwindet, wenn das Teil ersetzt oder auf 100 % repariert wird oder die Scheibe zerspringt. Auch Schüsse anderer Spieler auf Fahrzeuge bekommen das Fahrzeugmaterial.
- **JB's Fireflies:** in 3D gezeichnet und hinter Wänden verdeckt, oder in 3D aus (Option).
- Einschlag-Sound des Spiels und Treffer auf abgelegten Gegenständen gibt es auch in 3D. Die 2D-Sicht bleibt unverändert.
- Optionen: Optionen > Mods > Viewpoint 3D Effects. Alle stehen auch in der Sandbox (siehe [Server-Einstellungen](#server-einstellungen-sandbox--viewpoint-fixes)).

## Steuerung

### 5) Item Placement 3D (`ViewpointPlaceItems`)
Gegenstand ablegen (Rechtsklick > Gegenstand platzieren) zeigte in 3D keine Vorschau und tat nichts. Jetzt:
- 3D-Vorschau am Cursor oder Fadenkreuz, auf dem Boden oder auf Möbeln;
- **R** / **Shift+R** drehen;
- die normale „Modus wechseln“-Taste wechselt die Fläche;
- **Linksklick** legt ab, **Rechtsklick** bricht ab;
- ein Hinweis erscheint, wenn die Stelle ungültig ist.

Gegenstände ohne 3D-Modell zeigen keine Vorschau. Die 2D-Sicht bleibt unverändert.

### 6) Right Click in Mouse Mode (`ViewpointMouseRightClick`)
Im Mausmodus von Viewpoint (freier Cursor: Mittelklick, Beutefenster, Einstellungen, Pause) startete Rechtsklick die Zielhaltung. Jetzt öffnet er das normale Kontextmenü. In der Fadenkreuz-Sicht bleibt Rechtsklick Zielen. Schluckt eine andere Mod den Klick, öffnet sich trotzdem das Menü für das Feld unter der Maus.

### 7) Crosshair Use (`ViewpointCrosshairUse`)
Braucht Mods 6 und 9.
- **Linksklick** auf einen Lichtschalter, den du ansiehst, schaltet ihn, wie ein Linksklick in der normalen Sicht (sofort, wenn nah, kein Hinlaufen). Das Spiel findet das angeklickte Objekt über sein 2D-Bild; in 3D traf das nur, wenn du zufällig passend standest. Jetzt zählt das Objekt im Fadenkreuz.
- Ein **kurzer Rechtsklick** (unter 0,25 s) öffnet das normale Rechtsklick-Menü genau für das angesehene Objekt, mit freiem Mauszeiger. Gedrückt halten bleibt Zielen.

### 8) Prone on X (`ViewpointProneKey`)
Lethal Stealth: **X** legt dich hin, X erneut steht auf. Das gilt nur in der 3D-Sicht und nicht mit Schusswaffe in der Haupthand, weil X dort „Waffe durchladen“ ist. Der Eintrag „Hinlegen“ verschwindet aus dem Rechtsklick-Menü; die eigene Taste von Lethal Stealth geht weiter.

### 11) Over the Shoulder in 3D (`ViewpointShoulderLook3D`)
Braucht Mod 1 und Over the Shoulder. In der 3D-Sicht die **Umschau-Taste halten** (Standard **Feststelltaste**, Optionen > Tastenbelegung > „Look around in 3D“) und die Maus bewegen:
- die Kamera schaut sich um, während du weiter dorthin läufst, wohin du gingst;
- Kopf und Oberkörper drehen mit, das Sichtfeld weitet sich wie bei Over the Shoulder in 2D;
- Loslassen dreht die Kamera zurück, Zielen beendet das Umschauen.

Die mittlere Maustaste bleibt frei für Viewpoints Mausmodus. In 2D funktioniert Over the Shoulder wie vorher.

### 14) Hold to Zoom (`ViewpointZoomHold`)
Braucht Project Viewpoint QOL. Gezoomt wird nur, solange du eine Taste hältst (Standard linke Umschalttaste) und das Mausrad drehst, in größeren Schritten; Loslassen springt zur normalen Sicht zurück. Das Mausrad allein zoomt nicht mehr. Die Taste wählt immer jeder Spieler selbst; die Schrittweite kann der Server festlegen.

## Schießen

### 12) Aim Spread (`ViewpointAimSpread`)
Braucht **Viewpoint True Ballistics** ([3812864848](https://steamcommunity.com/sharedfiles/filedetails/?id=3812864848)) und wird danach geladen. Ändert das Spielgeschehen für deine eigenen Schüsse.
- Jeder Schuss geht an einen zufälligen Punkt im Zielkreis des Spiels (die Klammern), statt mit der eigenen Streuung von True Ballistics.
  - Der Kreis wird kleiner mit Zielen-Fertigkeit, ruhigem Zielen und Fokus und größer mit Bewegung und Moodles (Mods, die ihn ändern, wirken mit).
  - Standardmäßig gilt er in **1,5-facher** Größe. Danach fliegt die Kugel normal mit True Ballistics.
- Nur aktiv mit der Sandbox-Option **„Schusswaffen nutzen Schadenschance“ = Deaktiviert**. Mit „Nur Zombies“ oder „Alle“ gehen Schüsse genau aufs Fadenkreuz.
- Büsche halten Kugeln nicht auf, können sie aber ablenken: 10 % pro Busch, bis 10° seitlich und nach oben/unten (beides einstellbar). Die Kugel fliegt weiter und trifft, was auf der neuen Bahn liegt.
- Mit Project Viewpoint ADS: dessen Hüftfeuer-Streuung ist aus, solange Aim Spread an ist.

### 13) Crosshair Options (`ViewpointReticle`)
Viewpoints Mittelpunkt lässt sich immer zeigen, nur mit Schusswaffe, mit jeder Waffe, nur beim Zielen oder nie. Wählbar sind Punkt, Kreuz oder Ring sowie Größe, Deckkraft, Farbe und Rand. Die Zielklammern des Spiels lassen sich in 3D ausblenden.

### 15) Vehicle Ballistics & Penetration (`ViewpointVehicleBallistics`)
Realistische Fahrzeugtreffer, Durchschüsse durch Wände und Fahrzeuge, Abpraller und Schießen aus Fahrzeugen. Ausführlich: **[VehicleBallistics.md](VehicleBallistics.md)**.

## Interaktionsliste

### 9) Interaction List Cleanup (`ViewpointInteractList`)
Viewpoint baut die Liste neben dem Fadenkreuz aus dem Rechtsklick-Menü des Spiels. Mod-Einträge über dich selbst erschienen deshalb bei jedem Objekt und wurden sogar zur Überschrift („Wellness“ über einer Tür). Sie sind jetzt nur in Viewpoints Liste ausgeblendet:
- Lifestyle: Wellness/Yoga, Tanzen, Wissen teilen, Putzen;
- das Admin-Debug-Menü;
- Container Options, Decoholic, Door Bar, Open All Containers Auto-Schließen.

Hide-Anywhere-Einträge rücken nach oben. Hinweistexte wie „Missing a Hammer...“ tauchen nicht mehr auf. Das normale Rechtsklick-Menü bleibt unverändert.

---

## Server-Einstellungen (Sandbox > Viewpoint Fixes)

Alle Mod-Optionen der Mods 10, 12, 13, 14 und 15 stehen auf der Sandbox-Seite **Viewpoint Fixes**. Für jede der Mods 10, 12, 13 und 14 legt der Admin fest, **wer die Optionen einstellt**:

| Wert | Bedeutung |
|---|---|
| 1 = Spieler wählen selbst (Standard) | Jeder nutzt Optionen > Mods; die Sandbox-Werte gelten nicht. |
| 2 = Server-Werte (ausgegraut) | Alle nutzen die Sandbox-Werte; die Einträge unter Optionen > Mods sind gesperrt und zeigen einen Hinweis. |
| 3 = Server-Werte (ausgeblendet) | Wie 2, aber die Einträge sind ausgeblendet, damit die Liste kurz bleibt. |

- Die eigenen Werte der Spieler bleiben in `ModOptions.ini` und gelten in anderen Spielen wieder.
- Tastenbelegungen stellt immer jeder selbst ein.
- Änderungen, die ein Admin im laufenden Spiel macht, gelten nach spätestens einer Spielminute.

Alle Optionen mit Standardwerten: [viewpointfixes-sandbox.generated.md](viewpointfixes-sandbox.generated.md). Die Optionen der Fahrzeug-Mod: [vehicle-ballistics-options.generated.md](vehicle-ballistics-options.generated.md).

Um die Optionen **jeder anderen Mod** genauso zu steuern, gibt es [Mod Options Control](ModOptionsControl.md).

## Hinweise
- Die Mod-IDs stehen in Klammern. Wer zusätzlich die einzelnen Items „Viewpoint 2D to 3D“ / „– Extended Patch“ abonniert hat, schaltet sie aus: Sie nutzen dieselben IDs.
- Für Türbeleuchtung das Original Door Fix von GamerKreep nutzen ([3812151347](https://steamcommunity.com/sharedfiles/filedetails/?id=3812151347)).
- Probleme bitte mit Mod-Name und den passenden Zeilen aus `console.txt` melden ([Issues](https://github.com/T0XiQ96/project-viewpoint-fixes/issues)).
