package vpoac;

import java.util.ArrayList;
import me.zed_0xff.zombie_buddy.Patch;

/** Viewpoint sucht die Kachel-Geometrie eines Sprites nur im Grundspiel ("game"); hier kommt der Rest dazu. */
@Patch(className="viewpoint.world.TileMeshes", methodName="geometryFor")
public class PatchGeometry {
    @Patch.OnExit
    public static void exit(@Patch.Argument(0) Object sprite, @Patch.Return(readOnly=false) ArrayList result) {
        result = Geo.fix(sprite, result);
    }
}
