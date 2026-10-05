package vfa3d;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Rechnet Weltkoordinaten mit der Viewpoint-Kamera in Bildschirmpixel um.
 * Ersetzt die isometrische Projektion, solange Viewpoint aktiv ist.
 * Alles per Reflection, damit die Mod auch ohne Viewpoint-Klassen geladen werden kann.
 * Die Rechnung entspricht viewpoint.game.WorldToScreen.
 */
public final class Proj {
    private static final float TILE_HEIGHT = 2.4494896f;

    private static boolean init;
    private static boolean broken;
    private static long retryAt;

    private static Field viewEnabled;
    private static Field freeActive;
    private static Field freePlace;
    private static Field fpFrames;
    private static Field fNumber, fScene, fYaw, fPitch, fCamX, fCamY, fCamZ;
    private static Field sProjection;
    private static Method camDirection, camEye, camAt, tpView;

    private static final Matrix4f view = new Matrix4f();
    private static final Matrix4f viewProj = new Matrix4f();
    private static final Vector4f clip = new Vector4f();
    private static final float[] eye = new float[3];
    private static final float[] dir = new float[3];

    private static long cachedFrame = Long.MIN_VALUE;
    private static float oX, oY, oZ;
    private static int cachedPlayer = -1;

    private static Class<?> find(String name) throws ClassNotFoundException {
        ClassLoader[] loaders = {
            Proj.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        ClassNotFoundException last = null;
        for (ClassLoader l : loaders) {
            if (l == null) continue;
            try {
                return Class.forName(name, true, l);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last;
    }

    private static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }

    private static Method method(Class<?> c, String n, Class<?>... p) throws Exception {
        Method m = c.getDeclaredMethod(n, p);
        m.setAccessible(true);
        return m;
    }

    private static boolean ready() {
        if (init) return true;
        if (broken && System.currentTimeMillis() < retryAt) return false;
        try {
            Class<?> viewC = find("viewpoint.core.View");
            Class<?> frameC = find("viewpoint.core.Frame");
            Class<?> sceneC = find("viewpoint.render.SceneData");
            Class<?> cam = find("viewpoint.input.Camera");
            Class<?> tp = find("viewpoint.input.ThirdPerson");
            Class<?> free = find("viewpoint.input.FreeCam");
            Class<?> place = find("viewpoint.input.FreeCam$Place");
            Class<?> fp = find("viewpoint.FP");

            viewEnabled = field(viewC, "enabled");
            freeActive = field(free, "active");
            freePlace = field(free, "place");
            fpFrames = field(fp, "frames");
            fNumber = field(frameC, "number");
            fScene = field(frameC, "scene");
            fYaw = field(frameC, "viewYaw");
            fPitch = field(frameC, "viewPitch");
            fCamX = field(frameC, "camX");
            fCamY = field(frameC, "camY");
            fCamZ = field(frameC, "camZ");
            sProjection = field(sceneC, "projection");
            camDirection = method(cam, "direction", float.class, float.class, float[].class);
            camEye = method(cam, "eye", frameC, float.class, float[].class);
            camAt = method(cam, "at", frameC, place, float[].class, float[].class);
            tpView = method(tp, "view", frameC, float.class, float.class, float[].class);
            init = true;
            broken = false;
            System.out.println("[VFA3D] Viewpoint-Kamera gefunden");
            return true;
        } catch (Throwable t) {
            broken = true;
            retryAt = System.currentTimeMillis() + 5000L;
            System.out.println("[VFA3D] Viewpoint nicht verfuegbar: " + t);
            return false;
        }
    }

    /** true, wenn Viewpoint gerade das 3D-Bild rendert. */
    public static boolean active() {
        if (!ready()) return false;
        try {
            return viewEnabled.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Aktuellster Frame (Viewpoint puffert mehrere). */
    private static Object latestFrame() throws Exception {
        Object[] frames = (Object[]) fpFrames.get(null);
        Object best = null;
        long bestNo = Long.MIN_VALUE;
        for (Object f : frames) {
            if (f == null) continue;
            long n = fNumber.getLong(f);
            if (n > bestNo) {
                bestNo = n;
                best = f;
            }
        }
        return best;
    }

    private static void setup(Object frame, float width, float height) throws Exception {
        Object place = freeActive.getBoolean(null) ? freePlace.get(null) : null;
        if (place != null) {
            camAt.invoke(null, frame, place, eye, dir);
        } else {
            float yaw = fYaw.getFloat(frame);
            float pitch = fPitch.getFloat(frame);
            camDirection.invoke(null, yaw, pitch, dir);
            boolean ok = (Boolean) tpView.invoke(null, frame, yaw, pitch, eye);
            if (!ok) camEye.invoke(null, frame, yaw, eye);
        }
        view.setLookAt(eye[0], eye[1], eye[2],
                eye[0] + dir[0], eye[1] + dir[1], eye[2] + dir[2],
                0f, 1f, 0f);
        Matrix4f proj = (Matrix4f) sProjection.get(fScene.get(frame));
        proj.mul(view, viewProj);
        oX = fCamX.getFloat(frame);
        oY = fCamY.getFloat(frame);
        oZ = fCamZ.getFloat(frame);
    }

    /**
     * @param out  out[0]=x, out[1]=y in Pixeln relativ zum Spielerfenster
     * @return false, wenn der Punkt hinter der Kamera liegt oder Viewpoint nicht aktiv ist
     */
    public static synchronized boolean project(int player, float x, float y, float z,
                                               float width, float height, float[] out) {
        if (!active()) return false;
        try {
            Object frame = latestFrame();
            if (frame == null) return false;
            long no = fNumber.getLong(frame);
            // pro Frame genau einmal aufbauen (Lua ruft X und Y getrennt auf)
            if (no != cachedFrame || player != cachedPlayer) {
                setup(frame, width, height);
                cachedFrame = no;
                cachedPlayer = player;
            }
            viewProj.transform(-(x - oX), (z - oZ) * TILE_HEIGHT, -(y - oY), 1f, clip);
            if (clip.w < 0.05f) return false;
            out[0] = (0.5f + 0.5f * clip.x / clip.w) * width;
            out[1] = (0.5f - 0.5f * clip.y / clip.w) * height;
            return true;
        } catch (Throwable t) {
            if (!broken) {
                System.out.println("[VFA3D] Projektion fehlgeschlagen: " + t);
                broken = true;
                init = false;
                retryAt = System.currentTimeMillis() + 5000L;
            }
            return false;
        }
    }
}
