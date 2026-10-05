package vpfx;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.physics.BallisticsController;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector3;

import static org.lwjgl.opengl.GL33.*;

/**
 * 3D-Effekte fuer Project Viewpoint: Muendungsfeuer (Leuchten + kurzes Licht in der Szene), Pulverrauch am echten
 * Lauf, Funken/Staub am Einschlag, Gluehwuermchen von JB's Fireflies. Gezeichnet im Weltpass von Viewpoint mit
 * Tiefentest, also von Waenden verdeckt (gleiches Schema wie ViewpointFire / 3D Tracer Fix).
 *
 * Threads: onShot/onImpact/capture laufen auf dem Game-Thread, draw auf dem Render-Thread; capture legt pro Szene
 * eine unveraenderliche Liste ab, draw liest nur diese.
 * Koordinaten: Spielwelt x/y in Feldern, z in Stockwerken; Viewpoint rendert (-(x-camX), (z-camZ)*LEVEL, -(y-camY)).
 * Optionen und Gluehwuermchen kommen aus Lua-Globalen (VFA_ViewpointEffects3D.lua).
 */
public final class Fx {
    static final float LEVEL = 2.4494896f;
    private static final float HAND_HEIGHT = 1.1f;      // Torches.write addiert das fuer ids < 4096
    private static final int LIGHT_ID = 4001;
    private static final int MAX_PARTICLES = 400;
    private static final int MAX_TORCHES = 8;

    private static volatile boolean failed;
    private static boolean ready, seen, drawn;
    private static int program, vao;
    private static int logImpacts;

    // Reflection
    private static Field env, viewEnabled, fCamX, fCamY, fCamZ, fScene, fEyeX, fEyeZ, torchCount;
    private static Field rFrame, rIris, cViewProj, cView, cEyeX, cEyeY, cEyeZ;
    private static Field tId, tX, tY, tZ, tR, tG, tB, tAngleX, tAngleY, tDist, tStrength, tCone, tDot;
    private static Method rawgetKey, rawgetIndex, rawset, len, wipe, torchWrite, bindTarget;
    private static Method vpGet, m00, m10, m20, m01, m11, m21;
    private static Constructor<?> torchNew;
    private static Object torch;

    /** Partikel in Spielwelt-Koordinaten (Felder/Stockwerke). kind: 0 Blitz, 1 Rauch, 2 Funke, 3 Staub. */
    /** kind: 0 Blitz, 1 Rauch, 2 Einschlag-Leuchten, 3 Staub/Splitter (Farbe r/g/b), 4 Querschlaeger, 5 Funke. */
    private static final class P {
        int kind;
        double x, y, z;
        float vx, vy, vz;
        float age, life, size0, size1, alpha, seed;
        float r = 0.62f, g = 0.62f, b = 0.55f;
        boolean fp;
    }

    /** Einschussloch / Schramme: liegt auf der Oberflaeche (Normale nx/ny/nz, Welt mit z nach oben). */
    private static final class H {
        double x, y, z;
        float nx, ny, nz, size, r, g, b, a, age, life, seed;
        Object attached;     // Tuer/Fenster, auf dem das Loch sitzt (oder null)
        String state;        // deren Zustand beim Einschlag
    }
    private static final List<H> holes = new ArrayList<>();
    private static final List<H> pendingHoles = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HOLES = 300;

    /** mode 0 Blitz/Funke, 1 Rauch, 2 Gluehwuermchen (Punkt bei x/y/z), 3 Streifen von x/y/z nach bx/by/bz (Tracer). */
    record Item(int mode, float x, float y, float z, float bx, float by, float bz, float size, float size2,
                float r, float g, float b, float a, float seed) {
        static Item point(int mode, float x, float y, float z, float size, float r, float g, float b, float a, float seed) {
            return new Item(mode, x, y, z, x, y, z, size, size, r, g, b, a, seed);
        }
    }

    private static final List<P> particles = new ArrayList<>();
    private static final List<P> pending = Collections.synchronizedList(new ArrayList<>());
    private static final Map<Object, List<Item>> snapshots = Collections.synchronizedMap(new WeakHashMap<>());

    private static long lastNanos, frames;
    private static double lightX, lightY, lightZ;
    private static float lightLeft;          // Sekunden
    private static final float LIGHT_TIME = 0.09f;

    // ---------------------------------------------------------------- Setup

    private static Class<?> find(String name) throws ClassNotFoundException {
        ClassLoader[] loaders = { Fx.class.getClassLoader(), Thread.currentThread().getContextClassLoader(),
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

    private static synchronized void init() throws Exception {
        if (ready) return;
        Class<?> table = find("se.krka.kahlua.vm.KahluaTable");
        env = find("zombie.Lua.LuaManager").getField("env");
        rawgetKey = table.getMethod("rawget", Object.class);
        rawgetIndex = table.getMethod("rawget", int.class);
        rawset = table.getMethod("rawset", Object.class, Object.class);
        len = table.getMethod("len");
        wipe = table.getMethod("wipe");

        viewEnabled = field(find("viewpoint.core.View"), "enabled");
        Class<?> frame = find("viewpoint.core.Frame");
        fCamX = field(frame, "camX"); fCamY = field(frame, "camY"); fCamZ = field(frame, "camZ");
        fScene = field(frame, "scene");
        fEyeX = field(frame, "eyeX"); fEyeZ = field(frame, "eyeZ");
        torchCount = field(find("viewpoint.render.SceneData"), "torchCount");

        Class<?> renderer = find("viewpoint.render.WorldRenderer");
        rFrame = field(renderer, "frame");
        rIris = field(renderer, "irisDrawn");
        bindTarget = renderer.getDeclaredMethod("bindTarget");
        bindTarget.setAccessible(true);
        Class<?> context = find("viewpoint.render.FrameContext");
        cViewProj = field(context, "viewProjection");
        cView = field(context, "view");
        cEyeX = field(context, "eyeX"); cEyeY = field(context, "eyeY"); cEyeZ = field(context, "eyeZ");
        Class<?> mat = find("org.joml.Matrix4f");
        vpGet = mat.getMethod("get", float[].class);
        m00 = mat.getMethod("m00"); m10 = mat.getMethod("m10"); m20 = mat.getMethod("m20");
        m01 = mat.getMethod("m01"); m11 = mat.getMethod("m11"); m21 = mat.getMethod("m21");

        Class<?> torches = find("viewpoint.light.Torches");
        Class<?> ti = find("zombie.characters.IsoGameCharacter$TorchInfo");
        torchWrite = torches.getDeclaredMethod("write", find("viewpoint.render.SceneData"), ti, frame);
        torchWrite.setAccessible(true);
        torchNew = ti.getConstructor();
        tId = field(ti, "id"); tX = field(ti, "x"); tY = field(ti, "y"); tZ = field(ti, "z");
        tR = field(ti, "r"); tG = field(ti, "g"); tB = field(ti, "b");
        tAngleX = field(ti, "angleX"); tAngleY = field(ti, "angleY");
        tDist = field(ti, "dist"); tStrength = field(ti, "strength"); tCone = field(ti, "cone"); tDot = field(ti, "dot");
        ready = true;
    }

    public static void validate() {
        try {
            init();
            System.out.println("[ViewpointEffects3D] 1.0 ready: 3D muzzle flash, light, smoke, impacts, fireflies");
        } catch (Throwable t) {
            disable("init", t);
        }
    }

    private static void disable(String where, Throwable t) {
        if (!failed) {
            System.err.println("[ViewpointEffects3D] Disabled (" + where + "): " + t);
            t.printStackTrace();
        }
        failed = true;
        particles.clear();
        pending.clear();
        snapshots.clear();
    }

    // ---------------------------------------------------------------- Lua-Optionen

    private static Object global(String name) {
        try {
            Object g = env.get(null);
            return g == null ? null : rawgetKey.invoke(g, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean flag(String name, boolean dflt) {
        Object v = global(name);
        return v instanceof Boolean b ? b : dflt;
    }

    private static float number(String name, float dflt) {
        Object v = global(name);
        return v instanceof Number n ? n.floatValue() : dflt;
    }

    private static boolean viewpointOn() {
        try {
            if (!ready) init();
            return viewEnabled.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean flagPublic(String name, boolean dflt) { return flag(name, dflt); }
    static float numberPublic(String name, float dflt) { return number(name, dflt); }
    static boolean viewpointOnPublic() { return viewpointOn(); }

    // ---------------------------------------------------------------- Ereignisse (Game-Thread)

    private static float rnd(float a, float b) {
        return a + ThreadLocalRandom.current().nextFloat() * (b - a);
    }

    private static boolean gunpowder(Object weapon) {
        if (!(weapon instanceof HandWeapon w) || !w.isRanged()) return false;
        String t = w.getFullType();
        if (t == null) return true;
        String l = t.toLowerCase();
        return !(l.contains("bow") || l.contains("sling") || l.contains("nailgun"));
    }

    private static Field handModel, modelScript, animPlayer;
    private static Method attachmentById, attachmentWorldPos;
    private static Object muzzleId;
    private static boolean attachmentBroken;

    /**
     * Laufspitze ueber den "muzzle"-Anknuepfpunkt des gehaltenen Waffenmodells (ModelInstance.getAttachmentWorldPosition
     * auf primaryHandModel - das Modell, das Viewpoint zeichnet, inkl. Armhaltung/ADS). Liegt bei jeder Waffe mit
     * eigenem Modell an der echten Laufmuendung, auch bei Mod-Waffen. Wie VTBs MuzzleResolver.
     */
    private static boolean muzzleAttachment(IsoGameCharacter chr, Vector3 pos, Vector3 dir) {
        if (attachmentBroken) return false;
        try {
            if (handModel == null) {
                handModel = find("zombie.characters.IsoGameCharacter").getField("primaryHandModel");
                Class<?> mi = find("zombie.core.skinnedmodel.model.ModelInstance");
                modelScript = mi.getField("modelScript");
                animPlayer = mi.getField("animPlayer");
                Class<?> ms = find("zombie.scripting.objects.ModelScript");
                Class<?> idc = find("zombie.scripting.objects.ModelAttachmentId");
                muzzleId = idc.getField("MUZZLE").get(null);
                attachmentById = ms.getMethod("getAttachmentById", idc);
                attachmentWorldPos = mi.getMethod("getAttachmentWorldPosition", find("zombie.scripting.objects.ModelAttachment"),
                    Vector3.class, Vector3.class);
            }
            Object mi = handModel.get(chr);
            if (mi == null || animPlayer.get(mi) == null) return false;
            Object script = modelScript.get(mi);
            if (script == null) return false;
            Object att = attachmentById.invoke(script, muzzleId);
            if (att == null) return false;
            attachmentWorldPos.invoke(mi, att, pos, dir);
            double dx = pos.x - chr.getX(), dy = pos.y - chr.getY(), du = (pos.z - chr.getZ()) * LEVEL;
            return Float.isFinite(pos.x) && Float.isFinite(pos.y) && Float.isFinite(pos.z)
                && dx * dx + dy * dy < 1.6 * 1.6 && du > 0.2 && du < 2.3;
        } catch (Throwable t) {
            attachmentBroken = true;
            System.err.println("[ViewpointEffects3D] muzzle attachment not readable, using the ballistics muzzle: " + t);
            return false;
        }
    }

    /** Laufposition und -richtung (Richtung normiert, z in Metern) eines Schuetzen; false wenn unbrauchbar. */
    private static boolean muzzle(IsoGameCharacter chr, double[] out) {
        Vector3 am = new Vector3(), ad = new Vector3();
        if (muzzleAttachment(chr, am, ad)) {
            double dx = ad.x, dy = ad.y, dz = ad.z * LEVEL;
            double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (l < 1e-6) {
                // Richtung fehlt: Blickrichtung des Charakters
                double a = Math.toRadians(chr.getAnimAngle());
                dx = Math.cos(a); dy = Math.sin(a); dz = 0; l = 1;
            }
            out[0] = am.x; out[1] = am.y; out[2] = am.z;
            out[3] = dx / l; out[4] = dy / l; out[5] = dz / l;
            return true;
        }
        BallisticsController bc = chr.getBallisticsController();
        if (bc == null) return false;
        Vector3 m = new Vector3(), d = new Vector3();
        try {
            bc.calculateMuzzlePosition(m, d);
        } catch (Throwable t) {
            Vector3 mm = bc.getMuzzlePosition(), dd = bc.getMuzzleDirection();
            if (mm == null || dd == null) return false;
            m = mm; d = dd;
        }
        if (!Float.isFinite(m.x) || !Float.isFinite(m.y) || !Float.isFinite(m.z)) return false;
        if (Math.hypot(m.x - chr.getX(), m.y - chr.getY()) > 2.5) return false;   // nicht initialisiert
        double dx = d.x, dy = d.y, dz = d.z * LEVEL;
        double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6) return false;
        out[0] = m.x; out[1] = m.y; out[2] = m.z;
        out[3] = dx / l; out[4] = dy / l; out[5] = dz / l;
        return true;
    }

    /** Jeder Schuss eines beliebigen Charakters (CombatManager.attackCollisionCheck): Pulverrauch. */
    public static void onAttack(Object character, Object weapon) {
        if (failed || !(character instanceof IsoGameCharacter chr)) return;
        try {
            if (!viewpointOn() || !flag("VFA_FX_Smoke", true)) return;
            if (!(weapon instanceof HandWeapon w) || !w.isAimedFirearm() || !gunpowder(weapon)) return;
            double[] mu = new double[6];
            if (!muzzle(chr, mu)) return;
            double dx = mu[3], dy = mu[4], dz = mu[5];
            boolean fp = chr == IsoPlayer.players[0];
            int count = Math.round(6 * Math.max(0f, number("VFA_FX_SmokeAmount", 0.5f)));
            for (int i = 0; i < count; i++) {
                P s = new P();
                s.kind = 1;
                float along = rnd(0f, 0.25f);
                s.x = mu[0] + dx * along; s.y = mu[1] + dy * along; s.z = mu[2] + dz * along / LEVEL;
                float push = rnd(0.6f, 3.2f) * (1f - i / (float) Math.max(1, count) * 0.5f);
                s.vx = (float) (dx * push) + rnd(-0.25f, 0.25f);
                s.vy = (float) (dy * push) + rnd(-0.25f, 0.25f);
                s.vz = (float) (dz * push) + rnd(-0.05f, 0.15f);   // m/s, z hier in Metern
                s.life = rnd(0.9f, 1.6f);
                s.size0 = rnd(0.10f, 0.16f);
                s.size1 = rnd(0.55f, 0.85f);
                s.alpha = rnd(0.14f, 0.24f);
                s.seed = rnd(0f, 100f);
                s.fp = fp;
                pending.add(s);
            }
        } catch (Throwable t) {
            disable("attack", t);
        }
    }

    private static Method emEffect;
    private static Field effCurrentTime;

    /**
     * Nach EffectsManager.startMuzzleFlash: vanilla wuerfelt dort (je nach Waffe), ob es blitzt. Nur wenn der
     * Blitz gerade (neu) gestartet wurde - currentTime == 0 - gibt es den 3D-Blitz und das Licht.
     */
    public static void onMuzzleFlash(Object manager, Object character) {
        if (failed || manager == null || !(character instanceof IsoGameCharacter chr)) return;
        try {
            if (!viewpointOn()) return;
            if (emEffect == null) {
                emEffect = manager.getClass().getDeclaredMethod("getModelInstanceEffect", find("zombie.characters.IsoGameCharacter"));
                emEffect.setAccessible(true);
            }
            Object eff = emEffect.invoke(manager, chr);
            if (eff == null) return;
            if (effCurrentTime == null) effCurrentTime = field(eff.getClass(), "currentTime");
            if (effCurrentTime.getFloat(eff) != 0f) return;

            double[] mu = new double[6];
            if (!muzzle(chr, mu)) return;
            double dx = mu[3], dy = mu[4], dz = mu[5];
            if (flag("VFA_FX_Flash", true)) {
                P f = new P();
                f.kind = 0;
                f.x = mu[0] + dx * 0.04; f.y = mu[1] + dy * 0.04; f.z = mu[2] + dz * 0.04 / LEVEL;
                f.life = 0.065f;
                f.size0 = f.size1 = 0.5f * number("VFA_FX_FlashSize", 1.25f);
                f.alpha = 1f;
                pending.add(f);
            }
            if (flag("VFA_FX_Light", true)) {
                lightX = mu[0] + dx * 0.2; lightY = mu[1] + dy * 0.2; lightZ = mu[2];
                lightLeft = LIGHT_TIME;
            }
        } catch (Throwable t) {
            disable("muzzle flash", t);
        }
    }

    // ---------------------------------------------------------------- Bausteine fuer Impacts

    static void ricochet(double x, double y, double z, float dx, float dy, float dz) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6f) return;
        P q = new P();
        q.kind = 4;
        q.x = x; q.y = y; q.z = z;
        q.vx = dx / l; q.vy = dy / l; q.vz = dz / l;      // Richtung (Meter)
        q.life = rnd(0.10f, 0.16f);
        q.size0 = 0.012f;
        q.alpha = 1f;
        pending.add(q);
    }

    static void sparks(double x, double y, double z, float nx, float ny, float nz, int count, float scale) {
        if (count <= 0) return;
        P f = new P();
        f.kind = 2;
        f.x = x; f.y = y; f.z = z;
        f.life = 0.06f;
        f.size0 = f.size1 = 0.16f * scale;
        f.alpha = 1f;
        pending.add(f);
        for (int i = 0; i < count; i++) {
            P q = new P();
            q.kind = 5;
            q.x = x; q.y = y; q.z = z;
            float sp = rnd(2.5f, 7f) * Math.min(1.6f, scale);
            q.vx = nx * sp * 0.8f + rnd(-1f, 1f) * sp * 0.6f;
            q.vy = ny * sp * 0.8f + rnd(-1f, 1f) * sp * 0.6f;
            q.vz = nz * sp * 0.8f + rnd(-0.3f, 1f) * sp * 0.6f;
            q.life = rnd(0.12f, 0.35f);
            q.size0 = rnd(0.018f, 0.03f) * Math.min(1.5f, scale);
            q.alpha = 1f;
            pending.add(q);
        }
    }

    static void dust(double x, double y, double z, float nx, float ny, float nz, int count, float r, float g, float b, float scale) {
        for (int i = 0; i < count; i++) {
            P d = new P();
            d.kind = 3;
            d.x = x; d.y = y; d.z = z;
            float sp = rnd(0.4f, 1.6f);
            d.vx = nx * sp + rnd(-0.5f, 0.5f); d.vy = ny * sp + rnd(-0.5f, 0.5f); d.vz = nz * sp + rnd(0.05f, 0.6f);
            d.life = rnd(0.35f, 0.8f);
            d.size0 = rnd(0.03f, 0.06f) * scale;
            d.size1 = rnd(0.16f, 0.30f) * scale;
            d.alpha = rnd(0.35f, 0.55f);
            d.seed = rnd(0f, 100f);
            d.r = r; d.g = g; d.b = b;
            pending.add(d);
        }
    }

    static void holeSmoke(double x, double y, double z, float nx, float ny, float nz) {
        for (int i = 0; i < 2; i++) {
            P s = new P();
            s.kind = 1;
            s.x = x; s.y = y; s.z = z;
            s.vx = nx * 0.25f + rnd(-0.08f, 0.08f); s.vy = ny * 0.25f + rnd(-0.08f, 0.08f); s.vz = nz * 0.25f + rnd(0.05f, 0.2f);
            s.life = rnd(0.9f, 1.5f);
            s.size0 = 0.04f;
            s.size1 = rnd(0.18f, 0.28f);
            s.alpha = rnd(0.18f, 0.28f);
            s.seed = rnd(0f, 100f);
            pending.add(s);
        }
    }

    static void hole(double x, double y, double z, float nx, float ny, float nz, float size,
                     float r, float g, float b, float a, boolean surface) {
        if (!flag("VFA_FX_Holes", true)) return;
        H h = new H();
        h.x = x; h.y = y; h.z = z;
        h.nx = nx; h.ny = ny; h.nz = nz;
        h.size = size; h.r = r; h.g = g; h.b = b; h.a = a;
        h.life = (surface ? 25f : 120f) * Math.max(0.1f, number("VFA_FX_HoleTime", 5f));
        h.seed = rnd(0f, 100f);
        if (!surface) {
            h.attached = Attach.find(x, y, z);
            h.state = Attach.state(h.attached);
        }
        pendingHoles.add(h);
    }

    private static Method ammoKeyOf, soundPlay, squareProps, propsGet;

    /** Material des Feldes wie beim Einschlag-Sound: Tile-Eigenschaft "MaterialType". */
    static String materialOf(Object square) {
        if (square == null) return null;
        try {
            if (squareProps == null) squareProps = square.getClass().getMethod("getProperties");
            Object props = squareProps.invoke(square);
            if (props == null) return null;
            if (propsGet == null) propsGet = props.getClass().getMethod("get", String.class);
            return (String) propsGet.invoke(props, "MaterialType");
        } catch (Throwable t) {
            return null;
        }
    }

    static String ammoKey(Object ammoType) {
        if (ammoType == null) return null;
        try {
            if (ammoKeyOf == null) ammoKeyOf = ammoType.getClass().getMethod("getItemKey");
            Object k = ammoKeyOf.invoke(ammoType);
            return k == null ? ammoType.toString() : k.toString();
        } catch (Throwable t) {
            return ammoType.toString();
        }
    }

    /** Munitionstyp der Waffe in der Hand (InventoryItem.getAmmoType). */
    static Object weaponAmmo(IsoGameCharacter chr) {
        try {
            Object item = chr == null ? null : chr.getPrimaryHandItem();
            return item == null ? null : item.getClass().getMethod("getAmmoType").invoke(item);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Vanilla-Einschlag-Sound (SoundManager.instance.playImpactSound(square, ammoType)). */
    static void impactSound(Object square, Object ammoType) {
        if (square == null || ammoType == null) return;
        try {
            Object sm = find("zombie.SoundManager").getField("instance").get(null);
            if (sm == null) return;
            if (soundPlay == null)
                soundPlay = sm.getClass().getMethod("playImpactSound", find("zombie.iso.IsoGridSquare"), ammoType.getClass());
            soundPlay.invoke(sm, square, ammoType);
        } catch (Throwable t) {
            // ohne Sound weiter
        }
    }

    /**
     * Einschlaege von ViewpointAimSpread (Baumstamm, Boden nach Ablenkung), anderer Classloader -> Queue in den
     * System-Properties: {x, y, z, Art (0 = Material des Feldes, 1 = Baumstamm), dx, dy, dz in Metern}.
     */
    private static void drainSharedImpacts() {
        Object q = System.getProperties().get("vfa.fx.impacts");
        if (!(q instanceof java.util.Queue<?> queue)) return;
        IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.getCell();
        IsoPlayer p0 = IsoPlayer.players[0];
        Object ammo = weaponAmmo(p0);
        Object o;
        while ((o = queue.poll()) != null) {
            if (!(o instanceof float[] v) || v.length < 3 || !flag("VFA_FX_Impacts", true)) continue;
            IsoGridSquare sq = cell == null ? null : cell.getGridSquare((int) Math.floor(v[0]), (int) Math.floor(v[1]), (int) Math.floor(v[2]));
            boolean tree = v.length >= 4 && v[3] == 1f;
            String mat = tree ? "Wood_Solid" : materialOf(sq);
            if (!tree && mat == null) mat = "Dirt";
            float dx = v.length >= 7 ? v[4] : 1f, dy = v.length >= 7 ? v[5] : 0f, dz = v.length >= 7 ? v[6] : 0f;
            Impacts.hit(v[0], v[1], v[2], dx, dy, dz, mat, ammoKey(ammo));
            impactSound(sq, ammo);
        }
    }

    public static void onImpact(Object character, float x, float y, float z, Object square) {
        if (failed) return;
        try {
            if (!viewpointOn() || !flag("VFA_FX_Impacts", true)) return;
            if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) return;
            IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.getCell();
            IsoGridSquare sq = cell == null ? null : cell.getGridSquare((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (!Vtb.attackActive() && !Tracers.skipVanilla() && character instanceof IsoGameCharacter shooter
                && Vehicles.tracerEnd(character, shooter.getX(), shooter.getY(), shooter.getZ() + 1.3f / LEVEL, x, y, z,
                    ammoKey(weaponAmmo(shooter)))) return;                     // Fahrzeug: Material vom Fahrzeug
            if (sq != null && sq.getMovingObjects() != null && !sq.getMovingObjects().isEmpty()) return;  // Treffer am Ziel: Blut statt Staub
            if (logImpacts < 5 && character instanceof IsoGameCharacter chr) {
                logImpacts++;
                System.out.printf("[ViewpointEffects3D] impact %.2f %.2f %.2f (shooter %.2f %.2f %.2f)%n",
                    x, y, z, chr.getX(), chr.getY(), chr.getZ());
            }
            if (Vtb.attackActive()) return;              // True Ballistics: Einschlag kommt von der echten Kugel
            if (Tracers.skipVanilla()) return;           // Tracers.step macht den Einschlag beim Ankommen
            float dx = 1f, dy = 0f, dz = 0f;
            if (character instanceof IsoGameCharacter chr) { dx = x - chr.getX(); dy = y - chr.getY(); }
            Impacts.hit(x, y, z, dx, dy, dz, materialOf(sq), ammoKey(weaponAmmo(character instanceof IsoGameCharacter c ? c : null)));
        } catch (Throwable t) {
            disable("impact", t);
        }
    }

    // ---------------------------------------------------------------- Frame aufbauen (Game-Thread)

    static float light(IsoCell cell, double x, double y, double z) { return lightAt(cell, x, y, z); }

    private static float lightAt(IsoCell cell, double x, double y, double z) {
        if (cell == null) return 0.5f;
        IsoGridSquare sq = cell.getGridSquare((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (sq == null) return 0.4f;
        return Math.max(0f, Math.min(1f, sq.getLightLevel(0)));
    }

    private static void injectLight(Object frame, Object scene, float k) throws Exception {
        int n = torchCount.getInt(scene);
        if (n >= MAX_TORCHES) return;
        if (torch == null) torch = torchNew.newInstance();
        tId.setInt(torch, LIGHT_ID);
        tX.setFloat(torch, (float) lightX);
        tY.setFloat(torch, (float) lightY);
        tZ.setFloat(torch, (float) (lightZ - HAND_HEIGHT / LEVEL));
        tR.setFloat(torch, 1.0f); tG.setFloat(torch, 0.72f); tB.setFloat(torch, 0.38f);
        tAngleX.setFloat(torch, 0f); tAngleY.setFloat(torch, 0f);
        tCone.setBoolean(torch, false);
        tDot.setFloat(torch, -1f);
        tDist.setFloat(torch, 9f);
        tStrength.setFloat(torch, 2.4f * k * number("VFA_FX_LightStrength", 1.1f));
        torchWrite.invoke(null, scene, torch, frame);
    }

    public static void capture(Object frame) {
        if (failed || frame == null) return;
        try {
            if (!ready) init();
            Object scene = fScene.get(frame);
            boolean on = viewEnabled.getBoolean(null);
            Object g = env.get(null);
            if (g != null) {
                rawset.invoke(g, "VFA_FX_VpOn", on);
                rawset.invoke(g, "VFA_FX_Frame", (double) (++frames));
            }

            long now = System.nanoTime();
            float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1e9f);
            lastNanos = now;
            if (!on) {
                particles.clear();
                pending.clear();
                pendingHoles.clear();
                lightLeft = 0f;
                snapshots.put(scene, List.of());
                return;
            }

            drainSharedImpacts();
            Vehicles.drain(ammoKey(weaponAmmo(IsoPlayer.players[0])));
            synchronized (pending) {
                particles.addAll(pending);
                pending.clear();
            }
            synchronized (pendingHoles) {
                holes.addAll(pendingHoles);
                pendingHoles.clear();
            }
            while (holes.size() > MAX_HOLES) holes.remove(0);
            while (particles.size() > MAX_PARTICLES) particles.remove(0);

            IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.getCell();
            float cx = fCamX.getFloat(frame), cy = fCamY.getFloat(frame), cz = fCamZ.getFloat(frame);

            if (lightLeft > 0f) {
                injectLight(frame, scene, lightLeft / LIGHT_TIME);
                lightLeft -= Math.max(dt, 0.016f);
            }
            float flashBoost = lightLeft > 0f ? 1f : 0f;

            // Erste Person: Auge sitzt (waagerecht) fast im Spieler
            boolean fpView = false;
            IsoPlayer p0 = IsoPlayer.players[0];
            if (p0 != null) {
                float px = -(p0.getX() - cx), pz = -(p0.getY() - cy);
                float ex = fEyeX.getFloat(frame), ez = fEyeZ.getFloat(frame);
                fpView = Math.hypot(px - ex, pz - ez) < 0.45;
            }
            float fpScale = number("VFA_FX_SmokeFP", 0.8f);

            List<Item> list = new ArrayList<>();
            for (int i = particles.size() - 1; i >= 0; i--) {
                P q = particles.get(i);
                q.age += dt;
                if (q.age >= q.life) { particles.remove(i); continue; }
                float t = q.age / q.life;
                if (q.kind == 1 || q.kind == 3) {
                    float drag = (float) Math.exp(-(q.kind == 1 ? 5.0 : 3.0) * dt);
                    q.vx *= drag; q.vy *= drag; q.vz = q.vz * drag + (q.kind == 1 ? 0.10f : -0.4f) * dt;
                    q.x += q.vx * dt; q.y += q.vy * dt; q.z += q.vz * dt / LEVEL;
                } else if (q.kind == 5) {                       // Funke: Schwerkraft, leichte Bremsung
                    float drag = (float) Math.exp(-1.5 * dt);
                    q.vx *= drag; q.vy *= drag; q.vz = q.vz * drag - 9.8f * dt;
                    q.x += q.vx * dt; q.y += q.vy * dt; q.z += q.vz * dt / LEVEL;
                }
                float rx = (float) -(q.x - cx), ry = (float) ((q.z - cz) * LEVEL), rz = (float) -(q.y - cy);
                switch (q.kind) {
                    case 0 -> list.add(Item.point(0, rx, ry, rz, q.size0 * (0.7f + 0.3f * (1f - t)), 1f, 0.82f, 0.45f, 1f - t * 0.6f, 0f));
                    case 2 -> list.add(Item.point(0, rx, ry, rz, q.size0 * (1f - t * 0.5f), 1f, 0.75f, 0.4f, 1f - t, 0f));
                    case 5 -> list.add(Item.point(2, rx, ry, rz, q.size0, 1f, 0.72f + 0.2f * (1f - t), 0.35f, 1f - t * t, 0f));
                    case 4 -> {                                 // Querschlaeger: schneller kurzer Streifen
                        float headD = q.age * 70f, tailD = Math.max(0f, headD - 0.7f);
                        float hx = (float) -(q.x + q.vx * headD - cx), hy = (float) ((q.z + q.vz * headD / LEVEL - cz) * LEVEL), hz = (float) -(q.y + q.vy * headD - cy);
                        float tx = (float) -(q.x + q.vx * tailD - cx), ty = (float) ((q.z + q.vz * tailD / LEVEL - cz) * LEVEL), tz = (float) -(q.y + q.vy * tailD - cy);
                        list.add(new Item(3, tx, ty, tz, hx, hy, hz, q.size0, q.size0, 1f, 0.8f, 0.5f, 1f - t, 0f));
                    }
                    case 1, 3 -> {
                        float grow = 1f - (float) Math.pow(1f - Math.min(1f, t * 1.4f), 2.0);
                        float size = q.size0 + (q.size1 - q.size0) * grow;
                        if (q.kind == 1 && q.fp && fpView) size *= fpScale;
                        float fadeIn = Math.min(1f, t / 0.05f);
                        float a = q.alpha * fadeIn * (1f - t) * (1f - t * 0.3f);
                        float light = Math.max(0.10f, lightAt(cell, q.x, q.y, q.z));
                        light = Math.min(1f, light + flashBoost * 0.6f * (1f - Math.min(1f, q.age * 6f)));
                        float r, gg, b;
                        if (q.kind == 1) { r = 0.82f * light; gg = 0.82f * light; b = 0.82f * light; }
                        else { r = q.r * light; gg = q.g * light; b = q.b * light; }
                        list.add(Item.point(1, rx, ry, rz, size, r, gg, b, a, q.seed));
                    }
                    default -> {}
                }
            }

            for (int i = holes.size() - 1; i >= 0; i--) {
                H h = holes.get(i);
                h.age += dt;
                if (h.age >= h.life) { holes.remove(i); continue; }
                // Tuer aufgemacht / Fenster zerschlagen / entfernt: Loch weg statt in der Luft haengen
                if (h.attached != null && !Attach.state(h.attached).equals(h.state)) { holes.remove(i); continue; }
                float fade = Math.min(1f, (h.life - h.age) / (h.life * 0.2f));
                // im Dunkeln dunkel: Farbe mit dem Licht am Loch (plus kurz dem Muendungslicht) verrechnen
                float hl = Math.max(0.06f, lightAt(cell, h.x, h.y, h.z));
                hl = Math.min(1f, hl + flashBoost * 0.5f);
                list.add(new Item(4, (float) -(h.x - cx), (float) ((h.z - cz) * LEVEL), (float) -(h.y - cy),
                    -h.nx, h.nz, -h.ny, h.size, h.size, h.r * hl, h.g * hl, h.b * hl, h.a * fade, h.seed));
            }

            Vehicles.addHoles(list, cell, cx, cy, cz, flashBoost);

            if (g != null) {
                Object ff = rawgetKey.invoke(g, "VFA_FX_Fireflies");
                if (ff != null && !flag("VFA_FX_Fireflies3D", true)) wipe.invoke(ff);
                else if (ff != null) {
                    int n = ((Number) len.invoke(ff)).intValue();
                    for (int i = 1; i + 3 <= n; i += 4) {
                        Object ox = rawgetIndex.invoke(ff, i), oy = rawgetIndex.invoke(ff, i + 1),
                            oz = rawgetIndex.invoke(ff, i + 2), oa = rawgetIndex.invoke(ff, i + 3);
                        if (!(ox instanceof Number x) || !(oy instanceof Number y) || !(oz instanceof Number z)
                            || !(oa instanceof Number a)) continue;
                        list.add(Item.point(2, (float) -(x.doubleValue() - cx), (float) ((z.doubleValue() - cz) * LEVEL),
                            (float) -(y.doubleValue() - cy), 0.07f, 0.78f, 1f, 0.35f, Math.min(1f, a.floatValue()), 0f));
                    }
                    wipe.invoke(ff);   // Lua fuellt pro Bild neu
                }
            }

            Tracers.step(list, cx, cy, cz);
            Vtb.step(list, cx, cy, cz);

            snapshots.put(scene, List.copyOf(list));
            if (!list.isEmpty() && !seen) { seen = true; System.out.println("[ViewpointEffects3D] first effects captured (" + list.size() + ")"); }
        } catch (Throwable t) {
            disable("capture", t);
        }
    }

    // ---------------------------------------------------------------- Zeichnen (Render-Thread)

    private static int shader(int type, String source) {
        int s = glCreateShader(type);
        glShaderSource(s, source);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(s);
            glDeleteShader(s);
            throw new IllegalStateException(log);
        }
        return s;
    }

    private static void initGL() {
        if (program != 0) return;
        int vs = shader(GL_VERTEX_SHADER, """
            #version 330 core
            uniform mat4 vp;
            uniform int mode;
            uniform vec3 a;
            uniform vec3 b;
            uniform float size;
            uniform float size2;
            uniform vec3 right;
            uniform vec3 up;
            uniform vec3 eye;
            out vec2 uv;
            const vec2 corners[6]=vec2[6](vec2(0,0),vec2(1,0),vec2(1,1),vec2(0,0),vec2(1,1),vec2(0,1));
            void main(){
              vec2 q=corners[gl_VertexID];
              vec3 p;
              if(mode==4){            // Loch: Quadrat auf der Flaeche mit Normale b
                vec3 n=normalize(b);
                vec3 t=abs(n.y)>0.9?vec3(1,0,0):normalize(cross(n,vec3(0,1,0)));
                vec3 bt=cross(n,t);
                p=a+n*0.004+(t*(q.x-0.5)+bt*(q.y-0.5))*size;
                gl_Position=vp*vec4(p,1);
                uv=q;
                return;
              }
              if(mode==3){            // Band von a (Ende) nach b (Spitze), zur Kamera gedreht
                vec3 dir=b-a;
                vec3 mid=(a+b)*0.5;
                vec3 side=cross(dir,eye-mid);
                if(dot(side,side)<1e-10) side=up;
                side=normalize(side);
                float w=mix(size,size2,q.x)*(1.0+length(eye-mix(a,b,q.x))*0.015);
                p=mix(a,b,q.x)+side*(q.y-0.5)*w;
              } else {
                p=a+right*(q.x-0.5)*size+up*(q.y-0.5)*size;
              }
              gl_Position=vp*vec4(p,1);
              uv=q;
            }
            """);
        int fs = shader(GL_FRAGMENT_SHADER, """
            #version 330 core
            uniform int mode;
            uniform vec4 tint;
            uniform float seed;
            in vec2 uv;
            out vec4 color;
            float hash(vec2 p){ return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
            float noise(vec2 p){
              vec2 i=floor(p), f=fract(p);
              f=f*f*(3.0-2.0*f);
              return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
            }
            void main(){
              vec2 c=uv-0.5;
              float d=length(c)*2.0;
              if(mode==0){            // Blitz / Funke: hell, additiv
                float core=pow(max(1.0-d,0.0),2.0);
                float flareH=exp(-abs(c.y)*22.0)*max(1.0-abs(c.x)*2.0,0.0);
                float flareV=exp(-abs(c.x)*22.0)*max(1.0-abs(c.y)*2.0,0.0)*0.6;
                float i=min(core*1.4+flareH+flareV,1.6);
                float al=clamp(i*tint.a,0.0,1.0);
                if(al<0.004) discard;
                color=vec4(tint.rgb*(0.7+0.9*i),al);
              } else if(mode==1){     // Rauch: weich, verrauscht, Alpha-Mischung
                float n=noise(uv*4.0+seed)*0.6+noise(uv*9.0-seed)*0.4;
                float shape=smoothstep(1.0,0.15,d+ (n-0.5)*0.45);
                float al=shape*tint.a*(0.55+0.45*n);
                if(al<0.004) discard;
                color=vec4(tint.rgb*(0.85+0.3*n),al);
              } else if(mode==4){     // Loch: dunkler Kern, ausgefranster Rand, Alpha-Mischung
                float n=noise(uv*6.0+seed)*0.5+noise(uv*13.0-seed)*0.5;
                float rim=d+(n-0.5)*0.35;
                float core=smoothstep(0.55,0.25,rim);
                float halo=smoothstep(1.0,0.5,rim)*0.35;
                float al=clamp((core+halo)*tint.a,0.0,1.0);
                if(al<0.004) discard;
                color=vec4(tint.rgb*(0.8+0.4*n),al);
              } else if(mode==3){     // Tracer: zur Spitze hin heller, Raender weich, additiv
                float across=1.0-abs(uv.y*2.0-1.0);
                float i=pow(uv.x,1.3)*pow(across,0.8)*1.4;
                float al=clamp(i*tint.a,0.0,1.0);
                if(al<0.004) discard;
                color=vec4(tint.rgb*(0.8+0.6*i),al);
              } else {                // Gluehwuermchen: kleiner Leuchtpunkt, additiv
                float i=pow(max(1.0-d,0.0),3.0)*1.6;
                float al=clamp(i*tint.a,0.0,1.0);
                if(al<0.004) discard;
                color=vec4(tint.rgb*(0.8+0.6*i),al);
              }
            }
            """);
        int p = glCreateProgram();
        glAttachShader(p, vs);
        glAttachShader(p, fs);
        glLinkProgram(p);
        glDeleteShader(vs);
        glDeleteShader(fs);
        if (glGetProgrami(p, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(p);
            glDeleteProgram(p);
            throw new IllegalStateException(log);
        }
        program = p;
        vao = glGenVertexArrays();
    }

    private static void toggle(int cap, boolean on) { if (on) glEnable(cap); else glDisable(cap); }

    private static float f(Method m, Object o) throws Exception { return ((Number) m.invoke(o)).floatValue(); }

    /** Normaler Weltpass (WorldRenderer.drawWeather). Mit Shader-Pack zeichnet stattdessen drawIris. */
    public static void draw(Object scene) {
        if (failed) return;
        try {
            if (rIris.getBoolean(null)) return;
        } catch (Throwable t) {
            return;
        }
        render(scene, null, true);
    }

    private static Field cScene;

    /**
     * Shader-Pack (Iris) aktiv: nach IrisGeometry.weather, dem Wetter-Schritt der Pack-Pipeline. Der Farb- und
     * Tiefenpuffer des Packs ist dann gebunden, die Effekte landen im Bild des Packs (und bekommen dessen Bloom).
     */
    public static void drawIris(Object context) {
        if (failed || context == null) return;
        try {
            if (!ready) init();
            if (cScene == null) cScene = field(context.getClass(), "scene");
            render(cScene.get(context), context, false);
        } catch (Throwable t) {
            disable("draw (shader pack)", t);
        }
    }

    private static void render(Object scene, Object ctx, boolean bind) {
        List<Item> items = snapshots.get(scene);
        if (items == null || items.isEmpty()) return;
        try {
            Object context = ctx != null ? ctx : rFrame.get(null);
            Object vp = cViewProj.get(context), view = cView.get(context);
            float ex = cEyeX.getFloat(context), ey = cEyeY.getFloat(context), ez = cEyeZ.getFloat(context);
            float rx = f(m00, view), ry = f(m10, view), rz = f(m20, view);
            float ux = f(m01, view), uy = f(m11, view), uz = f(m21, view);

            // Rauch von hinten nach vorne, danach alles Leuchtende
            List<Item> decals = new ArrayList<>(), smoke = new ArrayList<>(), glow = new ArrayList<>();
            for (Item it : items) (it.mode == 4 ? decals : it.mode == 1 ? smoke : glow).add(it);
            smoke.sort((p, q) -> Float.compare(dist2(q, ex, ey, ez), dist2(p, ex, ey, ez)));

            int oldProgram = glGetInteger(GL_CURRENT_PROGRAM), oldVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            int fbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING), readFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int[] viewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, viewport);
            boolean blend = glIsEnabled(GL_BLEND), depth = glIsEnabled(GL_DEPTH_TEST), cull = glIsEnabled(GL_CULL_FACE),
                scissor = glIsEnabled(GL_SCISSOR_TEST), mask = glGetBoolean(GL_DEPTH_WRITEMASK);
            int df = glGetInteger(GL_DEPTH_FUNC), sr = glGetInteger(GL_BLEND_SRC_RGB), dr = glGetInteger(GL_BLEND_DST_RGB),
                sa = glGetInteger(GL_BLEND_SRC_ALPHA), da = glGetInteger(GL_BLEND_DST_ALPHA),
                er = glGetInteger(GL_BLEND_EQUATION_RGB), ea = glGetInteger(GL_BLEND_EQUATION_ALPHA);
            try {
                initGL();
                if (bind) bindTarget.invoke(null);
                glUseProgram(program);
                glBindVertexArray(vao);
                glEnable(GL_BLEND);
                glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LEQUAL);
                glDepthMask(false);
                glDisable(GL_CULL_FACE);
                glDisable(GL_SCISSOR_TEST);
                glUniformMatrix4fv(glGetUniformLocation(program, "vp"), false, (float[]) vpGet.invoke(vp, (Object) new float[16]));
                glUniform3f(glGetUniformLocation(program, "right"), rx, ry, rz);
                glUniform3f(glGetUniformLocation(program, "up"), ux, uy, uz);
                glUniform3f(glGetUniformLocation(program, "eye"), ex, ey, ez);
                uB = glGetUniformLocation(program, "b");
                uSize2 = glGetUniformLocation(program, "size2");
                int uMode = glGetUniformLocation(program, "mode"), uA = glGetUniformLocation(program, "a"),
                    uSize = glGetUniformLocation(program, "size"), uTint = glGetUniformLocation(program, "tint"),
                    uSeed = glGetUniformLocation(program, "seed");

                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                for (Item it : decals) drawItem(it, uMode, uA, uSize, uTint, uSeed);
                for (Item it : smoke) drawItem(it, uMode, uA, uSize, uTint, uSeed);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE);
                for (Item it : glow) drawItem(it, uMode, uA, uSize, uTint, uSeed);
                if (!drawn) { drawn = true; System.out.println("[ViewpointEffects3D] first 3D effects drawn"); }
            } finally {
                glUseProgram(oldProgram);
                glBindVertexArray(oldVao);
                glBlendEquationSeparate(er, ea);
                glBlendFuncSeparate(sr, dr, sa, da);
                toggle(GL_BLEND, blend);
                glDepthFunc(df);
                glDepthMask(mask);
                toggle(GL_DEPTH_TEST, depth);
                toggle(GL_CULL_FACE, cull);
                toggle(GL_SCISSOR_TEST, scissor);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                glBindFramebuffer(GL_READ_FRAMEBUFFER, readFbo);
                glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            }
        } catch (Throwable t) {
            disable("draw", t);
        }
    }

    private static float dist2(Item it, float ex, float ey, float ez) {
        float dx = it.x - ex, dy = it.y - ey, dz = it.z - ez;
        return dx * dx + dy * dy + dz * dz;
    }

    private static int uB, uSize2;

    private static void drawItem(Item it, int uMode, int uA, int uSize, int uTint, int uSeed) {
        glUniform1i(uMode, it.mode);
        glUniform3f(uA, it.x, it.y, it.z);
        glUniform3f(uB, it.bx, it.by, it.bz);
        glUniform1f(uSize, it.size);
        glUniform1f(uSize2, it.size2);
        glUniform4f(uTint, it.r, it.g, it.b, it.a);
        glUniform1f(uSeed, it.seed);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    private Fx() {}
}
