package vpveh;

import se.krka.kahlua.vm.KahluaTable;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * Einschussloecher, die am Fahrzeug bleiben: in der ModData des getroffenen Teils ("vvbHoles"), in
 * Fahrzeug-Koordinaten. Wird mit dem Teil gespeichert und an alle Clients geschickt; ViewpointEffects3D zeichnet sie
 * (Glas: Loch mit Sprung, Blech: Loch mit blankem Rand). Sie verschwinden, wenn die Scheibe zerspringt, das Teil
 * ersetzt oder auf 100 % repariert wird.
 */
final class Holes {
    static final String KEY = "vvbHoles";
    static final int MAX_PER_PART = 24;

    static VehiclePart carrier(BaseVehicle v, Model.Result r) {
        if (r.part != null) return r.part;
        VehiclePart e = Model.byId(v, "Engine");
        if (e != null) return e;
        return v.getParts() != null && v.getParts().getPartCount() > 0 ? v.getParts().getPartByIndex(0) : null;
    }

    static void add(BaseVehicle v, Model.Result r) {
        if (r.kind.equals(Model.OPEN) || r.material == null) return;
        if (r.kind.equals(Model.GLASS) && "shattered".equals(r.state)) return;
        VehiclePart p = carrier(v, r);
        if (p == null) return;
        try {
            KahluaTable md = p.getModData();
            KahluaTable list = md.rawget(KEY) instanceof KahluaTable t ? t : null;
            if (list == null) {
                list = Net.table();
                md.rawset(KEY, list);
            }
            int n = list.len();
            if (n >= MAX_PER_PART) {
                // aelteste raus
                for (int i = 1; i < n; i++) list.rawset(i, list.rawget(i + 1));
                list.rawset(n, null);
                n--;
            }
            KahluaTable h = Net.table();
            h.rawset("x", (double) r.local.x); h.rawset("y", (double) r.local.y); h.rawset("z", (double) r.local.z);
            h.rawset("nx", (double) r.normal.x); h.rawset("ny", (double) r.normal.y); h.rawset("nz", (double) r.normal.z);
            h.rawset("s", Math.min(3.0, Power.factor(r.energyIn)));
            h.rawset("m", r.kind.equals(Model.ARMOR) ? "armor" : r.kind.equals(Model.GLASS) ? (r.armoredGlass ? "glass_armored" : "glass") : r.material);
            list.rawset(n + 1, h);
            md.rawset("vvbItem", p.getInventoryItem() == null ? -1.0 : (double) p.getInventoryItem().getID());
            v.transmitPartModData(p);
        } catch (Throwable t) {
            System.err.println(Build.TAG + " storing a bullet hole failed: " + t);
        }
    }

    static void clearPart(BaseVehicle v, String partId) {
        VehiclePart p = Model.byId(v, partId);
        if (p == null) return;
        try {
            p.getModData().rawset(KEY, null);
            p.getModData().rawset("vvbTire", null);
            v.transmitPartModData(p);
        } catch (Throwable ignored) {
        }
    }

    private Holes() {}
}
