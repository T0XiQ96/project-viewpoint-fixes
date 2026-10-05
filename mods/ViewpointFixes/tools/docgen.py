# Erzeugt Markdown-Tabellen aller Sandbox-Optionen aus den Generatoren (sandbox_gen.py, vehicle_gen.py) und
# Mod Options Control (translations.py + sandbox-options.txt), auf Englisch und Deutsch.
# Aufruf: python docgen.py <Ausgabeordner>   -> <out>/en/*.generated.md, <out>/de/*.generated.md
import os, re, sys, json

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "docs_out")


def load_spec(path, stop):
    """Fuehrt nur den Datenteil eines Generators aus (bis zur Zeile, die mit 'stop' beginnt)."""
    src = open(path, encoding="utf-8").read()
    src = src[:src.index(stop)]
    g = {"__file__": path}
    exec(compile(src, path, "exec"), g)
    return g


def fmt(v, de):
    if isinstance(v, bool):
        return ("an" if v else "aus") if de else ("on" if v else "off")
    if isinstance(v, float):
        s = ("%g" % v)
        return s.replace(".", ",") if de else s
    return str(v)


def table(rows, de, sandbox_prefix):
    h = ("| Sandbox-Option | Bezeichnung | Standard | Bereich | Beschreibung |\n|---|---|---|---|---|\n" if de else
         "| Sandbox option | Label | Default | Range | Description |\n|---|---|---|---|---|\n")
    out = [h]
    for r in rows:
        name, typ, default, lo, hi, en, dee, ten, tde = r[:9]
        label, tip = (dee, tde) if de else (en, ten)
        rng = ""
        if typ in ("double", "integer"):
            rng = f"{fmt(lo, de)} – {fmt(hi, de)}"
        elif typ == "enum":
            vals = r[10] if de else r[9]
            rng = " / ".join(f"{i + 1} = {v}" for i, v in enumerate(vals))
        if typ == "enum":
            dflt = f"{default}"
        else:
            dflt = fmt(default, de)
        out.append(f"| `{sandbox_prefix}{name}` | {label} | {dflt} | {rng} | {tip.replace('|', '/')} |\n")
    return "".join(out)


def write(lang, name, text):
    d = os.path.join(OUT, lang)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


# ---------------------------------------------------------------- Vehicle Ballistics
vg = load_spec(os.path.join(HERE, "vehicle_gen.py"), "UI = {")
for lang, de in (("en", False), ("de", True)):
    parts = []
    for title_en, title_de, rows in (("Vehicle hits", "Fahrzeugtreffer", vg["VEHICLE"]),
                                     ("Shooting from vehicles and windows", "Aus Fahrzeugen schießen und Fenster", vg["DRIVE"]),
                                     ("Penetration and ricochets (needs Viewpoint True Ballistics)",
                                      "Durchschüsse und Abpraller (braucht Viewpoint True Ballistics)", vg["PEN"])):
        rows = [list(r) for r in rows]
        for r in rows:
            if r[0] == "In2D": r[2] = False
        parts.append(f"### {title_de if de else title_en}\n\n" + table(rows, de, "ViewpointFixes.VehicleBallistics_") + "\n")
    note = ("Bullet Impacts 2D (VFA_VehicleBallistics2D) hat dieselben Optionen unter `VehicleBallistics2D.<Name>` "
            "(Seite „Fahrzeug-Ballistik 2D“), dort ist „auch in der 2D-Sicht“ standardmäßig an.\n" if de else
            "Bullet Impacts 2D (VFA_VehicleBallistics2D) has the same options as `VehicleBallistics2D.<Name>` "
            "(page \"Vehicle Ballistics 2D\"); there \"also in the 2D view\" is on by default.\n")
    write(lang, "vehicle-ballistics-options.generated.md", "".join(parts) + note)

# ---------------------------------------------------------------- Sandbox-Spiegel ViewpointFixes
sg = load_spec(os.path.join(HERE, "sandbox_gen.py"), "def fmt(")
for lang, de in (("en", False), ("de", True)):
    parts = []
    ctl = sg["CONTROL_DE"] if de else sg["CONTROL_EN"]
    for mod, (prefix, t_en, t_de, opts) in sg["MODS"].items():
        rows = [("Control", "enum", 1, None, None, sg["CONTROL_EN"][0], sg["CONTROL_DE"][0], sg["CONTROL_TIP_EN"],
                 sg["CONTROL_TIP_DE"], list(sg["CONTROL_EN"][1:]), list(sg["CONTROL_DE"][1:]))]
        for o in opts:
            oid, name, typ, default, lo, hi, en, dee, ten, tde = o[:10]
            r = [name, typ, default, lo, hi, f"{en} (`{oid}`)", f"{dee} (`{oid}`)", ten, tde]
            if typ == "enum": r += [o[10], o[11]]
            rows.append(tuple(r))
        parts.append(f"### {t_de if de else t_en} ({mod})\n\n" + table(rows, de, f"ViewpointFixes.{prefix}_") + "\n")
    write(lang, "viewpointfixes-sandbox.generated.md", "".join(parts))

# ---------------------------------------------------------------- Mod Options Control
moc = next(p for p in (os.path.join(HERE, "..", "..", "ModOptionsControl"),                      # GitHub-Repo: mods/ModOptionsControl
                        os.path.join(HERE, "..", "..", "..", "..", "Workshop-Upload", "ModOptionsControl"))  # lokal
           if os.path.isdir(p))
sb_txt = open(os.path.join(moc, "Contents", "mods", "ModOptionsControl", "42", "media", "sandbox-options.txt"), encoding="utf-8").read()
tr = load_spec(os.path.join(moc, "tools", "translations.py"), "assert set(")
for lang, de in (("en", False), ("de", True)):
    sbt = tr["sb_de"] if de else tr["sb_en"]
    rows = []
    for m in re.finditer(r"option ModOptionsControl\.(\w+)\s*\{(.*?)\}", sb_txt, re.S):
        name, body = m.group(1), m.group(2)
        typ = re.search(r"type = (\w+)", body).group(1)
        dm = re.search(r"default = ([^,]*),", body)
        default = dm.group(1).strip() if dm else ""
        if typ == "boolean": default = fmt(default == "true", de)
        if typ == "enum":
            default = default + " = " + sbt.get(f"Sandbox_ModOptionsControl_{name}_option{default}", "")
        label = sbt.get(f"Sandbox_ModOptionsControl_{name}", name)
        tip = sbt.get(f"Sandbox_ModOptionsControl_{name}_tooltip", "")
        rows.append(f"| `ModOptionsControl.{name}` | {label} | {default or '–'} | {tip.replace('|', '/')} |\n")
    head = ("| Sandbox-Option | Bezeichnung | Standard | Beschreibung |\n|---|---|---|---|\n" if de else
            "| Sandbox option | Label | Default | Description |\n|---|---|---|---|\n")
    write(lang, "mod-options-control-options.generated.md", head + "".join(rows))

print("Doku-Tabellen in", OUT)
