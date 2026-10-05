package vpveh;

import java.lang.reflect.Field;
import java.util.Locale;

import org.joml.Vector3f;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleParts;
import zombie.vehicles.VehicleWindow;

/**
 * Aus dem Fahrzeug schiessen (ohne True-Ballistics-Teil; der steckt in DriveByVtb).
 *
 *  - Augenpunkt im Sitz, Austrittspunkt aus dem eigenen Fahrzeug, eigenes Fenster/Blech treffen
 *  - Seitenfenster ohne Kurbel-Funktion (Mod-Fahrzeuge) koennen per Sandbox kurbelbar gemacht werden
 */
final class DriveBy {

    /** Ergebnis der Vorbereitung: Start ausserhalb des Fahrzeugs, Richtung, Energie-Anteil (0 = Kugel bleibt drin). */
    static final class Exit {
        double x, y, u;            // Meter (u = Hoehe)
        double dx, dy, du;         // Einheitsvektor
        double energyFactor = 1;
        Model.Result own;          // getroffenes eigenes Teil (Fenster, Tuer ...) oder null
    }

    /** Augenpunkt des Sitzes in Weltkoordinaten (x, y, z in Stockwerken). */
    static boolean eye(IsoPlayer p, BaseVehicle v, Vector3f out) {
        int seat = v.getSeat(p);
        if (seat < 0) return false;
        Vector3f local = new Vector3f();
        v.getPassengerLocalPos(seat, local);
        local.y += 0.62f;
        Geometry.toWorld(v, local, out);
        return !Float.isNaN(out.x);
    }

    /**
     * Strahl vom Auge (ex, ey, eu Meter) in Richtung d: wo verlaesst er das eigene Fahrzeug, was wird dort getroffen.
     * apply = Schaden am eigenen Fahrzeug machen (Einzelspieler/Server).
     */
    static Exit exit(BaseVehicle v, double ex, double ey, double eu, double dx, double dy, double du, double energy, boolean apply,
                     int powerClass, double pf, int projectiles) {
        Exit e = new Exit();
        double l = Math.sqrt(dx * dx + dy * dy + du * du);
        dx /= l; dy /= l; du /= l;
        e.dx = dx; e.dy = dy; e.du = du;
        e.x = ex; e.y = ey; e.u = eu;
        Geometry.Hit hit = new Geometry.Hit();
        double far = 8;
        // von aussen zurueck zum Auge: erster Treffer = Austrittsstelle
        if (!Geometry.ray(v, ex + dx * far, ey + dy * far, (eu + du * far) / Geometry.LEVEL, ex, ey, eu / Geometry.LEVEL, hit)) return e;
        Vector3f w = new Vector3f();
        Geometry.toWorld(v, hit.local, w);
        e.x = w.x + dx * 0.08; e.y = w.y + dy * 0.08; e.u = w.z * Geometry.LEVEL + du * 0.08;
        Vector3f dl = new Vector3f();
        Geometry.dirToLocal(v, -dx, -dy, -du, dl);       // "von aussen" eingeordnet: gleiche Regeln wie ein Treffer von aussen
        Model.Result r = Model.classify(v, hit, dl, energy);
        e.own = r;
        switch (r.kind) {
            case Model.OPEN -> e.energyFactor = 1;
            case Model.GLASS -> {
                if (r.armoredGlass) e.energyFactor = 0;
                else e.energyFactor = r.laminated ? 0.85 : 0.9;
            }
            case Model.ARMOR, Model.ENGINE, Model.FUEL, Model.TIRE -> e.energyFactor = 0;
            default -> {
                double need = Penetration.resistance("VehicleBody");
                e.energyFactor = energy > need ? (energy - need) / energy : 0;
            }
        }
        if (apply && !r.kind.equals(Model.OPEN)) {
            r.self = true;
            Model.apply(v, r, powerClass, pf, projectiles);
        }
        return e;
    }

    /** Server: der Client meldet einen Schuss aus seinem Fahrzeug -> eigenes Fenster/Blech beschaedigen. */
    static void selfHit(IsoPlayer p, KahluaTable t) {
        if (p == null || t == null) return;
        BaseVehicle v = p.getVehicle();
        if (v == null || !(t.rawget("v") instanceof Double id) || id.shortValue() != v.getId()) return;
        if (!(p.getPrimaryHandItem() instanceof HandWeapon w)) return;
        Vector3f eye = new Vector3f();
        if (!eye(p, v, eye)) return;
        double dx = num(t, "dx"), dy = num(t, "dy"), du = num(t, "du");
        if (dx * dx + dy * dy + du * du < 1e-6) return;
        double[] m = Power.muzzle(w);
        exit(v, eye.x, eye.y, eye.z * Geometry.LEVEL, dx, dy, du, m[0], true, Power.classOf(m[0], (int) m[1]), Power.factor(m[0]), (int) m[1]);
    }

    private static double num(KahluaTable t, String k) {
        return t.rawget(k) instanceof Number n ? n.doubleValue() : 0;
    }

    static void sendSelfHit(IsoPlayer p, BaseVehicle v, double dx, double dy, double du) {
        KahluaTable t = Net.table();
        t.rawset("v", (double) v.getId());
        t.rawset("dx", dx); t.rawset("dy", dy); t.rawset("du", du);
        GameClient.instance.sendClientCommand(p, Build.MODULE, "selfhit", t);
    }

    // ------------------------------------------------------------------ Fenster kurbelbar machen

    private static Field openable;

    /** Seitenfenster ohne Kurbel-Funktion kurbelbar machen (nur Tuerfenster, keine Windschutzscheiben). */
    static int makeOpenable(BaseVehicle v) {
        if (v == null) return 0;
        int n = 0;
        try {
            if (openable == null) {
                openable = VehicleWindow.class.getDeclaredField("openable");
                openable.setAccessible(true);
            }
            VehicleParts ps = v.getParts();
            for (int i = 0; ps != null && i < ps.getPartCount(); i++) {
                VehiclePart p = ps.getPartByIndex(i);
                VehicleWindow w = p == null ? null : p.getWindow();
                if (w == null || w.isOpenable()) continue;
                String id = p.getId().toLowerCase(Locale.ROOT);
                if (!id.startsWith("window") || id.contains("windshield") || id.contains("windscreen")) continue;
                if (!(id.contains("left") || id.contains("right"))) continue;
                if (p.getParent() == null || p.getParent().getDoor() == null) continue;       // nur Fenster in Tueren
                openable.setBoolean(w, true);
                n++;
            }
        } catch (Throwable t) {
            System.err.println(Build.TAG + " making windows openable failed: " + t);
        }
        return n;
    }

    private DriveBy() {}
}
