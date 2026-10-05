package vplook;

import me.zed_0xff.zombie_buddy.Patch;

/** Bewegung aus der Animation: relativ zum Koerper. */
@Patch(className="viewpoint.input.Controls", methodName="rootMotion")
public class PatchRoot {
    @Patch.OnEnter
    public static void enter() { FreeLook.enter(false); }

    @Patch.OnExit(onThrowable=Throwable.class)
    public static void exit() { FreeLook.exit(); }
}