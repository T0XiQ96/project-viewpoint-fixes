package vpplace;

import me.zed_0xff.zombie_buddy.Patch;

/** Models.snapshot sammelt die Modelle fuer die 3D-Szene; direkt danach kommt der Platzier-Geist dazu. */
@Patch(className="viewpoint.models.Models", methodName="snapshot")
public class PatchGhost {
    @Patch.OnExit
    public static void exit(@Patch.Argument(0) Object frame) { Ghost.draw(frame); }
}
