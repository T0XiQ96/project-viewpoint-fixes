# Mod Options Control (Server)

Mod-ID `ModOptionsControl` · Build 42.13+ · kein Java, kein ZombieBuddy · [English](../en/ModOptionsControl.md)

Server-Admins legen fest, welche **Mod-Optionen** (Optionen > Mods) Spieler ändern dürfen, und stellen die Werte für alle ein. Das funktioniert mit jeder Mod, die das Mod-Optionen-System des Spiels nutzt (`PZAPI.ModOptions`); keine Mod muss geändert werden.

## Schnellstart

1. `ModOptionsControl` auf dem Server eintragen (`Mods=` und `WorkshopItems=`); Clients bekommen es automatisch.
2. Als Admin die eigenen Werte wie gewohnt unter Optionen > Mods einstellen.
3. Das **Admin-Fenster** öffnen: Admin-Panel > *Mod Options Control*, oder Optionen > Mods > *Mod Options Control* > *Admin-Fenster öffnen*.
4. In der linken Spalte einer Mod (Seitenzeile) oder einzelner Optionen klicken, bis **Gesperrt** dasteht.
5. **Meine Werte für alle gesperrten** (oder *Meinen Wert nehmen (Auswahl)* für eine Option) und dann **Speichern & senden** drücken.

Die Spieler bekommen die Werte sofort. Gesperrte Optionen verschwinden aus ihren Optionen > Mods.

## Regeln

Jede Seite (Mod) und jede Option hat eine Regel: **–** (erbt), **Gesperrt** oder **Frei**. Ob eine Option gesperrt ist, entscheidet in dieser Reihenfolge:
1. die Regel der Option selbst;
2. die Regel ihrer Seite;
3. die Sandbox-Listen *Immer frei* / *Immer gesperrt* (erst einzelne Optionen, dann Seiten);
4. die Sandbox-*Grundregel* (Standard: Frei).

Eine gesperrte Option bekommt den gespeicherten Admin-Wert. Ohne einen solchen gilt **der Standardwert der Mod**.

**Knöpfe im Admin-Fenster:**

| Knopf | Wirkung |
|---|---|
| Suchen | Mods und Optionen filtern |
| Alle auf-/zuklappen | |
| Regel wechseln (Auswahl) | |
| Alles sperren / Alles freigeben | |
| Alle Regeln löschen | |
| Meine Werte für alle gesperrten | |
| Meinen Wert nehmen (Auswahl) | |
| Standard der Mod (Auswahl) | entfernt den Server-Wert |
| Exportieren / Importieren | `Zomboid/Lua/ModOptionsControl_rules.txt`, um Regeln auf einen anderen Server zu übertragen |
| Speichern & senden | |

Berechtigung: alle, deren Rolle die Sandbox ändern darf (`Capability.SandboxOptions`), außerdem Zugriffsstufe Admin; im Einzelspieler immer. Unberechtigte Änderungen werden abgelehnt und im Server-Log vermerkt.

## Was Spieler sehen

- **Gesperrte Optionen ausgeblendet** (Standard): Es steht nur da, was sie ändern dürfen. Oben bei der Mod sagt ein Hinweis, wie viele Einstellungen der Server festlegt. Mods, bei denen nichts mehr übrig bleibt, verschwinden aus der Liste.
- **Nicht ausgeblendet:** Die Optionen bleiben sichtbar, ausgegraut und mit **[Server]** markiert.
- Auf der Seite *Mod Options Control* sehen Spieler, wie viele Optionen der Server festlegt, und können ausgeblendete ausgegraut einblenden.
- Die eigenen Werte bleiben in `ModOptions.ini` gespeichert. Erzwungene Werte werden nie dorthin geschrieben, andere Spiele und Server behalten also die Einstellungen des Spielers.
- Änderungen vom Admin und Sandbox-Änderungen im laufenden Spiel gelten sofort; die Optionsseite wird beim nächsten Öffnen neu aufgebaut.

## Sandbox (Seite *Mod Options Control*)

[mod-options-control-options.generated.md](mod-options-control-options.generated.md)

## Wie es funktioniert

- `PZAPI.ModOptions` ist reines Lua. Jede Option hat einen Wert und einen *aktiv*-Schalter, jede Seite eine `apply()`-Funktion.
- Mod Options Control setzt den Wert, ruft `onChangeApply` der Option und `apply()` der Seite auf, genau wie der Knopf *Übernehmen*, und sperrt den Eintrag.
- Laden und Speichern von `ModOptions.ini` sind umhüllt, damit erzwungene Werte nie die eigenen ersetzen.
- Ausgeblendet wird, indem die Einträge beim Aufbau des Mods-Reiters gefiltert werden; bei Regeländerungen wird dieser Reiter neu aufgebaut.
- Die Regeln liegen auf dem Server in der globalen ModData (`ModOptionsControl`, wird mit der Welt gespeichert) und gehen an jeden Client beim Beitritt und nach jeder Änderung.

## Gut zu wissen

- Beim Beitritt zu einem Server lädt das Spiel die Lua-Mods des Servers neu. Jede Mod, die ein Spieler im Mehrspieler unter Optionen > Mods sieht, ist also eine Server-Mod; Lua-Mods, die nur der Spieler aktiviert hat, laufen dort nicht.
- Mods mit eigenem Einstellungssystem (ältere „Mod Options“-Bibliothek, eigene Dateien) lassen sich nicht steuern.
- Tastenbelegungen werden nur gesperrt, wenn *Tastenbelegungen sperrbar* an ist.
- Für Project Viewpoint - Fixes gibt es zusätzlich einen eingebauten Schalter pro Mod unter Sandbox > Viewpoint Fixes; beides arbeitet zusammen.

## Dateien

| Datei | Zweck |
|---|---|
| `shared/ModOptionsControl/MOC_Shared.lua` | Regeln, Berechtigung, Prüfung |
| `server/ModOptionsControl/MOC_Server.lua` | Speichern, Senden |
| `client/ModOptionsControl/MOC_Client.lua` | Anwenden, Ausblenden, Neuaufbau |
| `client/ModOptionsControl/MOC_AdminUI.lua` | Admin-Fenster, eigene Optionsseite, Knopf im Admin-Panel |
