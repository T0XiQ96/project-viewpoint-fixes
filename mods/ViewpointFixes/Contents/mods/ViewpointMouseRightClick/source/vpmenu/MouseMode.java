package vpmenu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Viewpoint kennt zwei Zustaende: Fadenkreuz (Maus gefangen, Blick folgt der Maus) und Mausmodus (Mittelklick,
 * Lootfenster, Einstellungen, Pause: freier Zeiger). Der Zustand steht in viewpoint.FP.cursorMode (zaehlt nur bei eingeschaltetem 3D-Bild: View.enabled).
 * Im Fadenkreuz-Bild bleibt der Rechtsklick das Zielen/Haltung, im Mausmodus soll er das normale Menue oeffnen.
 */
public final class MouseMode {
    private static Field cursorMode, enabled;
    private static boolean failed, tried;
    private static Field env;
    private static Method rawget, rawset;

    public static void validate() {
        System.out.println("[ViewpointMouseRightClick] 1.0 ready: right click = menu in Viewpoint's mouse mode");
    }

    public static boolean on() {
        if (failed) return false;
        try {
            if (!tried) {
                cursorMode = Class.forName("viewpoint.FP").getDeclaredField("cursorMode");
                cursorMode.setAccessible(true);
                enabled = Class.forName("viewpoint.core.View").getField("enabled");
                tried = true;
            }
            return enabled.getBoolean(null) && cursorMode.getBoolean(null);
        } catch (ClassNotFoundException e) {
            return false; // Viewpoint noch nicht geladen: spaeter nochmal
        } catch (Throwable e) {
            failed = true;
            System.err.println("[ViewpointMouseRightClick] Disabled (Viewpoint not found or changed): " + e);
            return false;
        }
    }

    /**
     * Pro Frame: VFA_VpView (3D-Bild an) und VFA_VpMouse (Mausmodus an) fuer Lua. Setzt Lua VFA_WantMouseMode = true,
     * schaltet Viewpoint in den Mausmodus (freier Zeiger).
     */
    public static void frame() {
        if (failed) return;
        try {
            if (!tried) on();
            if (!tried) return;
            if (env == null) {
                env = Class.forName("zombie.Lua.LuaManager").getField("env");
                Class<?> table = Class.forName("se.krka.kahlua.vm.KahluaTable");
                rawget = table.getMethod("rawget", Object.class);
                rawset = table.getMethod("rawset", Object.class, Object.class);
            }
            Object globals = env.get(null);
            if (globals == null) return;
            if (Boolean.TRUE.equals(rawget.invoke(globals, "VFA_WantMouseMode"))) {
                cursorMode.setBoolean(null, true);
                rawset.invoke(globals, "VFA_WantMouseMode", Boolean.FALSE);
            }
            rawset.invoke(globals, "VFA_VpView", enabled.getBoolean(null));
            rawset.invoke(globals, "VFA_VpMouse", cursorMode.getBoolean(null));
        } catch (ClassNotFoundException e) {
            // Viewpoint noch nicht geladen
        } catch (Throwable e) {
            failed = true;
            System.err.println("[ViewpointMouseRightClick] Disabled: " + e);
        }
    }
}
