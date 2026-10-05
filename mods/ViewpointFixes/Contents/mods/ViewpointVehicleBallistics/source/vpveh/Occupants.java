package vpveh;

import java.util.concurrent.ThreadLocalRandom;

import org.joml.Vector3f;

import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

/**
 * Kugeln, die Glas oder Blech durchschlagen, koennen Insassen treffen: der Sitz, an dessen Oberkoerper die Flugbahn
 * am naechsten vorbeigeht. Koerperteil nach Hoehe (Kopf, Hals, Brust, Bauch, Becken, Oberschenkel), Wunde mit Kugel.
 */
final class Occupants {

    static void hit(BaseVehicle v, Model.Result r, double pf) {
        if (r.energyLeft <= 1) return;
        Vector3f d = new Vector3f(r.dirLocal);
        if (d.lengthSquared() < 1e-6f) return;
        d.normalize();
        Vector3f seat = new Vector3f(), w = new Vector3f();
        IsoGameCharacter best = null;
        float bestPerp = 1e9f, bestRel = 0;
        int bestSeat = -1;
        for (int i = 0; i < v.getMaxPassengers(); i++) {
            IsoGameCharacter c = v.getCharacter(i);
            if (c == null || c.isDead()) continue;
            v.getPassengerLocalPos(i, seat);
            w.set(seat).add(0, 0.45f, 0).sub(r.local);
            float proj = w.dot(d);
            if (proj < -0.15f || proj > 3.5f) continue;
            Vector3f closest = new Vector3f(d).mul(proj).add(r.local);
            float perp = closest.distance(seat.x, seat.y + 0.45f, seat.z);
            if (perp < bestPerp) {
                bestPerp = perp;
                best = c;
                bestSeat = i;
                bestRel = closest.y - seat.y;
            }
        }
        if (best == null || bestPerp > 0.6f) return;
        double chance = (bestPerp < 0.3f ? 0.9 : 0.55) * Math.min(1, r.energyLeft / Math.max(1, r.energyIn) + 0.2);
        if (ThreadLocalRandom.current().nextDouble() > chance) return;

        BodyPartType part;
        boolean left = ThreadLocalRandom.current().nextBoolean();
        if (bestRel > 0.72f) part = BodyPartType.Head;
        else if (bestRel > 0.62f) part = BodyPartType.Neck;
        else if (bestRel > 0.35f) part = BodyPartType.Torso_Upper;
        else if (bestRel > 0.12f) part = BodyPartType.Torso_Lower;
        else if (bestRel > 0.0f) part = BodyPartType.Groin;
        else part = left ? BodyPartType.UpperLeg_L : BodyPartType.UpperLeg_R;
        float damage = (float) Math.max(5, Math.min(80, 22 * Power.factor(r.energyLeft)));
        r.occupant = best;
        r.occupantPart = BodyPartType.ToIndex(part);
        r.occupantDamage = damage;
        if (best instanceof IsoPlayer p && GameServer.server) {
            Net.wound(p, r.occupantPart, damage);
        } else {
            applyWound(best, r.occupantPart, damage);
        }
    }

    static void applyWound(IsoGameCharacter c, int index, float damage) {
        if (c == null) return;
        try {
            if (c instanceof IsoZombie z) {
                z.setHealth(z.getHealth() - damage / 40f);
                return;
            }
            BodyDamage bd = c.getBodyDamage();
            if (bd == null) return;
            BodyPartType type = BodyPartType.FromIndex(index);
            BodyPart bp = bd.getBodyPart(type);
            if (bp == null) return;
            bp.setHaveBullet(true, 0);
            bd.AddDamage(type, damage);
        } catch (Throwable t) {
            System.err.println(Build.TAG + " occupant wound failed: " + t);
        }
    }

    private Occupants() {}
}
