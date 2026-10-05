package vpfx;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Einschlaege nach Material und Munition.
 *
 * Material kommt vom Spiel selbst: die Tile-Eigenschaft "MaterialType" des getroffenen Feldes - dieselbe, nach der
 * SoundManager.playImpactSound den Einschlag-Sound waehlt (Werte aus zombie.iso.enums.MaterialType). Die Munition
 * kommt aus dem AmmoType der Waffe bzw. des Tracers (auch Mod-Munition, erkannt am Namen).
 *
 * Pro Material: Loch (Groesse, Farbe; gehaertetes Metall/Panzerglas nur eine helle Schramme), Funken, Staub/Splitter
 * (Farbe, Menge) und Abprall-Neigung. Abpraller: je flacher der Einschlagwinkel und je haerter das Material, desto
 * wahrscheinlicher; dann kein Loch, sondern Funken und ein kurzer Querschlaeger-Streifen.
 */
final class Impacts {

    /** hole: Lochgroesse in m (0 = keins); scrape: helle Schramme statt Loch; spark: Funkenchance; ricochet: Grundchance. */
    private record Mat(float hole, boolean scrape, float holeR, float holeG, float holeB, float holeA,
                       float spark, float sparkSize, float dustR, float dustG, float dustB, int dust, float ricochet,
                       boolean surface) {}

    // Loch: dunkel; Staubfarbe je Material; surface = Boden/Gelaende (Loch nur als dunkler Fleck, kurzlebig)
    private static final Mat CONCRETE = new Mat(0.045f, false, 0.10f, 0.10f, 0.10f, 0.85f, 0.35f, 0.8f, 0.70f, 0.70f, 0.68f, 6, 0.30f, false);
    private static final Mat PLASTER  = new Mat(0.040f, false, 0.12f, 0.11f, 0.10f, 0.80f, 0.00f, 0.0f, 0.88f, 0.86f, 0.82f, 7, 0.05f, false);
    private static final Mat STONE    = new Mat(0.040f, false, 0.12f, 0.12f, 0.12f, 0.80f, 0.45f, 1.0f, 0.62f, 0.60f, 0.58f, 6, 0.40f, false);
    private static final Mat BRICK    = new Mat(0.045f, false, 0.14f, 0.07f, 0.05f, 0.85f, 0.25f, 0.7f, 0.62f, 0.33f, 0.24f, 6, 0.25f, false);
    private static final Mat WOOD     = new Mat(0.032f, false, 0.06f, 0.04f, 0.02f, 0.95f, 0.00f, 0.0f, 0.55f, 0.40f, 0.25f, 5, 0.03f, false);
    private static final Mat WOOD_SOLID = new Mat(0.028f, false, 0.05f, 0.035f, 0.02f, 0.95f, 0.00f, 0.0f, 0.50f, 0.36f, 0.22f, 4, 0.06f, false);
    private static final Mat METAL    = new Mat(0.020f, false, 0.04f, 0.04f, 0.05f, 0.95f, 0.90f, 1.0f, 0.35f, 0.35f, 0.36f, 2, 0.45f, false);
    private static final Mat METAL_LIGHT = new Mat(0.022f, false, 0.03f, 0.03f, 0.03f, 0.95f, 0.70f, 0.8f, 0.35f, 0.35f, 0.36f, 1, 0.20f, false);
    private static final Mat METAL_LARGE = new Mat(0.018f, false, 0.05f, 0.05f, 0.06f, 0.90f, 0.95f, 1.2f, 0.35f, 0.35f, 0.36f, 2, 0.55f, false);
    private static final Mat METAL_SOLID = new Mat(0.020f, true, 0.75f, 0.75f, 0.72f, 0.55f, 1.00f, 1.5f, 0.40f, 0.40f, 0.40f, 1, 0.80f, false);
    private static final Mat GLASS    = new Mat(0.025f, false, 0.85f, 0.88f, 0.90f, 0.45f, 0.30f, 0.5f, 0.90f, 0.95f, 1.00f, 4, 0.05f, false);
    private static final Mat GLASS_SOLID = new Mat(0.020f, true, 0.90f, 0.92f, 0.95f, 0.50f, 0.20f, 0.4f, 0.90f, 0.95f, 1.00f, 2, 0.50f, false);
    private static final Mat CINDER   = new Mat(0.048f, false, 0.10f, 0.10f, 0.10f, 0.85f, 0.30f, 0.7f, 0.66f, 0.66f, 0.64f, 7, 0.25f, false);
    private static final Mat PLASTIC  = new Mat(0.025f, false, 0.08f, 0.08f, 0.08f, 0.85f, 0.00f, 0.0f, 0.80f, 0.80f, 0.80f, 2, 0.05f, false);
    private static final Mat CERAMIC  = new Mat(0.030f, false, 0.20f, 0.20f, 0.20f, 0.70f, 0.10f, 0.5f, 0.92f, 0.92f, 0.90f, 5, 0.20f, false);
    private static final Mat SOFT     = new Mat(0.020f, false, 0.08f, 0.07f, 0.07f, 0.80f, 0.00f, 0.0f, 0.55f, 0.52f, 0.50f, 2, 0.00f, false);
    private static final Mat DIRT     = new Mat(0.060f, false, 0.10f, 0.07f, 0.05f, 0.60f, 0.00f, 0.0f, 0.45f, 0.35f, 0.24f, 8, 0.02f, true);
    private static final Mat GRASS    = new Mat(0.050f, false, 0.10f, 0.08f, 0.05f, 0.50f, 0.00f, 0.0f, 0.36f, 0.40f, 0.22f, 7, 0.02f, true);
    private static final Mat GRAVEL   = new Mat(0.045f, false, 0.12f, 0.12f, 0.12f, 0.55f, 0.35f, 0.7f, 0.55f, 0.53f, 0.50f, 7, 0.30f, true);
    private static final Mat SAND     = new Mat(0.060f, false, 0.30f, 0.26f, 0.18f, 0.45f, 0.00f, 0.0f, 0.78f, 0.70f, 0.52f, 9, 0.02f, true);
    private static final Mat SNOW     = new Mat(0.060f, false, 0.55f, 0.58f, 0.62f, 0.40f, 0.00f, 0.0f, 0.95f, 0.96f, 1.00f, 9, 0.00f, true);

    static Mat material(String name) {
        if (name == null) return CONCRETE;
        return switch (name) {
            case "Concrete", "Default" -> CONCRETE;
            case "Plaster" -> PLASTER;
            case "Stone" -> STONE;
            case "Brick" -> BRICK;
            case "Cinderblock" -> CINDER;
            case "Wood" -> WOOD;
            case "Wood_Solid" -> WOOD_SOLID;
            case "Metal" -> METAL;
            case "Metal_Light" -> METAL_LIGHT;
            case "Metal_Large" -> METAL_LARGE;
            case "Metal_Solid" -> METAL_SOLID;
            case "Glass", "Glass_Light" -> GLASS;
            case "Glass_Solid" -> GLASS_SOLID;
            case "Plastic" -> PLASTIC;
            case "Ceramic" -> CERAMIC;
            case "Rubber", "Fabric", "Carpet" -> SOFT;
            case "Dirt" -> DIRT;
            case "Grass" -> GRASS;
            case "Gravel" -> GRAVEL;
            case "Sand" -> SAND;
            case "Snow" -> SNOW;
            case "Flesh", "Flesh_Hollow" -> null;     // Treffer im Koerper: Blut macht das Spiel
            default -> CONCRETE;
        };
    }

    /** Munition: {Groesse/Wucht, Abprall-Faktor}. Am Namen erkannt, damit auch Mod-Munition passt. */
    static float[] ammo(String key) {
        if (key == null) return new float[] { 1f, 1f };
        String k = key.toLowerCase();
        if (k.contains("shotgun") || k.contains("shell") || k.contains("buck") || k.contains("gauge") || k.contains("12g"))
            return new float[] { 0.55f, 1.3f };
        if (k.contains("cap")) return new float[] { 0f, 0f };
        if (k.contains("308") || k.contains("762") || k.contains("3006") || k.contains("30-06") || k.contains("50bmg")
            || k.contains(".50") || k.contains("338") || k.contains("300") || k.contains("3030") || k.contains("545")
            || k.contains("556") || k.contains("223") || k.contains("rifle") || k.contains("7.62") || k.contains("5.56"))
            return new float[] { 1.35f, 0.75f };
        if (k.contains("357") || k.contains("44") || k.contains("magnum") || k.contains("454") || k.contains("500"))
            return new float[] { 1.15f, 0.9f };
        if (k.contains("22") || k.contains("380") || k.contains("25acp"))
            return new float[] { 0.7f, 1.1f };
        return new float[] { 0.95f, 1f };    // 9mm, .45, .38, 10mm, unbekannt
    }

    private static float rnd(float a, float b) { return a + ThreadLocalRandom.current().nextFloat() * (b - a); }

    /**
     * Einschlag bei (x, y, z) - z in Stockwerken - mit Flugrichtung (dx, dy, dz; dz in Metern, nicht normiert noetig).
     */
    static void hit(double x, double y, double z, float dx, float dy, float dz, String materialName, String ammoKey) {
        Mat m = material(materialName);
        if (m == null) return;
        float[] a = ammo(ammoKey);
        float power = a[0];
        if (power <= 0f) return;
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6f) { dx = 1f; dy = 0f; dz = 0f; l = 1f; }
        dx /= l; dy /= l; dz /= l;

        // Oberflaechen-Normale (Welt: x, y, z nach oben): Boden, Wand an Feldkante oder dem Schuetzen zugewandt
        float nx, ny, nz;
        double fx = x - Math.floor(x), fy = y - Math.floor(y), fz = z - Math.floor(z);
        double ex = Math.min(fx, 1 - fx), ey = Math.min(fy, 1 - fy);
        if (m.surface() || (fz < 0.03 && dz < 0f)) { nx = 0; ny = 0; nz = 1; }
        else if (ex < 0.12 && ex <= ey) { nx = dx > 0 ? -1 : 1; ny = 0; nz = 0; }
        else if (ey < 0.12) { nx = 0; ny = dy > 0 ? -1 : 1; nz = 0; }
        else {
            float h = (float) Math.hypot(dx, dy);
            if (h < 1e-4f) { nx = 0; ny = 0; nz = 1; } else { nx = -dx / h; ny = -dy / h; nz = 0; }
        }
        surface(x, y, z, dx, dy, dz, m, a, nx, ny, nz, null, true);
    }

    /**
     * Fahrzeug (ViewpointVehicleBallistics): Normale bekannt, Abpraller vom Schadensmodell, Loecher bleiben dort am
     * Fahrzeug (Vehicles.addHoles) - hier also keine festen Weltloecher.
     */
    static void vehicle(double x, double y, double z, float dx, float dy, float dz, String materialName, String ammoKey,
                        float nx, float ny, float nz, boolean ricochet) {
        Mat m = material(materialName);
        if (m == null) return;
        float[] a = ammo(ammoKey);
        if (a[0] <= 0f) return;
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6f) { dx = 1f; dy = 0f; dz = 0f; l = 1f; }
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-6f) { nx = -dx / l; ny = -dy / l; nz = -dz / l; nl = 1f; }
        surface(x, y, z, dx / l, dy / l, dz / l, m, a, nx / nl, ny / nl, nz / nl, ricochet, false);
    }

    private static void surface(double x, double y, double z, float dx, float dy, float dz, Mat m, float[] a,
                                float nx, float ny, float nz, Boolean forceRicochet, boolean holes) {
        float power = a[0];
        float cosIn = Math.abs(dx * nx + dy * ny + dz * nz);         // 1 = senkrecht, 0 = streifend
        float graze = 1f - cosIn;

        // Abpraller
        boolean ricochet = forceRicochet != null ? forceRicochet
            : ThreadLocalRandom.current().nextFloat() < m.ricochet() * a[1] * (0.25f + 0.75f * graze * graze);
        float zLift = 0.004f / Fx.LEVEL;
        double px = x + nx * 0.004, py = y + ny * 0.004, pz = z + nz * zLift;

        if (ricochet) {
            // reflektieren und etwas streuen
            float d = dx * nx + dy * ny + dz * nz;
            float rx = dx - 2 * d * nx + rnd(-0.25f, 0.25f), ry = dy - 2 * d * ny + rnd(-0.25f, 0.25f), rz = dz - 2 * d * nz + rnd(-0.1f, 0.3f);
            Fx.ricochet(px, py, pz, rx, ry, rz);
            Fx.sparks(px, py, pz, nx, ny, nz, Math.round(rnd(5, 9) * Math.max(0.6f, m.sparkSize())), Math.max(0.8f, m.sparkSize()) * power);
            if (holes && m.hole() > 0f) Fx.hole(px, py, pz, nx, ny, nz, m.hole() * 0.5f * power, 0.75f, 0.74f, 0.70f, 0.45f, m.surface());
        } else {
            if (holes && m.hole() > 0f) {
                if (m.scrape()) Fx.hole(px, py, pz, nx, ny, nz, m.hole() * power, m.holeR(), m.holeG(), m.holeB(), m.holeA(), m.surface());
                else Fx.hole(px, py, pz, nx, ny, nz, m.hole() * (0.8f + 0.4f * power) * rnd(0.85f, 1.15f),
                    m.holeR(), m.holeG(), m.holeB(), m.holeA(), m.surface());
            }
            if (ThreadLocalRandom.current().nextFloat() < m.spark())
                Fx.sparks(px, py, pz, nx, ny, nz, Math.round(rnd(3, 7) * m.sparkSize()), m.sparkSize() * power);
        }
        // Staub, Splitter, Rauch aus dem Loch (entgegen der Flugrichtung, also aus der Oberflaeche heraus)
        int count = Math.round(m.dust() * (0.6f + 0.5f * power));
        Fx.dust(px, py, pz, nx, ny, nz, count, m.dustR(), m.dustG(), m.dustB(), 0.8f + 0.4f * power);
        if (m.hole() > 0f && !m.surface() && !m.scrape()) Fx.holeSmoke(px, py, pz, nx, ny, nz);
    }

    private Impacts() {}
}
