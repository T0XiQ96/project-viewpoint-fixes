# Vehicle Ballistics & Penetration

Mod ID `ViewpointVehicleBallistics` (in [Project Viewpoint - Fixes](ViewpointFixes.md)) · copy for the 2D view: `VFA_VehicleBallistics2D` (in [Bullet Impacts 2D](BulletImpacts2D.md)) · [Deutsch](../de/VehicleBallistics.md)

## Requirements and installation

| | |
|---|---|
| Required | ZombieBuddy, on **clients and the dedicated server** (the server does the damage) |
| Recommended | Viewpoint True Ballistics: penetration, ricochets and shooting from vehicles need it |
| Optional | ViewpointEffects3D (3D impacts and lasting holes on vehicles), Bullet Impacts 2D (2D impacts on vehicles) |
| Load order | after `ViewpointTrueBallistics` and `ViewpointEffects3D` |

- The jar sits in `media/java/` (not `client/`), so ZombieBuddy loads it on the server too.
- Without the server part, vehicle damage in multiplayer stays vanilla, and hits through walls are rejected by True Ballistics' server check.
- If both `ViewpointVehicleBallistics` and `VFA_VehicleBallistics2D` are enabled, the ViewpointFixes version takes over and the copy stays idle.

## Why

Vanilla Project Zomboid does not care **where** a bullet hits a vehicle. It only looks at which side you shoot from, picks a random part of that side and applies a small amount of damage (`damage × 50 × DoorDamage/10 ÷ durability`). With an M4 you can fire 200 rounds into a car and it is still at 30 %, and a windshield, a tyre or a headlight does not react to being shot.

## How a hit is resolved

1. **Flight path**
   - With True Ballistics: the real impact point and velocity of the simulated round.
   - Without it (2D, only if *also in the 2D view* is on): muzzle position and aim direction, like the game.
   - In multiplayer the shooter's client sends the flight path to the server, which checks it (distance to shooter and vehicle, energy no higher than the weapon's) and uses it. Without it the server falls back to the aim direction.
2. **Vehicle shape:** the vehicle's own **physics shapes** from its script, including rotated boxes such as a sloped windshield. Shapes that do not match the vehicle's size, because of another scale or offset, are aligned to it automatically. If there are none, the extents box is used. Wheels are cylinders from their position, radius and width. This works with modded vehicles without any compatibility patch.
3. **Which part:** found from the hit point, the face normal and the vehicle's seats.
   - **Glass:** above the belt line (seat height + 0.3 m). The front or a sloped face → `Windshield`, the rear face → `WindshieldRear`, a side face → the side window whose door/seat is nearest.
   - **Tyre:** inside a wheel cylinder.
   - **Light:** outer front or rear corners at lamp height → `HeadlightLeft/Right`, `HeadlightRearLeft/Right` (any part with a light).
   - **Hood or engine:** front top → `EngineDoor`; front lower (grille) → `Radiator` / `Engine`.
   - **Trunk, tank, muffler:** rear → `TrunkDoor` / `DoorRear`, low rear → `Muffler` / `GasTank`.
   - **Door:** the side door of the nearest seat. In the rear third, low → sometimes `GasTank`.
   - **Roof and other panels:** body only.
4. **Armour:** an installed part whose ID contains *armor / armour / protection / cage / plate / plating / bulletproof*, and whose position words match the target part, takes the hit instead.

   | Target | Armour that counts |
   |---|---|
   | Windshield | armour with "Windshield", rear vs. front must match |
   | Side window | Front/Middle/Rear/Back + Left/Right |
   | Door | Door + L/R, or side armour |
   | Tyre | Wheel/Tire |
   | Engine | Engine/Hood/Front/Grill |
   | Rear parts | Rear |
   | Everything except lights | a part called just "Armor" (tanks) |

   This matches KI5's `DAMN…Armor` parts, Autotsar and other mods. KI5 vehicles with `RFsystem` / `CTIsystem` have run-flat tyres.

## What happens

| Part | Behaviour (defaults) |
|---|---|
| **Windshield** (laminated glass) | Keeps its holes and stays in place. Condition drops 100/15 per 9 mm hit (rifles about twice as much); it caves in after about 15 hits (*WindshieldShots*). The vanilla crack overlay grows with damage. The round goes through with about 85 % of its energy. |
| **Side and rear windows** (tempered glass) | 90 % chance to shatter completely at the first hit (*SideWindowShatter*); otherwise a hole with cracks. |
| **Armoured glass** (window item type contains armor/bulletproof/ballistic/reinforced) | Scrape, 25 % ricochets, wears slowly, the round stops. |
| **Open / rolled-down / broken window** | No glass damage; the round enters the vehicle. |
| **Tyres** | Flat after 1 hit (rifle, slug, magnum, ≥ 1200 J), 2 (pistol, SMG) or 3 (.22, .38, a single shotgun pellet, < 350 J). Every pellet of a shotgun counts on its own. Alternative *slow leak*: every hole leaks, more holes leak faster, flat after *TireLeakSeconds*. Run-flat vehicles only lose some condition. |
| **Lights** | Break at the first hit (*LightBreak* 100 %). |
| **Doors, hood, trunk, panels** | 3 % per 9 mm hit × calibre factor (*BodyDamage*). The round goes through if its energy is above *J_VehicleBody* (250 J). |
| **Engine / radiator** | 2 % per hit (*EngineDamage*); the engine block stops the round. |
| **Fuel tank** | Damage, and the tank leaks (about 0.4 l/s × calibre factor) if *FuelLeak* is on. |
| **Armour** | Wear 1.5 × calibre × *ArmorWear*; 35 % ricochets; only rounds above *J_MetalSolid* (6000 J, e.g. .50 BMG) get through with half the energy. |
| **Occupants** | A round that got through glass, a door or an open window can hit the person whose upper body is closest to its path (≤ 0.6 m). The chance depends on distance and remaining energy. The body part follows the height (head, neck, chest, belly, groin, thigh); the hit gives a bullet wound with damage by remaining energy. Players get the wound on their own client. |

The calibre factor is `√(energy / 500 J)`, so a 9 mm is 1, a 5.56 about 1.85 and a .22 about 0.55, limited to 0.3–4.

### Energy per calibre

With True Ballistics the energy comes from its profile (mass and real muzzle velocity of the calibre), so mod ammo and compatibility packs count too. Without it, the energy is estimated from the ammo name.

| Ammo | J (about) |
|---|---|
| .22 | 150 |
| buckshot pellet | 180 |
| .38 | 320 |
| 9 mm | 500 |
| .45 | 550 |
| .357 / 10 mm | 750 |
| .44 / magnum | 1300 |
| 5.56 / .223 / 5.45 | 1700 |
| 7.62x39 / .30-30 | 2000 |
| 12 gauge slug | 3000 |
| .308 / 7.62x51 / .30-06 | 3400 |
| .50 BMG / .338 | 9000 |

AP ammo (name contains `_ap`, "armorpiercing") counts ×1.6, hollow points (JHP, HP) ×0.6. True Ballistics flies its rounds with a velocity scale (default 0.4); this mod converts back to real joules.

## What was hit

The shooter sees a short text over the character, e.g. *Tyre rear left: flat (40%)*, *Windshield: bullet hole (73%)* or *Door front left: hit – someone inside was hit*. The server switches it on or off (*HitInfo*), and every player can hide it in Options > Mods > Vehicle Ballistics.

## Bullet holes and effects

Holes are saved in the hit part's mod data (`vvbHoles`, up to 24 per part, in vehicle coordinates). They are sent to all players and drawn by ViewpointEffects3D at the vehicle's current position, so they move with it. They disappear when the window shatters, the part is replaced or the part is repaired to 100 %. In the 2D view, Bullet Impacts 2D shows sparks, splinters and dust in the vehicle's material, but no lasting holes.

## Shooting from vehicles

Requirements: a firearm in the primary hand, sitting in a vehicle, True Ballistics installed, and the sandbox options *DriveBy* (and *DriverCanShoot* for the driver) on.

**Controls:** hold the **right mouse button** to aim. A crosshair appears at the mouse in 2D, or in the middle of the screen in 3D. **Left click** shoots; full auto keeps firing while the button is held.

**Direction:** in the 3D view (Viewpoint) the round goes where you look; in 2D it goes from your eye in the seat towards the mouse pointer.

**Your own vehicle:** the round first passes your vehicle where the line leaves it.

| Where it leaves | Result |
|---|---|
| Closed window | Hole or shatters, with the same rules as above; the energy drops |
| Open window | Nothing |
| Door / roof | Sheet metal |
| Armour or armoured glass | The round stays inside |

In multiplayer the server applies that damage.

**Everything else** is like a normal shot: sound, noise that attracts zombies, chambering and ammo, jams and racking. The spread is ×1.5 (*DriveBySpread*), and the driver gets another ×2 (*DriverSpread*).

The limit: there is no seated aiming animation, so the arms do not point at the target in third person.

### Rolling windows down

Vanilla already has *Open window / Close window* in the seat's radial menu for side windows marked openable; most vehicles (also KI5) inherit it. Door windows of modded vehicles that are not openable become openable (*OpenableWindows*). Vehicles without an opening animation still look closed, but shots pass.

## Penetration and ricochets (Viewpoint True Ballistics)

**Penetration:**
- When a round hits a wall, door, low wall, furniture or tree, it needs the material's energy (sandbox, joules), divided by `cos(angle)` (at most ×3) for flat angles.
- If it gets through, it plays the entry impact (hole, sound) and loses `need + EnergyLoss %` of the rest. It then flies on through the obstacle's thickness, slower (less damage through True Ballistics' damage falloff, more drop) and deflected by up to the material's angle × *DeflectionScale*.
- A round passes at most *MaxPenetrations* obstacles (default 3).

**Ricochets:** if the round does not get through a hard material (metal, stone, armour) at a flat angle (< *RicochetAngle*, 20°), it can bounce off and fly on with about 35 % of its energy.

**Vehicles:** the damage model decides (glass, sheet metal, engine block). If the round gets through both sides, it flies on behind the vehicle.

**Who can be hit:** zombies behind walls are hit (*PenetrationZombies*). **Players behind walls are not** by default (*PenetrationPlayers* off): rounds that passed or bounced off something fly past players.

**Server check:** True Ballistics rejects hits without a clear line of fire. The server part of this mod accepts them if every wall in between could really be pierced by that weapon's energy, and only for the target types allowed above.

Materials come from the hit tile's `MaterialType`, the same property that picks the impact sound. Doors count as wood, player-built walls as wood or metal by sprite, trees as "Tree". Default resistances:

| Material | J | | Material | J |
|---|---|---|---|---|
| Glass | 20 | | Metal (fences, appliances) | 600 |
| Fabric / carpet | 20 | | Solid wood | 900 |
| Plastic | 60 | | Heavy metal (containers) | 1000 |
| Plaster / drywall | 120 | | Tree trunk | 2200 |
| Sheet metal | 150 | | Armoured glass | 2500 |
| Wood (walls, doors) | 180 | | Cinder block | 3000 |
| Ceramic | 200 | | Brick | 3500 |
| Vehicle body (one door) | 250 | | Hardened metal / armour | 6000 |
| | | | Stone / concrete | 20000 |

So a 9 mm goes through a wooden door or drywall, a 5.56 through solid wood, a .308 through a tree trunk now and then, and nothing realistic goes through concrete. *PenetrationScale* multiplies all values.

## Sandbox options

All 50 options with defaults and ranges: **[vehicle-ballistics-options.generated.md](vehicle-ballistics-options.generated.md)** (page *Viewpoint Fixes*, names `ViewpointFixes.VehicleBallistics_*`; 2D copy: `VehicleBallistics2D.*`).

## Troubleshooting

- `console.txt` lines starting with `[VehicleBallistics]` show the loaded version, whether True Ballistics was found and any errors. An error switches only that part off and leaves vanilla behaviour.
- Console command `VehicleBallisticsVF_describe()` (2D copy: `VehicleBallistics2D_describe()`) prints how the mod sees the vehicle you sit in or stand next to: number of shapes, size, front direction, belt line, wheels.
- Wrong parts on one specific modded vehicle: please report the vehicle's script name and the `describe()` output.

## Files

| File | Purpose |
|---|---|
| `source/vpveh/Geometry.java` | vehicle shape, ray tests |
| `Model.java` | which part, damage |
| `Penetration.java` / `VtbBridge.java` | materials and True Ballistics hooks |
| `Shot.java` / `Net.java` | flow, client ↔ server |
| `DriveBy*.java` | shooting from vehicles |
| `Effects.java` | interface for effect mods |
| `Leaks`, `Holes`, `Occupants` | air and fuel leaks, lasting holes, people inside |
| `source/build.ps1` | builds both jars |
| `tools/vehicle_gen.py` | sandbox options, translations, Lua and mod.info for both versions |
