package vpspread;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.sprite.IsoReticle;

/**
 * Streuung im Zielkreis des Spiels fuer Viewpoint True Ballistics (VTB).
 *
 * Vanilla: Die Klammern "( o )" zeigen mit IsoReticle.currentCrosshairOffset (Pixel), wie unruhig gezielt wird -
 * der Wert waechst mit Bewegung/Zielstrafe/Moodles und schrumpft mit Aiming-Skill, ruhigem Zielen und Fokus, auch
 * mit allem, was andere Mods daran aendern. VTB rechnet sonst eine eigene Gauss-Streuung (AimSolution.sigma).
 *
 * Hier, nach vtb.aiming.AimProvider.solve: VTBs sigma wird 0 gesetzt und die Schussrichtung (AimSolution.direction)
 * um einen gleichverteilten Punkt im Kreis gedreht - Kreisradius (Pixel, mal VFA_SpreadScale) ueber Viewpoints
 * Projektion in einen Winkel umgerechnet. Die Kugel fliegt danach ganz normal mit VTB (Fall, Waende, Trefferzone).
 * Nur bei Sandbox "Firearms Use Damage Chance" = 1 (Disabled); sonst trifft der Schuss genau die Bildmitte.
 *
 * Optionen aus Lua-Globalen (VFA_ViewpointAimSpread.lua): VFA_SpreadOn, VFA_SpreadScale.
 */
public final class Spread {
    private static final int LOG_SHOTS = 8;
    /** In 3D etwas weiter als die Klammern; per Option einstellbar. */
    static final float DEFAULT_SCALE = 1.5f;

    private static volatile boolean failed;
    private static boolean ready;
    private static Field viewEnabled, fpFrames, fNumber, fScene, sProjection, reticleOffset, env;
    private static Method rawget, m11;
    private static Field solSigma, solDirection;
    private static Method deflect;
    private static int logged;

    static Class<?> find(String name) throws ClassNotFoundException {
        ClassLoader[] loaders = { Spread.class.getClassLoader(), Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader() };
        ClassNotFoundException last = null;
        for (ClassLoader l : loaders) {
            if (l == null) continue;
            try { return Class.forName(name, true, l); } catch (ClassNotFoundException e) { last = e; }
        }
        throw last;
    }

    static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }

    private static synchronized void init() throws Exception {
        if (ready) return;
        viewEnabled = field(find("viewpoint.core.View"), "enabled");
        Class<?> frame = find("viewpoint.core.Frame");
        fpFrames = field(find("viewpoint.FP"), "frames");
        fNumber = field(frame, "number");
        fScene = field(frame, "scene");
        sProjection = field(find("viewpoint.render.SceneData"), "projection");
        m11 = find("org.joml.Matrix4f").getMethod("m11");
        reticleOffset = field(IsoReticle.class, "currentCrosshairOffset");
        env = find("zombie.Lua.LuaManager").getField("env");
        rawget = find("se.krka.kahlua.vm.KahluaTable").getMethod("rawget", Object.class);
        ready = true;
    }

    public static void validate() {
        try {
            init();
            System.out.println("[ViewpointAimSpread] 2.0 ready: True Ballistics add-on (spread inside the game's aiming circle, bushes deflect)");
        } catch (Throwable t) {
            disable("init", t);
        }
    }

    static void disable(String where, Throwable t) {
        if (!failed) {
            System.err.println("[ViewpointAimSpread] Disabled (" + where + "): " + t);
            t.printStackTrace();
        }
        failed = true;
    }

    static Object global(String name) {
        try {
            if (!ready) init();
            Object g = env.get(null);
            return g == null ? null : rawget.invoke(g, name);
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean flag(String name, boolean dflt) {
        Object v = global(name);
        return v instanceof Boolean b ? b : dflt;
    }

    static float number(String name, float dflt) {
        Object v = global(name);
        return v instanceof Number n ? n.floatValue() : dflt;
    }

    private static Field sbInstance, sbDamageChance;
    private static Method sbGetValue;

    /** Sandbox "Firearms Use Damage Chance": 1 = Disabled, 2 = nur Zombies, 3 = alle Ziele. */
    static int damageChanceMode() {
        try {
            if (sbInstance == null) {
                Class<?> sb = find("zombie.SandboxOptions");
                sbInstance = field(sb, "instance");
                sbDamageChance = field(sb, "firearmUseDamageChance");
            }
            Object opt = sbDamageChance.get(sbInstance.get(null));
            if (sbGetValue == null) sbGetValue = opt.getClass().getMethod("getValue");
            return ((Number) sbGetValue.invoke(opt)).intValue();
        } catch (Throwable t) {
            return 1;
        }
    }

    /** 1/tan(fovY/2) aus Viewpoints Projektion des neuesten Frames. */
    private static float focal() throws Exception {
        Object[] frames = (Object[]) fpFrames.get(null);
        Object best = null;
        long bestNo = Long.MIN_VALUE;
        for (Object f : frames) {
            if (f == null) continue;
            long n = fNumber.getLong(f);
            if (n > bestNo) { bestNo = n; best = f; }
        }
        if (best == null) return 0f;
        Object proj = sProjection.get(fScene.get(best));
        return ((Number) m11.invoke(proj)).floatValue();
    }

    private static float lastPx;

    /** Winkelradius (Bogenmass) des Zielkreises des Spiels. */
    private static float radiusAngle() throws Exception {
        IsoReticle reticle = IsoReticle.getInstance(0);
        if (reticle == null) return 0f;
        float px = reticleOffset.getFloat(reticle) * number("VFA_SpreadScale", DEFAULT_SCALE);
        float f = focal();
        int h = Core.getInstance().getScreenHeight();
        lastPx = px;
        if (px <= 0f || f <= 0f || h <= 0) return 0f;
        return (float) Math.atan(px / (h * 0.5f) / f);
    }

    /** Nach VTBs AimProvider.solve (nur eigene Schuesse von Spieler 1). */
    public static void afterSolve(Object player, Object solution, boolean ok) {
        if (failed || !ok || solution == null || player == null || player != IsoPlayer.players[0]) return;
        try {
            if (!flag("VFA_SpreadOn", true)) return;            // aus: VTBs eigene Streuung bleibt
            if (!ready) init();
            if (solSigma == null) {
                solSigma = field(solution.getClass(), "sigma");
                solDirection = field(solution.getClass(), "direction");
                Object v = solDirection.get(solution);
                deflect = find("vtb.projectile.ProjectilePhysics").getMethod("deflect", v.getClass(), double.class, double.class);
            }
            solSigma.setDouble(solution, 0.0);
            int mode = damageChanceMode();
            if (mode != 1) {
                if (logged < 1) {
                    logged = LOG_SHOTS;
                    System.out.println("[ViewpointAimSpread] sandbox FirearmUseDamageChance = " + mode + ": shots go exactly to the reticle (spread only at 1 = Disabled)");
                }
                return;
            }
            float angle = radiusAngle();
            if (angle <= 0f) return;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            double radius = Math.sqrt(r.nextDouble()) * angle;   // gleichverteilt ueber die Kreisflaeche
            double phi = r.nextDouble() * Math.PI * 2.0;
            double right = radius * Math.cos(phi), up = radius * Math.sin(phi);
            deflect.invoke(null, solDirection.get(solution), right, up);
            if (logged < LOG_SHOTS) {
                logged++;
                System.out.printf("[ViewpointAimSpread] shot %d: circle %.0f px = %.2f deg, off %.2f deg%n",
                    logged, lastPx, Math.toDegrees(angle), Math.toDegrees(radius));
            }
        } catch (Throwable t) {
            disable("spread", t);
        }
    }

    private Spread() {}
}
