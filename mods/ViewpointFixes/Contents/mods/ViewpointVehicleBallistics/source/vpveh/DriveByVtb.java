package vpveh;

import java.util.List;

import org.joml.Vector3f;

import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.vehicles.BaseVehicle;

/**
 * Schuss aus dem Fahrzeug ueber Viewpoint True Ballistics: eine echte VTB-Kugel (Schaden, Netzwerk, Server-Pruefung
 * wie bei jedem Schuss), nur Startpunkt und Richtung kommen von hier:
 *  - 3D-Sicht (Viewpoint): Blickrichtung der Kamera
 *  - 2D: vom Auge zum Mauszeiger
 * Zuerst trifft die Kugel das eigene Fahrzeug an der Austrittsstelle (geschlossenes Fenster: Loch/zersplittert;
 * offenes Fenster: nichts). Nur geladen, wenn VTB da ist (Aufruf aus Lua, geprueft mit LuaApi.driveByAvailable).
 */
final class DriveByVtb {
    // Ueberschreibung fuer AimProvider.solve (Spiel-Thread, nur waehrend spawnShot)
    static boolean overriding;
    static double mx, my, mu, dx, dy, du;
    static double spreadFactor = 1;

    static boolean available() {
        try {
            Class.forName("vtb.projectile.BallisticsManager", false, DriveByVtb.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** true, wenn ein Schuss entstanden ist. tx/ty/tz: Ziel in der Welt (2D), view = Kamera-Blickrichtung (3D). */
    static boolean fire(IsoPlayer p, double tx, double ty, double tz, boolean view, double spread) {
        if (p == null || !p.isLocalPlayer()) return false;
        BaseVehicle v = p.getVehicle();
        if (v == null || !(p.getPrimaryHandItem() instanceof HandWeapon w) || !w.isAimedFirearm()) return false;
        Vector3f eye = new Vector3f();
        if (!DriveBy.eye(p, v, eye)) return false;
        double ex = eye.x, ey = eye.y, eu = eye.z * Geometry.LEVEL;
        double ddx, ddy, ddu;
        if (view && vtb.aiming.ViewpointBridge.available) {
            vtb.V3 d = new vtb.V3();
            vtb.aiming.ViewpointBridge.direction(vtb.aiming.ViewpointBridge.yaw(), vtb.aiming.ViewpointBridge.pitch(), d);
            vtb.V3 cam = new vtb.V3();
            if (vtb.aiming.ViewpointBridge.camera(cam) && Math.hypot(cam.x - ex, cam.y - ey) < 1.2) { ex = cam.x; ey = cam.y; eu = cam.u; }
            ddx = d.x; ddy = d.y; ddu = d.u;
        } else {
            ddx = tx - ex; ddy = ty - ey; ddu = tz * Geometry.LEVEL - eu;
        }
        double l = Math.sqrt(ddx * ddx + ddy * ddy + ddu * ddu);
        if (l < 1e-4) return false;
        ddx /= l; ddy /= l; ddu /= l;

        double[] m = Power.muzzle(w);
        boolean authority = !GameClient.client;
        DriveBy.Exit exit = DriveBy.exit(v, ex, ey, eu, ddx, ddy, ddu, m[0], authority, Power.classOf(m[0], (int) m[1]),
            Power.factor(m[0]), (int) m[1]);
        if (GameClient.client && exit.own != null && !Model.OPEN.equals(exit.own.kind)) DriveBy.sendSelfHit(p, v, ddx, ddy, ddu);
        if (exit.own != null) {
            Effects.remember(v, exit.own);
            if (authority && exit.own.part != null) Net.hitInfo(p, v, exit.own);
        }

        // VTB-Schuss (zaehlt auch dann als Schuss, wenn die Kugel im eigenen Fahrzeug stecken bleibt)
        if (GameClient.client && p.getNetworkCharacterAI() instanceof zombie.characters.NetworkPlayerAI ai) ai.onShot();
        mx = exit.x; my = exit.y; mu = exit.u;
        dx = ddx; dy = ddy; du = ddu;
        spreadFactor = spread;
        overriding = true;
        vtb.projectile.ShotGroup g;
        try {
            g = vtb.projectile.BallisticsManager.INSTANCE.spawnShot(p, w, false);
        } finally {
            overriding = false;
        }
        if (g == null) return false;
        // Energieverlust am eigenen Fenster/Blech: alle Kugeln dieses Schusses langsamer, oder gar keine Kugel
        double f = Math.sqrt(Math.max(0, exit.energyFactor));
        List<vtb.projectile.BallisticProjectile> list = vtb.projectile.BallisticsManager.INSTANCE.activeProjectiles();
        for (vtb.projectile.BallisticProjectile pr : list) {
            if (pr.group != g) continue;
            if (f <= 0.01) {
                pr.alive = false;
                g.pelletsAlive--;
            } else if (f < 0.999) {
                pr.velocity.set(pr.velocity.x * f, pr.velocity.y * f, pr.velocity.u * f);
            }
            pr.addIgnore(p);
        }
        return true;
    }

    /** AimProvider.solve, danach: Startpunkt/Richtung fuer den Drive-by-Schuss einsetzen. */
    static boolean afterSolve(Object solution, boolean ok) {
        if (!overriding || !(solution instanceof vtb.aiming.AimSolution s)) return ok;
        s.muzzle.set(mx, my, mu);
        s.camera.set(mx, my, mu);
        s.forward.set(dx, dy, du);
        s.direction.set(dx, dy, du);
        s.aimPoint.set(mx + dx * 50, my + dy * 50, mu + du * 50);
        s.muzzleBlocked = false;
        s.aimHit = false;
        s.sigma *= Math.max(0.1, spreadFactor);
        return true;
    }

    private DriveByVtb() {}
}
