package vpveh;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

/**
 * Sandbox-Werte (SandboxVars.<SANDBOX>.<PREFIX><Name>), alle 2 Sekunden neu gelesen.
 * Fehlt ein Wert (Mod-Sandbox nicht geladen), gilt der Standard aus dem Code.
 */
final class Cfg {
    private static long readAt;
    private static KahluaTable table;

    private static KahluaTable vars() {
        long now = System.currentTimeMillis();
        if (now - readAt > 2000 || table == null) {
            readAt = now;
            table = null;
            try {
                Object sv = LuaManager.env == null ? null : LuaManager.env.rawget("SandboxVars");
                if (sv instanceof KahluaTable t) {
                    Object page = t.rawget(Build.SANDBOX);
                    if (page instanceof KahluaTable p) table = p;
                }
            } catch (Throwable ignored) {
            }
        }
        return table;
    }

    private static Object raw(String name) {
        KahluaTable t = vars();
        return t == null ? null : t.rawget(Build.PREFIX + name);
    }

    static boolean bool(String name, boolean def) {
        Object v = raw(name);
        return v instanceof Boolean b ? b : def;
    }

    static double num(String name, double def) {
        Object v = raw(name);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    static int integer(String name, int def) {
        return (int) Math.round(num(name, def));
    }

    // ------------------------------------------------------------------ Fahrzeuge

    static boolean enabled() { return bool("Enabled", true); }
    static boolean in2D() { return bool("In2D", !Build.PRIMARY); }
    static boolean hitInfo() { return bool("HitInfo", true); }
    static double damageScale() { return num("DamageScale", 1.0); }
    static int windshieldShots() { return Math.max(1, integer("WindshieldShots", 15)); }
    static double sideWindowShatter() { return num("SideWindowShatter", 90) / 100.0; }
    static double lightBreak() { return num("LightBreak", 100) / 100.0; }
    static int tireShots(int powerClass) {
        return Math.max(1, switch (powerClass) {
            case Power.STRONG -> integer("TireShotsStrong", 1);
            case Power.WEAK -> integer("TireShotsWeak", 3);
            default -> integer("TireShotsMedium", 2);
        });
    }
    /** 1 = sofort platt, 2 = langsamer Luftverlust. */
    static int tireMode() { return integer("TireMode", 1); }
    static double tireLeakSeconds() { return Math.max(1, num("TireLeakSeconds", 30)); }
    static boolean runFlat() { return bool("RunFlat", true); }
    static double bodyDamage() { return num("BodyDamage", 3.0); }
    static double engineDamage() { return num("EngineDamage", 2.0); }
    static boolean fuelLeak() { return bool("FuelLeak", true); }
    static boolean occupants() { return bool("Occupants", true); }
    static double armorWear() { return num("ArmorWear", 1.0); }

    // ------------------------------------------------------------------ Durchschuesse (True Ballistics)

    static boolean penetration() { return bool("Penetration", true); }
    static boolean penetrationZombies() { return bool("PenetrationZombies", true); }
    static boolean penetrationPlayers() { return bool("PenetrationPlayers", false); }
    static double penetrationScale() { return num("PenetrationScale", 1.0); }
    static int maxPenetrations() { return Math.max(0, integer("MaxPenetrations", 3)); }
    static double energyLoss() { return num("EnergyLoss", 25) / 100.0; }
    static double deflectionScale() { return num("DeflectionScale", 1.0); }
    static boolean ricochets() { return bool("Ricochets", true); }
    static double ricochetScale() { return num("RicochetScale", 1.0); }
    static double ricochetAngle() { return num("RicochetAngle", 20); }
    static double resistance(String material, double def) { return Math.max(0, num("J_" + material, def)); }

    private Cfg() {}
}
