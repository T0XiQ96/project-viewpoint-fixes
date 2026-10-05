package vpveh;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import zombie.inventory.InventoryItem;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * Luftverlust (Reifen) und Spritverlust (Tank) nach Treffern, auf dem Server bzw. im Einzelspieler.
 * Endet, wenn leer, das Teil ausgebaut/ersetzt oder das Fahrzeug weg ist.
 */
final class Leaks {
    private static final class Leak {
        final WeakReference<BaseVehicle> vehicle;
        final String part;
        final boolean tire;
        final InventoryItem item;
        double perSecond;          // Reifen: Anteil der Kapazitaet pro Sekunde; Tank: Liter pro Sekunde
        double sinceSend;

        Leak(BaseVehicle v, VehiclePart p, boolean tire, double perSecond) {
            this.vehicle = new WeakReference<>(v);
            this.part = p.getId();
            this.tire = tire;
            this.item = p.getInventoryItem();
            this.perSecond = perSecond;
        }
    }

    private static final List<Leak> leaks = new ArrayList<>();
    private static long last;

    /** strength: Reifen 1 = ein grosses Loch (platt nach TireLeakSeconds), Tank: Liter/s. */
    static void add(BaseVehicle v, VehiclePart p, double strength, boolean tire) {
        if (v == null || p == null) return;
        double rate = tire ? strength / Cfg.tireLeakSeconds() : strength;
        synchronized (leaks) {
            for (Leak l : leaks) {
                if (l.vehicle.get() == v && l.part.equals(p.getId())) {
                    l.perSecond += rate;            // weiteres Loch: schneller
                    return;
                }
            }
            leaks.add(new Leak(v, p, tire, rate));
        }
    }

    static void tick() {
        long now = System.nanoTime();
        if (last == 0) { last = now; return; }
        double dt = (now - last) / 1e9;
        if (dt < 0.5) return;
        last = now;
        dt = Math.min(dt, 5);
        synchronized (leaks) {
            for (int i = leaks.size() - 1; i >= 0; i--) {
                Leak l = leaks.get(i);
                BaseVehicle v = l.vehicle.get();
                VehiclePart p = v == null ? null : Model.byId(v, l.part);
                if (p == null || p.getInventoryItem() != l.item || p.getInventoryItem() == null) { leaks.remove(i); continue; }
                float cap = Math.max(1, p.getContainerCapacity());
                float amount = p.getContainerContentAmount();
                float loss = (float) (l.tire ? l.perSecond * cap * dt : l.perSecond * dt);
                float next = Math.max(0, amount - loss);
                l.sinceSend += dt;
                boolean done = next <= 0.01f;
                try {
                    p.setContainerContentAmount(next, true, false);
                    if (l.tire && p.getWheelIndex() >= 0) v.setTireInflation(p.getWheelIndex(), next / cap);
                    if (done || l.sinceSend >= 2) {
                        l.sinceSend = 0;
                        v.transmitPartModData(p);
                    }
                } catch (Throwable t) {
                    leaks.remove(i);
                    continue;
                }
                if (done) leaks.remove(i);
            }
        }
    }

    private Leaks() {}
}
