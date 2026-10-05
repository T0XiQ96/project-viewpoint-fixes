package vplist;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Viewpoint baut seine Interaktionsliste ununterbrochen aus dem Rechtsklick-Menue des Spiels. Mods, die beim Bauen
 * des Menues Hinweistexte ueber dem Charakter einblenden (z. B. Fix Floor: "Missing a Hammer..."), loesen sie damit
 * staendig aus. Die Lua-Seite setzt waehrenddessen die Lua-Variable VFA_InHarvest auf true.
 */
public final class Hush {
    private static boolean failed, ready;
    private static Field env;
    private static Method rawget;

    private static synchronized void init() throws Exception {
        if (ready) return;
        env = Class.forName("zombie.Lua.LuaManager").getField("env");
        rawget = Class.forName("se.krka.kahlua.vm.KahluaTable").getMethod("rawget", Object.class);
        ready = true;
    }

    public static void validate() {
        System.out.println("[ViewpointInteractList] 1.0 ready: no hint texts while Viewpoint builds its list");
    }

    public static boolean on() {
        if (failed) return false;
        try {
            if (!ready) init();
            Object globals = env.get(null);
            return globals != null && Boolean.TRUE.equals(rawget.invoke(globals, "VFA_InHarvest"));
        } catch (Throwable e) {
            failed = true;
            System.err.println("[ViewpointInteractList] hint filter off: " + e);
            return false;
        }
    }
}
