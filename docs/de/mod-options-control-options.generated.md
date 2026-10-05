| Sandbox-Option | Bezeichnung | Standard | Beschreibung |
|---|---|---|---|
| `ModOptionsControl.Enabled` | Mod Options Control aktivieren | an | Aus: jeder Spieler stellt alle Mod-Optionen frei ein, alle Regeln werden ignoriert. |
| `ModOptionsControl.DefaultPolicy` | Grundregel | 1 = Frei | Für Optionen ohne eigene Regel. Frei (Standard): Spieler dürfen alles ändern, bis ein Admin etwas sperrt. Gesperrt: alles nutzt die Admin-Werte, außer es ist freigegeben. |
| `ModOptionsControl.HideLocked` | Gesperrte Optionen ausblenden | an | An: gesperrte Optionen verschwinden aus Optionen > Mods, Spieler sehen nur, was sie ändern dürfen (auf der Seite Mod Options Control können sie sie ausgegraut einblenden). Aus: sie bleiben sichtbar, ausgegraut und mit [Server] markiert. |
| `ModOptionsControl.AdminsBypass` | Admins behalten eigene Optionen | an | An: wer die Sandbox ändern darf, wird nicht gesperrt, stellt seine Werte ein und übernimmt sie im Admin-Fenster für alle. Aus: Admins bekommen dieselben Werte wie alle (auch im Einzelspieler). |
| `ModOptionsControl.KeybindsLockable` | Tastenbelegungen sperrbar | aus | Aus (empfohlen): Tastenbelegungen in Mod-Optionen stellt immer jeder selbst ein. |
| `ModOptionsControl.LockedList` | Immer gesperrt | – | Optionale Liste, getrennt mit ; - eine Seiten-ID sperrt alle Optionen dieser Mod, SeitenID.optionID eine einzelne. Die Seiten-IDs stehen im Admin-Fenster in Klammern. Regeln aus dem Admin-Fenster haben Vorrang. |
| `ModOptionsControl.FreeList` | Immer frei | – | Wie „Immer gesperrt“, für Ausnahmen, wenn die Grundregel Gesperrt ist. |
