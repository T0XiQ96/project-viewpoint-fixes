package vplook;

import me.zed_0xff.zombie_buddy.Patch;

/** Laufrichtung aus den Bewegungstasten: relativ zum Koerper, nicht zur Kamera. */
@Patch(className="viewpoint.input.Controls", methodName="moveVector")
public class PatchMove {
    @Patch.OnEnter
    public static void enter() { FreeLook.enter(false); }

    @Patch.OnExit(onThrowable=Throwable.class)
    public static void exit() { FreeLook.exit(); }
}