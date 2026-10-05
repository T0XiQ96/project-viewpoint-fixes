package vpplace;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Zeichnet den Vorschau-Gegenstand des Platzier-Cursors (ISPlace3DItemCursor) in Viewpoints 3D-Szene.
 * Die Lua-Seite (VFA_ViewpointPlaceItems.lua) legt Gegenstand, Feld, Position und Drehung in die Lua-Tabelle
 * VFA_PlaceGhost. Hier wird der Gegenstand genauso eingesammelt wie Viewpoint es mit am Boden liegenden
 * Gegenstaenden macht (Models.item/own), nur mit den Werten des Cursors.
 * Alles per Reflection: die Spielklassen sind neuer als der Compiler (Java 25), Viewpoint liegt nicht im Classpath.
 */
public final class Ghost {
    private static boolean failed, announced, ready;
    private static Field env, sink, capturing, drawers;
    private static Method own, rawget, renderMain;
    private static Class<?> tableClass, itemClass, squareClass;

    private static synchronized void init() throws Exception {
        if (ready) return;
        Class<?> models = Class.forName("viewpoint.models.Models");
        Class<?> frame = Class.forName("viewpoint.core.Frame");
        tableClass = Class.forName("se.krka.kahlua.vm.KahluaTable");
        itemClass = Class.forName("zombie.inventory.InventoryItem");
        squareClass = Class.forName("zombie.iso.IsoGridSquare");
        env = Class.forName("zombie.Lua.LuaManager").getField("env");
        rawget = tableClass.getMethod("rawget", Object.class);
        sink = models.getDeclaredField("sink");
        sink.setAccessible(true);
        capturing = models.getDeclaredField("capturing");
        capturing.setAccessible(true);
        drawers = frame.getDeclaredField("drawers");
        drawers.setAccessible(true);
        own = models.getDeclaredMethod("own", frame, int.class, squareClass, itemClass);
        own.setAccessible(true);
        renderMain = Class.forName("zombie.core.skinnedmodel.model.WorldItemModelDrawer").getMethod("renderMain",
                itemClass, squareClass, squareClass, float.class, float.class, float.class, float.class, float.class,
                boolean.class);
        ready = true;
    }

    public static void validate() {
        if (failed) return;
        try {
            init();
            System.out.println("[ViewpointPlaceItems] 1.0 ready: 3D preview for item placement.");
        } catch (Throwable e) { fail(e); }
    }

    private static void fail(Throwable e) {
        failed = true;
        System.err.println("[ViewpointPlaceItems] Disabled: " + e);
        e.printStackTrace();
    }

    private static float num(Object table, String key) throws Exception {
        Object v = rawget.invoke(table, key);
        return v instanceof Number n ? n.floatValue() : 0f;
    }

    @SuppressWarnings("unchecked")
    public static void draw(Object frame) {
        if (failed) return;
        try {
            if (!ready) init();
            Object globals = env.get(null);
            if (globals == null) return;
            Object t = rawget.invoke(globals, "VFA_PlaceGhost");
            if (t == null || !tableClass.isInstance(t)) return;
            Object item = rawget.invoke(t, "item"), sq = rawget.invoke(t, "sq");
            if (!itemClass.isInstance(item) || !squareClass.isInstance(sq)) return;
            if (!announced) { announced = true; System.out.println("[ViewpointPlaceItems] first preview drawn"); }
            ArrayList<Object> list = (ArrayList<Object>) drawers.get(frame);
            int before = list.size();
            sink.set(null, list);
            try {
                capturing.setBoolean(null, true);
                try {
                    renderMain.invoke(null, item, sq, sq, num(t, "x"), num(t, "y"), num(t, "z"), 0f, num(t, "rot"), true);
                } finally {
                    capturing.setBoolean(null, false);
                }
                own.invoke(null, frame, before, sq, item);
            } finally {
                sink.set(null, null);
            }
        } catch (Throwable e) { fail(e); }
    }
}
