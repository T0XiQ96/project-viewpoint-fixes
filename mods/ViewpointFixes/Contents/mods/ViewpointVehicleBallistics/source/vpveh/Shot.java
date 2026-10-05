package vpveh;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.joml.Vector3f;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.physics.BallisticsController;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector3;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

/**
 * Ein Fahrzeugtreffer von Anfang bis Ende.
 *
 * Woher die Flugbahn kommt:
 *  - True Ballistics: Einschlagpunkt und Geschwindigkeit der echten Kugel (VtbBridge setzt sie vor processHit).
 *  - sonst (2D/vanilla): Muendung und Blickrichtung wie im Spiel selbst.
 *  - Server: die Angaben, die der Client des Schuetzen kurz vorher geschickt hat (geprueft), sonst Blickrichtung.
 *
 * Wer Schaden macht: Einzelspieler und Server. Ein Mehrspieler-Client bestimmt nur das Teil, schickt die Angaben an
 * den Server und meldet "getroffen", damit das Spiel das Trefferpaket sendet.
 */
final class Shot {
    // ------------------------------------------------------------------ True-Ballistics-Kontext (Spiel-Thread)

    private static BaseVehicle vtbVehicle;
    private static double vx, vy, vu, vdx, vdy, vdu, vEnergy;
    private static long vtbAt;
    static Model.Result lastResult;

    static void setVtb(BaseVehicle v, double x, double y, double u, double dx, double dy, double du, double energy) {
        vtbVehicle = v;
        vx = x; vy = y; vu = u; vdx = dx; vdy = dy; vdu = du; vEnergy = energy;
        vtbAt = System.nanoTime();
    }

    /** Nach BallisticsManager.impact: Kontext gilt nur fuer diesen Aufruf. */
    static void clearVtb() {
        vtbVehicle = null;
    }

    // ------------------------------------------------------------------ Angaben vom Client (Server)

    private record Pending(int shooter, short vehicle, double ax, double ay, double az, double bx, double by, double bz,
                           double energy, boolean vtb, long at) {}

    private static final ArrayDeque<Pending> pending = new ArrayDeque<>();

    /** Lua (Server): Angaben eines Clients zu seinem Treffer. */
    static void detail(IsoPlayer player, KahluaTable t) {
        if (player == null || t == null) return;
        try {
            short vid = (short) num(t, "v");
            BaseVehicle v = zombie.vehicles.VehicleManager.instance.getVehicleByID(vid);
            if (v == null) return;
            double ax = num(t, "ax"), ay = num(t, "ay"), az = num(t, "az"), bx = num(t, "bx"), by = num(t, "by"), bz = num(t, "bz");
            // Plausibel? Start beim Schuetzen, Ziel beim Fahrzeug
            if (dist2(ax, ay, player.getX(), player.getY()) > 9 || Math.abs(az - player.getZ()) > 1.5) return;
            if (dist2(bx, by, v.getX(), v.getY()) > 64) return;
            HandWeapon w = player.getPrimaryHandItem() instanceof HandWeapon hw ? hw : null;
            double maxE = w == null ? 4000 : Power.muzzle(w)[0] * 1.05;
            double e = Math.max(0, Math.min(maxE, num(t, "e")));
            synchronized (pending) {
                pending.addLast(new Pending(player.getOnlineID(), vid, ax, ay, az, bx, by, bz, e, t.rawget("vtb") == Boolean.TRUE, System.nanoTime()));
                while (pending.size() > 64) pending.removeFirst();
            }
        } catch (Throwable ignored) {
        }
    }

    private static double num(KahluaTable t, String k) {
        Object o = t.rawget(k);
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    private static double dist2(double ax, double ay, double bx, double by) {
        return (ax - bx) * (ax - bx) + (ay - by) * (ay - by);
    }

    private static Pending take(IsoGameCharacter shooter, BaseVehicle v) {
        if (!(shooter instanceof IsoPlayer p)) return null;
        long now = System.nanoTime();
        synchronized (pending) {
            Iterator<Pending> it = pending.iterator();
            while (it.hasNext()) {
                Pending d = it.next();
                if (now - d.at() > 2_000_000_000L) { it.remove(); continue; }
                if (d.shooter() == p.getOnlineID() && d.vehicle() == v.getId()) {
                    it.remove();
                    return d;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Treffer

    /**
     * BaseVehicle.processRangeHit (Fernkampf). null = Spiel macht es selbst, sonst unser Ergebnis.
     */
    static Boolean rangeHit(BaseVehicle v, IsoGameCharacter shooter, HandWeapon w, float damage) {
        if (!Main.active() || !Cfg.enabled() || v == null || shooter == null || w == null) return null;
        try {
            double ax, ay, az, bx, by, bz, energy;
            boolean vtb;
            boolean fresh = vtbVehicle == v && System.nanoTime() - vtbAt < 500_000_000L;
            double[] muzzle = Power.muzzle(w);
            if (GameServer.server) {
                Pending d = take(shooter, v);
                if (d != null) {
                    ax = d.ax(); ay = d.ay(); az = d.az(); bx = d.bx(); by = d.by(); bz = d.bz();
                    energy = d.energy();
                    vtb = d.vtb();
                } else {
                    double ang = shooter.getLookAngleRadians();
                    ax = shooter.getX(); ay = shooter.getY(); az = shooter.getZ() + 1.3 / Geometry.LEVEL;
                    double r = w.getMaxRange() * 1.5 + 2;
                    bx = ax + Math.cos(ang) * r; by = ay + Math.sin(ang) * r; bz = az;
                    energy = muzzle[0];
                    vtb = false;
                }
            } else if (fresh) {
                double l = Math.sqrt(vdx * vdx + vdy * vdy + vdu * vdu);
                if (l < 1e-6) return null;
                double ux = vdx / l, uy = vdy / l, uu = vdu / l;
                ax = vx - ux * 1.5; ay = vy - uy * 1.5; az = (vu - uu * 1.5) / Geometry.LEVEL;
                bx = vx + ux * 1.0; by = vy + uy * 1.0; bz = (vu + uu * 1.0) / Geometry.LEVEL;
                energy = vEnergy;
                vtb = true;
            } else {
                // wie das Spiel: Muendung + Blickrichtung
                double ang = shooter.getLookAngleRadians();
                ax = shooter.getX(); ay = shooter.getY(); az = shooter.getZ() + 1.3 / Geometry.LEVEL;
                try {
                    BallisticsController bc = shooter.getBallisticsController();
                    Vector3 m = bc == null ? null : bc.getMuzzlePosition();
                    if (m != null && !Float.isNaN(m.x) && Math.abs(m.x - ax) < 3) { ax = m.x; ay = m.y; az = m.z; }
                } catch (Throwable ignored) {
                }
                double r = w.getMaxRange() * 1.5 + 2;
                bx = ax + Math.cos(ang) * r; by = ay + Math.sin(ang) * r; bz = az;
                energy = muzzle[0];
                vtb = false;
            }
            if (!vtb && !Cfg.in2D()) return null;          // 2D-Schuesse nur mit Schalter

            Geometry.Hit hit = new Geometry.Hit();
            if (!Geometry.ray(v, ax, ay, az, bx, by, bz, hit)) return Boolean.FALSE;     // knapp vorbei
            Vector3f dirLocal = new Vector3f();
            Geometry.dirToLocal(v, bx - ax, by - ay, (bz - az) * Geometry.LEVEL, dirLocal);
            Model.Result r = Model.classify(v, hit, dirLocal, energy);

            if (GameClient.client) {
                Net.sendDetail(shooter, v, ax, ay, az, bx, by, bz, energy, vtb);
                lastResult = estimate(r);
                Effects.remember(v, r);
                return Boolean.TRUE;
            }
            int cls = Power.classOf(energy, (int) muzzle[1]);
            Model.apply(v, r, cls, Power.factor(energy), (int) muzzle[1]);
            lastResult = r;
            Effects.remember(v, r);
            if (shooter instanceof IsoPlayer p) Net.hitInfo(p, v, r);
            try { zombie.CombatManager.getInstance().processMaintenanceCheck(shooter, w, v); } catch (Throwable ignored) {}
            return Boolean.TRUE;
        } catch (Throwable t) {
            System.err.println(Build.TAG + " vehicle hit failed, using vanilla: " + t);
            t.printStackTrace();
            return null;
        }
    }

    /** Mehrspieler-Client: Durchschuss nur schaetzen (Schaden macht der Server). */
    private static Model.Result estimate(Model.Result r) {
        r.energyLeft = 0;
        switch (r.kind) {
            case Model.OPEN -> { r.penetrated = true; r.energyLeft = r.energyIn * 0.98; }
            case Model.GLASS -> { if (!r.armoredGlass) { r.penetrated = true; r.energyLeft = r.energyIn * 0.88; } }
            case Model.BODY, Model.ROOF -> {
                double need = Penetration.resistance("VehicleBody");
                if (r.energyIn > need) { r.penetrated = true; r.energyLeft = (r.energyIn - need) * (1 - Cfg.energyLoss() * 0.5); }
            }
            default -> {}
        }
        return r;
    }

    private Shot() {}
}
