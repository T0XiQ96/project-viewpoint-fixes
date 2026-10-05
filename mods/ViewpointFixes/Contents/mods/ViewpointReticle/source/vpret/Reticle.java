package vpret;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;

import static org.lwjgl.opengl.GL33.*;

/**
 * Viewpoints Mittelpunkt mit Optionen.
 *
 * Viewpoint zeichnet in SceneDrawer.drawCrosshair ein schwarzes 4x4-Quadrat mit weissem 2x2-Kern per glScissor +
 * glClear (immer deckend). Hier wird das ersetzt: sichtbar je nach Modus, eigene Form/Groesse/Deckkraft/Farbe,
 * gezeichnet als halbtransparente Rechtecke bzw. Ring. Mit den Standardwerten zeichnet Viewpoint selbst.
 *
 * Optionen aus Lua (VFA_ViewpointReticle.lua): VFA_RET_Mode 1 immer, 2 nur Schusswaffe, 3 jede Waffe, 4 nur beim
 * Zielen, 5 nie; VFA_RET_Style 1 Punkt, 2 Kreuz, 3 Ring; VFA_RET_Size (Pixel), VFA_RET_Opacity, VFA_RET_Color 1..6,
 * VFA_RET_Outline, VFA_RET_Brackets (Zielkreis-Klammern des Spiels in 3D).
 */
public final class Reticle {
    private static volatile boolean failed;
    private static Field env;
    private static Method rawget;
    private static int program, vao;

    private static Object global(String name) {
        try {
            if (env == null) {
                env = Class.forName("zombie.Lua.LuaManager").getField("env");
                rawget = Class.forName("se.krka.kahlua.vm.KahluaTable").getMethod("rawget", Object.class);
            }
            Object g = env.get(null);
            return g == null ? null : rawget.invoke(g, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static float num(String n, float d) { Object v = global(n); return v instanceof Number x ? x.floatValue() : d; }
    private static boolean flag(String n, boolean d) { Object v = global(n); return v instanceof Boolean b ? b : d; }

    public static void validate() {
        System.out.println("[ViewpointReticle] 1.0 ready: options for Viewpoint's centre dot");
    }

    public static boolean hideBrackets() {
        return !flag("VFA_RET_Brackets", true);
    }

    private static boolean visible(int mode) {
        if (mode == 1) return true;
        if (mode == 5) return false;
        IsoPlayer p = IsoPlayer.players[0];
        if (p == null) return false;
        if (mode == 4) return p.isAiming();
        InventoryItem item = p.getPrimaryHandItem();
        if (!(item instanceof HandWeapon w)) return false;
        if (mode == 2) return w.isAimedFirearm();
        return true;   // mode 3: jede Waffe in der Hand
    }

    /** true = Viewpoints eigene Zeichnung ueberspringen. */
    public static boolean replace() {
        if (failed) return false;
        try {
            int mode = Math.round(num("VFA_RET_Mode", 1));
            int style = Math.round(num("VFA_RET_Style", 1));
            float size = num("VFA_RET_Size", 2), opacity = num("VFA_RET_Opacity", 1);
            int color = Math.round(num("VFA_RET_Color", 1));
            boolean outline = flag("VFA_RET_Outline", true);
            if (!visible(mode)) return true;
            if (style == 1 && size == 2 && opacity >= 0.999f && color == 1 && outline) return false;   // wie Viewpoint
            draw(style, size, opacity, color, outline);
            return true;
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ViewpointReticle] Disabled: " + t);
            return false;
        }
    }

    private static final float[][] COLORS = {
        { 1f, 1f, 1f }, { 0.35f, 1f, 0.35f }, { 1f, 0.3f, 0.3f }, { 1f, 0.9f, 0.3f }, { 0.35f, 0.9f, 1f }, { 1f, 0.45f, 1f } };

    private static int shader(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == 0) { String l = glGetShaderInfoLog(s); glDeleteShader(s); throw new IllegalStateException(l); }
        return s;
    }

    private static void initGL() {
        if (program != 0) return;
        int vs = shader(GL_VERTEX_SHADER, """
            #version 330 core
            uniform vec4 rect;      // x0 y0 x1 y1 in NDC
            out vec2 uv;
            const vec2 c[6]=vec2[6](vec2(0,0),vec2(1,0),vec2(1,1),vec2(0,0),vec2(1,1),vec2(0,1));
            void main(){ vec2 q=c[gl_VertexID]; uv=q; gl_Position=vec4(mix(rect.xy,rect.zw,q),0,1); }
            """);
        int fs = shader(GL_FRAGMENT_SHADER, """
            #version 330 core
            uniform vec4 color;
            uniform int ring;       // 0 Rechteck, 1 Ring
            uniform float inner;    // Innenradius 0..1 (Ring)
            in vec2 uv;
            out vec4 o;
            void main(){
              if(ring==1){ float d=length(uv-0.5)*2.0; if(d>1.0||d<inner) discard; }
              o=color;
            }
            """);
        int p = glCreateProgram();
        glAttachShader(p, vs); glAttachShader(p, fs); glLinkProgram(p);
        glDeleteShader(vs); glDeleteShader(fs);
        if (glGetProgrami(p, GL_LINK_STATUS) == 0) { String l = glGetProgramInfoLog(p); glDeleteProgram(p); throw new IllegalStateException(l); }
        program = p;
        vao = glGenVertexArrays();
    }

    private static int[] vp;

    /** Rechteck in Pixeln um die Bildmitte (dx0..dx1, dy0..dy1). */
    private static void rect(float cx, float cy, float x0, float y0, float x1, float y1, float[] c, float a, int ring, float inner) {
        float w = vp[2], h = vp[3];
        glUniform4f(glGetUniformLocation(program, "rect"),
            (cx + x0) / w * 2f - 1f, (cy + y0) / h * 2f - 1f, (cx + x1) / w * 2f - 1f, (cy + y1) / h * 2f - 1f);
        glUniform4f(glGetUniformLocation(program, "color"), c[0], c[1], c[2], a);
        glUniform1i(glGetUniformLocation(program, "ring"), ring);
        glUniform1f(glGetUniformLocation(program, "inner"), inner);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    private static void draw(int style, float size, float opacity, int color, boolean outline) {
        vp = new int[4];
        glGetIntegerv(GL_VIEWPORT, vp);
        float cx = vp[2] * 0.5f, cy = vp[3] * 0.5f;
        float[] col = COLORS[Math.max(0, Math.min(COLORS.length - 1, color - 1))];
        float[] black = { 0f, 0f, 0f };
        float a = Math.max(0.05f, Math.min(1f, opacity));
        float s = Math.max(1f, size);

        int oldProgram = glGetInteger(GL_CURRENT_PROGRAM), oldVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        boolean blend = glIsEnabled(GL_BLEND), depth = glIsEnabled(GL_DEPTH_TEST), scissor = glIsEnabled(GL_SCISSOR_TEST),
            cull = glIsEnabled(GL_CULL_FACE);
        int sr = glGetInteger(GL_BLEND_SRC_RGB), dr = glGetInteger(GL_BLEND_DST_RGB), sa = glGetInteger(GL_BLEND_SRC_ALPHA),
            da = glGetInteger(GL_BLEND_DST_ALPHA);
        try {
            initGL();
            glUseProgram(program);
            glBindVertexArray(vao);
            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_DEPTH_TEST); glDisable(GL_SCISSOR_TEST); glDisable(GL_CULL_FACE);
            float h = s * 0.5f;
            switch (style) {
                case 2 -> {     // Kreuz mit Luecke
                    float len = s * 2.5f, gap = s, t = Math.max(1f, s * 0.5f);
                    if (outline) {
                        rect(cx, cy, gap - 1, -t / 2 - 1, gap + len + 1, t / 2 + 1, black, a, 0, 0);
                        rect(cx, cy, -gap - len - 1, -t / 2 - 1, -gap + 1, t / 2 + 1, black, a, 0, 0);
                        rect(cx, cy, -t / 2 - 1, gap - 1, t / 2 + 1, gap + len + 1, black, a, 0, 0);
                        rect(cx, cy, -t / 2 - 1, -gap - len - 1, t / 2 + 1, -gap + 1, black, a, 0, 0);
                    }
                    rect(cx, cy, gap, -t / 2, gap + len, t / 2, col, a, 0, 0);
                    rect(cx, cy, -gap - len, -t / 2, -gap, t / 2, col, a, 0, 0);
                    rect(cx, cy, -t / 2, gap, t / 2, gap + len, col, a, 0, 0);
                    rect(cx, cy, -t / 2, -gap - len, t / 2, -gap, col, a, 0, 0);
                }
                case 3 -> {     // Ring
                    float r = s * 2.5f;
                    if (outline) rect(cx, cy, -r - 1, -r - 1, r + 1, r + 1, black, a, 1, Math.max(0f, (r - 2.5f) / (r + 1)));
                    rect(cx, cy, -r, -r, r, r, col, a, 1, Math.max(0f, (r - 1.5f) / r));
                }
                default -> {    // Punkt
                    if (outline) rect(cx, cy, -h - 1, -h - 1, h + 1, h + 1, black, a, 0, 0);
                    rect(cx, cy, -h, -h, h, h, col, a, 0, 0);
                }
            }
        } finally {
            glUseProgram(oldProgram);
            glBindVertexArray(oldVao);
            glBlendFuncSeparate(sr, dr, sa, da);
            if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
            if (depth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
            if (scissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
            if (cull) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
        }
    }

    private Reticle() {}
}
