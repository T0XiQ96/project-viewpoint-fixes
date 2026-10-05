package vfabi;

import me.zed_0xff.zombie_buddy.Patch;

/** Endpunkt einer Kugel (Wand, Hindernis, Fahrzeug, Ziel): die Ueberladung von addTracerEffect mit 6 Parametern. */
public final class Patches {
    @Patch(className = "zombie.CombatManager", methodName = "addTracerEffect")
    public static class Tracer {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object character, @Patch.Argument(1) float range,
                                @Patch.Argument(2) float x, @Patch.Argument(3) float y, @Patch.Argument(4) float z,
                                @Patch.Argument(5) Object square) {
            Impacts.add(character, x, y, z, square);
        }
    }

    private Patches() {}
}
