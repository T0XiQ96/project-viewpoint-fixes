package vplist;

import me.zed_0xff.zombie_buddy.Patch;

/** Haltet die kleinen Hinweistexte ueber dem Charakter zurueck, solange Viewpoint seine Liste baut. */
@Patch(className="zombie.characters.IsoGameCharacter", methodName="setHaloNote")
public class PatchHalo {
    @Patch.OnEnter(skipOn=true)
    public static boolean enter() { return Hush.on(); }
}
