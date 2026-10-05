package vpveh;

/** Fassung dieses Jars. Die Kopie in Bullet Impacts 2D wird aus derselben Quelle mit anderen Werten gebaut (build.ps1). */
final class Build {
    /** true = ViewpointFixes-Fassung (hat Vorrang), false = Kopie in Bullet Impacts 2D. */
    static final boolean PRIMARY = true;
    /** SandboxVars-Tabelle und Namens-Praefix der Optionen. */
    static final String SANDBOX = "ViewpointFixes";
    static final String PREFIX = "VehicleBallistics_";
    /** Lua-Name der Java-Bruecke und Modul fuer Client/Server-Befehle. */
    static final String LUA = "VehicleBallisticsVF";
    static final String MODULE = "VehicleBallisticsVF";
    static final String TAG = "[VehicleBallistics]";

    private Build() {}
}
