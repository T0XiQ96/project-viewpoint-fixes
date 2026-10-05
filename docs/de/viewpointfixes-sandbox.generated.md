### 3D-Waffeneffekte (ViewpointEffects3D)

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.Effects3D_Control` | Wer stellt die Optionen ein | 1 | 1 = Spieler wählen selbst / 2 = Server-Werte (ausgegraut) / 3 = Server-Werte (ausgeblendet) | Spieler wählen selbst: jeder nutzt Optionen > Mods, die Werte darunter gelten nicht. Server-Werte: alle nutzen die Werte darunter; die Einträge unter Optionen > Mods sind ausgegraut oder ausgeblendet. Tastenbelegungen stellt immer jeder selbst ein. |
| `ViewpointFixes.Effects3D_Tracers` | Leuchtspuren (`tracers`) | an |  | Die Leuchtspuren des Spiels in 3D. Aussehen je Munitionstyp. |
| `ViewpointFixes.Effects3D_TracerWidth` | Dicke der Leuchtspur (`tracerWidth`) | 1,1 | 0,3 – 3 | 1 = wie vom Spiel vorgegeben. |
| `ViewpointFixes.Effects3D_TracerPath` | Schwache Flugbahn-Linie (`tracerPath`) | an |  | Die dünne, verblassende Linie von der Mündung zur Kugel. |
| `ViewpointFixes.Effects3D_Flash` | Mündungsfeuer (`flash`) | an |  | Heller Blitz an der echten Mündung, wenn das Spiel sein Mündungsfeuer zeigt. |
| `ViewpointFixes.Effects3D_FlashSize` | Größe des Mündungsfeuers (`flashSize`) | 1,25 | 0,3 – 2 | 1,25 = Standardgröße. |
| `ViewpointFixes.Effects3D_Light` | Mündungsfeuer beleuchtet die Umgebung (`light`) | an |  | Kurzes warmes Licht an der Mündung bei jedem Schuss. |
| `ViewpointFixes.Effects3D_LightStrength` | Stärke des Mündungslichts (`lightStrength`) | 1,1 | 0,2 – 2 | 1,1 = Standard. |
| `ViewpointFixes.Effects3D_Smoke` | Pulverrauch (`smoke`) | an |  | Rauchwolken, die an der Mündung entstehen und in der Welt bleiben. |
| `ViewpointFixes.Effects3D_SmokeAmount` | Rauchmenge (`smokeAmount`) | 0,5 | 0 – 2 | 0,5 = Standard. |
| `ViewpointFixes.Effects3D_SmokeFP` | Rauchgröße in der Ich-Perspektive (`smokeFP`) | 0,8 | 0,5 – 3 | Faktor auf die Wolkengröße in der Ich-Perspektive (3. Person immer 1). |
| `ViewpointFixes.Effects3D_Impacts` | Einschläge (`impacts`) | an |  | Staub, Splitter, Funken und Abpraller je nach Material. Größe je nach Munition. |
| `ViewpointFixes.Effects3D_Holes` | Einschusslöcher (`holes`) | an |  | Löcher bleiben an Wänden und Objekten, kurzlebige Spuren auf Erde, Gras, Sand und Schnee. |
| `ViewpointFixes.Effects3D_HoleTime` | Lebensdauer der Einschusslöcher (`holeTime`) | 5 | 0,1 – 5 | 1 = etwa 2 Minuten an Wänden, 25 Sekunden am Boden; 5 = etwa 10 / 2 Minuten. |
| `ViewpointFixes.Effects3D_Fireflies` | JB's Fireflies in 3D (`fireflies`) | an |  | An: Glühwürmchen in 3D, hinter Wänden verdeckt. Aus: keine Glühwürmchen in der 3D-Sicht. |

### Streuung im Zielkreis (ViewpointAimSpread)

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.AimSpread_Control` | Wer stellt die Optionen ein | 1 | 1 = Spieler wählen selbst / 2 = Server-Werte (ausgegraut) / 3 = Server-Werte (ausgeblendet) | Spieler wählen selbst: jeder nutzt Optionen > Mods, die Werte darunter gelten nicht. Server-Werte: alle nutzen die Werte darunter; die Einträge unter Optionen > Mods sind ausgegraut oder ausgeblendet. Tastenbelegungen stellt immer jeder selbst ein. |
| `ViewpointFixes.AimSpread_Spread` | Streuung im Zielkreis des Spiels (`spread`) | an |  | Jeder Schuss geht an einen zufälligen Punkt im Zielkreis des Spiels. Nur mit „Schusswaffen nutzen Schadenschance“ = Deaktiviert. |
| `ViewpointFixes.AimSpread_Scale` | Faktor auf die Kreisgröße (`scale`) | 1,5 | 0,25 – 2 | 1 = genau die Klammern, 1,5 = Standard. |
| `ViewpointFixes.AimSpread_Cover` | Büsche lenken Kugeln ab (`cover`) | an |  | Büsche halten Kugeln nicht auf, können sie aber ablenken. |
| `ViewpointFixes.AimSpread_BushChance` | Ablenkchance pro Busch (%) (`bushChance`) | 10 | 0 – 50 | Chance, dass eine Kugel in einem Busch abgelenkt wird. |
| `ViewpointFixes.AimSpread_DeflectDeg` | Ablenkwinkel (Grad) (`deflectDeg`) | 10 | 0 – 30 | Wie weit eine abgelenkte Kugel seitlich und nach oben/unten abweichen kann. |

### Fadenkreuz (ViewpointReticle)

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.Reticle_Control` | Wer stellt die Optionen ein | 1 | 1 = Spieler wählen selbst / 2 = Server-Werte (ausgegraut) / 3 = Server-Werte (ausgeblendet) | Spieler wählen selbst: jeder nutzt Optionen > Mods, die Werte darunter gelten nicht. Server-Werte: alle nutzen die Werte darunter; die Einträge unter Optionen > Mods sind ausgegraut oder ausgeblendet. Tastenbelegungen stellt immer jeder selbst ein. |
| `ViewpointFixes.Reticle_Mode` | Punkt anzeigen (`mode`) | 1 | 1 = Immer / 2 = Nur mit Schusswaffe / 3 = Mit jeder Waffe / 4 = Nur beim Zielen / 5 = Nie | Wann Viewpoints Mittelpunkt gezeichnet wird. |
| `ViewpointFixes.Reticle_Style` | Form (`style`) | 1 | 1 = Punkt / 2 = Kreuz / 3 = Ring | Form des Fadenkreuzes. |
| `ViewpointFixes.Reticle_Size` | Größe (Pixel) (`size`) | 2 | 1 – 12 | 2 = Viewpoints Standardpunkt. |
| `ViewpointFixes.Reticle_Opacity` | Deckkraft (`opacity`) | 1 | 0,05 – 1 | 1 = voll deckend. |
| `ViewpointFixes.Reticle_Color` | Farbe (`color`) | 1 | 1 = Weiß / 2 = Grün / 3 = Rot / 4 = Gelb / 5 = Cyan / 6 = Pink |  |
| `ViewpointFixes.Reticle_Outline` | Dunkler Rand (`outline`) | an |  | Dünner dunkler Rand, damit das Fadenkreuz auf hellem Grund sichtbar bleibt. |
| `ViewpointFixes.Reticle_Brackets` | Zielkreis-Klammern des Spiels zeigen (`brackets`) | an |  | Die Klammern, die zeigen, wie ruhig du zielst (3D-Sicht). |

### Zoom halten (ViewpointZoomHold)

| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |
|---|---|---|---|---|
| `ViewpointFixes.ZoomHold_Control` | Wer stellt die Optionen ein | 1 | 1 = Spieler wählen selbst / 2 = Server-Werte (ausgegraut) / 3 = Server-Werte (ausgeblendet) | Spieler wählen selbst: jeder nutzt Optionen > Mods, die Werte darunter gelten nicht. Server-Werte: alle nutzen die Werte darunter; die Einträge unter Optionen > Mods sind ausgegraut oder ausgeblendet. Tastenbelegungen stellt immer jeder selbst ein. |
| `ViewpointFixes.ZoomHold_Step` | Zoomschritt pro Mausrad-Raste (`step`) | 15 | 5 – 40 | Änderung des Sichtfelds pro Raste. Die Zoom-Taste stellt jeder Spieler selbst ein. |

