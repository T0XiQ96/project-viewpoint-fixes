package vpveh;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.joml.Vector3f;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleParts;
import zombie.vehicles.VehicleWindow;

/**
 * Was am Fahrzeug getroffen wurde und was dabei passiert.
 *
 * classify(): nur bestimmen (Client, Effekte), apply(): Schaden machen (Einzelspieler / Server).
 *
 * Teile werden ueber die ueblichen Teil-IDs gefunden (Windshield, WindshieldRear, Window*, Door*, Tire*, Headlight*,
 * EngineDoor, TrunkDoor, GasTank, Engine, Muffler ...), die praktisch alle Fahrzeug-Mods verwenden; Fenster ueber
 * getWindow(), Lichter ueber getLight(), Reifen ueber getWheelIndex(). Panzerungen: eingebaute Teile mit
 * Armor/Protection/Cage/Plate im Namen, zugeordnet ueber die Lage-Woerter (Front, Rear/Back, Left, Right, Windshield,
 * Door, Wheel/Tire, Engine/Hood) - so passt es zu KI5 (DAMN...Armor), Autotsar und anderen.
 */
final class Model {
    // Arten
    static final String GLASS = "glass", TIRE = "tire", LIGHT = "light", BODY = "body", ENGINE = "engine", FUEL = "fuel",
        ARMOR = "armor", ROOF = "roof", OPEN = "open";

    static final class Result {
        String kind = BODY;
        VehiclePart part;          // getroffenes Teil (Panzerung: das Panzerteil)
        VehiclePart behind;        // bei Panzerung: das geschuetzte Teil
        String state = "damaged";
        int condition = -1;
        boolean laminated, armoredGlass, ricochet, penetrated;
        boolean self;              // Schuss von innen (Drive-by): keine Insassen treffen
        double energyIn, energyLeft;
        String material = "Metal_Light";
        IsoGameCharacter occupant;
        int occupantPart = -1;     // BodyPartType-Index
        float occupantDamage;
        final Vector3f local = new Vector3f(), normal = new Vector3f(), dirLocal = new Vector3f();
        final Vector3f world = new Vector3f();
        String partId() { return part == null ? null : part.getId(); }
    }

    private static float rnd(float a, float b) { return a + ThreadLocalRandom.current().nextFloat() * (b - a); }
    private static boolean chance(double p) { return ThreadLocalRandom.current().nextDouble() < p; }
    private static String lc(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }

    // ------------------------------------------------------------------ Teile suchen

    static VehiclePart byId(BaseVehicle v, String id) {
        VehicleParts ps = v.getParts();
        return ps == null ? null : ps.getPartById(id);
    }

    static boolean installed(VehiclePart p) {
        if (p == null) return false;
        try {
            if (p.getItemType() != null && !p.getItemType().isEmpty()) return p.getInventoryItem() != null;
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static boolean has(String id, String... words) {
        for (String w : words) if (id.contains(w)) return true;
        return false;
    }

    private static boolean leftId(String id) { return id.contains("left") || id.endsWith("l") && id.contains("door"); }
    private static boolean rightId(String id) { return id.contains("right") || id.endsWith("r") && id.contains("door"); }
    private static boolean rearId(String id) { return has(id, "rear", "back"); }

    /** Sitz-Laengsposition eines Teils (Tuer/Fenster), sonst NaN. */
    private static float seatZ(BaseVehicle v, VehiclePart door) {
        if (door == null) return Float.NaN;
        Vector3f p = new Vector3f();
        for (int i = 0; i < v.getMaxPassengers(); i++) {
            if (v.getPassengerDoor(i) == door || v.getPassengerDoor2(i) == door) {
                v.getPassengerLocalPos(i, p);
                return p.z;
            }
        }
        return Float.NaN;
    }

    /** Guertellinie: unterhalb Blech, oberhalb Glas. */
    static float beltY(BaseVehicle v, Geometry.Shape sh) {
        float sum = 0;
        int n = 0;
        Vector3f p = new Vector3f();
        for (int i = 0; i < Math.min(2, v.getMaxPassengers()); i++) {
            try {
                v.getPassengerLocalPos(i, p);
                if (!Float.isNaN(p.y)) { sum += p.y; n++; }
            } catch (Throwable ignored) {
            }
        }
        float h = sh.top - sh.bottom;
        float belt = n > 0 ? sum / n + 0.30f : sh.bottom + 0.55f * h;
        return Math.max(sh.bottom + 0.35f * h, Math.min(sh.bottom + 0.80f * h, belt));
    }

    /** Panzerung, die dieses Teil schuetzt (eingebaut, Zustand > 0), sonst null. */
    static VehiclePart armorFor(BaseVehicle v, String target, float u) {
        VehicleParts ps = v.getParts();
        if (ps == null) return null;
        String t = lc(target);
        boolean tLeft = u > 0;
        VehiclePart whole = null;
        for (int i = 0; i < ps.getPartCount(); i++) {
            VehiclePart p = ps.getPartByIndex(i);
            if (p == null || p.getCondition() <= 0 || !installed(p)) continue;
            String id = lc(p.getId());
            if (!has(id, "armor", "armour", "protection", "cage", "plate", "plating", "bulletproof")) continue;
            if (id.equals("armor") || id.equals("armour") || id.endsWith("_armor_1") || id.endsWith("armor1")) { whole = p; continue; }
            boolean aLeft = leftId(id), aRight = rightId(id);
            boolean sideOk = (!aLeft && !aRight) || (tLeft ? aLeft : aRight);
            if (t.contains("windshield")) {
                if (id.contains("windshield") || id.contains("windscreen")) {
                    if (rearId(t) == rearId(id)) return p;
                }
                continue;
            }
            if (t.startsWith("window")) {
                if (id.contains("windshield") || id.contains("windscreen")) continue;
                if (!sideOk || (!aLeft && !aRight)) continue;
                boolean tRear = rearId(t), tMid = t.contains("middle");
                boolean idRear = rearId(id), idMid = id.contains("middle") || id.contains("mid"), idFront = id.contains("front");
                if (!idRear && !idMid && !idFront) return p;                 // ganze Seite
                if (tRear ? idRear : tMid ? idMid : idFront) return p;
                continue;
            }
            if (t.startsWith("door")) {
                if ((id.contains("door") || (!id.contains("window") && !id.contains("windshield"))) && (aLeft || aRight) && sideOk) return p;
                continue;
            }
            if (t.startsWith("tire")) {
                if (has(id, "wheel", "tire", "tyre")) return p;
                continue;
            }
            if (has(t, "engine", "radiator", "hood")) {
                if (has(id, "engine", "hood", "front", "grill", "radiator")) return p;
                continue;
            }
            if (has(t, "trunk", "gastank", "muffler") || (t.startsWith("door") && rearId(t))) {
                if (rearId(id) && !id.contains("window") && !id.contains("windshield")) return p;
            }
        }
        if (whole != null && !t.startsWith("headlight")) return whole;
        return null;
    }

    /** Notlaufreifen: KI5/damnlib (Teil RFsystem bzw. CTIsystem), andere Mods ueber Rad-Panzerung (armorFor). */
    static boolean magicTires(BaseVehicle v) {
        return byId(v, "RFsystem") != null || byId(v, "CTIsystem") != null;
    }

    private static boolean armoredGlassItem(VehiclePart p) {
        try {
            InventoryItem it = p.getInventoryItem();
            String ty = it == null ? "" : lc(it.getFullType());
            return has(ty, "armor", "armour", "bulletproof", "ballistic", "reinforced");
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------ bestimmen

    /** Treffer einordnen. dirLocal: Flugrichtung in Fahrzeug-Koordinaten. */
    static Result classify(BaseVehicle v, Geometry.Hit hit, Vector3f dirLocal, double energy) {
        Result r = new Result();
        Geometry.Shape sh = Geometry.shape(v);
        r.local.set(hit.local);
        r.normal.set(hit.normal);
        r.dirLocal.set(dirLocal).normalize();
        r.energyIn = energy;
        Geometry.toWorld(v, hit.local, r.world);
        VehicleParts ps = v.getParts();
        if (sh == null || ps == null) return r;

        float u = Geometry.side(sh, hit.local), f = Geometry.along(sh, hit.local);
        Vector3f n = hit.normal;
        float nf = n.z * sh.front;
        boolean faceSide = Math.abs(n.x) > Math.max(Math.abs(n.y), Math.abs(n.z));
        boolean faceTop = n.y > 0.6f, faceBottom = n.y < -0.6f;
        float belt = beltY(v, sh);
        float h01 = Geometry.height(sh, hit.local);
        float beltH = (belt - sh.bottom) / Math.max(0.05f, sh.top - sh.bottom);

        // 1) Reifen
        if (hit.wheel >= 0) {
            for (int i = 0; i < ps.getPartCount(); i++) {
                VehiclePart p = ps.getPartByIndex(i);
                if (p != null && p.getWheelIndex() == hit.wheel && lc(p.getId()).contains("tire")) {
                    r.kind = TIRE;
                    r.part = p;
                    r.material = "Rubber";
                    if (!installed(p)) { r.kind = BODY; r.part = null; r.material = "Metal"; }   // nur Felge/Bremse
                    return withArmor(v, r, u);
                }
            }
        }

        // 2) Glas oberhalb der Guertellinie
        if (hit.local.y >= belt && !faceBottom) {
            VehiclePart glass = null;
            if (faceSide) {
                glass = sideWindow(v, ps, u > 0, hit.local.z, sh);
            } else if (f > 0 && nf > 0.25f) {                 // Front oder schraege Scheibe (Normale zeigt nach vorn)
                glass = windshield(ps, false);
            } else if (f < 0 && nf < -0.25f) {
                glass = windshield(ps, true);
            }
            if (glass != null) {
                r.part = glass;
                r.kind = GLASS;
                String id = lc(glass.getId());
                r.laminated = id.contains("windshield") && !rearId(id);
                VehicleWindow w = glass.getWindow();
                if (!installed(glass) || w == null || w.isDestroyed()) {
                    r.kind = OPEN;
                    r.state = "open";
                    r.material = null;
                    return r;
                }
                if (w.isOpenable() && w.isOpen() && hit.local.y > belt + 0.05f) {
                    r.kind = OPEN;
                    r.state = "open";
                    r.material = null;
                    return r;
                }
                r.armoredGlass = armoredGlassItem(glass);
                r.material = r.armoredGlass ? "Glass_Solid" : "Glass";
                return withArmor(v, r, u);
            }
            if (faceTop) {
                r.kind = ROOF;
                r.material = "Metal_Light";
                return withArmor(v, r, u);
            }
        }

        // 3) Lichter
        boolean frontFace = nf > 0.5f || f > 0.88f, rearFace = nf < -0.5f || f < -0.88f;
        if ((frontFace || rearFace) && Math.abs(u) > 0.40f && h01 > 0.15f && h01 < beltH + 0.05f) {
            VehiclePart light = light(ps, frontFace, u > 0);
            if (light != null && installed(light)) {
                r.kind = LIGHT;
                r.part = light;
                r.material = "Glass_Light";
                return r;
            }
        }

        // 4) Blech unterhalb
        if (f > 0.5f && (frontFace || faceTop)) {
            VehiclePart hood = byId(v, "EngineDoor");
            if (faceTop || h01 > beltH * 0.7f) {
                r.part = installed(hood) ? hood : null;
                r.kind = r.part != null ? BODY : ENGINE;
                if (r.part == null) r.part = byId(v, "Engine");
            } else {
                r.kind = ENGINE;
                VehiclePart rad = byId(v, "Radiator");
                r.part = rad != null && installed(rad) ? rad : byId(v, "Engine");
            }
            r.material = "Metal_Light";
            return withArmor(v, r, u);
        }
        if (f < -0.5f && (rearFace || faceTop)) {
            VehiclePart trunk = byId(v, "TrunkDoor");
            if (trunk == null) trunk = byId(v, "DoorRear");
            if (trunk != null && installed(trunk) && (faceTop || h01 > 0.3f)) {
                r.part = trunk;
            } else {
                VehiclePart tank = byId(v, "GasTank");
                VehiclePart muffler = byId(v, "Muffler");
                r.part = h01 < 0.3f && muffler != null && installed(muffler) ? muffler : tank;
                if (r.part == tank && tank != null) r.kind = FUEL;
            }
            r.material = "Metal_Light";
            return withArmor(v, r, u);
        }
        if (faceBottom) {
            VehiclePart muffler = byId(v, "Muffler");
            r.part = muffler != null && installed(muffler) ? muffler : byId(v, "GasTank");
            if (r.part != null && lc(r.part.getId()).contains("gastank")) r.kind = FUEL;
            r.material = "Metal";
            return withArmor(v, r, u);
        }
        // Seite: Tuer am naechsten Sitz, sonst Tank im hinteren Drittel, sonst Karosserie
        VehiclePart door = sideDoor(v, ps, u > 0, hit.local.z);
        if (door != null) {
            r.part = door;
        } else if (f < -0.1f && h01 < beltH) {
            VehiclePart tank = byId(v, "GasTank");
            if (tank != null && chance(0.35)) { r.part = tank; r.kind = FUEL; }
        }
        r.material = "Metal_Light";
        return withArmor(v, r, u);
    }

    private static Result withArmor(BaseVehicle v, Result r, float u) {
        String target = r.part != null ? r.part.getId() : (r.kind.equals(ROOF) ? "Roof" : "Body");
        VehiclePart armor = armorFor(v, target, u);
        if (armor != null) {
            r.behind = r.part;
            r.part = armor;
            r.kind = ARMOR;
            r.material = "Metal_Solid";
        }
        return r;
    }

    private static VehiclePart windshield(VehicleParts ps, boolean rear) {
        VehiclePart best = null;
        for (int i = 0; i < ps.getPartCount(); i++) {
            VehiclePart p = ps.getPartByIndex(i);
            if (p == null || p.getWindow() == null) continue;
            String id = lc(p.getId());
            if (!(id.contains("windshield") || id.contains("windscreen"))) continue;
            if (rearId(id) == rear) return p;
        }
        return best;
    }

    private static VehiclePart sideWindow(BaseVehicle v, VehicleParts ps, boolean left, float z, Geometry.Shape sh) {
        VehiclePart best = null;
        float bestD = 1e9f;
        for (int i = 0; i < ps.getPartCount(); i++) {
            VehiclePart p = ps.getPartByIndex(i);
            if (p == null || p.getWindow() == null) continue;
            String id = lc(p.getId());
            if (id.contains("windshield") || id.contains("windscreen")) continue;
            if (left ? !id.contains("left") : !id.contains("right")) continue;
            float wz = seatZ(v, p.getParent());
            if (Float.isNaN(wz)) {
                // ohne Tuer: grobe Lage nach Name
                float f = rearId(id) ? -0.45f : id.contains("middle") ? -0.1f : 0.2f;
                wz = sh.com.z + sh.front * f * sh.half.z;
            }
            float d = Math.abs(wz - z);
            if (d < bestD) { bestD = d; best = p; }
        }
        return bestD < 1.1f ? best : null;
    }

    private static VehiclePart sideDoor(BaseVehicle v, VehicleParts ps, boolean left, float z) {
        VehiclePart best = null;
        float bestD = 1e9f;
        for (int i = 0; i < ps.getPartCount(); i++) {
            VehiclePart p = ps.getPartByIndex(i);
            if (p == null || p.getDoor() == null) continue;
            String id = lc(p.getId());
            if (!id.startsWith("door") || (left ? !id.contains("left") : !id.contains("right"))) continue;
            float dz = seatZ(v, p);
            if (Float.isNaN(dz)) continue;
            float d = Math.abs(dz - z);
            if (d < bestD) { bestD = d; best = p; }
        }
        return bestD < 0.9f && installed(best) ? best : null;
    }

    private static VehiclePart light(VehicleParts ps, boolean front, boolean left) {
        VehiclePart fallback = null;
        for (int i = 0; i < ps.getPartCount(); i++) {
            VehiclePart p = ps.getPartByIndex(i);
            if (p == null || p.getLight() == null) continue;
            String id = lc(p.getId());
            boolean rear = rearId(id);
            if (rear == front) continue;
            if (left ? id.contains("left") : id.contains("right")) return p;
            if (!id.contains("left") && !id.contains("right")) fallback = p;
        }
        return fallback;
    }

    // ------------------------------------------------------------------ Schaden

    /** Schaden machen (nur Einzelspieler oder Server). */
    static void apply(BaseVehicle v, Result r, int powerClass, double pf, int projectiles) {
        double scale = Cfg.damageScale();
        r.energyLeft = r.energyIn;
        switch (r.kind) {
            case OPEN -> {
                r.penetrated = true;
                r.energyLeft = r.energyIn * 0.98;
            }
            case GLASS -> glass(v, r, pf, scale);
            case TIRE -> tire(v, r, powerClass, pf, scale);
            case LIGHT -> {
                VehiclePart p = r.part;
                if (chance(Cfg.lightBreak())) setCondition(v, p, 0);
                else setCondition(v, p, p.getCondition() - (int) Math.round(50 * pf * scale));
                r.state = p.getCondition() <= 0 ? "broken" : "damaged";
                try { v.transmitPartLight(p); } catch (Throwable ignored) {}
            }
            case ARMOR -> {
                VehiclePart p = r.part;
                int wear = (int) Math.max(1, Math.round(1.5 * pf * Cfg.armorWear() * scale));
                setCondition(v, p, p.getCondition() - wear);
                r.state = "armor";
                r.ricochet = chance(0.35);
                // Nur schwere Munition schlaegt durch
                double need = Penetration.resistance("Metal_Solid");
                if (r.energyIn > need && need > 0) {
                    r.penetrated = true;
                    r.energyLeft = (r.energyIn - need) * 0.5;
                } else {
                    r.energyLeft = 0;
                }
            }
            case ENGINE -> {
                VehiclePart p = r.part;
                if (p != null) setCondition(v, p, p.getCondition() - (int) Math.max(1, Math.round(Cfg.engineDamage() * pf * scale * rnd(0.7f, 1.3f))));
                VehiclePart engine = byId(v, "Engine");
                if (engine != null && engine != p && chance(Math.min(0.9, 0.3 * pf)))
                    setCondition(v, engine, engine.getCondition() - (int) Math.max(1, Math.round(Cfg.engineDamage() * pf * scale)));
                r.state = "damaged";
                r.energyLeft = 0;                    // Motorblock haelt auf
            }
            case FUEL -> {
                VehiclePart p = r.part;
                setCondition(v, p, p.getCondition() - (int) Math.max(1, Math.round(Cfg.bodyDamage() * pf * scale * rnd(0.7f, 1.3f))));
                r.state = "damaged";
                if (Cfg.fuelLeak() && p.getContainerContentAmount() > 0) {
                    Leaks.add(v, p, 0.4 * pf, false);
                    r.state = "leaking";
                }
                r.energyLeft = 0;
            }
            default -> {    // Blech: Tuer, Haube, Kofferraum, Karosserie, Dach
                VehiclePart p = r.part;
                if (p != null) setCondition(v, p, p.getCondition() - (int) Math.max(1, Math.round(Cfg.bodyDamage() * pf * scale * rnd(0.7f, 1.3f))));
                r.state = "damaged";
                double need = Penetration.resistance("VehicleBody");
                if (r.energyIn * rnd(0.8f, 1.2f) > need) {
                    r.penetrated = true;
                    r.energyLeft = (r.energyIn - need) * (1 - Cfg.energyLoss() * 0.5);
                } else {
                    r.energyLeft = 0;
                    r.ricochet = chance(0.15);
                }
            }
        }
        if (r.part != null) r.condition = r.part.getCondition();
        Holes.add(v, r);
        if (r.penetrated && Cfg.occupants() && !r.self) Occupants.hit(v, r, pf);
    }

    private static void glass(BaseVehicle v, Result r, double pf, double scale) {
        VehiclePart p = r.part;
        VehicleWindow w = p.getWindow();
        int cond = p.getCondition();
        int loss;
        if (r.armoredGlass) {
            loss = (int) Math.max(1, Math.round(100.0 / (Cfg.windshieldShots() * 4.0) * pf * scale));
            r.ricochet = chance(0.25);
            r.state = "scrape";
            r.energyLeft = 0;
        } else if (r.laminated) {
            loss = (int) Math.max(1, Math.round(100.0 / Cfg.windshieldShots() * pf * scale));
            r.state = "hole";
            r.penetrated = true;
            r.energyLeft = r.energyIn * 0.85;
        } else if (chance(Cfg.sideWindowShatter())) {
            loss = cond;
            r.penetrated = true;
            r.energyLeft = r.energyIn * 0.9;
        } else {
            loss = (int) Math.max(1, Math.round(35 * pf * scale));
            r.state = "hole";
            r.penetrated = true;
            r.energyLeft = r.energyIn * 0.9;
        }
        if (loss >= cond) loss = cond;
        if (w != null) w.damage(loss);
        else setCondition(v, p, cond - loss);
        if (w != null && w.isDestroyed() || p.getCondition() <= 0) {
            r.state = "shattered";
            Holes.clearPart(v, p.getId());
        } else {
            try { v.transmitPartCondition(p); } catch (Throwable ignored) {}
        }
    }

    private static void tire(BaseVehicle v, Result r, int powerClass, double pf, double scale) {
        VehiclePart p = r.part;
        int idx = p.getWheelIndex();
        if (Cfg.runFlat() && (magicTires(v) || armorFor(v, "Tire", 0) != null)) {
            setCondition(v, p, p.getCondition() - (int) Math.max(1, Math.round(3 * pf * scale)));
            r.state = "runflat";
            r.energyLeft = 0;
            return;
        }
        KahluaTable md = p.getModData();
        double acc = md.rawget("vvbTire") instanceof Double d ? d : 0;
        acc += 1.0 / Cfg.tireShots(powerClass);
        md.rawset("vvbTire", acc);
        setCondition(v, p, p.getCondition() - (int) Math.max(1, Math.round(8 * pf * scale)));
        boolean leakMode = Cfg.tireMode() == 2;
        if (acc >= 0.999) {
            if (leakMode) {
                Leaks.add(v, p, 1.0, true);
                r.state = "leaking";
            } else {
                deflate(v, p, idx);
                r.state = "flat";
            }
        } else {
            r.state = leakMode ? "leaking" : "holed";
            if (leakMode) Leaks.add(v, p, 0.35, true);
        }
        r.energyLeft = 0;
        try { v.transmitPartModData(p); } catch (Throwable ignored) {}
    }

    static void deflate(BaseVehicle v, VehiclePart p, int idx) {
        try {
            p.setContainerContentAmount(0, true, true);
            if (idx >= 0) v.setTireInflation(idx, 0);
            v.transmitPartModData(p);
        } catch (Throwable t) {
            System.err.println(Build.TAG + " deflating a tyre failed: " + t);
        }
    }

    static void setCondition(BaseVehicle v, VehiclePart p, int c) {
        if (p == null) return;
        p.setCondition(Math.max(0, Math.min(100, c)));
        try {
            v.transmitPartCondition(p);
        } catch (Throwable ignored) {
        }
        try {
            v.updatePartStats();
        } catch (Throwable ignored) {
        }
    }

    private Model() {}
}
