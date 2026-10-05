package vpfx;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import zombie.characters.IsoPlayer;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;
import zombie.vehicles.BaseVehicle;

/**
 * Einschlaege an Fahrzeugen, ueber Vehicle Ballistics (ViewpointVehicleBallistics, sonst die Kopie in Bullet
 * Impacts 2D), nur per Reflection - ohne die Mod bleibt alles wie vorher.
 *
 *  - eigene Treffer: das Schadensmodell meldet Punkt, Normale und Material (Glas, Blech, Reifen, Panzerung, Lampe)
 *  - fremde Kugeln (Tracer anderer Spieler): Material ueber dieselbe Fahrzeugform bestimmt
 *  - bleibende Loecher: aus der ModData der Fahrzeugteile, jedes Bild an der aktuellen Fahrzeugposition
 *    (fahren also mit). Glas: helles Loch mit Sprung-Hof; Blech: dunkles Loch; Panzerung: helle Schramme.
 */
final class Vehicles {
    private static final float LEVEL = Fx.LEVEL;
    private static boolean checked;
    private static Method next, classify, at, holes;
    private static final List<float[]> tmp = new ArrayList<>();

    private static void init() {
        if (checked) return;
        checked = true;
        for (String pkg : new String[] { "vpveh", "vbi2d" }) {
            try {
                Class<?> c = Class.forName(pkg + ".Effects", true, Vehicles.class.getClassLoader());
                next = c.getMethod("nextImpact");
                classify = c.getMethod("classifyRay", double.class, double.class, double.class, double.class, double.class, double.class, double.class);
                at = c.getMethod("vehicleAt", double.class, double.class, double.class, double.class);
                holes = c.getMethod("holes", Object.class, List.class);
                System.out.println("[ViewpointEffects3D] Vehicle Ballistics found (" + pkg + "): vehicle impacts and lasting bullet holes");
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    static boolean present() {
        init();
        return next != null;
    }

    static String material(int code) {
        return switch (code) {
            case 1 -> "Glass";
            case 2 -> "Glass_Solid";
            case 3 -> "Rubber";
            case 4 -> "Metal_Solid";
            case 5 -> "Glass_Light";
            case 6 -> "Metal";
            case 7 -> null;            // offenes Fenster: nichts
            default -> "Metal_Light";
        };
    }

    private static void impact(float[] e, String ammoKey) {
        String m = material((int) e[9]);
        if (m == null) return;
        Impacts.vehicle(e[0], e[1], e[2], e[6], e[7], e[8], m, ammoKey, e[3], e[4], e[5], e[10] > 0.5f);
    }

    /** Game-Thread, jedes Bild: eigene Fahrzeugtreffer zeichnen. */
    static void drain(String ammoKey) {
        if (!present()) return;
        try {
            Object o;
            int n = 0;
            while (n++ < 32 && (o = next.invoke(null)) instanceof float[] e) {
                if (Fx.flagPublic("VFA_FX_Impacts", true)) impact(e, ammoKey);
            }
        } catch (Throwable t) {
            next = null;
            System.err.println("[ViewpointEffects3D] vehicle impacts disabled: " + t);
        }
    }

    /**
     * Kugel endet bei (ex, ey, ez): Fahrzeug? true = erledigt (kein Feld-Material-Effekt).
     * Eigene Schuesse kommen schon aus drain(), fremde werden hier bestimmt.
     */
    static boolean tracerEnd(Object shooter, double sx, double sy, double sz, double ex, double ey, double ez, String ammoKey) {
        if (!present()) return false;
        try {
            if (!(Boolean) at.invoke(null, ex, ey, ez, 0.35)) return false;
            if (shooter instanceof IsoPlayer p && p.isLocalPlayer()) return true;
            double dx = ex - sx, dy = ey - sy, dz = ez - sz;
            double l = Math.sqrt(dx * dx + dy * dy + dz * dz * LEVEL * LEVEL);
            if (l < 1e-4) return true;
            Object r = classify.invoke(null, sx, sy, sz, ex + dx / l * 0.5, ey + dy / l * 0.5, ez + dz / l * 0.5, 500.0);
            if (r instanceof float[] e && Fx.flagPublic("VFA_FX_Impacts", true)) impact(e, ammoKey);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Game-Thread: bleibende Loecher an Fahrzeugen in der Naehe der Kamera in die Bildliste. */
    static void addHoles(List<Fx.Item> list, IsoCell cell, float cx, float cy, float cz, float flashBoost) {
        if (!present() || holes == null || !Fx.flagPublic("VFA_FX_Holes", true) || cell == null) return;
        try {
            for (BaseVehicle v : IsoWorld.instance.getCell().getVehicles()) {
                if (v == null) continue;
                float dx = v.getX() - cx, dy = v.getY() - cy;
                if (dx * dx + dy * dy > 30f * 30f) continue;
                tmp.clear();
                holes.invoke(null, v, tmp);
                for (float[] h : tmp) {
                    int code = (int) h[7];
                    float s = Math.max(0.5f, h[6]);
                    double x = h[0] + h[3] * 0.004, y = h[1] + h[4] * 0.004, z = h[2] + h[5] * 0.004 / LEVEL;
                    float light = Math.max(0.08f, Math.min(1f, Fx.light(cell, x, y, z) + flashBoost * 0.5f));
                    float rx = (float) -(x - cx), ry = (float) ((z - cz) * LEVEL), rz = (float) -(y - cy);
                    float nx = -h[3], ny = h[5], nz = -h[4];
                    float seed = (float) ((h[0] * 13.1 + h[1] * 7.7 + h[2] * 3.3) % 100);
                    switch (code) {
                        case 1 -> {          // Glas: Sprung-Hof + Loch
                            list.add(new Fx.Item(4, rx, ry, rz, nx, ny, nz, 0.09f * s, 0.09f * s, 0.92f * light, 0.95f * light, 1f * light, 0.22f, seed));
                            list.add(new Fx.Item(4, rx, ry, rz, nx, ny, nz, 0.018f * s, 0.018f * s, 0.15f * light, 0.16f * light, 0.18f * light, 0.85f, seed + 1));
                        }
                        case 2, 4 -> list.add(new Fx.Item(4, rx, ry, rz, nx, ny, nz, 0.03f * s, 0.03f * s, 0.78f * light, 0.78f * light, 0.75f * light, 0.55f, seed));
                        case 3 -> list.add(new Fx.Item(4, rx, ry, rz, nx, ny, nz, 0.015f * s, 0.015f * s, 0.02f, 0.02f, 0.02f, 0.9f, seed));
                        case 5 -> {}           // Lampe: kaputt ist kaputt
                        default -> {           // Blech: blanker Rand + dunkles Loch
                            list.add(new Fx.Item(4, rx, ry, rz, nx, ny, nz, 0.03f * s, 0.03f * s, 0.62f * light, 0.62f * light, 0.6f * light, 0.5f, seed));
                            list.add(new Fx.Item(4, rx, ry, rz, nx, ny, nz, 0.016f * s, 0.016f * s, 0.03f, 0.03f, 0.035f, 0.95f, seed + 1));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            holes = null;
            System.err.println("[ViewpointEffects3D] vehicle bullet holes disabled: " + t);
        }
    }

    private Vehicles() {}
}
