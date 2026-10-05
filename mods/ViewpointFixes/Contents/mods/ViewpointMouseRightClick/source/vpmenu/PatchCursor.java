package vpmenu;

import me.zed_0xff.zombie_buddy.Patch;

/** Laeuft jeden Frame nach Viewpoints Mausmodus-Umschaltung: meldet den Zustand an Lua und nimmt Wuensche von Lua an. */
@Patch(className="viewpoint.FP", methodName="cursorMode")
public class PatchCursor {
    @Patch.OnExit
    public static void exit() { MouseMode.frame(); }
}
