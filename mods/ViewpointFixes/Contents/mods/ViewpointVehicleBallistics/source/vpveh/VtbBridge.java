package vpveh;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.joml.Vector3f;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.iso.IsoGridSquare;
import zombie.vehicles.BaseVehicle;

/**
 * Anbindung an Viewpoint True Ballistics (nur geladen, wenn VTB da ist: alle Aufrufe kommen aus Patches auf VTB-Klassen).
 * Durchschuss-Regeln und Materialien: siehe Penetration.
 */
final class VtbBridge {

    // ------------------------------------------------------------------ VTB-Anbindung

    private static volatile boolean failed;
    private static Field resField;
    private static Method worldImpact, finish, tracer;
    private static final Set<Object> penetrated = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object, Integer> count = new WeakHashMap<>();

    private static boolean init(Object manager) {
        if (resField != null) return true;
        if (failed) return false;
        try {
            Class<?> c = manager.getClass();
            resField = c.getDeclaredField("res");
            resField.setAccessible(true);
            Class<?> p = Class.forName("vtb.projectile.BallisticProjectile", true, c.getClassLoader());
            Class<?> v3 = Class.forName("vtb.V3", true, c.getClassLoader());
            worldImpact = c.getDeclaredMethod("worldImpact", p, v3, int.class, int.class, int.class, int.class);
            worldImpact.setAccessible(true);
            finish = c.getDeclaredMethod("finish", p);
            finish.setAccessible(true);
            Class<?> sq = IsoGridSquare.class;
            tracer = c.getDeclaredMethod("tracer", p, sq);
            tracer.setAccessible(true);
            System.out.println(Build.TAG + " True Ballistics found: penetration and vehicle model active");
            return true;
        } catch (Throwable t) {
            failed = true;
            System.err.println(Build.TAG + " True Ballistics bridge disabled: " + t);
            return false;
        }
    }

    /** Echte Energie in J: VTB fliegt mit VelocityScale (Standard 0.4), Widerstaende sind in echten Joule. */
    static double realEnergy(vtb.projectile.BallisticProjectile p) {
        double s = Math.max(0.05, vtb.config.BallisticsConfig.velocityScale);
        return p.energy() / (s * s);
    }

    static boolean wasPenetrated(Object projectile) { return projectile != null && penetrated.contains(projectile); }

    /**
     * BallisticsManager.impact(projectile), vor dem Original. true = selbst erledigt (Original ueberspringen).
     */
    static boolean onImpact(Object manager, Object projectile) {
        if (!Main.active() || manager == null || projectile == null || !init(manager)) return false;
        try {
            vtb.projectile.BallisticProjectile p = (vtb.projectile.BallisticProjectile) projectile;
            vtb.collision.ProjectileCollision.Result res = (vtb.collision.ProjectileCollision.Result) resField.get(manager);
            if (p.group == null || p.group.test) return false;
            switch (res.kind) {
                case vtb.collision.ProjectileCollision.VEHICLE -> {
                    Shot.setVtb(res.vehicle, res.point.x, res.point.y, res.point.u, p.velocity.x, p.velocity.y, p.velocity.u, realEnergy(p));
                    if (!Cfg.enabled() || !Cfg.penetration()) return false;     // Original + unser Modell ueber processHit
                    return vehicle(manager, p, res);
                }
                case vtb.collision.ProjectileCollision.CHARACTER -> {
                    if (!penetrated.contains(p)) return false;
                    boolean player = res.character instanceof IsoPlayer;
                    boolean zombie = res.character instanceof IsoZombie;
                    if ((player && !Cfg.penetrationPlayers()) || (zombie && !Cfg.penetrationZombies())) {
                        p.addIgnore(res.character);      // fliegt vorbei
                        return true;
                    }
                    return false;
                }
                case vtb.collision.ProjectileCollision.WORLD -> {
                    if (!Cfg.penetration()) return false;
                    return world(manager, p, res);
                }
                default -> { return false; }
            }
        } catch (Throwable t) {
            failed = true;
            System.err.println(Build.TAG + " penetration disabled after an error: " + t);
            t.printStackTrace();
            return false;
        }
    }

    private static boolean world(Object manager, vtb.projectile.BallisticProjectile p, vtb.collision.ProjectileCollision.Result res) throws Exception {
        int kind = res.world.kind;
        boolean tree = kind == vtb.collision.WorldCollision.HIT_TREE;
        if (!(kind == vtb.collision.WorldCollision.HIT_WALL || kind == vtb.collision.WorldCollision.HIT_LOW
            || kind == vtb.collision.WorldCollision.HIT_FURNITURE || tree)) return false;
        int done = count.getOrDefault(p, 0);
        double speed = p.speed();
        if (speed < 1e-3) return false;
        double dx = p.velocity.x / speed, dy = p.velocity.y / speed, du = p.velocity.u / speed;
        String material = tree ? "Tree" : Penetration.materialAt(res.world.tileX, res.world.tileY, res.world.tileZ, res.point.x, res.point.y);
        Penetration.Mat m = Penetration.mat(material);
        // Normale (Feldkante) und Einfallswinkel
        double fx = res.point.x - Math.floor(res.point.x), fy = res.point.y - Math.floor(res.point.y);
        double nx, ny;
        if (Math.min(fx, 1 - fx) < Math.min(fy, 1 - fy)) { nx = dx > 0 ? -1 : 1; ny = 0; } else { nx = 0; ny = dy > 0 ? -1 : 1; }
        double cos = Math.abs(dx * nx + dy * ny);                 // 1 = senkrecht
        if (kind == vtb.collision.WorldCollision.HIT_FURNITURE || tree) cos = Math.max(cos, 0.7);
        double energy = realEnergy(p);
        double need = m.joule() * Math.max(0.01, Cfg.penetrationScale()) / Math.max(0.33, cos);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (done < Cfg.maxPenetrations() && energy * (0.85 + 0.3 * rnd.nextDouble()) > need) {
            worldImpact.invoke(manager, p, res.point, kind, res.world.tileX, res.world.tileY, res.world.tileZ);   // Eintritt: Loch, Klang
            double left = (energy - need) * (1 - Cfg.energyLoss());
            if (left <= 1) { finish.invoke(manager, p); return true; }
            double f = Math.sqrt(left / energy);
            double dev = Math.toRadians(m.deflect() * Cfg.deflectionScale()) * (rnd.nextDouble() * 2 - 1);
            double devU = Math.toRadians(m.deflect() * Cfg.deflectionScale() * 0.6) * (rnd.nextDouble() * 2 - 1);
            double c = Math.cos(dev), s = Math.sin(dev);
            double ndx = dx * c - dy * s, ndy = dx * s + dy * c, ndu = du + devU;
            double l = Math.sqrt(ndx * ndx + ndy * ndy + ndu * ndu);
            double v = speed * f;
            p.velocity.set(ndx / l * v, ndy / l * v, ndu / l * v);
            double th = m.thickness() + 0.02;
            p.position.set(res.point.x + dx * th, res.point.y + dy * th, res.point.u + du * th);
            p.previousPosition.set(p.position);
            penetrated.add(p);
            count.put(p, done + 1);
            exitEffect(p, kind, res.world.tileX, res.world.tileY, res.world.tileZ);
            return true;
        }
        // Abpraller: hartes Material, flacher Winkel
        double grazeDeg = Math.toDegrees(Math.asin(Math.min(1, cos)));
        if (Cfg.ricochets() && grazeDeg < Cfg.ricochetAngle() && rnd.nextDouble() < m.hardness() * Cfg.ricochetScale() * (1 - grazeDeg / Cfg.ricochetAngle() * 0.6)) {
            worldImpact.invoke(manager, p, res.point, kind, res.world.tileX, res.world.tileY, res.world.tileZ);
            double d = dx * nx + dy * ny;
            double rx = dx - 2 * d * nx, ry = dy - 2 * d * ny, ru = du * 0.5 + (rnd.nextDouble() - 0.5) * 0.1;
            double l = Math.sqrt(rx * rx + ry * ry + ru * ru);
            double v = speed * Math.sqrt(0.35);
            p.velocity.set(rx / l * v, ry / l * v, ru / l * v);
            p.position.set(res.point.x + nx * 0.03, res.point.y + ny * 0.03, res.point.u);
            p.previousPosition.set(p.position);
            penetrated.add(p);     // abgeprallte Kugeln gelten fuer die Spieler-Regel wie Durchschuesse
            count.put(p, done + 1);
            return true;
        }
        return false;
    }

    /** Ausschussloch ueber ViewpointEffects3D (falls geladen). */
    private static Method fxImpact;
    private static boolean fxChecked;

    private static void exitEffect(vtb.projectile.BallisticProjectile p, int kind, int tx, int ty, int tz) {
        if (zombie.network.GameServer.server) return;
        try {
            if (!fxChecked) {
                fxChecked = true;
                Class<?> c = Class.forName("vpfx.Vtb", true, Penetration.class.getClassLoader());
                for (Method m : c.getMethods()) if (m.getName().equals("afterWorldImpact")) fxImpact = m;
            }
            if (fxImpact == null) return;
            vtb.V3 back = new vtb.V3(p.position.x, p.position.y, p.position.u);
            // Richtung umdrehen: das Loch zeigt nach hinten, Splitter fliegen mit
            fxImpact.invoke(null, p, back, kind, tx, ty, tz);
        } catch (Throwable ignored) {
        }
    }

    private static boolean vehicle(Object manager, vtb.projectile.BallisticProjectile p, vtb.collision.ProjectileCollision.Result res) throws Exception {
        BaseVehicle v = res.vehicle;
        double ratio = p.speed() / Math.max(1.0, p.muzzleVelocity);
        int hitsBefore = p.group.characterHits;
        Shot.lastResult = null;
        vtb.combat.DamageBridge.hitVehicle(p.group, v, ratio);        // -> processHit -> unser Modell
        tracer.invoke(manager, p, null);
        Model.Result r = Shot.lastResult;
        Shot.lastResult = null;
        if (r == null || !r.penetrated || r.energyLeft <= 1) {
            finish.invoke(manager, p);
            return true;
        }
        // Austritt: Strahl von hinten zurueck
        double speed = p.speed();
        double dx = p.velocity.x / speed, dy = p.velocity.y / speed, du = p.velocity.u / speed;
        double fx = res.point.x + dx * 8, fy = res.point.y + dy * 8, fu = res.point.u + du * 8;
        Geometry.Hit exit = new Geometry.Hit();
        double left = r.energyLeft;
        double ex = res.point.x + dx * 0.5, ey = res.point.y + dy * 0.5, eu = res.point.u + du * 0.5;
        if (Geometry.ray(v, fx, fy, fu / Geometry.LEVEL, res.point.x, res.point.y, res.point.u / Geometry.LEVEL, exit)) {
            Vector3f w = new Vector3f();
            Geometry.toWorld(v, exit.local, w);
            ex = w.x + dx * 0.05; ey = w.y + dy * 0.05; eu = w.z * Geometry.LEVEL + du * 0.05;
            // zweite Wand: Glas oder Blech
            Geometry.Shape sh = Geometry.shape(v);
            boolean glass = sh != null && exit.local.y >= Model.beltY(v, sh);
            left -= Penetration.resistance(glass ? "Glass" : "VehicleBody");
        }
        if (left <= 1) {
            finish.invoke(manager, p);
            return true;
        }
        double f = Math.sqrt(left / Math.max(1, realEnergy(p)));
        p.velocity.set(p.velocity.x * f, p.velocity.y * f, p.velocity.u * f);
        p.position.set(ex, ey, eu);
        p.previousPosition.set(p.position);
        // Fahrzeug zaehlt bei VTB als Treffer (maxHitCount) - fuer das Ziel dahinter wieder freigeben
        p.group.characterHits = hitsBefore;
        penetrated.add(p);
        return true;
    }

    // ------------------------------------------------------------------ Server: Schusslinie durch Waende

    /**
     * Nach ServerValidator.lineOfFireProblem: blockiert -> pruefen, ob die Waende dazwischen durchschlagbar sind.
     * Gibt null zurueck, wenn der Treffer erlaubt ist.
     */
    static String allowThroughWalls(Object shooterObj, Object targetObj, String problem) {
        if (problem == null || !Main.active() || !Cfg.penetration()) return problem;
        try {
            if (!(shooterObj instanceof IsoPlayer shooter) || !(targetObj instanceof zombie.characters.IsoGameCharacter target)) return problem;
            if (target instanceof IsoPlayer && !Cfg.penetrationPlayers()) return problem;
            if (target instanceof IsoZombie && !Cfg.penetrationZombies()) return problem;
            Object item = shooter.getPrimaryHandItem();
            if (!(item instanceof zombie.inventory.types.HandWeapon w)) return problem;
            double energy = Power.muzzle(w)[0];
            vtb.collision.PzWorld world = new vtb.collision.PzWorld().bind();
            if (!world.ready()) return problem;
            vtb.collision.WorldCollision.Hit hit = new vtb.collision.WorldCollision.Hit();
            double su = shooter.getZ() * Geometry.LEVEL + 1.35, tu = target.getZ() * Geometry.LEVEL + 1.1;
            vtb.V3 a = new vtb.V3(shooter.getX(), shooter.getY(), su), b = new vtb.V3(target.getX(), target.getY(), tu);
            double dx = b.x - a.x, dy = b.y - a.y, du = b.u - a.u;
            double len = Math.sqrt(dx * dx + dy * dy + du * du);
            if (len < 1e-3) return null;
            dx /= len; dy /= len; du /= len;
            for (int i = 0; i <= Cfg.maxPenetrations(); i++) {
                if (!vtb.collision.WorldCollision.trace(a, b, world, hit, null)) return null;        // frei
                if (i == Cfg.maxPenetrations()) return problem;
                int k = hit.kind;
                if (k == vtb.collision.WorldCollision.HIT_FLOOR || k == vtb.collision.WorldCollision.HIT_CEILING
                    || k == vtb.collision.WorldCollision.HIT_VOID) return problem;
                String m = k == vtb.collision.WorldCollision.HIT_TREE ? "Tree"
                    : Penetration.materialAt(hit.tileX, hit.tileY, hit.tileZ, hit.point.x, hit.point.y);
                double need = Penetration.resistance(m);
                if (energy < need * 0.85) return problem;
                energy = (energy - need) * (1 - Cfg.energyLoss());
                Penetration.Mat mm = Penetration.mat(m);
                a.set(hit.point.x + dx * (mm.thickness() + 0.05), hit.point.y + dy * (mm.thickness() + 0.05), hit.point.u + du * (mm.thickness() + 0.05));
            }
            return problem;
        } catch (Throwable t) {
            return problem;
        }
    }

    private VtbBridge() {}
}
