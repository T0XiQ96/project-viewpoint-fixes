# Erzeugt fuer Vehicle Ballistics beide Fassungen aus einer Beschreibung:
#   ViewpointFixes/Contents/mods/ViewpointVehicleBallistics/42/...      (Sandbox-Seite "Viewpoint Fixes", Praefix VehicleBallistics_)
#   BulletImpacts2D/Contents/mods/VFA_VehicleBallistics2D/42/...        (eigene Seite "Vehicle Ballistics 2D")
# Inhalt: mod.info, sandbox-options.txt, Translate/EN+DE (Sandbox.json, UI.json), Lua (Client/Server aus source/lua).
# Das Jar baut source/build.ps1.
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
VF_MODS = os.path.join(HERE, "..", "Contents", "mods")
SRC = os.path.join(VF_MODS, "ViewpointVehicleBallistics", "source")
BI2D_MODS = os.path.join(HERE, "..", "..", "BulletImpacts2D", "Contents", "mods")

B, D, I, E = "boolean", "double", "integer", "enum"
# (Name, Typ, Standard, min, max, EN, DE, Tipp EN, Tipp DE [, Werte EN, Werte DE])
VEHICLE = [
    ("Enabled", B, True, None, None, "Vehicle hits: enable", "Fahrzeugtreffer: aktiv",
     "Shots hit the real part of a vehicle (glass, tyres, lights, doors, hood, engine, tank, armour) instead of a random part of that side.",
     "Schüsse treffen das echte Teil eines Fahrzeugs (Glas, Reifen, Lichter, Türen, Haube, Motor, Tank, Panzerung) statt eines zufälligen Teils der Seite."),
    ("In2D", B, None, None, None, "Vehicle hits: also in the normal 2D view", "Fahrzeugtreffer: auch in der normalen 2D-Sicht",
     "Also for shots without True Ballistics (vanilla firing, isometric view). The shot then flies straight along your aim at muzzle height.",
     "Auch für Schüsse ohne True Ballistics (normales Schießen, isometrische Sicht). Der Schuss fliegt dann gerade in Blickrichtung auf Mündungshöhe."),
    ("HitInfo", B, True, None, None, "Vehicle hits: tell the shooter what was hit", "Fahrzeugtreffer: Schütze sieht, was getroffen wurde",
     "A short text over your character, e.g. \"Tyre rear left: flat\". Each player can switch it off in the mod options.",
     "Ein kurzer Text über deinem Charakter, z. B. „Reifen hinten links: platt“. Jeder Spieler kann es in den Mod-Optionen abschalten."),
    ("DamageScale", D, 1.0, 0.1, 5.0, "Vehicle hits: damage factor", "Fahrzeugtreffer: Schadensfaktor",
     "Multiplies all part damage from bullets.", "Multipliziert jeden Teileschaden durch Kugeln."),
    ("WindshieldShots", I, 15, 1, 60, "Windshield: hits until it caves in", "Windschutzscheibe: Treffer bis sie einbricht",
     "Laminated glass keeps its holes and stays in place. Hits from a 9 mm until it falls in; stronger rounds count more (rifle about 2x), weaker less.",
     "Verbundglas behält seine Löcher und bleibt drin. Treffer einer 9 mm, bis sie einbricht; stärkere Munition zählt mehr (Gewehr etwa doppelt), schwächere weniger."),
    ("SideWindowShatter", I, 90, 0, 100, "Side and rear windows: shatter chance per hit (%)", "Seiten- und Heckscheiben: Chance zu zerspringen pro Treffer (%)",
     "Tempered glass usually bursts completely at the first hit. Otherwise a hole with cracks.",
     "Einscheiben-Sicherheitsglas zerspringt meist beim ersten Treffer komplett. Sonst ein Loch mit Sprüngen."),
    ("LightBreak", I, 100, 0, 100, "Lights: break chance per hit (%)", "Lichter: Chance kaputtzugehen pro Treffer (%)", "", ""),
    ("TireShotsStrong", I, 1, 1, 10, "Tyres: hits until flat (rifles, slugs, magnums)", "Reifen: Treffer bis platt (Gewehre, Flintenlaufgeschosse, Magnum)", "", ""),
    ("TireShotsMedium", I, 2, 1, 10, "Tyres: hits until flat (pistols, SMGs)", "Reifen: Treffer bis platt (Pistolen, MPs)", "", ""),
    ("TireShotsWeak", I, 3, 1, 10, "Tyres: hits until flat (.22, .38, single shotgun pellets)", "Reifen: Treffer bis platt (.22, .38, einzelne Schrotkugeln)",
     "Every pellet of a shotgun counts on its own, so a close shotgun blast usually flattens a tyre.",
     "Jede Schrotkugel zählt einzeln, ein naher Schrotschuss macht einen Reifen also meist platt."),
    ("TireMode", E, 1, None, None, "Tyres: how they lose air", "Reifen: wie sie Luft verlieren",
     "Flat at once: after the hits above the tyre is flat. Slow leak: every hole leaks, more holes faster; flat after the time below.",
     "Sofort platt: nach den Treffern oben ist der Reifen platt. Langsamer Luftverlust: jedes Loch verliert Luft, mehr Löcher schneller; platt nach der Zeit unten.",
     ["Flat at once", "Slow leak"], ["Sofort platt", "Langsamer Luftverlust"]),
    ("TireLeakSeconds", I, 30, 1, 600, "Tyres: seconds until flat (slow leak)", "Reifen: Sekunden bis platt (langsamer Luftverlust)", "", ""),
    ("RunFlat", B, True, None, None, "Tyres: run-flat on armoured vehicles", "Reifen: Notlauf bei gepanzerten Fahrzeugen",
     "Vehicles with a run-flat or tyre inflation system (KI5) or wheel armour only lose some condition.",
     "Fahrzeuge mit Notlauf- oder Reifendruck-System (KI5) oder Radpanzerung verlieren nur etwas Zustand."),
    ("BodyDamage", D, 3.0, 0.0, 25.0, "Body panels: damage per hit (%)", "Blech: Schaden pro Treffer (%)",
     "Doors, hood, trunk, bumpers - for a 9 mm, stronger rounds more.", "Türen, Haube, Kofferraum, Stoßstangen - für eine 9 mm, stärkere Munition mehr."),
    ("EngineDamage", D, 2.0, 0.0, 25.0, "Engine and radiator: damage per hit (%)", "Motor und Kühler: Schaden pro Treffer (%)", "", ""),
    ("FuelLeak", B, True, None, None, "Fuel tank leaks when hit", "Getroffener Tank verliert Sprit", "", ""),
    ("Occupants", B, True, None, None, "Shots through glass and doors can hit people inside", "Schüsse durch Glas und Türen können Insassen treffen",
     "Depends on the energy left: rifles go through doors, small calibres often stay in the sheet metal.",
     "Hängt von der Restenergie ab: Gewehre gehen durch Türen, kleine Kaliber bleiben oft im Blech stecken."),
    ("ArmorWear", D, 1.0, 0.0, 10.0, "Vehicle armour: wear per hit", "Fahrzeugpanzerung: Abnutzung pro Treffer",
     "Armour plates and armoured glass (KI5 and other mods) stop most rounds with a scrape and wear down slowly.",
     "Panzerplatten und Panzerglas (KI5 und andere Mods) halten die meisten Kugeln mit einer Schramme auf und nutzen sich langsam ab."),
]
DRIVE = [
    ("DriveBy", B, True, None, None, "Shooting from vehicles", "Aus Fahrzeugen schießen",
     "In a vehicle with a firearm in hand: hold the right mouse button to aim, left click to shoot (full auto keeps firing). A closed window you shoot through gets a hole or shatters; open (rolled down) windows stay intact. Needs Viewpoint True Ballistics.",
     "Im Fahrzeug mit Schusswaffe in der Hand: rechte Maustaste halten = zielen, Linksklick = schießen (Dauerfeuer schießt weiter). Ein geschlossenes Fenster, durch das du schießt, bekommt ein Loch oder zerspringt; offene (heruntergekurbelte) Fenster bleiben heil. Braucht Viewpoint True Ballistics."),
    ("DriverCanShoot", B, True, None, None, "Shooting from vehicles: the driver may shoot", "Aus Fahrzeugen schießen: Fahrer darf schießen", "", ""),
    ("DriveBySpread", D, 1.5, 0.1, 5.0, "Shooting from vehicles: spread factor", "Aus Fahrzeugen schießen: Faktor Streuung",
     "Multiplies the normal spread (skill, moodles, movement).", "Multipliziert die normale Streuung (Können, Moodles, Bewegung)."),
    ("DriverSpread", D, 2.0, 1.0, 5.0, "Shooting from vehicles: extra spread for the driver", "Aus Fahrzeugen schießen: zusätzliche Streuung für den Fahrer", "", ""),
    ("OpenableWindows", B, True, None, None, "Make side windows of modded vehicles openable", "Seitenfenster von Mod-Fahrzeugen kurbelbar machen",
     "Door windows that cannot be rolled down get the normal Open window / Close window option. Vehicles without an opening animation still look closed, but shots pass.",
     "Türfenster, die sich nicht herunterkurbeln lassen, bekommen die normale Option Fenster öffnen / schließen. Fahrzeuge ohne Öffnungs-Animation sehen weiter geschlossen aus, Schüsse gehen aber durch."),
]
PEN = [
    ("Penetration", B, True, None, None, "Penetration: enable (True Ballistics)", "Durchschüsse: aktiv (True Ballistics)",
     "Rounds can go through walls, doors, furniture, trees and vehicles if their energy is high enough. They fly on slower (less damage) and slightly deflected. Needs Viewpoint True Ballistics.",
     "Kugeln können Wände, Türen, Möbel, Bäume und Fahrzeuge durchschlagen, wenn ihre Energie reicht. Danach fliegen sie langsamer (weniger Schaden) und leicht abgelenkt weiter. Braucht Viewpoint True Ballistics."),
    ("PenetrationZombies", B, True, None, None, "Penetration: zombies behind walls can be hit", "Durchschüsse: Zombies hinter Wänden treffbar", "", ""),
    ("PenetrationPlayers", B, False, None, None, "Penetration: players behind walls can be hit", "Durchschüsse: Spieler hinter Wänden treffbar",
     "Off: rounds that went through something pass players behind it.", "Aus: Kugeln, die etwas durchschlagen haben, fliegen an Spielern dahinter vorbei."),
    ("PenetrationScale", D, 1.0, 0.1, 10.0, "Penetration: material resistance factor", "Durchschüsse: Faktor Materialwiderstand",
     "Multiplies every material value below. Higher = fewer penetrations.", "Multipliziert alle Materialwerte unten. Höher = weniger Durchschüsse."),
    ("MaxPenetrations", I, 3, 0, 10, "Penetration: obstacles per round", "Durchschüsse: Hindernisse pro Kugel", "", ""),
    ("EnergyLoss", I, 25, 0, 90, "Penetration: extra energy loss (%)", "Durchschüsse: zusätzlicher Energieverlust (%)",
     "Of the energy left after an obstacle (tumbling, deformation).", "Von der Energie, die nach einem Hindernis übrig ist (Taumeln, Verformung)."),
    ("DeflectionScale", D, 1.0, 0.0, 5.0, "Penetration: deflection factor", "Durchschüsse: Faktor Ablenkung", "", ""),
    ("Ricochets", B, True, None, None, "Ricochets: enable", "Abpraller: aktiv",
     "Rounds that do not get through hard material at a flat angle can bounce off and fly on.",
     "Kugeln, die hartes Material im flachen Winkel nicht durchschlagen, können abprallen und weiterfliegen."),
    ("RicochetScale", D, 1.0, 0.0, 5.0, "Ricochets: chance factor", "Abpraller: Faktor Wahrscheinlichkeit", "", ""),
    ("RicochetAngle", I, 20, 1, 60, "Ricochets: maximum angle to the surface (degrees)", "Abpraller: größter Winkel zur Oberfläche (Grad)", "", ""),
]
MATERIALS = [
    ("Glass", 20, "Glass", "Glas"), ("Plaster", 120, "Plaster / drywall", "Putz / Gipskarton"),
    ("Wood", 180, "Wood (walls, doors)", "Holz (Wände, Türen)"), ("WoodSolid", 900, "Solid wood (logs, heavy furniture)", "Massivholz (Balken, schwere Möbel)"),
    ("Tree", 2200, "Tree trunks", "Baumstämme"), ("MetalLight", 150, "Sheet metal", "Dünnes Blech"),
    ("Metal", 600, "Metal (fences, appliances)", "Metall (Zäune, Geräte)"), ("MetalLarge", 1000, "Heavy metal (containers)", "Schweres Metall (Container)"),
    ("MetalSolid", 6000, "Hardened metal / armour", "Gehärtetes Metall / Panzerung"), ("GlassArmored", 2500, "Armoured glass", "Panzerglas"),
    ("Brick", 3500, "Brick", "Ziegel"), ("Cinderblock", 3000, "Cinder block", "Hohlblockstein"),
    ("Stone", 20000, "Stone / concrete", "Stein / Beton"), ("Plastic", 60, "Plastic", "Kunststoff"),
    ("Fabric", 20, "Fabric / carpet", "Stoff / Teppich"), ("Ceramic", 200, "Ceramic", "Keramik"),
    ("VehicleBody", 250, "Vehicle body (one door)", "Fahrzeugblech (eine Tür)"),
]
for key, j, en, de in MATERIALS:
    PEN.append(("J_" + key, I, j, 0, 50000, f"Penetration energy: {en} (J)", f"Durchschlagsenergie: {de} (J)",
                "Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180.",
                "Energie, die eine Kugel zum Durchschlagen braucht. Zum Vergleich: .22 etwa 150 J, 9 mm 500, .45 550, .357 750, 5,56 1700, 7,62x39 2000, .308 3400, Flintenlaufgeschoss 3000, Schrotkugel 180."))

UI = {
    "EN": {
        "UI_VVB_Title": "Vehicle Ballistics", "UI_VVB_ShowInfo": "Show what I hit on vehicles",
        "UI_VVB_ShowInfo_tooltip": "A short text over your character after a hit (only if the server allows it).",
        "UI_VVB_Body": "Body", "UI_VVB_Occupant": "someone inside was hit",
        "UI_VVB_State_hole": "bullet hole", "UI_VVB_State_shattered": "shattered", "UI_VVB_State_scrape": "scrape, holds",
        "UI_VVB_State_flat": "flat", "UI_VVB_State_holed": "holed", "UI_VVB_State_leaking": "losing air", "UI_VVB_State_fuel": "leaking fuel",
        "UI_VVB_State_runflat": "run-flat, holds", "UI_VVB_State_broken": "broken", "UI_VVB_State_damaged": "hit",
        "UI_VVB_State_armor": "armour holds", "UI_VVB_State_open": "through the open window",
        "UI_VVB_DriveByNeedsVTB": "Shooting from vehicles needs Viewpoint True Ballistics",
    },
    "DE": {
        "UI_VVB_Title": "Fahrzeug-Ballistik", "UI_VVB_ShowInfo": "Anzeigen, was ich am Fahrzeug getroffen habe",
        "UI_VVB_ShowInfo_tooltip": "Ein kurzer Text über deinem Charakter nach einem Treffer (nur wenn der Server es erlaubt).",
        "UI_VVB_Body": "Karosserie", "UI_VVB_Occupant": "Insasse getroffen",
        "UI_VVB_State_hole": "Einschussloch", "UI_VVB_State_shattered": "zersplittert", "UI_VVB_State_scrape": "Schramme, hält",
        "UI_VVB_State_flat": "platt", "UI_VVB_State_holed": "Loch", "UI_VVB_State_leaking": "verliert Luft", "UI_VVB_State_fuel": "verliert Sprit",
        "UI_VVB_State_runflat": "Notlauf, hält", "UI_VVB_State_broken": "kaputt", "UI_VVB_State_damaged": "getroffen",
        "UI_VVB_State_armor": "Panzerung hält", "UI_VVB_State_open": "durchs offene Fenster",
        "UI_VVB_DriveByNeedsVTB": "Aus Fahrzeugen schießen braucht Viewpoint True Ballistics",
    },
}

VARIANTS = [
    dict(folder=os.path.join(VF_MODS, "ViewpointVehicleBallistics"), ns="ViewpointFixes", prefix="VehicleBallistics_",
         page="ViewpointFixes", page_en="Viewpoint Fixes", page_de="Viewpoint Fixes", title_en="Vehicle Ballistics", title_de="Fahrzeug-Ballistik",
         lua="VehicleBallisticsVF", module="VehicleBallisticsVF", modpage="ViewpointVehicleBallistics", in2d=False,
         name="Project Viewpoint - Vehicle Ballistics & Penetration", modid="ViewpointVehicleBallistics",
         jar="media/java/ViewpointVehicleBallistics.jar", pkg="vpveh", file="VFA_ViewpointVehicleBallistics"),
    dict(folder=os.path.join(BI2D_MODS, "VFA_VehicleBallistics2D"), ns="VehicleBallistics2D", prefix="",
         page="VehicleBallistics2D", page_en="Vehicle Ballistics 2D", page_de="Fahrzeug-Ballistik 2D", title_en="", title_de="",
         lua="VehicleBallistics2D", module="VehicleBallistics2D", modpage="VehicleBallistics2D", in2d=True,
         name="Vehicle Ballistics 2D", modid="VFA_VehicleBallistics2D", jar="media/java/VehicleBallistics2D.jar", pkg="vbi2d",
         file="VFA_VehicleBallistics2D"),
]

MODINFO_VF = """name={name}
id={modid}
description=Shots hit the real part of a vehicle: windshields keep their bullet holes until they cave in, side windows shatter, tyres go flat after 1-3 hits (by calibre) or lose air slowly, lights break, doors, hood, engine and fuel tank take damage, armour (KI5 and other mods) holds with a scrape, bullets through glass and doors can hit people inside. The shooter sees what was hit. Works with modded vehicles (their physics shapes, wheels and part names). With Viewpoint True Ballistics also penetration through walls, doors, furniture, trees and vehicles by material and energy, and ricochets. Everything in Sandbox > Viewpoint Fixes. Needed on server and clients (ZombieBuddy on the server).
author=Very Far Away
modversion=1.0
versionMin=42.21
require=\\ZombieBuddy
loadModAfter=ViewpointTrueBallistics,ViewpointEffects3D,VFA_BulletImpacts2D,VFA_VehicleBallistics2D
javaJarFile={jar}
javaPkgName={pkg}
ZBVersionMin=2.3.0
"""
MODINFO_2D = """name={name}
id={modid}
description=Bullet Impacts 2D add-on: shots hit the real part of a vehicle in the normal view - windshields keep holes until they cave in, side windows shatter, tyres go flat after 1-3 hits by calibre (or lose air slowly), lights break, doors, hood, engine and tank take damage, armour holds, bullets can hit people inside. The shooter sees what was hit. Works with modded vehicles. Options in Sandbox > Vehicle Ballistics 2D. Needed on server and clients (ZombieBuddy on the server). If Project Viewpoint - Fixes (Vehicle Ballistics) is also enabled, that one takes over and this one stays idle.
author=Very Far Away
modversion=1.0
versionMin=42.21
require=\\ZombieBuddy
loadModAfter=VFA_BulletImpacts2D,ViewpointTrueBallistics
javaJarFile={jar}
javaPkgName={pkg}
ZBVersionMin=2.3.0
"""


def fmt(v):
    if isinstance(v, bool): return "true" if v else "false"
    return repr(v) if isinstance(v, float) else str(v)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


for var in VARIANTS:
    media = os.path.join(var["folder"], "42", "media")
    ns, prefix = var["ns"], var["prefix"]
    lines = ["VERSION = 1,", ""]
    tr = {"EN": {f"Sandbox_{var['page']}": var["page_en"]}, "DE": {f"Sandbox_{var['page']}": var["page_de"]}}
    for o in VEHICLE + DRIVE + PEN:
        name, typ, default, lo, hi, en, de, ten, tde = o[:9]
        if name == "In2D": default = var["in2d"]
        key = f"{ns}_{prefix}{name}" if prefix else f"{ns}_{name}"
        lines += [f"option {ns}.{prefix}{name}", "{", f"    type = {typ},"]
        if typ == E: lines.append(f"    numValues = {len(o[9])},")
        elif typ in (D, I): lines += [f"    min = {fmt(lo)},", f"    max = {fmt(hi)},"]
        lines += [f"    default = {fmt(default)},", f"    page = {var['page']},", f"    translation = {key},"]
        if typ == E: lines.append(f"    valueTranslation = {key},")
        lines += ["}", ""]
        for lang, label, tip, values in (("EN", en, ten, o[9] if typ == E else None), ("DE", de, tde, o[10] if typ == E else None)):
            t = tr[lang]
            title = var["title_en"] if lang == "EN" else var["title_de"]
            t[f"Sandbox_{key}"] = f"{title}: {label}" if title else label
            if tip: t[f"Sandbox_{key}_tooltip"] = tip
            if values:
                for i, v in enumerate(values): t[f"Sandbox_{key}_option{i + 1}"] = v
    write(os.path.join(media, "sandbox-options.txt"), "\n".join(lines))
    for lang in ("EN", "DE"):
        write(os.path.join(media, "lua", "shared", "Translate", lang, "Sandbox.json"), json.dumps(tr[lang], ensure_ascii=False, indent=4) + "\n")
        write(os.path.join(media, "lua", "shared", "Translate", lang, "UI.json"), json.dumps(UI[lang], ensure_ascii=False, indent=4) + "\n")
    for side in ("client", "server"):
        with open(os.path.join(SRC, "lua", side + ".lua"), encoding="utf-8") as f: text = f.read()
        text = (text.replace("@LUA@", var["lua"]).replace("@MODULE@", var["module"]).replace("@PAGE@", var["modpage"])
                .replace("@NAME@", var["name"]).replace("@NS@", var["ns"]).replace("@PREFIX@", var["prefix"]))
        suffix = "" if side == "client" else "_Server"
        write(os.path.join(media, "lua", side, var["file"] + suffix + ".lua"), text)
    tpl = MODINFO_VF if var["in2d"] is False else MODINFO_2D
    write(os.path.join(var["folder"], "42", "mod.info"), tpl.format(**var))
    print("ok", var["modid"], len(VEHICLE + DRIVE + PEN), "Optionen")
