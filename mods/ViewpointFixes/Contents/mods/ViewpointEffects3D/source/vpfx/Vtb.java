package vpfx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Anbindung an Viewpoint True Ballistics (VTB), nur per Reflection (fehlt VTB, passiert hier nichts).
 *
 * - Tracer: VTB simuliert eigene Schuesse als echte Projektile und schaltet dafuer die Tracer des Spiels ab.
 *   Jede lebende VTB-Kugel wird hier als 3D-Streifen gezeichnet, an ihrer echten Position (Fall, Ablenkung,
 *   Durchschuss inklusive). Farben/Dicken/Laengen kommen aus den Tracer-Einstellungen des Spiels fuer den
 *   Munitionstyp (IsoBulletTracerEffects.updateSettings), also wirken auch Waffen-Mods.
 *   Mitspieler: VTB schickt ihnen normale TracerInfo, die kommen als Spiel-Tracer an (Tracers.step).
 * - Kein doppelter Tracer: Beim Welteinschlag legt VTB im Einzelspieler kurz einen Spiel-Tracer an (nur um
 *   hitIsoGridSquare aufrufen zu koennen). Solche Effects werden markiert und von Tracers.step nicht gezeichnet.
 * - Einschlag: nach BallisticsManager.worldImpact (Punkt, Art: Wand/Boden/Baum/...; Sound macht VTB) die
 *   Material-Optik ueber Impacts.
 */
public final class Vtb {
    private static final float LEVEL = Fx.LEVEL;

    private static volatile boolean failed, absent;
    private static boolean ready, logged;
    private static Object manager;
    private static Method activeList, attackActive;
    private static Field pPos, pVel, pOrigin, pTraveled, pGroup, gWeapon, vx, vy, vu;
    private static int hitTree = -1, hitFloor = -1, hitCeiling = -1;
    private static int worldImpactDepth;
    private static final Set<Object> artifacts = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object, float[]> colors = new WeakHashMap<>();

    private static Class<?> find(String name) throws ClassNotFoundException {
        ClassLoader[] loaders = { Vtb.class.getClassLoader(), Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader() };
        ClassNotFoundException last = null;
        for (ClassLoader l : loaders) {
            if (l == null) continue;
            try { return Class.forName(name, true, l); } catch (ClassNotFoundException e) { last = e; }
        }
        throw last;
    }

    private static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }

    private static boolean init() {
        if (ready) return true;
        if (absent || failed) return false;
        try {
            Class<?> bm = find("vtb.projectile.BallisticsManager");
            manager = bm.getField("INSTANCE").get(null);
            activeList = bm.getMethod("activeProjectiles");
            attackActive = bm.getMethod("attackActive");
            Class<?> pr = find("vtb.projectile.BallisticProjectile");
            pPos = pr.getField("position"); pVel = pr.getField("velocity"); pOrigin = pr.getField("origin");
            pTraveled = pr.getField("traveled"); pGroup = pr.getField("group");
            gWeapon = find("vtb.projectile.ShotGroup").getField("weapon");
            Class<?> v3 = find("vtb.V3");
            vx = v3.getField("x"); vy = v3.getField("y"); vu = v3.getField("u");
            Class<?> wc = find("vtb.collision.WorldCollision");
            hitTree = wc.getField("HIT_TREE").getInt(null);
            hitFloor = wc.getField("HIT_FLOOR").getInt(null);
            hitCeiling = wc.getField("HIT_CEILING").getInt(null);
            ready = true;
            System.out.println("[ViewpointEffects3D] True Ballistics found: its rounds are drawn as 3D tracers");
            return true;
        } catch (ClassNotFoundException e) {
            absent = true;
            return false;
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ViewpointEffects3D] True Ballistics bridge disabled: " + t);
            return false;
        }
    }

    /** VTB hat diesen Angriff uebernommen (zwischen attackCollisionCheck enter und exit). */
    static boolean attackActive() {
        if (!init()) return false;
        try {
            return (Boolean) attackActive.invoke(manager);
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------- Hilfs-Tracer beim Welteinschlag

    public static void enterWorldImpact() { worldImpactDepth++; }
    public static void exitWorldImpact() { if (worldImpactDepth > 0) worldImpactDepth--; }

    /** IsoBulletTracerEffects.addEffect exit: waehrend worldImpact angelegt = VTB-Hilfs-Tracer. */
    public static void afterAddEffect(Object effect) {
        if (effect != null && worldImpactDepth > 0) artifacts.add(effect);
    }

    static boolean isArtifact(Object effect) { return artifacts.contains(effect); }

    static void forgetArtifact(Object effect) { artifacts.remove(effect); }

    // ---------------------------------------------------------------- Einschlag

    /** Nach BallisticsManager.worldImpact(projectile, point, kind, tileX, tileY, tileZ). */
    public static void afterWorldImpact(Object projectile, Object point, int kind, int tx, int ty, int tz) {
        if (!init() || projectile == null || point == null) return;
        try {
            if (!Fx.viewpointOnPublic() || !Fx.flagPublic("VFA_FX_Impacts", true)) return;
            double x = vx.getDouble(point), y = vy.getDouble(point), u = vu.getDouble(point);
            Object vel = pVel.get(projectile);
            float dx = (float) vx.getDouble(vel), dy = (float) vy.getDouble(vel), du = (float) vu.getDouble(vel);
            Object weapon = gWeapon.get(pGroup.get(projectile));
            Object ammo = weapon == null ? null : weapon.getClass().getMethod("getAmmoType").invoke(weapon);
            String material;
            if (kind == hitTree) material = "Wood_Solid";
            else {
                Object sq = zombie.iso.IsoWorld.instance == null ? null
                    : zombie.iso.IsoWorld.instance.getCell().getGridSquare(tx, ty, tz);
                material = Fx.materialOf(sq);
                if (material == null && (kind == hitFloor || kind == hitCeiling)) material = "Dirt";
            }
            double z = u / LEVEL;
            if (kind == hitFloor) z = Math.floor(z + 0.02) + 0.001;     // genau auf den Boden
            Impacts.hit(x, y, z, dx, dy, du, material, Fx.ammoKey(ammo));
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ViewpointEffects3D] True Ballistics impact disabled: " + t);
        }
    }

    // ---------------------------------------------------------------- Tracer der VTB-Kugeln

    /** Farben/Dicken des Spiel-Tracers fuer einen Munitionstyp: {r,g,b,a, w0,w1, tr,tg,tb,ta, trailLen, len}. */
    private static float[] colorsFor(Object ammo) {
        if (ammo == null) return null;
        float[] c = colors.get(ammo);
        if (c == null) {
            c = Tracers.settingsFor(ammo);
            if (c != null) colors.put(ammo, c);
        }
        return c;
    }

    /** Game-Thread (Fx.capture): alle lebenden VTB-Kugeln als Streifen. */
    static void step(List<Fx.Item> out, float cx, float cy, float cz) {
        if (!init() || !Fx.flagPublic("VFA_FX_Tracers", true)) return;
        try {
            List<?> list = (List<?>) activeList.invoke(manager);
            if (list == null || list.isEmpty()) return;
            float scale = Fx.numberPublic("VFA_FX_TracerWidth", 1.1f);
            float camU = cz * LEVEL;
            for (Object p : list) {
                Object pos = pPos.get(p), vel = pVel.get(p);
                double x = vx.getDouble(pos), y = vy.getDouble(pos), u = vu.getDouble(pos);
                double vX = vx.getDouble(vel), vY = vy.getDouble(vel), vU = vu.getDouble(vel);
                double speed = Math.sqrt(vX * vX + vY * vY + vU * vU);
                if (speed < 1e-3) continue;
                double traveled = pTraveled.getDouble(p);
                Object weapon = gWeapon.get(pGroup.get(p));
                Object ammo = weapon == null ? null : weapon.getClass().getMethod("getAmmoType").invoke(weapon);
                float[] c = colorsFor(ammo);
                float r = 1f, g = 0.85f, b = 0.45f, a = 0.9f, w0 = 0.01f, w1 = 0.03f, len = 1.2f;
                float tr = 1f, tg = 0.6f, tb = 0.3f, ta = 0.35f, trail = 4f;
                if (c != null) {
                    r = c[0]; g = c[1]; b = c[2]; a = c[3]; w0 = c[4]; w1 = c[5];
                    tr = c[6]; tg = c[7]; tb = c[8]; ta = c[9]; trail = c[10]; len = c[11];
                }
                // Geschoss: mindestens so lang, dass es bei hoher Geschwindigkeit pro Bild sichtbar bleibt
                double streak = Math.min(traveled, Math.max(len, Math.min(4.0, speed * 0.02)));
                double trailLen = Math.min(traveled, Math.max(streak, trail));
                double ux = vX / speed, uy = vY / speed, uu = vU / speed;
                float hx = (float) -(x - cx), hy = (float) (u - camU), hz = (float) -(y - cy);
                if (ta > 0.004f && trailLen > 0.05) {
                    float sx = (float) -(x - ux * trailLen - cx), sy = (float) (u - uu * trailLen - camU), sz = (float) -(y - uy * trailLen - cy);
                    out.add(new Fx.Item(3, sx, sy, sz, hx, hy, hz, w(w0, scale) * 0.6f, w(w1, scale), tr, tg, tb, Math.min(1f, ta), 0f));
                }
                float sx = (float) -(x - ux * streak - cx), sy = (float) (u - uu * streak - camU), sz = (float) -(y - uy * streak - cy);
                out.add(new Fx.Item(3, sx, sy, sz, hx, hy, hz, w(w0, scale), w(w1, scale), r, g, b, Math.min(1f, a), 0f));
            }
            if (!logged) {
                logged = true;
                System.out.println("[ViewpointEffects3D] first True Ballistics round drawn");
            }
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ViewpointEffects3D] True Ballistics tracers disabled: " + t);
            t.printStackTrace();
        }
    }

    private static float w(float px, float scale) {
        return Math.max(0.006f, Math.min(0.2f, px / 64f * scale));
    }

    private Vtb() {}
}
