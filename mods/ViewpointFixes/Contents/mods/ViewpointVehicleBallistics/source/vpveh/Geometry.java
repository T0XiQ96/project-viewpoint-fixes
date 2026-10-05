package vpveh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Vector3f;

import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

/**
 * Treffpunkt einer Kugel am Fahrzeug, in Fahrzeug-Koordinaten (x seitlich, +x = links; y oben; z laengs).
 *
 * Form des Fahrzeugs: die Physik-Boxen aus seinem Script (auch schraege Boxen, z. B. Windschutzscheibe), sonst der
 * Quader aus extents/centerOfMassOffset. Boxen, die nicht zum Quader passen (andere Skalierung/Verschiebung), werden
 * auf ihn ausgerichtet - so passt es bei jeder Mod. Raeder: Zylinder aus Radposition, Radius und Breite.
 *
 * Welt: x, y in Feldern (= Metern), z in Stockwerken; Physik: x - offsetX, z * 2.44949, y - offsetY.
 */
final class Geometry {
    static final float LEVEL = 2.44949f;

    /** Ergebnis eines Strahltests. */
    static final class Hit {
        boolean valid;
        float t;                       // 0..1 auf dem Strahl
        final Vector3f local = new Vector3f();
        final Vector3f normal = new Vector3f();
        int wheel = -1;                // Radindex bei Reifentreffer
        float exitT = -1;              // Austritt aus dem Fahrzeug (fuer Durchschuss), -1 = unbekannt
        void clear() { valid = false; t = 2; wheel = -1; exitT = -1; local.zero(); normal.zero(); }
    }

    /** Box in Fahrzeug-Koordinaten. */
    private record Box(Vector3f c, Vector3f h, Matrix3f r, Matrix3f rt) {}

    /** Je Fahrzeug-Script: Boxen, Massstab, Fahrtrichtung. */
    static final class Shape {
        final List<Box> boxes = new ArrayList<>();
        Vector3f com, half;            // Quader aus extents
        float front = 1;               // +1: vorne = +z
        float bottom, top;
    }

    private static final Map<String, Shape> shapes = new HashMap<>();

    static Shape shape(BaseVehicle v) {
        VehicleScript s = v.getScript();
        if (s == null) return null;
        String key = s.getFullName();
        Shape sh = shapes.get(key);
        if (sh == null) {
            sh = build(s);
            shapes.put(key, sh);
        }
        return sh;
    }

    private static Shape build(VehicleScript s) {
        Shape sh = new Shape();
        Vector3f ext = new Vector3f(s.getExtents());
        Vector3f com = new Vector3f(s.getCenterOfMassOffset());
        sh.com = com;
        sh.half = new Vector3f(ext).mul(0.5f);
        sh.bottom = com.y - sh.half.y;
        sh.top = com.y + sh.half.y;
        // Fahrtrichtung aus den Vorderraedern
        float fz = 0, rz = 0;
        int nf = 0, nr = 0;
        for (int i = 0; i < s.getWheelCount(); i++) {
            VehicleScript.Wheel w = s.getWheel(i);
            if (w == null) continue;
            if (w.front) { fz += w.offset.z; nf++; } else { rz += w.offset.z; nr++; }
        }
        if (nf > 0 && nr > 0 && fz / nf < rz / nr) sh.front = -1;

        List<Box> raw = new ArrayList<>();
        float minX = 1e9f, minY = 1e9f, minZ = 1e9f, maxX = -1e9f, maxY = -1e9f, maxZ = -1e9f;
        for (int i = 0; i < s.getPhysicsShapeCount(); i++) {
            VehicleScript.PhysicsShape ps = s.getPhysicsShape(i);
            if (ps == null || ps.type != VehicleScript.PHYSICS_SHAPE_BOX) continue;
            Vector3f h = new Vector3f(ps.extents).mul(0.5f);
            if (h.x <= 0 || h.y <= 0 || h.z <= 0) continue;
            Matrix3f r = new Matrix3f().rotateXYZ((float) Math.toRadians(ps.rotate.x), (float) Math.toRadians(ps.rotate.y),
                (float) Math.toRadians(ps.rotate.z));
            Box b = new Box(new Vector3f(ps.offset), h, r, new Matrix3f(r).transpose());
            raw.add(b);
            // Huelle der gedrehten Box
            for (int k = 0; k < 8; k++) {
                Vector3f p = new Vector3f((k & 1) == 0 ? -h.x : h.x, (k & 2) == 0 ? -h.y : h.y, (k & 4) == 0 ? -h.z : h.z);
                r.transform(p).add(b.c());
                minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
                minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
            }
        }
        if (!raw.isEmpty()) {
            float len = maxZ - minZ, wid = maxX - minX;
            float k = 1;
            if (len > 0.05f && ext.z > 0.05f) {
                float ratio = ext.z / len;
                if (ratio > 1.3f || ratio < 0.75f) k = ratio;          // anderer Massstab (Modell-Einheiten)
            }
            float cx = (minX + maxX) * 0.5f * k, cy = (minY + maxY) * 0.5f * k, cz = (minZ + maxZ) * 0.5f * k;
            Vector3f shift = new Vector3f();
            if (Math.abs(cz - com.z) > 0.25f * ext.z || Math.abs(cx - com.x) > 0.25f * Math.max(0.1f, ext.x)) {
                shift.set(com.x - cx, 0, com.z - cz);
            }
            if (Math.abs(cy - com.y) > 0.5f * Math.max(0.1f, ext.y)) shift.y = com.y - cy;
            for (Box b : raw) {
                Vector3f c = new Vector3f(b.c()).mul(k).add(shift);
                Vector3f h = new Vector3f(b.h()).mul(k);
                sh.boxes.add(new Box(c, h, b.r(), b.rt()));
            }
            if (wid * k < 0.3f * ext.x) sh.boxes.clear();               // offensichtlich unbrauchbar
        }
        if (sh.boxes.isEmpty()) {
            Matrix3f id = new Matrix3f();
            sh.boxes.add(new Box(new Vector3f(com), new Vector3f(sh.half), id, new Matrix3f()));
        }
        return sh;
    }

    // ------------------------------------------------------------------ Koordinaten

    private static final ThreadLocal<Transform> TF = ThreadLocal.withInitial(Transform::new);

    /** Weltpunkt (x, y, z in Stockwerken) -> Fahrzeug-Koordinaten. */
    static boolean toLocal(BaseVehicle v, double wx, double wy, double wz, Vector3f out) {
        Transform t = TF.get();
        v.getWorldTransform(t);
        WorldSimulation ws = WorldSimulation.instance;
        float ox = ws == null ? 0 : ws.offsetX, oy = ws == null ? 0 : ws.offsetY;
        float px = (float) (wx - ox) - t.origin.x, py = (float) (wz * LEVEL) - t.origin.y, pz = (float) (wy - oy) - t.origin.z;
        // inverse Rotation = transponierte Basis
        Matrix3f b = t.basis;
        out.set(b.m00 * px + b.m01 * py + b.m02 * pz, b.m10 * px + b.m11 * py + b.m12 * pz, b.m20 * px + b.m21 * py + b.m22 * pz);
        return !(Float.isNaN(out.x) || Float.isNaN(out.y) || Float.isNaN(out.z));
    }

    /** Richtung (Welt, z in Metern) -> Fahrzeug-Koordinaten. */
    static void dirToLocal(BaseVehicle v, double dx, double dy, double dzMeters, Vector3f out) {
        Transform t = TF.get();
        v.getWorldTransform(t);
        Matrix3f b = t.basis;
        float px = (float) dx, py = (float) dzMeters, pz = (float) dy;
        out.set(b.m00 * px + b.m01 * py + b.m02 * pz, b.m10 * px + b.m11 * py + b.m12 * pz, b.m20 * px + b.m21 * py + b.m22 * pz);
    }

    /** Fahrzeug-Koordinaten -> Welt (x, y, z in Stockwerken). */
    static void toWorld(BaseVehicle v, Vector3f local, Vector3f out) {
        Transform t = TF.get();
        v.getWorldTransform(t);
        Vector3f p = new Vector3f(local);
        t.basis.transform(p);
        p.add(t.origin);
        WorldSimulation ws = WorldSimulation.instance;
        float ox = ws == null ? 0 : ws.offsetX, oy = ws == null ? 0 : ws.offsetY;
        out.set(p.x + ox, p.z + oy, p.y / LEVEL);
    }

    /** Lokale Richtung -> Welt (x, y, z in Metern). */
    static void dirToWorld(BaseVehicle v, Vector3f local, Vector3f out) {
        Transform t = TF.get();
        v.getWorldTransform(t);
        Vector3f p = new Vector3f(local);
        t.basis.transform(p);
        out.set(p.x, p.z, p.y);
    }

    // ------------------------------------------------------------------ Strahltest

    /**
     * Strahl a -> b (Welt, z in Stockwerken) gegen das Fahrzeug. Liefert den ersten Treffer (Box oder Rad).
     */
    static boolean ray(BaseVehicle v, double ax, double ay, double az, double bx, double by, double bz, Hit out) {
        out.clear();
        Shape sh = shape(v);
        if (sh == null) return false;
        Vector3f a = new Vector3f(), b = new Vector3f();
        if (!toLocal(v, ax, ay, az, a) || !toLocal(v, bx, by, bz, b)) return false;
        Vector3f d = new Vector3f(b).sub(a);
        if (d.lengthSquared() < 1e-10f) return false;

        float bestT = 2, exit = -1;
        Vector3f n = new Vector3f(), bestN = new Vector3f();
        float[] te = new float[2];
        for (Box box : sh.boxes) {
            if (slab(box, a, d, te, n)) {
                if (te[0] < bestT) { bestT = te[0]; bestN.set(n); }
                exit = Math.max(exit, te[1]);
            }
        }
        // Raeder
        VehicleScript s = v.getScript();
        int wheel = -1;
        for (int i = 0; s != null && i < s.getWheelCount(); i++) {
            VehicleScript.Wheel w = s.getWheel(i);
            if (w == null) continue;
            float susp = 0;
            try { susp = v.wheelInfo[i].suspensionLength; } catch (Throwable ignored) {}
            float cy = w.offset.y - susp;
            float t = cylinder(a, d, w.offset.x, cy, w.offset.z, w.radius, Math.max(0.1f, w.width), n);
            if (t >= 0 && t <= 1 && t < bestT + 0.04f) {
                if (t < bestT) { bestT = t; bestN.set(n); }
                wheel = i;
            }
        }
        if (bestT > 1) return false;
        out.valid = true;
        out.t = bestT;
        out.local.set(d).mul(bestT).add(a);
        out.normal.set(bestN);
        out.exitT = exit;
        // Treffer am Karosserie-Rand, aber im Radkreis: Reifen
        if (wheel < 0 && s != null) {
            for (int i = 0; i < s.getWheelCount(); i++) {
                VehicleScript.Wheel w = s.getWheel(i);
                if (w == null) continue;
                float susp = 0;
                try { susp = v.wheelInfo[i].suspensionLength; } catch (Throwable ignored) {}
                float dy = out.local.y - (w.offset.y - susp), dz = out.local.z - w.offset.z;
                if (Math.abs(out.local.x - w.offset.x) < Math.max(0.1f, w.width) * 0.5f + 0.18f
                    && dy * dy + dz * dz < w.radius * w.radius * 0.95f) {
                    wheel = i;
                    break;
                }
            }
        }
        out.wheel = wheel;
        return true;
    }

    /** Strahl gegen gedrehte Box; te = {Eintritt, Austritt}, n = Normale der Eintrittsflaeche (Fahrzeug-Koordinaten). */
    private static boolean slab(Box box, Vector3f a, Vector3f d, float[] te, Vector3f n) {
        Vector3f o = new Vector3f(a).sub(box.c());
        box.rt().transform(o);
        Vector3f dir = new Vector3f(d);
        box.rt().transform(dir);
        float t0 = -1e9f, t1 = 1e9f;
        int axis = -1;
        float sign = 0;
        for (int i = 0; i < 3; i++) {
            float oi = o.get(i), di = dir.get(i), hi = box.h().get(i);
            if (Math.abs(di) < 1e-8f) {
                if (oi < -hi || oi > hi) return false;
                continue;
            }
            float ta = (-hi - oi) / di, tb = (hi - oi) / di;
            float s = -1;
            if (ta > tb) { float x = ta; ta = tb; tb = x; s = 1; }
            if (ta > t0) { t0 = ta; axis = i; sign = s; }
            t1 = Math.min(t1, tb);
            if (t0 > t1) return false;
        }
        if (t1 < 0 || t0 > 1) return false;
        te[0] = Math.max(0, t0);
        te[1] = t1;
        n.zero();
        if (axis >= 0) n.setComponent(axis, sign);
        box.r().transform(n);
        return true;
    }

    /** Strahl gegen Zylinder (Achse x) mit Mitte c, Radius r, Breite w. Liefert t oder -1. */
    private static float cylinder(Vector3f a, Vector3f d, float cx, float cy, float cz, float r, float w, Vector3f n) {
        float oy = a.y - cy, oz = a.z - cz;
        float A = d.y * d.y + d.z * d.z, B = 2 * (oy * d.y + oz * d.z), C = oy * oy + oz * oz - r * r;
        float best = -1;
        if (A > 1e-9f) {
            float disc = B * B - 4 * A * C;
            if (disc >= 0) {
                float t = (-B - (float) Math.sqrt(disc)) / (2 * A);
                float x = a.x + d.x * t;
                if (t >= 0 && Math.abs(x - cx) <= w * 0.5f) {
                    best = t;
                    n.set(0, oy + d.y * t, oz + d.z * t).normalize();
                }
            }
        }
        // Seitenflaechen (Felge/Flanke)
        if (Math.abs(d.x) > 1e-8f) {
            for (float side : new float[] { -1, 1 }) {
                float t = (cx + side * w * 0.5f - a.x) / d.x;
                if (t < 0 || (best >= 0 && t >= best)) continue;
                float y = a.y + d.y * t - cy, z = a.z + d.z * t - cz;
                if (y * y + z * z <= r * r) { best = t; n.set(side, 0, 0); }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ Bereiche

    /** u: -1 rechts .. +1 links; f: -1 hinten .. +1 vorne; h: 0 unten .. 1 oben (bezogen auf den Quader). */
    static float side(Shape sh, Vector3f p) { return (p.x - sh.com.x) / Math.max(0.05f, sh.half.x); }
    static float along(Shape sh, Vector3f p) { return sh.front * (p.z - sh.com.z) / Math.max(0.05f, sh.half.z); }
    static float height(Shape sh, Vector3f p) { return (p.y - sh.bottom) / Math.max(0.05f, sh.top - sh.bottom); }

    private Geometry() {}
}
