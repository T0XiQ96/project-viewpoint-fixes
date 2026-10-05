package vplook;

import me.zed_0xff.zombie_buddy.Patch;

/** Vor der Bewegung (einmal pro Update): Taste auswerten, Koerperwinkel einsetzen. */
@Patch(className="viewpoint.input.Controls", methodName="beforeMovement")
public class PatchBefore {
    @Patch.OnEnter
    public static void enter() { FreeLook.enter(true); }

    @Patch.OnExit(onThrowable=Throwable.class)
    public static void exit() { FreeLook.exit(); }
}