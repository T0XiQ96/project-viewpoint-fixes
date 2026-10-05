package vpveh;

import me.zed_0xff.zombie_buddy.Exposer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.IsoPlayer;

/** Lua-Bruecke. Name je Fassung (Build.LUA): VehicleBallisticsVF bzw. VehicleBallistics2D. */
@Exposer.LuaClass(name = Build.LUA)
public class LuaApi {
    /** Laeuft diese Fassung (die andere ist nicht geladen oder hat Vorrang)? */
    public static boolean isActive() {
        return Main.active() && Cfg.enabled();
    }

    /** Server: Angaben eines Clients zu seinem Fahrzeugtreffer. */
    public static void detail(IsoPlayer player, KahluaTable args) {
        if (Main.active()) Shot.detail(player, args);
    }

    /** Einzelspieler: naechste Meldung (Treffer-Info / Wunde) oder nil. */
    public static KahluaTable poll() {
        return Net.poll();
    }

    /** Server/Einzelspieler, jeden Tick: Luftverlust und Spritverlust. */
    public static void tick() {
        if (Main.active()) Leaks.tick();
    }

    /** Client: Wunde am eigenen Charakter (vom Server geschickt). */
    public static void wound(IsoPlayer player, double bodyPart, double damage) {
        Occupants.applyWound(player, (int) bodyPart, (float) damage);
    }

    /**
     * Fuer Effekt-Mods (Bullet Impacts 2D): Material am Fahrzeug fuer eine Kugel a -> b (Welt, z in Stockwerken).
     * "Metal_Light", "Glass", "Glass_Solid", "Rubber", "Metal_Solid", "Glass_Light", "Metal", "open" oder nil (kein Fahrzeug).
     */
    public static String vehicleMaterial(double ax, double ay, double az, double bx, double by, double bz) {
        float[] e = Effects.classifyRay(ax, ay, az, bx, by, bz, Power.MEDIUM_J);
        if (e == null) return null;
        return switch ((int) e[9]) {
            case Effects.GLASS -> "Glass";
            case Effects.GLASS_ARMORED -> "Glass_Solid";
            case Effects.RUBBER -> "Rubber";
            case Effects.ARMOR -> "Metal_Solid";
            case Effects.LAMP -> "Glass_Light";
            case Effects.METAL -> "Metal";
            case Effects.OPEN -> "open";
            default -> "Metal_Light";
        };
    }

    // ------------------------------------------------------------------ Aus dem Fahrzeug schiessen

    private static int vtbState;    // 0 unbekannt, 1 da, 2 fehlt

    /** Gibt es Viewpoint True Ballistics (noetig fuer Schuesse aus dem Fahrzeug)? */
    public static boolean driveByAvailable() {
        if (vtbState == 0) {
            try {
                Class.forName("vtb.projectile.BallisticsManager", false, LuaApi.class.getClassLoader());
                vtbState = 1;
            } catch (Throwable t) {
                vtbState = 2;
            }
        }
        return vtbState == 1 && Main.active();
    }

    /** Schuss aus dem Fahrzeug: Ziel (2D) oder Kamera-Blickrichtung (view = true, 3D). true = Schuss entstanden. */
    public static boolean driveBy(IsoPlayer player, double tx, double ty, double tz, boolean view, double spread) {
        if (!driveByAvailable()) return false;
        try {
            return DriveByVtb.fire(player, tx, ty, tz, view, spread);
        } catch (Throwable t) {
            System.err.println(Build.TAG + " drive-by shot failed: " + t);
            t.printStackTrace();
            return false;
        }
    }

    /** Server: Schuss aus dem eigenen Fahrzeug -> eigenes Fenster/Blech. */
    public static void selfHit(IsoPlayer player, KahluaTable args) {
        if (Main.active()) DriveBy.selfHit(player, args);
    }

    /** Seitenfenster eines Fahrzeugs kurbelbar machen (Anzahl geaenderter Fenster). */
    public static int makeOpenable(Object vehicle) {
        return vehicle instanceof zombie.vehicles.BaseVehicle v ? DriveBy.makeOpenable(v) : 0;
    }

    /** Diagnose: wie die Mod ein Fahrzeug sieht (Konsole). */
    public static String describe(Object vehicle) {
        return Effects.describe(vehicle);
    }
}
