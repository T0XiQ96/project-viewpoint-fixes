package vfa3d;

import me.zed_0xff.zombie_buddy.Exposer;

/** Lua: VFA3D.active() ist true, solange Viewpoint das 3D-Bild rendert. */
@Exposer.LuaClass(name = "VFA3D")
public class LuaApi {
    public static boolean active() {
        return Proj.active();
    }
}
