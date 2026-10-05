package vfabi;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.zed_0xff.zombie_buddy.Exposer;
import zombie.characters.IsoGameCharacter;

/**
 * Bullet Impacts 2D: sammelt die Einschlagpunkte des Spiels (CombatManager.addTracerEffect) fuer die Lua-Seite.
 * Lua holt sie mit VFABulletImpacts.poll() ab: "x;y;z;schuetzeX;schuetzeY;Munitionstyp" oder nil.
 * Das Zeichnen (Loecher, Funken, Staub, Abpraller nach Material) macht VFA_BulletImpacts2D.lua.
 */
@Exposer.LuaClass(name = "VFABulletImpacts")
public final class Impacts {
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static volatile boolean failed;

    public static void add(Object character, float x, float y, float z, Object square) {
        if (failed || Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) return;
        try {
            float sx = x, sy = y;
            String ammo = "";
            if (character instanceof IsoGameCharacter c) {
                sx = c.getX();
                sy = c.getY();
                Object item = c.getPrimaryHandItem();
                if (item != null) {
                    Object a = item.getClass().getMethod("getAmmoType").invoke(item);
                    if (a != null) {
                        Object k = a.getClass().getMethod("getItemKey").invoke(a);
                        ammo = String.valueOf(k != null ? k : a);
                    }
                }
            }
            while (queue.size() > 64) queue.poll();
            queue.add(String.format(Locale.ROOT, "%.3f;%.3f;%.3f;%.3f;%.3f;%s", x, y, z, sx, sy, ammo.replace(';', ',')));
        } catch (Throwable t) {
            failed = true;
            System.err.println("[BulletImpacts2D] disabled: " + t);
        }
    }

    /** Naechster Einschlag oder null. */
    public static String poll() {
        return queue.poll();
    }

    public static void validate() {
        System.out.println("[BulletImpacts2D] 1.0 ready");
    }

    private Impacts() {}
}
