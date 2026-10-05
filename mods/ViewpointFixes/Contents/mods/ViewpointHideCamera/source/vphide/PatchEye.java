package vphide;

import me.zed_0xff.zombie_buddy.Patch;

/** Controls.put setzt die Augenposition der Kamera; danach wird sie bei Bedarf abgesenkt. */
@Patch(className="viewpoint.input.Controls", methodName="put")
public class PatchEye {
    @Patch.OnExit
    public static void exit(@Patch.Argument(0) Object character, @Patch.Argument(1) Object frame) {
        Eye.apply(character, frame);
    }
}
