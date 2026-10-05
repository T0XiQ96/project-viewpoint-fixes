package vpret;

import me.zed_0xff.zombie_buddy.Patch;

/** ZombieBuddy-Patches; Reticle ist oeffentlich, weil der Advice-Code in die Zielklasse kopiert wird. */
public final class Patches {

    /** Viewpoints Mittelpunkt: ersetzt durch Reticle.draw (oder gar nicht gezeichnet). */
    @Patch(className = "viewpoint.SceneDrawer", methodName = "drawCrosshair")
    public static class Crosshair {
        @Patch.OnEnter(skipOn = true)
        public static boolean enter() {
            return Reticle.replace();
        }
    }

    /** Die Zielkreis-Klammern des Spiels, die Viewpoint in 3D zeichnet. */
    @Patch(className = "viewpoint.Hooks", methodName = "skipIsoReticle")
    public static class Brackets {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object reticle, @Patch.Return(readOnly = false) boolean skip) {
            if (Reticle.hideBrackets()) skip = true;
        }
    }

    private Patches() {}
}
