# Bullet Impacts 2D

Steam Workshop: [3813696499](https://steamcommunity.com/sharedfiles/filedetails/?id=3813696499) · Build 42.21 · [English](../en/BulletImpacts2D.md)

Das Workshop-Item enthält zwei Mods.

## VFA_BulletImpacts2D – Effekte (Client)

Einschusslöcher, Funken, Staub, Splitter und Abpraller in der normalen isometrischen Sicht.

- **Einschlagpunkt:** der des Spiels (wo der Tracer endet: Wand, Tür, Fenster, Fahrzeug, Hindernis), ausgelesen von einem kleinen Java-Hook.
- **Material:** aus dem `MaterialType` des getroffenen Feldes, derselben Eigenschaft, die das Spiel für den Einschlag-Sound nutzt.

  | Material | Effekt |
  |---|---|
  | Holz | dunkles Loch, Splitter |
  | Metall | kleines Loch, Funken |
  | Gehärtetes Metall / Panzerglas | Schramme, oft Abpraller |
  | Stein, Ziegel, Beton | Loch, farbiger Staub |
  | Erde, Gras, Sand, Schnee | Spritzer, kurzlebige Spur |

- **Fahrzeuge:** Ist Vehicle Ballistics geladen (eine der beiden Fassungen), nehmen Einschläge das Fahrzeugmaterial (Glas, Blech, Gummi, Panzerung) statt des Bodens darunter. Feste Löcher gibt es an Fahrzeugen nicht, weil sie wegfahren.
- **Abpraller** sind auf harten Flächen bei flachem Winkel wahrscheinlicher. Die **Größe** hängt von der Munition ab.
- **Türen und Fenster:** Löcher verschwinden, wenn die Tür oder das Fenster geöffnet oder zerstört wird.
- **Licht:** Effekte werden im Dunkeln dunkler.
- **Optionen:** Optionen > Mods > Bullet Impacts 2D (Löcher, Lebensdauer, Funken, Staub, Abpraller, Größe).
- **Voraussetzung:** ZombieBuddy. Jeder Spieler, der die Effekte will, braucht die Mod.
- **3D-Sicht:** Mit Project Viewpoints 3D-Sicht tritt diese Mod zurück; ViewpointEffects3D zeichnet die 3D-Effekte.

## VFA_VehicleBallistics2D – Fahrzeugtreffer (Server + Client, optional)

Dasselbe Fahrzeugmodell wie [Vehicle Ballistics & Penetration](VehicleBallistics.md), als eigene Mod für Server ohne Project Viewpoint - Fixes.
- Sandbox-Seite *Fahrzeug-Ballistik 2D*, Optionen `VehicleBallistics2D.*`.
- *Auch in der 2D-Sicht* ist hier standardmäßig an.
- Ist `ViewpointVehicleBallistics` auch aktiv, übernimmt diese, und die Kopie bleibt untätig (Java und Lua).
- Braucht ZombieBuddy auf Server und Clients.
