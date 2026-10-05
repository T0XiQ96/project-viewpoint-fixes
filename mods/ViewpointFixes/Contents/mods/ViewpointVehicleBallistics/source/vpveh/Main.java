package vpveh;

public class Main {
    public static void main(String[] args) {
        if (Build.PRIMARY) System.setProperty("vehicleBallistics.primary", "1");
        System.out.println(Build.TAG + " loaded (" + (Build.PRIMARY ? "Project Viewpoint - Fixes" : "Bullet Impacts 2D") + ")");
    }

    /** Laeuft diese Fassung? Die ViewpointFixes-Fassung immer, die Kopie nur ohne sie. */
    static boolean active() {
        return Build.PRIMARY || System.getProperty("vehicleBallistics.primary") == null;
    }
}
