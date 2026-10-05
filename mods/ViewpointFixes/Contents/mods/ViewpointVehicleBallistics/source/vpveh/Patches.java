package vpveh;

import me.zed_0xff.zombie_buddy.Patch;

/** ZombieBuddy-Patches. Alle Aufrufe fangen ihre Fehler selbst; im Zweifel laeuft das Original. */
public final class Patches {

    private static final ThreadLocal<Boolean> RESULT = new ThreadLocal<>();

    /** Fahrzeugtreffer mit einer Fernkampfwaffe (Client, Server, Einzelspieler). */
    @Patch(className = "zombie.vehicles.BaseVehicle", methodName = "processHit")
    public static class ProcessHit {
        @Patch.OnEnter(skipOn = true)
        public static boolean enter(@Patch.This Object vehicle, @Patch.Argument(0) Object shooter, @Patch.Argument(1) Object weapon,
                                    @Patch.Argument(2) float damage) {
            RESULT.remove();
            if (!(weapon instanceof zombie.inventory.types.HandWeapon w) || !w.isRanged()) return false;
            if (!(vehicle instanceof zombie.vehicles.BaseVehicle v) || !(shooter instanceof zombie.characters.IsoGameCharacter c)) return false;
            Boolean r = Shot.rangeHit(v, c, w, damage);
            if (r == null) return false;
            RESULT.set(r);
            return true;
        }

        @Patch.OnExit
        public static void exit(@Patch.Return(readOnly = false) boolean hit) {
            Boolean r = RESULT.get();
            if (r != null) {
                hit = r;
                RESULT.remove();
            }
        }
    }

    /** True Ballistics: Einschlag einer Kugel - Durchschuss, Abpraller, Fahrzeug-Modell. */
    @Patch(className = "vtb.projectile.BallisticsManager", methodName = "impact")
    public static class VtbImpact {
        @Patch.OnEnter(skipOn = true)
        public static boolean enter(@Patch.This Object manager, @Patch.Argument(0) Object projectile) {
            return VtbBridge.onImpact(manager, projectile);
        }

        @Patch.OnExit(onThrowable = Throwable.class)
        public static void exit() {
            Shot.clearVtb();
        }
    }

    /** True Ballistics (Server): Treffer ohne freie Schusslinie - durch durchschlagbare Waende erlauben. */
    @Patch(className = "vtb.network.ServerValidator", methodName = "lineOfFireProblem")
    public static class VtbLineOfFire {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object shooter, @Patch.Argument(1) Object target,
                                @Patch.Return(readOnly = false) String problem) {
            problem = VtbBridge.allowThroughWalls(shooter, target, problem);
        }
    }

    /** True Ballistics: Zielloesung - beim Schuss aus dem Fahrzeug Startpunkt und Richtung von DriveByVtb. */
    @Patch(className = "vtb.aiming.AimProvider", methodName = "solve")
    public static class VtbAim {
        @Patch.OnExit
        public static void exit(@Patch.Argument(8) Object solution, @Patch.Return(readOnly = false) boolean ok) {
            if (DriveByVtb.overriding) ok = DriveByVtb.afterSolve(solution, ok);
        }
    }

    private Patches() {}
}
