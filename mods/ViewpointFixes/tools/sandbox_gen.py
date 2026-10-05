# Erzeugt fuer die ViewpointFixes-Mods mit Optionen die Sandbox-Seite "Viewpoint Fixes":
#   <mod>/42/media/sandbox-options.txt
#   <mod>/42/media/lua/shared/Translate/{EN,DE}/Sandbox.json  (+ UI-Hinweistexte in UI_VF.json)
#   <mod>/42/media/lua/client/ViewpointFixes/VF_SandboxLock.lua  (Kopie aus ViewpointEffects3D)
# Aufruf: python tools/sandbox_gen.py   (aus dem ViewpointFixes-Ordner oder von ueberall)
import json, os, shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Contents", "mods")
LOCK_SRC = os.path.join(ROOT, "ViewpointEffects3D", "42", "media", "lua", "client", "ViewpointFixes", "VF_SandboxLock.lua")

# (optionId, SandboxName, Typ, Standard, min, max, EN-Name, DE-Name, EN-Tooltip, DE-Tooltip [, Werte EN, Werte DE])
MODS = {
    "ViewpointEffects3D": ("Effects3D", "3D Gun Effects", "3D-Waffeneffekte", [
        ("tracers", "Tracers", "boolean", True, None, None, "Bullet tracers", "Leuchtspuren",
         "The game's own tracers, drawn in 3D. Look per ammo type.", "Die Leuchtspuren des Spiels in 3D. Aussehen je Munitionstyp."),
        ("tracerWidth", "TracerWidth", "double", 1.1, 0.3, 3.0, "Tracer thickness", "Dicke der Leuchtspur",
         "1 = as set by the game.", "1 = wie vom Spiel vorgegeben."),
        ("tracerPath", "TracerPath", "boolean", True, None, None, "Faint bullet path line", "Schwache Flugbahn-Linie",
         "The thin fading line from the muzzle to the bullet.", "Die dünne, verblassende Linie von der Mündung zur Kugel."),
        ("flash", "Flash", "boolean", True, None, None, "Muzzle flash glow", "Mündungsfeuer",
         "A bright flash at the real muzzle whenever the game shows its own muzzle flash.", "Heller Blitz an der echten Mündung, wenn das Spiel sein Mündungsfeuer zeigt."),
        ("flashSize", "FlashSize", "double", 1.25, 0.3, 2.0, "Muzzle flash size", "Größe des Mündungsfeuers",
         "1.25 = default size.", "1,25 = Standardgröße."),
        ("light", "Light", "boolean", True, None, None, "Muzzle flash lights the surroundings", "Mündungsfeuer beleuchtet die Umgebung",
         "A short warm light at the muzzle for every shot.", "Kurzes warmes Licht an der Mündung bei jedem Schuss."),
        ("lightStrength", "LightStrength", "double", 1.1, 0.2, 2.0, "Muzzle light strength", "Stärke des Mündungslichts",
         "1.1 = default.", "1,1 = Standard."),
        ("smoke", "Smoke", "boolean", True, None, None, "Gun smoke", "Pulverrauch",
         "Smoke puffs that start at the muzzle and stay in the world.", "Rauchwolken, die an der Mündung entstehen und in der Welt bleiben."),
        ("smokeAmount", "SmokeAmount", "double", 0.5, 0.0, 2.0, "Smoke amount", "Rauchmenge",
         "0.5 = default.", "0,5 = Standard."),
        ("smokeFP", "SmokeFP", "double", 0.8, 0.5, 3.0, "Smoke size in first person", "Rauchgröße in der Ich-Perspektive",
         "Factor on the puff size in first person (third person always 1).", "Faktor auf die Wolkengröße in der Ich-Perspektive (3. Person immer 1)."),
        ("impacts", "Impacts", "boolean", True, None, None, "Bullet impacts", "Einschläge",
         "Dust, splinters, sparks and ricochets by material. Size depends on the ammo.", "Staub, Splitter, Funken und Abpraller je nach Material. Größe je nach Munition."),
        ("holes", "Holes", "boolean", True, None, None, "Bullet holes", "Einschusslöcher",
         "Holes stay on walls and objects, short-lived marks on dirt, grass, sand and snow.", "Löcher bleiben an Wänden und Objekten, kurzlebige Spuren auf Erde, Gras, Sand und Schnee."),
        ("holeTime", "HoleTime", "double", 5.0, 0.1, 5.0, "Bullet hole lifetime", "Lebensdauer der Einschusslöcher",
         "1 = about 2 minutes on walls, 25 seconds on the ground; 5 = about 10 / 2 minutes.", "1 = etwa 2 Minuten an Wänden, 25 Sekunden am Boden; 5 = etwa 10 / 2 Minuten."),
        ("fireflies", "Fireflies", "boolean", True, None, None, "JB's Fireflies in 3D", "JB's Fireflies in 3D",
         "On: fireflies drawn in 3D, hidden behind walls. Off: no fireflies in the 3D view.", "An: Glühwürmchen in 3D, hinter Wänden verdeckt. Aus: keine Glühwürmchen in der 3D-Sicht."),
    ]),
    "ViewpointAimSpread": ("AimSpread", "Aim Spread", "Streuung im Zielkreis", [
        ("spread", "Spread", "boolean", True, None, None, "Spread inside the game's aiming circle", "Streuung im Zielkreis des Spiels",
         "Every shot leaves at a random point inside the game's aiming circle. Only with Firearms Use Damage Chance = Disabled.",
         "Jeder Schuss geht an einen zufälligen Punkt im Zielkreis des Spiels. Nur mit „Schusswaffen nutzen Schadenschance“ = Deaktiviert."),
        ("scale", "Scale", "double", 1.5, 0.25, 2.0, "Circle size factor", "Faktor auf die Kreisgröße",
         "1 = exactly the brackets, 1.5 = default.", "1 = genau die Klammern, 1,5 = Standard."),
        ("cover", "Cover", "boolean", True, None, None, "Bushes deflect bullets", "Büsche lenken Kugeln ab",
         "Bushes do not stop bullets but can deflect them.", "Büsche halten Kugeln nicht auf, können sie aber ablenken."),
        ("bushChance", "BushChance", "integer", 10, 0, 50, "Deflection chance per bush (%)", "Ablenkchance pro Busch (%)",
         "Chance that a bullet flying through a bush is deflected.", "Chance, dass eine Kugel in einem Busch abgelenkt wird."),
        ("deflectDeg", "DeflectDeg", "integer", 10, 0, 30, "Deflection angle (degrees)", "Ablenkwinkel (Grad)",
         "How far a deflected bullet can turn away, sideways and up/down.", "Wie weit eine abgelenkte Kugel seitlich und nach oben/unten abweichen kann."),
    ]),
    "ViewpointReticle": ("Reticle", "Crosshair Options", "Fadenkreuz", [
        ("mode", "Mode", "enum", 1, None, None, "Show the dot", "Punkt anzeigen",
         "When Viewpoint's centre dot is drawn.", "Wann Viewpoints Mittelpunkt gezeichnet wird.",
         ["Always", "Only with a firearm", "With any weapon", "Only while aiming", "Never"],
         ["Immer", "Nur mit Schusswaffe", "Mit jeder Waffe", "Nur beim Zielen", "Nie"]),
        ("style", "Style", "enum", 1, None, None, "Style", "Form", "Shape of the crosshair.", "Form des Fadenkreuzes.",
         ["Dot", "Cross", "Ring"], ["Punkt", "Kreuz", "Ring"]),
        ("size", "Size", "integer", 2, 1, 12, "Size (pixels)", "Größe (Pixel)", "2 = Viewpoint's default dot.", "2 = Viewpoints Standardpunkt."),
        ("opacity", "Opacity", "double", 1.0, 0.05, 1.0, "Opacity", "Deckkraft", "1 = fully opaque.", "1 = voll deckend."),
        ("color", "Color", "enum", 1, None, None, "Colour", "Farbe", "", "",
         ["White", "Green", "Red", "Yellow", "Cyan", "Pink"], ["Weiß", "Grün", "Rot", "Gelb", "Cyan", "Pink"]),
        ("outline", "Outline", "boolean", True, None, None, "Dark outline", "Dunkler Rand",
         "A thin dark edge so the crosshair stays visible on bright backgrounds.", "Dünner dunkler Rand, damit das Fadenkreuz auf hellem Grund sichtbar bleibt."),
        ("brackets", "Brackets", "boolean", True, None, None, "Show the game's aiming brackets", "Zielkreis-Klammern des Spiels zeigen",
         "The brackets that show how steady your aim is (3D view).", "Die Klammern, die zeigen, wie ruhig du zielst (3D-Sicht)."),
    ]),
    "ViewpointZoomHold": ("ZoomHold", "Hold to Zoom", "Zoom halten", [
        ("step", "Step", "integer", 15, 5, 40, "Zoom step per wheel notch", "Zoomschritt pro Mausrad-Raste",
         "Field-of-view change per notch. The zoom key itself always stays the player's choice.",
         "Änderung des Sichtfelds pro Raste. Die Zoom-Taste stellt jeder Spieler selbst ein."),
    ]),
}

CONTROL_EN = ("Who sets the options", "Players choose", "Server values (greyed out)", "Server values (hidden)")
CONTROL_DE = ("Wer stellt die Optionen ein", "Spieler wählen selbst", "Server-Werte (ausgegraut)", "Server-Werte (ausgeblendet)")
CONTROL_TIP_EN = ("Players choose: everyone uses Options > Mods, the values below are ignored. "
                  "Server values: everyone uses the values below; the entries in Options > Mods are greyed out or hidden. "
                  "Key bindings always stay the player's choice.")
CONTROL_TIP_DE = ("Spieler wählen selbst: jeder nutzt Optionen > Mods, die Werte darunter gelten nicht. "
                  "Server-Werte: alle nutzen die Werte darunter; die Einträge unter Optionen > Mods sind ausgegraut oder ausgeblendet. "
                  "Tastenbelegungen stellt immer jeder selbst ein.")

UI = {
    "EN": {"UI_ViewpointFixes_SetByServer": "Some settings of this mod are set by the server (Sandbox > Viewpoint Fixes).",
           "UI_ViewpointFixes_SetByServerHidden": "The settings of this mod are set by the server (Sandbox > Viewpoint Fixes) and hidden here."},
    "DE": {"UI_ViewpointFixes_SetByServer": "Einige Einstellungen dieser Mod legt der Server fest (Sandbox > Viewpoint Fixes).",
           "UI_ViewpointFixes_SetByServerHidden": "Die Einstellungen dieser Mod legt der Server fest (Sandbox > Viewpoint Fixes); sie sind hier ausgeblendet."},
}


def fmt(v):
    if isinstance(v, bool): return "true" if v else "false"
    if isinstance(v, float): return repr(v)
    return str(v)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


for mod, (prefix, title_en, title_de, opts) in MODS.items():
    media = os.path.join(ROOT, mod, "42", "media")
    lines = ["VERSION = 1,", ""]
    tr = {"EN": {"Sandbox_ViewpointFixes": "Viewpoint Fixes"}, "DE": {"Sandbox_ViewpointFixes": "Viewpoint Fixes"}}
    base = f"ViewpointFixes_{prefix}"
    lines += [f"option ViewpointFixes.{prefix}_Control", "{",
              "    type = enum,", "    numValues = 3,", "    default = 1,", "    page = ViewpointFixes,",
              f"    translation = {base}_Control,", "    valueTranslation = ViewpointFixes_Control,", "}", ""]
    for lang, ctl, tip, title in (("EN", CONTROL_EN, CONTROL_TIP_EN, title_en), ("DE", CONTROL_DE, CONTROL_TIP_DE, title_de)):
        t = tr[lang]
        t[f"Sandbox_{base}_Control"] = f"{title}: {ctl[0]}"
        t[f"Sandbox_{base}_Control_tooltip"] = tip
        for i in range(3):
            t[f"Sandbox_ViewpointFixes_Control_option{i + 1}"] = ctl[i + 1]
    for o in opts:
        oid, name, typ, default, lo, hi, en, de, ten, tde = o[:10]
        lines += [f"option ViewpointFixes.{prefix}_{name}", "{", f"    type = {typ},"]
        if typ == "enum":
            lines.append(f"    numValues = {len(o[10])},")
        elif typ in ("double", "integer"):
            lines += [f"    min = {fmt(lo)},", f"    max = {fmt(hi)},"]
        lines += [f"    default = {fmt(default)},", "    page = ViewpointFixes,", f"    translation = {base}_{name},"]
        if typ == "enum":
            lines.append(f"    valueTranslation = {base}_{name},")
        lines += ["}", ""]
        for lang, label, tip, values in (("EN", en, ten, o[10] if typ == "enum" else None),
                                         ("DE", de, tde, o[11] if typ == "enum" else None)):
            t = tr[lang]
            t[f"Sandbox_{base}_{name}"] = f"{title_en if lang == 'EN' else title_de}: {label}"
            if tip: t[f"Sandbox_{base}_{name}_tooltip"] = tip
            if values:
                for i, v in enumerate(values):
                    t[f"Sandbox_{base}_{name}_option{i + 1}"] = v
    write(os.path.join(media, "sandbox-options.txt"), "\n".join(lines))
    for lang in ("EN", "DE"):
        write(os.path.join(media, "lua", "shared", "Translate", lang, "Sandbox.json"),
              json.dumps(tr[lang], ensure_ascii=False, indent=4) + "\n")
        ui_path = os.path.join(media, "lua", "shared", "Translate", lang, "UI.json")
        ui = {}
        if os.path.exists(ui_path):
            with open(ui_path, encoding="utf-8") as f: ui = json.load(f)
        ui.update(UI[lang])
        write(ui_path, json.dumps(ui, ensure_ascii=False, indent=4) + "\n")
    dst = os.path.join(media, "lua", "client", "ViewpointFixes", "VF_SandboxLock.lua")
    if os.path.abspath(dst) != os.path.abspath(LOCK_SRC):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(LOCK_SRC, dst)
    print(mod, "->", len(opts), "Optionen")
