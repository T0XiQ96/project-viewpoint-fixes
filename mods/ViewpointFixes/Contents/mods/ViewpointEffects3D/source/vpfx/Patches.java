package vpfx;

import me.zed_0xff.zombie_buddy.Patch;

/** ZombieBuddy-Patches; alles geht an Fx bzw. Tracers, die ihre Fehler selbst fangen. */
public final class Patches {

    /** Ein Angriff/Schuss eines beliebigen Charakters (auch Mitspieler): Rauch. */
    @Patch(className = "zombie.CombatManager", methodName = "attackCollisionCheck")
    public static class Attack {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object character, @Patch.Argument(1) Object weapon) {
            Fx.onAttack(character, weapon);
        }
    }

    /** Vanilla-Muendungsfeuer: entscheidet selbst (Waffe, Zufall), ob es blitzt; nur dann 3D-Blitz und Licht. */
    @Patch(className = "zombie.EffectsManager", methodName = "startMuzzleFlash")
    public static class Flash {
        @Patch.OnExit
        public static void exit(@Patch.This Object manager, @Patch.Argument(0) Object character) {
            Fx.onMuzzleFlash(manager, character);
        }
    }

    /** Endpunkt der Kugel (Wand, Fahrzeug, Ziel); die Ueberladung mit 6 Parametern. */
    @Patch(className = "zombie.CombatManager", methodName = "addTracerEffect")
    public static class Tracer {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object character, @Patch.Argument(1) float range,
                                @Patch.Argument(2) float x, @Patch.Argument(3) float y, @Patch.Argument(4) float z,
                                @Patch.Argument(5) Object square) {
            Fx.onImpact(character, x, y, z, square);
        }
    }

    /** Vanilla-2D-Tracer: in 3D uebernimmt Tracers das Fortbewegen und Zeichnen. */
    @Patch(className = "zombie.iso.objects.IsoBulletTracerEffects", methodName = "render")
    public static class VanillaTracers {
        @Patch.OnEnter(skipOn = true)
        public static boolean enter() {
            return Tracers.skipVanilla();
        }
    }

    /** Game-Thread, einmal pro Viewpoint-Frame, nachdem Viewpoint seine Lampen geschrieben hat. */
    @Patch(className = "viewpoint.light.Torches", methodName = "snapshot")
    public static class Capture {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object frame) {
            Fx.capture(frame);
        }
    }

    /** Render-Thread: Weltpass von Viewpoint, Tiefenpuffer der Szene ist gebunden. */
    @Patch(className = "viewpoint.render.WorldRenderer", methodName = "drawWeather")
    public static class Draw {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object scene) {
            Fx.draw(scene);
        }
    }

    /** True Ballistics: Welteinschlag einer Kugel (Wand, Boden, Baum, Moebel). */
    @Patch(className = "vtb.projectile.BallisticsManager", methodName = "worldImpact")
    public static class VtbWorldImpact {
        @Patch.OnEnter
        public static void enter(@Patch.Argument(0) Object projectile) {
            Vtb.enterWorldImpact();
        }

        @Patch.OnExit(onThrowable = Throwable.class)
        public static void exit(@Patch.Argument(0) Object projectile, @Patch.Argument(1) Object point,
                                @Patch.Argument(2) int kind, @Patch.Argument(3) int tileX, @Patch.Argument(4) int tileY,
                                @Patch.Argument(5) int tileZ) {
            Vtb.exitWorldImpact();
            Vtb.afterWorldImpact(projectile, point, kind, tileX, tileY, tileZ);
        }
    }

    /** Neuer Spiel-Tracer: waehrend eines VTB-Welteinschlags angelegt = Hilfs-Tracer, nicht zeichnen. */
    @Patch(className = "zombie.iso.objects.IsoBulletTracerEffects", methodName = "addEffect")
    public static class AddEffect {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) Object character, @Patch.Return Object effect) {
            Vtb.afterAddEffect(effect);
        }
    }

    /** Shader-Pack (Iris): Wetter-Schritt der Pack-Pipeline, deren Puffer gebunden sind. */
    @Patch(className = "viewpoint.render.IrisGeometry", methodName = "weather")
    public static class IrisWeather {
        @Patch.OnExit
        public static void exit(@Patch.Argument(2) Object context) {
            Fx.drawIris(context);
        }
    }

    private Patches() {}
}
