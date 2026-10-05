package vplook;

import me.zed_0xff.zombie_buddy.Patch;

/** Nach der Bewegung (Koerperdrehung): relativ zum Koerper. */
@Patch(className="viewpoint.input.Controls", methodName="afterMovement")
public class PatchAfter {
    @Patch.OnEnter
    public static void enter() { FreeLook.enter(false); }

    @Patch.OnExit(onThrowable=Throwable.class)
    public static void exit() { FreeLook.exit(); }
}