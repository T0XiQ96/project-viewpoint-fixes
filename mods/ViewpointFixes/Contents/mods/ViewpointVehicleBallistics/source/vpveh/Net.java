package vpveh;

import java.util.ArrayDeque;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

/**
 * Nachrichten: Client -> Server (Angaben zum Treffer), Server -> Schuetze (was getroffen wurde),
 * Server -> getroffener Insasse (Wunde). Im Einzelspieler landen die Meldungen in einer Warteschlange, die die
 * Lua-Seite jeden Tick abholt (LuaApi.poll).
 */
final class Net {
    private static final ArrayDeque<KahluaTable> local = new ArrayDeque<>();

    static KahluaTable table() {
        return LuaManager.platform.newTable();
    }

    static void sendDetail(IsoGameCharacter shooter, BaseVehicle v, double ax, double ay, double az, double bx, double by, double bz,
                           double energy, boolean vtb) {
        if (!(shooter instanceof IsoPlayer p) || !p.isLocalPlayer()) return;
        KahluaTable t = table();
        t.rawset("v", (double) v.getId());
        t.rawset("ax", ax); t.rawset("ay", ay); t.rawset("az", az);
        t.rawset("bx", bx); t.rawset("by", by); t.rawset("bz", bz);
        t.rawset("e", energy);
        t.rawset("vtb", vtb);
        GameClient.instance.sendClientCommand(p, Build.MODULE, "detail", t);
    }

    /** Was getroffen wurde -> Schuetze (Halo-Text, Lua). */
    static void hitInfo(IsoPlayer shooter, BaseVehicle v, Model.Result r) {
        if (!Cfg.hitInfo()) return;
        KahluaTable t = table();
        t.rawset("v", (double) v.getId());
        t.rawset("kind", r.kind);
        t.rawset("state", r.state);
        if (r.part != null) t.rawset("part", r.part.getId());
        if (r.behind != null) t.rawset("behind", r.behind.getId());
        t.rawset("cond", (double) r.condition);
        if (r.occupant != null) t.rawset("occupant", true);
        send(shooter, "hit", t);
    }

    /** Wunde beim Insassen (nur der eigene Client darf den Koerper eines Spielers aendern). */
    static void wound(IsoPlayer target, int bodyPart, float damage) {
        KahluaTable t = table();
        t.rawset("part", (double) bodyPart);
        t.rawset("dmg", (double) damage);
        send(target, "wound", t);
    }

    private static void send(IsoPlayer p, String command, KahluaTable t) {
        t.rawset("cmd", command);
        if (GameServer.server) {
            GameServer.sendServerCommand(p, Build.MODULE, command, t);
        } else {
            synchronized (local) {
                local.addLast(t);
                while (local.size() > 32) local.removeFirst();
            }
        }
    }

    static KahluaTable poll() {
        synchronized (local) {
            return local.pollFirst();
        }
    }

    private Net() {}
}
