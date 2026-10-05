package vpspread;

import me.zed_0xff.zombie_buddy.Patch;

/**
 * ZombieBuddy-Patches auf Viewpoint True Ballistics (VTB). Alle Typen als Object, VTB wird nur per Reflection
 * angefasst; fehlt VTB, laufen diese Patches einfach ins Leere.
 */
public final class Patches {

    /** VTB hat die Schussrichtung berechnet: Streuung im Zielkreis des Spiels statt VTBs eigener Streuung. */
    @Patch(className = "vtb.aiming.AimProvider", methodName = "solve")
    public static class Solve {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object player, @Patch.Argument(8) Object solution,
                                @Patch.Return boolean ok) {
            Spread.afterSolve(player, solution, ok);
        }
    }

    /** Ein Flugschritt einer VTB-Kugel: Buesche auf dem Weg koennen sie ablenken. */
    @Patch(className = "vtb.projectile.ProjectilePhysics", methodName = "step")
    public static class Step {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object position, @Patch.Argument(1) Object velocity,
                                @Patch.Argument(2) Object previous) {
            Bushes.afterStep(position, velocity, previous);
        }
    }

    /** Kugel zurueck in VTBs Pool: gemerkte Buesche vergessen. */
    @Patch(className = "vtb.projectile.BallisticProjectile", methodName = "reset")
    public static class Reset {
        @Patch.OnExit
        public static void exit(@Patch.This Object projectile) {
            Bushes.forget(projectile);
        }
    }

    private Patches() {}
}
