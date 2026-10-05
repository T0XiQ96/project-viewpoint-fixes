package vfa3d;

import zombie.iso.IsoCamera;

/** Gemeinsamer Teil von isoToScreenX/Y. */
public final class Hooks {
    /** Wert fuer Punkte hinter der Kamera: weit ausserhalb, damit Mods sie wegclippen. */
    public static final float OFFSCREEN = -100000f;

    private static final float[] px = new float[2];
    private static long lastKey;
    private static boolean lastOk;
    private static int lastPlayer = -1;
    private static float lx = Float.NaN, ly, lz;

    private Hooks() {}

    /** @return Pixel (relativ zum Spielerfenster, wie vanilla isoToScreen*) oder NaN, wenn die 2D-Rechnung bleiben soll */
    public static float screen(int player, float x, float y, float z, boolean wantX, float original) {
        try {
            if (!(player == lastPlayer && x == lx && y == ly && z == lz)) {
                int w = IsoCamera.getScreenWidth(player);
                int h = IsoCamera.getScreenHeight(player);
                lastOk = Proj.project(player, x, y, z, w, h, px);
                lastPlayer = player;
                lx = x;
                ly = y;
                lz = z;
            }
            if (!lastOk) {
                return Proj.active() ? OFFSCREEN : original;
            }
            return wantX ? px[0] : px[1];
        } catch (Throwable t) {
            return original;
        }
    }
}
