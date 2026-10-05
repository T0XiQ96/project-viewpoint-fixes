package vphide;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Versteckt man sich mit Hide Anywhere unter einem Bett, bleibt Viewpoints Kamera auf Augenhoehe des Stehenden.
 * Die Lua-Seite setzt dann VFA_HideEye (Augenhoehe in Viewpoint-Einheiten; ein Stockwerk = 2.4494896).
 * Hier wird die senkrechte Kameraposition (Frame.eyeY) darauf gesetzt - gleiche Formel wie in Controls.put.
 */
public final class Eye {
    private static final float LEVEL = 2.4494896f;
    private static boolean failed, ready;
    private static Field env, eyeY, camZ;
    private static Method rawget, getZ;

    private static synchronized void init(Object character) throws Exception {
        if (ready) return;
        Class<?> frame = Class.forName("viewpoint.core.Frame");
        eyeY = frame.getField("eyeY");
        camZ = frame.getField("camZ");
        env = Class.forName("zombie.Lua.LuaManager").getField("env");
        rawget = Class.forName("se.krka.kahlua.vm.KahluaTable").getMethod("rawget", Object.class);
        getZ = character.getClass().getMethod("getZ");
        ready = true;
    }

    public static void validate() {
        System.out.println("[ViewpointHideCamera] 1.0 ready: camera under the bed while hiding");
    }

    public static void apply(Object character, Object frame) {
        if (failed || character == null || frame == null) return;
        try {
            if (!ready) init(character);
            Object globals = env.get(null);
            if (globals == null) return;
            Object h = rawget.invoke(globals, "VFA_HideEye");
            if (!(h instanceof Number n)) return;
            float z = ((Number) getZ.invoke(character)).floatValue();
            float cam = camZ.getFloat(frame);
            eyeY.setFloat(frame, n.floatValue() + (z - cam) * LEVEL);
        } catch (Throwable e) {
            failed = true;
            System.err.println("[ViewpointHideCamera] Disabled: " + e);
        }
    }
}
