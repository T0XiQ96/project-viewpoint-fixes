package vpoac;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Open All Containers tauscht das Sprite eines Behaelters gegen ein eigenes "offen"-Sprite (ct_oac_*).
 * Viewpoint baut 3D-Meshes aus der Kachel-Geometrie (TileGeometryManager) und fragt dort nur die Daten des
 * Grundspiels ab. Fuer die OAC-Sprites gibt es dort nichts -> kein Mesh -> der Behaelter ist unsichtbar.
 * Hier: 1) Geometrie von Open All Containers selbst nehmen, 2) sonst die Geometrie des geschlossenen Original-Sprites
 * (Zuordnung offen -> Original schreibt die Lua-Seite in VFA_OacOriginal).
 * Alles per Reflection: Spielklassen sind neuer als der Compiler, Viewpoint liegt nicht im Classpath.
 */
public final class Geo {
    private static final String MOD = "OpenAllContainers";
    private static boolean failed, ready, announced;
    private static Field name, tileset, index, env;
    private static Method geometry, mods, assigned, rawget;
    private static Object geometryManager, assignManager;

    private static synchronized void init() throws Exception {
        if (ready) return;
        Class<?> sprite = Class.forName("zombie.iso.sprite.IsoSprite");
        name = sprite.getField("name");
        tileset = sprite.getField("tilesetName");
        index = sprite.getField("tileSheetIndex");
        Class<?> gm = Class.forName("zombie.tileDepth.TileGeometryManager");
        geometryManager = gm.getMethod("getInstance").invoke(null);
        geometry = gm.getMethod("getGeometry", String.class, String.class, int.class, int.class);
        mods = gm.getMethod("getModIDs");
        Class<?> am = Class.forName("zombie.tileDepth.TileDepthTextureAssignmentManager");
        assignManager = am.getMethod("getInstance").invoke(null);
        assigned = am.getMethod("getAssignedTileName", String.class, String.class);
        env = Class.forName("zombie.Lua.LuaManager").getField("env");
        rawget = Class.forName("se.krka.kahlua.vm.KahluaTable").getMethod("rawget", Object.class);
        ready = true;
    }

    public static void validate() {
        if (failed) return;
        try {
            init();
            System.out.println("[ViewpointOpenContainers] 1.0 ready: tile geometry for Open All Containers.");
        } catch (Throwable e) { fail(e); }
    }

    private static void fail(Throwable e) {
        failed = true;
        System.err.println("[ViewpointOpenContainers] Disabled: " + e);
        e.printStackTrace();
    }

    private static boolean has(ArrayList<?> l) { return l != null && !l.isEmpty(); }

    private static ArrayList<?> at(String mod, String sheet, int i) throws Exception {
        if (sheet == null || i < 0) return null;
        return (ArrayList<?>) geometry.invoke(geometryManager, mod, sheet, i % 8, i / 8);
    }

    /** Geometrie unter Modkennung oder ueber die Depth-Zuordnung "Sprite -> anderes Tile". */
    private static ArrayList<?> find(String mod, String sheet, int i, String spriteName) throws Exception {
        ArrayList<?> r = at(mod, sheet, i);
        if (has(r) || spriteName == null) return r;
        String other = (String) assigned.invoke(assignManager, mod, spriteName);
        int cut = other == null ? -1 : other.lastIndexOf('_');
        if (cut <= 0) return r;
        try { return at(mod, other.substring(0, cut), Integer.parseInt(other.substring(cut + 1))); }
        catch (NumberFormatException e) { return r; }
    }

    private static String original(String openSprite) throws Exception {
        Object globals = env.get(null);
        if (globals == null) return null;
        Object map = rawget.invoke(globals, "VFA_OacOriginal");
        if (map == null) return null;
        Object o = rawget.invoke(map, openSprite);
        return o instanceof String s ? s : null;
    }

    public static ArrayList<?> fix(Object sprite, ArrayList<?> current) {
        if (has(current) || failed || sprite == null) return current;
        try {
            if (!ready) init();
            String n = (String) name.get(sprite);
            if (n == null || !n.startsWith("ct_oac_")) return current;
            String sheet = (String) tileset.get(sprite);
            int i = index.getInt(sprite);
            ArrayList<?> r = null;
            for (Object id : (ArrayList<?>) mods.invoke(geometryManager)) {
                if (id instanceof String s && s.endsWith(MOD)) {
                    r = find(s, sheet, i, n);
                    if (has(r)) break;
                }
            }
            if (!has(r)) {
                String o = original(n);
                int cut = o == null ? -1 : o.lastIndexOf('_');
                if (cut > 0) {
                    try { r = find("game", o.substring(0, cut), Integer.parseInt(o.substring(cut + 1)), o); }
                    catch (NumberFormatException ignored) { }
                }
            }
            if (has(r)) {
                if (!announced) { announced = true; System.out.println("[ViewpointOpenContainers] first open-container mesh made (" + n + ")"); }
                return r;
            }
        } catch (Throwable e) { fail(e); }
        return current;
    }
}
