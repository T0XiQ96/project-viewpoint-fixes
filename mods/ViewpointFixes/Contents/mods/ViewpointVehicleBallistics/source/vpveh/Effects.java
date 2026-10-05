package vpveh;

import java.util.ArrayDeque;
import java.util.List;

import org.joml.Vector3f;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.IsoWorld;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleParts;

/**
 * Schnittstelle fuer Effekt-Mods (ViewpointEffects3D, Bullet Impacts 2D), per Reflection aufrufbar:
 *  - nextImpact(): Fahrzeug-Einschlaege dieses Clients (Material, Punkt, Normale, Abpraller)
 *  - classifyRay(...): Einschlag einer fremden Kugel bestimmen (ohne Schaden)
 *  - vehicleAt(...): liegt ein Punkt an einem Fahrzeug (dann keine Feld-Material-Effekte)
 *  - holes(...): bleibende Einschussloecher eines Fahrzeugs in Weltkoordinaten
 *
 * Material-Codes: 0 Blech, 1 Glas, 2 Panzerglas, 3 Gummi, 4 Panzerung, 5 Lampenglas, 6 Metall (dick), 7 offen.
 */
public final class Effects {
    public static final int METAL_LIGHT = 0, GLASS = 1, GLASS_ARMORED = 2, RUBBER = 3, ARMOR = 4, LAMP = 5, METAL = 6, OPEN = 7;

    private static final ArrayDeque<float[]> impacts = new ArrayDeque<>();

    static int code(Model.Result r) {
        return switch (r.kind) {
            case Model.GLASS -> r.armoredGlass ? GLASS_ARMORED : GLASS;
            case Model.TIRE -> RUBBER;
            case Model.ARMOR -> ARMOR;
            case Model.LIGHT -> LAMP;
            case Model.OPEN -> OPEN;
            case Model.ENGINE, Model.FUEL -> METAL;
            default -> METAL_LIGHT;
        };
    }

    static void remember(BaseVehicle v, Model.Result r) {
        if (zombie.network.GameServer.server) return;
        float[] e = pack(v, r);
        synchronized (impacts) {
            impacts.addLast(e);
            while (impacts.size() > 64) impacts.removeFirst();
        }
    }

    private static float[] pack(BaseVehicle v, Model.Result r) {
        Vector3f n = new Vector3f(), d = new Vector3f();
        Geometry.dirToWorld(v, r.normal, n);
        Geometry.dirToWorld(v, r.dirLocal, d);
        boolean ric = r.ricochet || r.kind.equals(Model.ARMOR) && !r.penetrated;
        return new float[] { r.world.x, r.world.y, r.world.z, n.x, n.y, n.z, d.x, d.y, d.z, code(r), ric ? 1 : 0,
            (float) Power.factor(r.energyIn), v.getId(), r.local.x, r.local.y, r.local.z, r.normal.x, r.normal.y, r.normal.z };
    }

    /** {wx, wy, wz(Stockwerke), nx, ny, nz (Welt, z oben), dx, dy, dz, code, ricochet, power, vehicleId, lx, ly, lz, lnx, lny, lnz} oder null. */
    public static float[] nextImpact() {
        synchronized (impacts) {
            return impacts.pollFirst();
        }
    }

    /** Einschlag einer Kugel a -> b (Welt, z in Stockwerken) am naechsten Fahrzeug bestimmen, ohne Schaden. */
    public static float[] classifyRay(double ax, double ay, double az, double bx, double by, double bz, double energy) {
        try {
            if (IsoWorld.instance == null || IsoWorld.instance.getCell() == null) return null;
            BaseVehicle best = null;
            Geometry.Hit bestHit = null;
            for (BaseVehicle v : IsoWorld.instance.getCell().getVehicles()) {
                if (v == null) continue;
                double mx = Math.min(ax, bx) - 6, Mx = Math.max(ax, bx) + 6, my = Math.min(ay, by) - 6, My = Math.max(ay, by) + 6;
                if (v.getX() < mx || v.getX() > Mx || v.getY() < my || v.getY() > My) continue;
                Geometry.Hit h = new Geometry.Hit();
                if (Geometry.ray(v, ax, ay, az, bx, by, bz, h) && (bestHit == null || h.t < bestHit.t)) { best = v; bestHit = h; }
            }
            if (best == null) return null;
            Vector3f dl = new Vector3f();
            Geometry.dirToLocal(best, bx - ax, by - ay, (bz - az) * Geometry.LEVEL, dl);
            Model.Result r = Model.classify(best, bestHit, dl, energy);
            return pack(best, r);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Liegt (x, y, z) auf oder an einem Fahrzeug (Toleranz in Metern)? */
    public static boolean vehicleAt(double x, double y, double z, double margin) {
        try {
            if (IsoWorld.instance == null || IsoWorld.instance.getCell() == null) return false;
            Vector3f l = new Vector3f();
            for (BaseVehicle v : IsoWorld.instance.getCell().getVehicles()) {
                if (v == null || Math.abs(v.getX() - x) > 6 || Math.abs(v.getY() - y) > 6) continue;
                Geometry.Shape sh = Geometry.shape(v);
                if (sh == null || !Geometry.toLocal(v, x, y, z, l)) continue;
                if (Math.abs(l.x - sh.com.x) < sh.half.x + margin && Math.abs(l.z - sh.com.z) < sh.half.z + margin
                    && l.y > sh.bottom - margin && l.y < sh.top + margin) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Bleibende Loecher eines Fahrzeugs: je Loch {wx, wy, wz, nx, ny, nz, size, code, vehicleId}.
     * Nur Teile, die eingebaut und nicht voll repariert sind.
     */
    public static void holes(Object vehicle, List<float[]> out) {
        if (!(vehicle instanceof BaseVehicle v)) return;
        VehicleParts ps = v.getParts();
        if (ps == null) return;
        Vector3f p = new Vector3f(), n = new Vector3f(), w = new Vector3f(), wn = new Vector3f();
        for (int i = 0; i < ps.getPartCount(); i++) {
            VehiclePart part = ps.getPartByIndex(i);
            if (part == null) continue;
            KahluaTable md;
            try { md = part.getModData(); } catch (Throwable t) { continue; }
            if (md == null || !(md.rawget(Holes.KEY) instanceof KahluaTable list)) continue;
            if (part.getCondition() >= 100 || !Model.installed(part)) continue;
            if (part.getWindow() != null && part.getWindow().isDestroyed()) continue;
            Object item = md.rawget("vvbItem");
            if (item instanceof Double id && part.getInventoryItem() != null && id.intValue() != part.getInventoryItem().getID()) continue;
            for (int k = 1; k <= list.len(); k++) {
                if (!(list.rawget(k) instanceof KahluaTable h)) continue;
                p.set(f(h, "x"), f(h, "y"), f(h, "z"));
                n.set(f(h, "nx"), f(h, "ny"), f(h, "nz"));
                Geometry.toWorld(v, p, w);
                Geometry.dirToWorld(v, n, wn);
                String m = h.rawget("m") instanceof String s ? s : "";
                int code = switch (m) {
                    case "glass" -> GLASS;
                    case "glass_armored" -> GLASS_ARMORED;
                    case "armor" -> ARMOR;
                    case "Rubber" -> RUBBER;
                    case "Glass_Light" -> LAMP;
                    case "Metal" -> METAL;
                    default -> METAL_LIGHT;
                };
                out.add(new float[] { w.x, w.y, w.z, wn.x, wn.y, wn.z, f(h, "s"), code, v.getId() });
            }
        }
    }

    private static float f(KahluaTable t, String k) {
        return t.rawget(k) instanceof Number n ? n.floatValue() : 0f;
    }

    static String describe(Object vehicle) {
        if (!(vehicle instanceof BaseVehicle v)) return "no vehicle";
        Geometry.Shape sh = Geometry.shape(v);
        if (sh == null) return "no script";
        return String.format("%s: %d boxes, half %.2f %.2f %.2f, com %.2f %.2f %.2f, front %s, belt %.2f, wheels %d",
            v.getScript().getFullName(), sh.boxes.size(), sh.half.x, sh.half.y, sh.half.z, sh.com.x, sh.com.y, sh.com.z,
            sh.front > 0 ? "+z" : "-z", Model.beltY(v, sh), v.getScript().getWheelCount());
    }

    private Effects() {}
}
