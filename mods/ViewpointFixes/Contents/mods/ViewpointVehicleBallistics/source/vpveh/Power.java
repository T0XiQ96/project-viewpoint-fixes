package vpveh;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import zombie.inventory.types.HandWeapon;

/**
 * Wucht eines Schusses: Muendungsenergie in Joule.
 *
 * Mit Viewpoint True Ballistics kommt sie aus dessen Profil (Masse und echte Muendungsgeschwindigkeit je Kaliber,
 * auch Mod-Munition und Kompatibilitaetspakete). Ohne VTB: Schaetzung am Munitionsnamen, sonst an der Waffe.
 * Panzerbrechend (AP) zaehlt staerker, Hohlspitz (HP/JHP) schwaecher. Schrot: Energie je Kugel.
 */
final class Power {
    static final int WEAK = 0, MEDIUM = 1, STRONG = 2;
    static final double MEDIUM_J = 500;    // 9 mm als Bezug: Faktor 1

    private static final Map<HandWeapon, double[]> cache = new WeakHashMap<>();
    private static boolean vtbChecked, vtbOk;
    private static Method getProfile, energy;
    private static Field muzzle, pellets;

    private static void initVtb() {
        if (vtbChecked) return;
        vtbChecked = true;
        try {
            Class<?> api = Class.forName("vtb.api.BallisticsAPI", true, Power.class.getClassLoader());
            getProfile = api.getMethod("getWeaponProfile", HandWeapon.class);
            Class<?> prof = Class.forName("vtb.weapon.BallisticProfile", true, Power.class.getClassLoader());
            energy = prof.getMethod("energy", double.class);
            muzzle = prof.getField("muzzleVelocity");
            pellets = prof.getField("pelletCount");
            vtbOk = true;
        } catch (Throwable t) {
            vtbOk = false;
        }
    }

    static boolean vtbPresent() {
        initVtb();
        return vtbOk;
    }

    /** {Energie je Projektil in J, Anzahl Projektile}. */
    static double[] muzzle(HandWeapon w) {
        if (w == null) return new double[] { MEDIUM_J, 1 };
        double[] c = cache.get(w);
        if (c != null) return c;
        double e = -1, n = 1;
        initVtb();
        if (vtbOk) {
            try {
                Object p = getProfile.invoke(null, w);
                if (p != null) {
                    e = (Double) energy.invoke(p, muzzle.getDouble(p));
                    n = Math.max(1, pellets.getInt(p));
                }
            } catch (Throwable ignored) {
            }
        }
        String ammo = ammoName(w);
        if (e <= 0) {
            double[] guess = byName(ammo, w);
            e = guess[0];
            n = guess[1];
        }
        e *= modifier(ammo);
        c = new double[] { e, n };
        cache.put(w, c);
        return c;
    }

    static String ammoName(HandWeapon w) {
        try {
            Object a = w.getAmmoType();
            if (a != null) {
                Object k = a.getClass().getMethod("getItemKey").invoke(a);
                return String.valueOf(k != null ? k : a).toLowerCase();
            }
        } catch (Throwable ignored) {
        }
        try {
            return String.valueOf(w.getFullType()).toLowerCase();
        } catch (Throwable t) {
            return "";
        }
    }

    private static double modifier(String k) {
        if (k == null) return 1;
        if (k.contains("_ap") || k.contains("ap_") || k.contains("armorpiercing") || k.contains("armourpiercing") || k.endsWith("ap")) return 1.6;
        if (k.contains("jhp") || k.contains("hollow") || k.contains("_hp") || k.endsWith("hp")) return 0.6;
        return 1;
    }

    private static double[] byName(String k, HandWeapon w) {
        if (k == null) k = "";
        if (k.contains("slug")) return new double[] { 3000, 1 };
        if (k.contains("shotgun") || k.contains("shell") || k.contains("buck") || k.contains("gauge") || k.contains("12g"))
            return new double[] { 180, 9 };
        if (k.contains("50bmg") || k.contains("50cal") || k.contains("338") || k.contains("lapua")) return new double[] { 9000, 1 };
        if (k.contains("308") || k.contains("762x51") || k.contains("3006") || k.contains("30-06") || k.contains("762x54")
            || k.contains("300")) return new double[] { 3400, 1 };
        if (k.contains("762") || k.contains("3030") || k.contains("7.62")) return new double[] { 2000, 1 };
        if (k.contains("556") || k.contains("223") || k.contains("545") || k.contains("5.56")) return new double[] { 1700, 1 };
        if (k.contains("57x28") || k.contains("46x30")) return new double[] { 550, 1 };
        if (k.contains("44") || k.contains("magnum") || k.contains("454") || k.contains("500")) return new double[] { 1300, 1 };
        if (k.contains("357") || k.contains("10mm")) return new double[] { 750, 1 };
        if (k.contains("45")) return new double[] { 550, 1 };
        if (k.contains("38")) return new double[] { 320, 1 };
        if (k.contains("22") || k.contains("25acp")) return new double[] { 150, 1 };
        if (k.contains("9mm") || k.contains("9x19") || k.contains("bullets9")) return new double[] { 500, 1 };
        // unbekannt: nach Waffe
        try {
            if (w != null && w.getProjectileCount() > 1) return new double[] { 180, w.getProjectileCount() };
            float range = w == null ? 15 : w.getMaxRange();
            float dmg = w == null ? 1 : w.getMaxDamage();
            if (range > 30 || dmg > 3) return new double[] { 2500, 1 };
            if (range > 20) return new double[] { 1500, 1 };
        } catch (Throwable ignored) {
        }
        return new double[] { MEDIUM_J, 1 };
    }

    /** Faktor fuer Schaden: 1 = 9 mm, ~1.85 = 5.56, ~0.55 = .22. */
    static double factor(double joules) {
        return Math.max(0.3, Math.min(4.0, Math.sqrt(Math.max(1, joules) / MEDIUM_J)));
    }

    static int classOf(double joules, int projectiles) {
        if (projectiles > 1 && joules < 600) return WEAK;
        if (joules >= 1200) return STRONG;
        if (joules < 350) return WEAK;
        return MEDIUM;
    }

    private Power() {}
}
