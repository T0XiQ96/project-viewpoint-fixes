package vpmenu;

import me.zed_0xff.zombie_buddy.Patch;

/** Rechtsklick gilt im Viewpoint-Mausmodus nicht als "Zielen halten". */
@Patch(className="zombie.characters.component.CharacterInputComponent", methodName="isAimKeyDownInternal")
public class PatchAim {
    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly=false) boolean result) {
        if (result && MouseMode.on()) result = false;
    }
}
