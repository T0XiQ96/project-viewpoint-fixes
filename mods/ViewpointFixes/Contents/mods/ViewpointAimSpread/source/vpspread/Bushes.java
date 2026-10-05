package vpspread;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTree;
import zombie.iso.sprite.IsoSprite;

/**
 * Buesche lenken VTB-Kugeln ab.
 *
 * Nach jedem Flugschritt (vtb.projectile.ProjectilePhysics.step) wird die Strecke vorher -> nachher auf Busch-Felder
 * abgelaufen (Busch-Sprites, kein Baum; Baumstaemme stoppt VTB selbst). Jeder Busch wuerfelt pro Kugel einmal:
 * mit VFA_CoverBushChance (Standard 10 %) wird die Geschwindigkeit in einem Kegel bis VFA_CoverDeflectDeg
 * (Standard 10 Grad, seitlich und in der Hoehe) gedreht, ihr Betrag bleibt. Danach fliegt die Kugel mit VTB weiter
 * und trifft, was auf der neuen Bahn liegt - auch das urspruengliche Ziel, wenn es noch drauf liegt, oder den Boden.
 * Koordinaten wie VTB: metrisch (x, y in Feldern, u = Hoehe in Metern; 1 Stockwerk = sqrt 6 m).
 */
public final class Bushes {
    private static final double LEVEL = 2.4494896;
    private static final double BUSH_HEIGHT = 1.6;   // m ueber dem Boden
    private static final double STEP = 0.25;

    private static volatile boolean failed;
    private static Field vx, vy, vu, velocityOf;
    private static final Map<Object, Set<Long>> passed = new IdentityHashMap<>();
    private static int logged;

    private static void fields(Object v3) throws Exception {
        if (vx != null) return;
        Class<?> c = v3.getClass();
        vx = c.getField("x"); vy = c.getField("y"); vu = c.getField("u");
    }

    static boolean bush(IsoGridSquare sq) {
        if (sq == null) return false;
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject o = sq.getObjects().get(i);
            if (o == null || o instanceof IsoTree) continue;
            IsoSprite s = o.getSprite();
            String n = s == null ? null : s.getName();
            if (n != null && (n.startsWith("f_bushes") || n.startsWith("vegetation_foliage") || n.startsWith("vegetation_ornamental")))
                return true;
        }
        return false;
    }

    public static void afterStep(Object pos, Object vel, Object prev) {
        if (failed || pos == null || vel == null || prev == null) return;
        try {
            if (!Spread.flag("VFA_CoverOn", true)) return;
            fields(pos);
            double ax = vx.getDouble(prev), ay = vy.getDouble(prev), au = vu.getDouble(prev);
            double bx = vx.getDouble(pos), by = vy.getDouble(pos), bu = vu.getDouble(pos);
            double len = Math.hypot(bx - ax, by - ay);
            if (len < 1e-6) return;
            IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.getCell();
            if (cell == null) return;
            float chance = Math.max(0f, Math.min(1f, Spread.number("VFA_CoverBushChance", 0.10f)));

            if (passed.size() > 4096) passed.clear();
            Set<Long> seen = passed.computeIfAbsent(vel, k -> new HashSet<>());
            for (double s = 0; s <= len; s += STEP) {
                double t = s / len;
                double x = ax + (bx - ax) * t, y = ay + (by - ay) * t, u = au + (bu - au) * t;
                int level = (int) Math.floor(u / LEVEL);
                if (u - level * LEVEL > BUSH_HEIGHT) continue;            // ueber dem Busch
                int qx = (int) Math.floor(x), qy = (int) Math.floor(y);
                long key = ((long) qx << 40) ^ ((long) qy << 8) ^ (level & 0xff);
                if (!seen.add(key)) continue;
                if (!bush(cell.getGridSquare(qx, qy, level))) continue;
                if (ThreadLocalRandom.current().nextFloat() >= chance) continue;
                deflect(vel);
                if (logged < 10) {
                    logged++;
                    System.out.printf("[ViewpointAimSpread] round deflected by a bush at %d,%d%n", qx, qy);
                }
                return;   // ab hier neue Richtung; der Rest dieses Schritts zaehlt nicht mehr
            }
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ViewpointAimSpread] Bush deflection disabled: " + t);
        }
    }

    /** Geschwindigkeit in einem Kegel drehen, Betrag behalten (Basis wie VTBs ProjectilePhysics.deflect). */
    private static void deflect(Object vel) throws Exception {
        double x = vx.getDouble(vel), y = vy.getDouble(vel), u = vu.getDouble(vel);
        double speed = Math.sqrt(x * x + y * y + u * u);
        if (speed < 1e-9) return;
        double dx = x / speed, dy = y / speed, du = u / speed;
        double maxRad = Math.toRadians(Math.max(0f, Spread.number("VFA_CoverDeflectDeg", 10f)));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double rad = maxRad * Math.sqrt(r.nextDouble()), phi = r.nextDouble() * Math.PI * 2.0;
        double right = rad * Math.cos(phi), up = rad * Math.sin(phi);

        double rx = dy, ry = -dx;
        double rl = Math.hypot(rx, ry);
        if (rl < 1e-9) { rx = 1; ry = 0; rl = 1; }
        rx /= rl; ry /= rl;
        double ux = ry * du, uy = -rx * du, uu = rx * dy - ry * dx;
        double tr = Math.tan(right), tu = Math.tan(up);
        double nx = dx + rx * tr + ux * tu, ny = dy + ry * tr + uy * tu, nu = du + uu * tu;
        double nl = Math.sqrt(nx * nx + ny * ny + nu * nu);
        vx.setDouble(vel, nx / nl * speed);
        vy.setDouble(vel, ny / nl * speed);
        vu.setDouble(vel, nu / nl * speed);
    }

    /** VTB legt die Kugel zurueck in den Pool (BallisticProjectile.reset). */
    public static void forget(Object projectile) {
        if (projectile == null) return;
        try {
            if (velocityOf == null) velocityOf = projectile.getClass().getField("velocity");
            passed.remove(velocityOf.get(projectile));
        } catch (Throwable t) {
            passed.clear();
        }
    }

    private Bushes() {}
}
