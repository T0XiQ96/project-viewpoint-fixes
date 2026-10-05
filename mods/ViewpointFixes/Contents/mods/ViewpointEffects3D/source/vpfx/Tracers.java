package vpfx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import zombie.characters.IsoPlayer;

/**
 * Die Tracer des Spiels (IsoBulletTracerEffects) in 3D.
 *
 * Vanilla legt pro Schuss ein Effect an (eigene Schuesse und die von Mitspielern) mit Startpunkt, Richtung,
 * Reichweite und den Einstellungen des Munitionstyps (Farben, Laenge, Dicke, Tempo - daher wirken auch
 * Waffen-Mods). Gezeichnet wird es isometrisch in IsoBulletTracerEffects.render(), das unter Viewpoint nicht zu
 * sehen ist. In der 3D-Ansicht wird render() uebersprungen; step() macht dasselbe Fortbewegen wie vanilla
 * (updateSettings, timer += Multiplikator / 1.6, Strecke = timer * Tempo / 200, Entfernen am Ende) und liefert
 * Streifen fuer Fx.draw.
 *
 * Knicke: ViewpointAimSpread meldet abgelenkte Kugeln (Busch) und am Stamm gestoppte Kugeln ueber die Queue
 * "vfa.fx.bends" in den System-Properties. Ein neuer Tracer des eigenen Spielers uebernimmt einen frischen Knick:
 * bis zum Knickpunkt die Vanilla-Linie, danach die neue Richtung ueber die gemeldete Reststrecke (0 = endet dort).
 */
public final class Tracers {
    private static final float LEVEL = 2.4494896f;
    private static final float PIXEL = 1f / 64f;    // Dicke ist in Bildpunkten angegeben; 64 px ~ 1 Feld

    private static volatile boolean failed;
    private static boolean ready, logged;
    private static Method getInstance, updateSettings, poolRelease, gtInstance, gtMultiplier, gtPaused;
    private static Field effects, effectPool, ammoType, character, x0, y0, z0, dir, timer, speed, range, length;
    private static Field pR, pG, pB, pA, pW0, pW1, tR, tG, tB, tA, tLen, tW0, tW1;
    private static Field hR, hG, hB, hA, hW0, hW1, hFade;
    private static Field vx, vy, vz, square, playSound, start, end, current;
    private static Method hitSquare;
    private static Object combat;

    /** Knick an einem Effect: Strecke bis zum Knick (Effect-Einheiten), neue Richtung (Effect-Raum), Reststrecke. */
    private record Bend(float at, float nx, float ny, float nz, float rest) {}
    private static final Map<Object, Bend> bends = new IdentityHashMap<>();
    private static final Map<Object, float[]> known = new IdentityHashMap<>();   // Effect -> Startpunkt beim ersten Sehen

    private static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }

    private static synchronized void init() throws Exception {
        if (ready) return;
        ClassLoader l = Tracers.class.getClassLoader();
        Class<?> te = Class.forName("zombie.iso.objects.IsoBulletTracerEffects", true, l);
        Class<?> e = Class.forName("zombie.iso.objects.IsoBulletTracerEffects$Effect", true, l);
        Class<?> ammo = Class.forName("zombie.scripting.objects.AmmoType", true, l);
        getInstance = te.getMethod("getInstance");
        updateSettings = te.getDeclaredMethod("updateSettings", ammo, e);
        updateSettings.setAccessible(true);
        effects = field(te, "effects");
        effectPool = field(te, "effectPool");
        poolRelease = Class.forName("zombie.popman.ObjectPool", true, l).getMethod("release", Object.class);
        Class<?> gt = Class.forName("zombie.GameTime", true, l);
        gtInstance = gt.getMethod("getInstance");
        gtMultiplier = gt.getMethod("getMultiplier");
        gtPaused = gt.getMethod("isGamePaused");

        ammoType = field(e, "ammoType");
        character = field(e, "character");
        x0 = field(e, "x0"); y0 = field(e, "y0"); z0 = field(e, "z0");
        dir = field(e, "directionVector");
        timer = field(e, "timer"); speed = field(e, "projectileSpeed");
        range = field(e, "currentRange"); length = field(e, "projectileLength");
        pR = field(e, "projectileRed"); pG = field(e, "projectileGreen"); pB = field(e, "projectileBlue");
        pA = field(e, "projectileAlpha");
        pW0 = field(e, "projectileStartThickness"); pW1 = field(e, "projectileEndThickness");
        tR = field(e, "projectileTrailRed"); tG = field(e, "projectileTrailGreen"); tB = field(e, "projectileTrailBlue");
        tA = field(e, "projectileTrailAlpha"); tLen = field(e, "projectileTrailLength");
        tW0 = field(e, "projectileTrailStartThickness"); tW1 = field(e, "projectileTrailEndThickness");
        hR = field(e, "projectilePathRed"); hG = field(e, "projectilePathGreen"); hB = field(e, "projectilePathBlue");
        hA = field(e, "projectilePathAlpha"); hFade = field(e, "projectilePathFadeRate");
        hW0 = field(e, "projectilePathStartThickness"); hW1 = field(e, "projectilePathEndThickness");
        square = field(e, "isoGridSquare"); playSound = field(e, "playSound");
        start = field(e, "start"); end = field(e, "end"); current = field(e, "current");
        Class<?> cm = Class.forName("zombie.CombatManager", true, l);
        combat = cm.getMethod("getInstance").invoke(null);
        hitSquare = cm.getMethod("hitIsoGridSquare", Class.forName("zombie.iso.IsoGridSquare", true, l),
            Class.forName("org.joml.Vector3f", true, l), e);
        Class<?> v3 = Class.forName("org.joml.Vector3f", true, l);
        vx = v3.getField("x"); vy = v3.getField("y"); vz = v3.getField("z");
        ready = true;
    }

    public static boolean skipVanilla() {
        return !failed && Fx.flagPublic("VFA_FX_Tracers", true) && Fx.viewpointOnPublic();
    }

    private static float g(Field f, Object o) throws Exception { return f.getFloat(o); }

    private static java.lang.reflect.Constructor<?> effectNew;

    /**
     * Tracer-Einstellungen des Spiels fuer einen Munitionstyp (fuer die VTB-Kugeln): ein leeres Effect bekommt sie
     * per updateSettings. {r,g,b,a, Dicke Anfang/Ende, Schweif r,g,b,a, Schweiflaenge, Geschosslaenge}.
     */
    static float[] settingsFor(Object ammo) {
        if (failed || ammo == null) return null;
        try {
            if (!ready) init();
            Object inst = getInstance.invoke(null);
            if (inst == null) return null;
            if (effectNew == null) {
                effectNew = Class.forName("zombie.iso.objects.IsoBulletTracerEffects$Effect", true, Tracers.class.getClassLoader()).getDeclaredConstructor();
                effectNew.setAccessible(true);
            }
            Object e = effectNew.newInstance();
            updateSettings.invoke(inst, ammo, e);
            return new float[] { g(pR, e), g(pG, e), g(pB, e), g(pA, e), g(pW0, e), g(pW1, e),
                g(tR, e), g(tG, e), g(tB, e), g(tA, e), g(tLen, e), g(length, e) };
        } catch (Throwable t) {
            return null;
        }
    }

    private static float w(float px, float scale) {
        return Math.max(0.006f, Math.min(0.2f, px * PIXEL * scale));
    }

    /** Frischen Knick (juenger als 0.4 s) fuer einen neuen Tracer des eigenen Spielers. */
    private static Bend takeBend(float ox, float oy, float oz, float dx, float dy, float dz) {
        Object q = System.getProperties().get("vfa.fx.bends");
        if (!(q instanceof Queue<?> queue)) return null;
        float now = (float) (System.nanoTime() / 1e6 % 1e7);
        Object o;
        while ((o = queue.poll()) != null) {
            if (!(o instanceof float[] v) || v.length < 8) continue;
            if (Math.abs(now - v[7]) > 400f) continue;
            // Strecke bis zum Knick entlang der Vanilla-Richtung (gleiche Einheiten wie der Effect)
            float at = Math.max(0f, (v[0] - ox) * dx + (v[1] - oy) * dy + (v[2] - oz) * dz);
            float nz = v[5] / LEVEL;                       // Steigung Meter/Feld -> Stockwerke/Feld
            float len = (float) Math.sqrt(v[3] * v[3] + v[4] * v[4] + nz * nz);
            if (len < 1e-6f || v[6] <= 0f) return new Bend(at, dx, dy, dz, 0f);
            return new Bend(at, v[3] / len, v[4] / len, nz / len, v[6] * len);
        }
        return null;
    }

    /** Game-Thread: Tracer fortbewegen und als Streifen (Render-Koordinaten) an out anhaengen. */
    static void step(List<Fx.Item> out, float cx, float cy, float cz) {
        if (failed || !skipVanilla()) return;
        try {
            if (!ready) init();
            Object inst = getInstance.invoke(null);
            if (inst == null) return;
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) effects.get(inst);
            if (list == null || list.isEmpty()) { known.clear(); bends.clear(); return; }
            Object time = gtInstance.invoke(null);
            boolean paused = (Boolean) gtPaused.invoke(time);
            float add = ((Number) gtMultiplier.invoke(time)).floatValue() / 1.6f;
            float scale = Fx.numberPublic("VFA_FX_TracerWidth", 1.1f);
            boolean path = Fx.flagPublic("VFA_FX_TracerPath", true);

            for (int i = list.size() - 1; i >= 0; i--) {
                Object e = list.get(i);
                if (Vtb.isArtifact(e)) {
                    // Hilfs-Tracer von True Ballistics (Kugel wird schon als VTB-Projektil gezeichnet): nur ablaufen lassen
                    playSound.setBoolean(e, false);
                    if (!paused) timer.setFloat(e, g(timer, e) + add);
                    if (g(timer, e) * g(speed, e) / 200f > g(range, e) + g(length, e)) {
                        list.remove(i);
                        known.remove(e);
                        Vtb.forgetArtifact(e);
                        poolRelease.invoke(effectPool.get(inst), e);
                    }
                    continue;
                }
                Object ammo = ammoType.get(e);
                if (ammo != null) updateSettings.invoke(inst, ammo, e);
                Object d = dir.get(e);
                float ox = g(x0, e), oy = g(y0, e), oz = g(z0, e);
                float dx = vx.getFloat(d), dy = vy.getFloat(d), dz = vz.getFloat(d);
                float maxRange = g(range, e), len = g(length, e);
                float dist = g(timer, e) * g(speed, e) / 200f;

                // neuer Tracer? (Effects werden wiederverwendet: Startpunkt vergleichen)
                float[] k = known.get(e);
                if (k == null || k[0] != ox || k[1] != oy || k[2] != oz) {
                    known.put(e, new float[] { ox, oy, oz });
                    bends.remove(e);
                    if (character.get(e) == IsoPlayer.players[0]) {
                        Bend b = takeBend(ox, oy, oz, dx, dy, dz);
                        if (b != null) {
                            bends.put(e, b);
                            playSound.setBoolean(e, false);   // Einschlag kommt von AimSpread (Stamm/Boden)
                        }
                    }
                }
                Bend bend = bends.get(e);
                if (bend != null) maxRange = bend.at() + bend.rest();
                float head = Math.min(dist, maxRange);
                float t = maxRange > 0f ? Math.min(1f, dist / maxRange) : 1f;

                // Geschoss
                float tail = Math.max(0f, head - len);
                poly(out, bend, ox, oy, oz, dx, dy, dz, tail, head, cx, cy, cz,
                    w(g(pW0, e), scale), w(g(pW1, e), scale), g(pR, e), g(pG, e), g(pB, e), g(pA, e));
                // Schweif
                float trailTail = Math.max(0f, head - g(tLen, e));
                float trailA = g(tA, e) * (1f - t);
                if (trailA > 0.004f) poly(out, bend, ox, oy, oz, dx, dy, dz, trailTail, head, cx, cy, cz,
                    w(g(tW0, e), scale), w(g(tW1, e), scale), g(tR, e), g(tG, e), g(tB, e), trailA);
                // Bahn (blasse Linie vom Lauf bis zum Geschoss)
                float pathA = g(hA, e) * (1f - t * g(hFade, e));
                if (path && head > 0.05f && pathA > 0.004f) poly(out, bend, ox, oy, oz, dx, dy, dz, 0f, head, cx, cy, cz,
                    w(g(hW0, e), scale), w(g(hW1, e), scale), g(hR, e), g(hG, e), g(hB, e), pathA);

                arrive(e, ox, oy, oz, dx, dy, dz, dist, ammo);
                if (!paused) timer.setFloat(e, g(timer, e) + add);
                if (dist > maxRange + len) {
                    list.remove(i);
                    known.remove(e);
                    bends.remove(e);
                    poolRelease.invoke(effectPool.get(inst), e);
                }
            }
            if (!logged) {
                logged = true;
                System.out.println("[ViewpointEffects3D] game tracers drawn in 3D");
            }
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ViewpointEffects3D] Tracers disabled: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Wie Effect.render() beim Ankommen am Ende: einmalig CombatManager.hitIsoGridSquare (Spiellogik, z. B.
     * abgelegte Gegenstaende), sonst Einschlag-Sound des Spiels; dazu die 3D-Optik nach Material und Munition.
     */
    private static void arrive(Object e, float ox, float oy, float oz, float dx, float dy, float dz, float dist,
                               Object ammo) throws Exception {
        Object sq = square.get(e);
        if (sq == null || !playSound.getBoolean(e)) return;
        Object s0 = start.get(e), s1 = end.get(e), cur = current.get(e);
        float cx = ox + dx * dist, cy = oy + dy * dist, cz = oz + dz * dist;
        vx.setFloat(cur, cx); vy.setFloat(cur, cy); vz.setFloat(cur, cz);
        float sx = vx.getFloat(s0), sy = vy.getFloat(s0), sz = vz.getFloat(s0);
        float ex = vx.getFloat(s1), ey = vy.getFloat(s1), ez = vz.getFloat(s1);
        float toCur = (cx - sx) * (cx - sx) + (cy - sy) * (cy - sy) + (cz - sz) * (cz - sz);
        float toEnd = (ex - sx) * (ex - sx) + (ey - sy) * (ey - sy) + (ez - sz) * (ez - sz);
        if (toCur <= toEnd) return;                          // noch unterwegs
        playSound.setBoolean(e, false);
        if ((Boolean) hitSquare.invoke(combat, sq, cur, e)) return;
        Fx.impactSound(sq, ammo);
        if (Vehicles.tracerEnd(character.get(e), sx, sy, sz, ex, ey, ez, Fx.ammoKey(ammo))) return;   // Fahrzeug
        if (Fx.flagPublic("VFA_FX_Impacts", true))
            Impacts.hit(ex, ey, ez, dx, dy, dz * LEVEL, Fx.materialOf(sq), Fx.ammoKey(ammo));
    }

    /** Streifen von Strecke 'from' bis 'to' entlang der (ggf. geknickten) Bahn. */
    private static void poly(List<Fx.Item> out, Bend b, float ox, float oy, float oz, float dx, float dy, float dz,
                             float from, float to, float cx, float cy, float cz, float w0, float w1,
                             float r, float g, float bl, float a) {
        if (to <= from) return;
        if (b == null || to <= b.at()) {
            out.add(streak(ox, oy, oz, dx, dy, dz, from, to, cx, cy, cz, w0, w1, r, g, bl, a));
            return;
        }
        float at = b.at();
        float kx = ox + dx * at, ky = oy + dy * at, kz = oz + dz * at;
        float split = Math.max(0f, Math.min(1f, (at - from) / (to - from)));
        float wk = w0 + (w1 - w0) * split;
        if (from < at) out.add(streak(ox, oy, oz, dx, dy, dz, from, at, cx, cy, cz, w0, wk, r, g, bl, a));
        float s0 = Math.max(0f, from - at), s1 = to - at;
        if (s1 > s0) out.add(streak(kx, ky, kz, b.nx(), b.ny(), b.nz(), s0, s1, cx, cy, cz, from < at ? wk : w0, w1, r, g, bl, a));
    }

    private static Fx.Item streak(float ox, float oy, float oz, float dx, float dy, float dz, float from, float to,
                                  float cx, float cy, float cz, float w0, float w1, float r, float g, float b, float a) {
        float ax = ox + dx * from, ay = oy + dy * from, az = oz + dz * from;
        float bx = ox + dx * to, by = oy + dy * to, bz = oz + dz * to;
        return new Fx.Item(3, -(ax - cx), (az - cz) * LEVEL, -(ay - cy), -(bx - cx), (bz - cz) * LEVEL, -(by - cy),
            w0, w1, r, g, b, Math.min(1f, a), 0f);
    }

    private Tracers() {}
}
