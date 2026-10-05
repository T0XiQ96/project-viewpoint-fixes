package vplook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Umschauen in Viewpoints 3D-Ansicht, solange Lua VFA_FreeLook = true setzt.
 *
 * Viewpoint fuehrt Kamera und Koerper ueber denselben Blickwinkel (viewpoint.input.Look.yaw): die Maus dreht ihn,
 * Laufrichtung und Koerperdrehung richten sich danach (viewpoint.input.Controls, vor/waehrend/nach der Bewegung).
 * Beim Umschauen merkt sich diese Klasse den Winkel beim Druecken der Taste ("Koerper") und setzt ihn nur fuer die
 * Dauer dieser Bewegungs-Aufrufe ein; danach steht wieder der Kamerawinkel drin, inklusive Mausbewegung, die
 * zwischendurch dazukam. Die Kamera dreht also frei, Koerper und Laufrichtung bleiben. Nach dem Loslassen dreht die
 * Kamera weich zurueck auf den Koerper.
 * Alles per Reflection auf oeffentliche Felder, damit die Mod ohne Viewpoint nichts tut.
 */
public final class FreeLook {
    private static final float RETURN_SECONDS = 0.08f;
    private static final float DONE = 0.01f;

    private static boolean ready, failed;
    private static Field yaw;
    private static Field env;
    private static Method rawget;

    private static boolean active, returning, swapped;
    private static float body, saved;
    private static int depth;
    private static long lastNanos;

    public static void validate() {
        System.out.println("[ViewpointShoulderLook3D] 2.0 ready: hold the look key to look around in 3D");
    }

    private static boolean init() {
        try {
            yaw = Class.forName("viewpoint.input.Look").getField("yaw");
            env = Class.forName("zombie.Lua.LuaManager").getField("env");
            rawget = Class.forName("se.krka.kahlua.vm.KahluaTable").getMethod("rawget", Object.class);
            ready = true;
            return true;
        } catch (ClassNotFoundException e) {
            return false; // Viewpoint noch nicht geladen
        } catch (Throwable e) {
            fail(e);
            return false;
        }
    }

    private static void fail(Throwable e) {
        failed = true;
        System.err.println("[ViewpointShoulderLook3D] free look off (Viewpoint not found or changed): " + e);
    }

    private static boolean wanted() throws Exception {
        Object globals = env.get(null);
        return globals != null && Boolean.TRUE.equals(rawget.invoke(globals, "VFA_FreeLook"));
    }

    private static float wrap(float a) {
        return (float) Math.atan2(Math.sin(a), Math.cos(a));
    }

    /** Einmal pro Spieler-Update: Taste auswerten, nach dem Loslassen die Kamera zurueckdrehen. */
    private static void update() throws Exception {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        float current = yaw.getFloat(null);
        if (wanted()) {
            if (!active && !returning) body = current;
            active = true;
            returning = false;
        } else if (active) {
            active = false;
            returning = true;
        }
        if (returning) {
            float diff = wrap(current - body);
            if (Math.abs(diff) < DONE) {
                yaw.setFloat(null, current - diff);
                returning = false;
            } else {
                float k = 1f - (float) Math.exp(-dt / RETURN_SECONDS);
                yaw.setFloat(null, current - diff * k);
            }
        }
    }

    /** Vor einem Bewegungs-Aufruf von Viewpoint: Koerperwinkel einsetzen. */
    public static void enter(boolean tick) {
        if (depth++ > 0 || failed) return;
        try {
            if (!ready && !init()) return;
            if (tick) update();
            if (active || returning) {
                saved = yaw.getFloat(null);
                yaw.setFloat(null, body);
                swapped = true;
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    /** Danach: Kamerawinkel zurueck, plus was die Maus inzwischen dazugegeben hat. */
    public static void exit() {
        if (depth > 0) depth--;
        if (depth > 0 || !swapped) return;
        swapped = false;
        try {
            float now = yaw.getFloat(null);
            yaw.setFloat(null, saved + (now - body));
        } catch (Throwable e) {
            fail(e);
        }
    }
}
