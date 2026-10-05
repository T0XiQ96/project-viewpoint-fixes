package vpveh;

import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;

/**
 * Durchschuesse und Abpraller fuer Viewpoint True Ballistics (zur Laufzeit, VTB-Dateien bleiben unveraendert).
 *
 * Jede Kugel hat bei VTB Masse und Geschwindigkeit, also Energie. Trifft sie eine Wand, Tuer, ein Moebel oder einen
 * Baum, braucht sie je nach Material eine bestimmte Energie, um durchzukommen (Sandbox, in Joule). Schraeg auftreffen
 * kostet mehr. Danach fliegt sie mit der Restenergie weiter (langsamer = weniger Schaden durch VTBs Damage Falloff),
 * leicht abgelenkt. Reicht es nicht und das Material ist hart und der Winkel flach, kann sie abprallen.
 * Fahrzeuge: das Schadensmodell entscheidet (Glas, Blech, Motorblock); geht sie durch, fliegt sie hinter dem
 * Fahrzeug weiter.
 *
 * Spieler hinter durchschossenen Waenden werden standardmaessig nicht getroffen (Sandbox), Zombies schon.
 */
final class Penetration {

    record Mat(double joule, double thickness, double deflect, double hardness) {}

    static Mat mat(String m) {
        if (m == null) return new Mat(Double.POSITIVE_INFINITY, 0.15, 0, 0.35);
        return switch (m) {
            case "Glass", "Glass_Light" -> new Mat(Cfg.resistance("Glass", 20), 0.02, 2, 0.05);
            case "Glass_Solid" -> new Mat(Cfg.resistance("GlassArmored", 2500), 0.05, 4, 0.5);
            case "Plaster" -> new Mat(Cfg.resistance("Plaster", 120), 0.15, 4, 0.05);
            case "Wood" -> new Mat(Cfg.resistance("Wood", 180), 0.15, 6, 0.03);
            case "Wood_Solid" -> new Mat(Cfg.resistance("WoodSolid", 900), 0.25, 9, 0.06);
            case "Tree" -> new Mat(Cfg.resistance("Tree", 2200), 0.4, 10, 0.08);
            case "Metal_Light" -> new Mat(Cfg.resistance("MetalLight", 150), 0.05, 5, 0.2);
            case "Metal" -> new Mat(Cfg.resistance("Metal", 600), 0.05, 7, 0.45);
            case "Metal_Large" -> new Mat(Cfg.resistance("MetalLarge", 1000), 0.08, 8, 0.55);
            case "Metal_Solid" -> new Mat(Cfg.resistance("MetalSolid", 6000), 0.1, 10, 0.8);
            case "Brick" -> new Mat(Cfg.resistance("Brick", 3500), 0.25, 12, 0.3);
            case "Cinderblock" -> new Mat(Cfg.resistance("Cinderblock", 3000), 0.25, 12, 0.3);
            case "Plastic" -> new Mat(Cfg.resistance("Plastic", 60), 0.05, 3, 0.05);
            case "Fabric", "Carpet" -> new Mat(Cfg.resistance("Fabric", 20), 0.1, 2, 0);
            case "Ceramic" -> new Mat(Cfg.resistance("Ceramic", 200), 0.05, 6, 0.2);
            case "Rubber" -> new Mat(150, 0.05, 4, 0.05);
            case "VehicleBody" -> new Mat(Cfg.resistance("VehicleBody", 250), 0.05, 5, 0.2);
            case "Stone", "Concrete", "Default" -> new Mat(Cfg.resistance("Stone", 20000), 0.3, 12, 0.4);
            default -> new Mat(Cfg.resistance("Stone", 20000), 0.3, 12, 0.35);
        };
    }

    /** Benoetigte Energie in J (mit Sandbox-Faktor). */
    static double resistance(String material) {
        return mat(material).joule() * Math.max(0.01, Cfg.penetrationScale());
    }

    // ------------------------------------------------------------------ Material eines Hindernisses

    static String materialAt(int x, int y, int z, double px, double py) {
        IsoGridSquare sq = IsoWorld.instance == null || IsoWorld.instance.getCell() == null ? null
            : IsoWorld.instance.getCell().getGridSquare(x, y, z);
        String m = materialOf(sq);
        if (m != null) return m;
        // Wand gehoert oft zum Nachbarfeld (Nord-/Westkante)
        double fx = px - Math.floor(px), fy = py - Math.floor(py);
        int nx = x, ny = y;
        if (Math.min(fx, 1 - fx) < Math.min(fy, 1 - fy)) nx += fx < 0.5 ? -1 : 1; else ny += fy < 0.5 ? -1 : 1;
        IsoGridSquare n = IsoWorld.instance == null || IsoWorld.instance.getCell() == null ? null
            : IsoWorld.instance.getCell().getGridSquare(nx, ny, z);
        m = materialOf(n);
        return m;
    }

    static String materialOf(IsoGridSquare sq) {
        if (sq == null) return null;
        try {
            for (int i = 0; i < sq.getObjects().size(); i++) {
                IsoObject o = sq.getObjects().get(i);
                if (o instanceof IsoDoor) return "Wood";
                if (o instanceof IsoThumpable t) {
                    String sprite = t.getSprite() == null ? "" : String.valueOf(t.getSprite().getName()).toLowerCase();
                    if (sprite.contains("metal")) return "Metal";
                    if (t.isDoor() || t.isWindow() || sprite.contains("wood") || sprite.contains("carpentry")) return "Wood";
                }
            }
            Object props = sq.getProperties();
            if (props != null) {
                Object v = props.getClass().getMethod("get", String.class).invoke(props, "MaterialType");
                if (v != null) return v.toString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static double thickness(String m) { return mat(m).thickness(); }
    static double deflect(String m) { return mat(m).deflect(); }
    static double hardness(String m) { return mat(m).hardness(); }
    /** Energie ohne Sandbox-Faktor (fuer VtbBridge, die den Faktor selbst anwendet). */
    static double joule(String m) { return mat(m).joule(); }

    private Penetration() {}
}
