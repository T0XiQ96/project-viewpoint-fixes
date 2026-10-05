package vpfx;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

/**
 * Einschussloecher an beweglichen Teilen (Tueren, Fenster, Tore).
 *
 * Ein Loch liegt fest in der Welt. Sitzt es auf einer Tuer und die Tuer geht auf, hinge es sonst in der Luft.
 * Beim Einschlag wird deshalb die Tuer / das Fenster auf der Feldkante am Einschlagpunkt gesucht und ihr Zustand
 * gemerkt (offen, zerstoert/zersplittert, noch auf dem Feld). Aendert er sich, verschwindet das Loch.
 */
final class Attach {
    private static final double EDGE = 0.15;      // Felder: so nah muss das Loch an der Tuer-/Fensterkante liegen
    private static Class<?> door, window, thumpable;
    private static boolean ready, broken;
    private static final Map<Class<?>, Method[]> methods = new HashMap<>();

    private static Class<?> cls(String n) {
        try { return Class.forName(n, true, Attach.class.getClassLoader()); } catch (Throwable t) { return null; }
    }

    private static void init() {
        if (ready) return;
        door = cls("zombie.iso.objects.IsoDoor");
        window = cls("zombie.iso.objects.IsoWindow");
        thumpable = cls("zombie.iso.objects.IsoThumpable");
        ready = true;
    }

    private static Method m(Class<?> c, String... names) {
        for (String n : names) {
            try { return c.getMethod(n); } catch (Throwable ignored) {}
        }
        return null;
    }

    /** {getNorth, isOpen, isDestroyed, isSmashed, getObjectIndex, isDoor, isWindow} je Klasse. */
    private static Method[] of(Class<?> c) {
        return methods.computeIfAbsent(c, k -> new Method[] {
            m(k, "getNorth", "isNorth"), m(k, "isOpen", "IsOpen"), m(k, "isDestroyed"), m(k, "isSmashed"),
            m(k, "getObjectIndex"), m(k, "isDoor"), m(k, "isWindow") });
    }

    private static Object call(Method mm, Object o) {
        if (mm == null) return null;
        try { return mm.invoke(o); } catch (Throwable t) { return null; }
    }

    private static boolean movable(IsoObject o) {
        if (door != null && door.isInstance(o)) return true;
        if (window != null && window.isInstance(o)) return true;
        if (thumpable != null && thumpable.isInstance(o)) {
            Method[] mm = of(o.getClass());
            return Boolean.TRUE.equals(call(mm[5], o)) || Boolean.TRUE.equals(call(mm[6], o));
        }
        return false;
    }

    /** Tuer/Fenster, auf dessen Kante (x, y) liegt, oder null. z in Stockwerken. */
    static Object find(double x, double y, double z) {
        if (broken) return null;
        try {
            init();
            IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.getCell();
            if (cell == null) return null;
            int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
            Object best = null;
            double bestD = EDGE;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    IsoGridSquare sq = cell.getGridSquare(ix + dx, iy + dy, iz);
                    if (sq == null) continue;
                    for (int i = 0; i < sq.getObjects().size(); i++) {
                        IsoObject o = sq.getObjects().get(i);
                        if (o == null || !movable(o)) continue;
                        double ox = ix + dx, oy = iy + dy;
                        boolean north = Boolean.TRUE.equals(call(of(o.getClass())[0], o));
                        // Tueren/Fenster liegen auf der Nord- (y = Feldrand) oder Westkante (x = Feldrand) ihres Feldes
                        double d = north
                            ? Math.abs(y - oy) + Math.max(0, Math.max(ox - x, x - (ox + 1)))
                            : Math.abs(x - ox) + Math.max(0, Math.max(oy - y, y - (oy + 1)));
                        if (d < bestD) { bestD = d; best = o; }
                    }
                }
            }
            return best;
        } catch (Throwable t) {
            broken = true;
            System.err.println("[ViewpointEffects3D] door/window holes disabled: " + t);
            return null;
        }
    }

    /** Zustand als Text: offen, zerstoert, zersplittert, noch auf dem Feld. */
    static String state(Object o) {
        if (o == null) return "";
        Method[] mm = of(o.getClass());
        Object idx = call(mm[4], o);
        boolean present = !(idx instanceof Number n) || n.intValue() >= 0;
        return call(mm[1], o) + "|" + call(mm[2], o) + "|" + call(mm[3], o) + "|" + present;
    }

    private Attach() {}
}
