# Schreibt Translate/EN und Translate/DE (Sandbox.json, UI.json) fuer Mod Options Control.
import json, os

B = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Contents", "mods", "ModOptionsControl", "42", "media", "lua", "shared", "Translate")

sb_en = {
    "Sandbox_ModOptionsControl": "Mod Options Control",
    "Sandbox_ModOptionsControl_Enabled": "Enable Mod Options Control",
    "Sandbox_ModOptionsControl_Enabled_tooltip": "Off: every player sets all mod options freely, all rules are ignored.",
    "Sandbox_ModOptionsControl_DefaultPolicy": "Default rule",
    "Sandbox_ModOptionsControl_DefaultPolicy_tooltip": "For options without their own rule. Free (default): players may change everything until an admin locks something. Locked: everything uses the admin's values unless freed.",
    "Sandbox_ModOptionsControl_DefaultPolicy_option1": "Free",
    "Sandbox_ModOptionsControl_DefaultPolicy_option2": "Locked",
    "Sandbox_ModOptionsControl_HideLocked": "Hide locked options",
    "Sandbox_ModOptionsControl_HideLocked_tooltip": "On: locked options disappear from Options > Mods, so players only see what they may change (they can still show them greyed out on the Mod Options Control page). Off: they stay visible, greyed out and marked [Server].",
    "Sandbox_ModOptionsControl_AdminsBypass": "Admins keep their own options",
    "Sandbox_ModOptionsControl_AdminsBypass_tooltip": "On: players who may change the sandbox are not locked, so they can set their values and copy them for everyone in the admin window. Off: admins get the same values as everyone (also in single player).",
    "Sandbox_ModOptionsControl_KeybindsLockable": "Key bindings can be locked",
    "Sandbox_ModOptionsControl_KeybindsLockable_tooltip": "Off (recommended): key bindings in mod options always stay the player's choice.",
    "Sandbox_ModOptionsControl_LockedList": "Always locked",
    "Sandbox_ModOptionsControl_LockedList_tooltip": "Optional list separated by ; - a page ID locks all options of that mod, PageID.optionID one option. Page IDs are shown in brackets in the admin window. Rules set in the admin window win.",
    "Sandbox_ModOptionsControl_FreeList": "Always free",
    "Sandbox_ModOptionsControl_FreeList_tooltip": "Like “Always locked”, for exceptions when the default rule is Locked.",
}
sb_de = {
    "Sandbox_ModOptionsControl": "Mod Options Control",
    "Sandbox_ModOptionsControl_Enabled": "Mod Options Control aktivieren",
    "Sandbox_ModOptionsControl_Enabled_tooltip": "Aus: jeder Spieler stellt alle Mod-Optionen frei ein, alle Regeln werden ignoriert.",
    "Sandbox_ModOptionsControl_DefaultPolicy": "Grundregel",
    "Sandbox_ModOptionsControl_DefaultPolicy_tooltip": "Für Optionen ohne eigene Regel. Frei (Standard): Spieler dürfen alles ändern, bis ein Admin etwas sperrt. Gesperrt: alles nutzt die Admin-Werte, außer es ist freigegeben.",
    "Sandbox_ModOptionsControl_DefaultPolicy_option1": "Frei",
    "Sandbox_ModOptionsControl_DefaultPolicy_option2": "Gesperrt",
    "Sandbox_ModOptionsControl_HideLocked": "Gesperrte Optionen ausblenden",
    "Sandbox_ModOptionsControl_HideLocked_tooltip": "An: gesperrte Optionen verschwinden aus Optionen > Mods, Spieler sehen nur, was sie ändern dürfen (auf der Seite Mod Options Control können sie sie ausgegraut einblenden). Aus: sie bleiben sichtbar, ausgegraut und mit [Server] markiert.",
    "Sandbox_ModOptionsControl_AdminsBypass": "Admins behalten eigene Optionen",
    "Sandbox_ModOptionsControl_AdminsBypass_tooltip": "An: wer die Sandbox ändern darf, wird nicht gesperrt, stellt seine Werte ein und übernimmt sie im Admin-Fenster für alle. Aus: Admins bekommen dieselben Werte wie alle (auch im Einzelspieler).",
    "Sandbox_ModOptionsControl_KeybindsLockable": "Tastenbelegungen sperrbar",
    "Sandbox_ModOptionsControl_KeybindsLockable_tooltip": "Aus (empfohlen): Tastenbelegungen in Mod-Optionen stellt immer jeder selbst ein.",
    "Sandbox_ModOptionsControl_LockedList": "Immer gesperrt",
    "Sandbox_ModOptionsControl_LockedList_tooltip": "Optionale Liste, getrennt mit ; - eine Seiten-ID sperrt alle Optionen dieser Mod, SeitenID.optionID eine einzelne. Die Seiten-IDs stehen im Admin-Fenster in Klammern. Regeln aus dem Admin-Fenster haben Vorrang.",
    "Sandbox_ModOptionsControl_FreeList": "Immer frei",
    "Sandbox_ModOptionsControl_FreeList_tooltip": "Wie „Immer gesperrt“, für Ausnahmen, wenn die Grundregel Gesperrt ist.",
}
ui_en = {
    "UI_MOC_Title": "Mod Options Control", "UI_MOC_ServerTag": "[Server]",
    "UI_MOC_PageNote": "%1 settings of this mod are set by the server (marked [Server]).",
    "UI_MOC_PageNoteHidden": "%1 settings of this mod are set by the server and hidden.",
    "UI_MOC_PlayerHelp": "The server can set mod options for everyone. Options set by the server are hidden or greyed out; your own values stay saved for other games.",
    "UI_MOC_ShowLocked": "Show options set by the server",
    "UI_MOC_ShowLocked_tooltip": "Show them greyed out instead of hiding them (applies the next time you open the options).",
    "UI_MOC_OpenAdmin": "Open admin window",
    "UI_MOC_OpenAdmin_tooltip": "Lock mod options for players and set their values. Needs permission to change the sandbox.",
    "UI_MOC_StatusMenu": "Only active in a game.", "UI_MOC_StatusNone": "The server does not set any mod options.",
    "UI_MOC_Status": "The server sets %1 options of %2 mods.", "UI_MOC_Denied": "Mod Options Control: no permission",
    "UI_MOC_ValueDefault": "(mod default)", "UI_MOC_On": "on", "UI_MOC_Off": "off", "UI_MOC_Search": "Search",
    "UI_MOC_LockAll": "Lock everything", "UI_MOC_FreeAll": "Free everything", "UI_MOC_ClearRules": "Clear all rules",
    "UI_MOC_TakeAllMine": "My values for all locked", "UI_MOC_Export": "Export", "UI_MOC_Import": "Import",
    "UI_MOC_TakeMine": "Use my value (selected)", "UI_MOC_UseDefault": "Mod default (selected)", "UI_MOC_Cycle": "Change rule (selected)",
    "UI_MOC_ExpandAll": "Expand / collapse all", "UI_MOC_Save": "Save & send",
    "UI_MOC_Locked": "Locked", "UI_MOC_Free": "Free", "UI_MOC_Unsaved": "(not saved yet)",
    "UI_MOC_Info": "%1 options locked for players. Default rule: %2. %3",
    "UI_MOC_Exported": "Saved to Zomboid/Lua/%1", "UI_MOC_NoFile": "File Zomboid/Lua/%1 not found",
    "UI_MOC_Imported": "Rules loaded - press Save & send", "UI_MOC_ImportFailed": "Could not read the file",
    "UI_MOC_Sent": "Rules sent to the server",
}
ui_de = {
    "UI_MOC_Title": "Mod Options Control", "UI_MOC_ServerTag": "[Server]",
    "UI_MOC_PageNote": "%1 Einstellungen dieser Mod legt der Server fest (mit [Server] markiert).",
    "UI_MOC_PageNoteHidden": "%1 Einstellungen dieser Mod legt der Server fest; sie sind ausgeblendet.",
    "UI_MOC_PlayerHelp": "Der Server kann Mod-Optionen für alle festlegen. Solche Optionen sind ausgeblendet oder ausgegraut; deine eigenen Werte bleiben für andere Spiele gespeichert.",
    "UI_MOC_ShowLocked": "Vom Server festgelegte Optionen zeigen",
    "UI_MOC_ShowLocked_tooltip": "Ausgegraut zeigen statt ausblenden (gilt beim nächsten Öffnen der Optionen).",
    "UI_MOC_OpenAdmin": "Admin-Fenster öffnen",
    "UI_MOC_OpenAdmin_tooltip": "Mod-Optionen für Spieler sperren und ihre Werte festlegen. Braucht das Recht, die Sandbox zu ändern.",
    "UI_MOC_StatusMenu": "Nur im laufenden Spiel aktiv.", "UI_MOC_StatusNone": "Der Server legt keine Mod-Optionen fest.",
    "UI_MOC_Status": "Der Server legt %1 Optionen von %2 Mods fest.", "UI_MOC_Denied": "Mod Options Control: keine Berechtigung",
    "UI_MOC_ValueDefault": "(Standard der Mod)", "UI_MOC_On": "an", "UI_MOC_Off": "aus", "UI_MOC_Search": "Suchen",
    "UI_MOC_LockAll": "Alles sperren", "UI_MOC_FreeAll": "Alles freigeben", "UI_MOC_ClearRules": "Alle Regeln löschen",
    "UI_MOC_TakeAllMine": "Meine Werte für alle gesperrten", "UI_MOC_Export": "Exportieren", "UI_MOC_Import": "Importieren",
    "UI_MOC_TakeMine": "Meinen Wert nehmen (Auswahl)", "UI_MOC_UseDefault": "Standard der Mod (Auswahl)", "UI_MOC_Cycle": "Regel wechseln (Auswahl)",
    "UI_MOC_ExpandAll": "Alle auf-/zuklappen", "UI_MOC_Save": "Speichern & senden",
    "UI_MOC_Locked": "Gesperrt", "UI_MOC_Free": "Frei", "UI_MOC_Unsaved": "(noch nicht gespeichert)",
    "UI_MOC_Info": "%1 Optionen für Spieler gesperrt. Grundregel: %2. %3",
    "UI_MOC_Exported": "Gespeichert in Zomboid/Lua/%1", "UI_MOC_NoFile": "Datei Zomboid/Lua/%1 nicht gefunden",
    "UI_MOC_Imported": "Regeln geladen - Speichern & senden drücken", "UI_MOC_ImportFailed": "Datei konnte nicht gelesen werden",
    "UI_MOC_Sent": "Regeln an den Server geschickt",
}
assert set(ui_en) == set(ui_de) and set(sb_en) == set(sb_de)
for lang, sb, ui in (("EN", sb_en, ui_en), ("DE", sb_de, ui_de)):
    os.makedirs(os.path.join(B, lang), exist_ok=True)
    for name, d in (("Sandbox.json", sb), ("UI.json", ui)):
        with open(os.path.join(B, lang, name), "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, ensure_ascii=False, indent=4)
            f.write("\n")
print("ok")
