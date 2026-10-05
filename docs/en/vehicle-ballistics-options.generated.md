### Vehicle hits

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.VehicleBallistics_Enabled` | Vehicle hits: enable | on |  | Shots hit the real part of a vehicle (glass, tyres, lights, doors, hood, engine, tank, armour) instead of a random part of that side. |
| `ViewpointFixes.VehicleBallistics_In2D` | Vehicle hits: also in the normal 2D view | off |  | Also for shots without True Ballistics (vanilla firing, isometric view). The shot then flies straight along your aim at muzzle height. |
| `ViewpointFixes.VehicleBallistics_HitInfo` | Vehicle hits: tell the shooter what was hit | on |  | A short text over your character, e.g. "Tyre rear left: flat". Each player can switch it off in the mod options. |
| `ViewpointFixes.VehicleBallistics_DamageScale` | Vehicle hits: damage factor | 1 | 0.1 – 5 | Multiplies all part damage from bullets. |
| `ViewpointFixes.VehicleBallistics_WindshieldShots` | Windshield: hits until it caves in | 15 | 1 – 60 | Laminated glass keeps its holes and stays in place. Hits from a 9 mm until it falls in; stronger rounds count more (rifle about 2x), weaker less. |
| `ViewpointFixes.VehicleBallistics_SideWindowShatter` | Side and rear windows: shatter chance per hit (%) | 90 | 0 – 100 | Tempered glass usually bursts completely at the first hit. Otherwise a hole with cracks. |
| `ViewpointFixes.VehicleBallistics_LightBreak` | Lights: break chance per hit (%) | 100 | 0 – 100 |  |
| `ViewpointFixes.VehicleBallistics_TireShotsStrong` | Tyres: hits until flat (rifles, slugs, magnums) | 1 | 1 – 10 |  |
| `ViewpointFixes.VehicleBallistics_TireShotsMedium` | Tyres: hits until flat (pistols, SMGs) | 2 | 1 – 10 |  |
| `ViewpointFixes.VehicleBallistics_TireShotsWeak` | Tyres: hits until flat (.22, .38, single shotgun pellets) | 3 | 1 – 10 | Every pellet of a shotgun counts on its own, so a close shotgun blast usually flattens a tyre. |
| `ViewpointFixes.VehicleBallistics_TireMode` | Tyres: how they lose air | 1 | 1 = Flat at once / 2 = Slow leak | Flat at once: after the hits above the tyre is flat. Slow leak: every hole leaks, more holes faster; flat after the time below. |
| `ViewpointFixes.VehicleBallistics_TireLeakSeconds` | Tyres: seconds until flat (slow leak) | 30 | 1 – 600 |  |
| `ViewpointFixes.VehicleBallistics_RunFlat` | Tyres: run-flat on armoured vehicles | on |  | Vehicles with a run-flat or tyre inflation system (KI5) or wheel armour only lose some condition. |
| `ViewpointFixes.VehicleBallistics_BodyDamage` | Body panels: damage per hit (%) | 3 | 0 – 25 | Doors, hood, trunk, bumpers - for a 9 mm, stronger rounds more. |
| `ViewpointFixes.VehicleBallistics_EngineDamage` | Engine and radiator: damage per hit (%) | 2 | 0 – 25 |  |
| `ViewpointFixes.VehicleBallistics_FuelLeak` | Fuel tank leaks when hit | on |  |  |
| `ViewpointFixes.VehicleBallistics_Occupants` | Shots through glass and doors can hit people inside | on |  | Depends on the energy left: rifles go through doors, small calibres often stay in the sheet metal. |
| `ViewpointFixes.VehicleBallistics_ArmorWear` | Vehicle armour: wear per hit | 1 | 0 – 10 | Armour plates and armoured glass (KI5 and other mods) stop most rounds with a scrape and wear down slowly. |

### Shooting from vehicles and windows

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.VehicleBallistics_DriveBy` | Shooting from vehicles | on |  | In a vehicle with a firearm in hand: hold the right mouse button to aim, left click to shoot (full auto keeps firing). A closed window you shoot through gets a hole or shatters; open (rolled down) windows stay intact. Needs Viewpoint True Ballistics. |
| `ViewpointFixes.VehicleBallistics_DriverCanShoot` | Shooting from vehicles: the driver may shoot | on |  |  |
| `ViewpointFixes.VehicleBallistics_DriveBySpread` | Shooting from vehicles: spread factor | 1.5 | 0.1 – 5 | Multiplies the normal spread (skill, moodles, movement). |
| `ViewpointFixes.VehicleBallistics_DriverSpread` | Shooting from vehicles: extra spread for the driver | 2 | 1 – 5 |  |
| `ViewpointFixes.VehicleBallistics_OpenableWindows` | Make side windows of modded vehicles openable | on |  | Door windows that cannot be rolled down get the normal Open window / Close window option. Vehicles without an opening animation still look closed, but shots pass. |

### Penetration and ricochets (needs Viewpoint True Ballistics)

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.VehicleBallistics_Penetration` | Penetration: enable (True Ballistics) | on |  | Rounds can go through walls, doors, furniture, trees and vehicles if their energy is high enough. They fly on slower (less damage) and slightly deflected. Needs Viewpoint True Ballistics. |
| `ViewpointFixes.VehicleBallistics_PenetrationZombies` | Penetration: zombies behind walls can be hit | on |  |  |
| `ViewpointFixes.VehicleBallistics_PenetrationPlayers` | Penetration: players behind walls can be hit | off |  | Off: rounds that went through something pass players behind it. |
| `ViewpointFixes.VehicleBallistics_PenetrationScale` | Penetration: material resistance factor | 1 | 0.1 – 10 | Multiplies every material value below. Higher = fewer penetrations. |
| `ViewpointFixes.VehicleBallistics_MaxPenetrations` | Penetration: obstacles per round | 3 | 0 – 10 |  |
| `ViewpointFixes.VehicleBallistics_EnergyLoss` | Penetration: extra energy loss (%) | 25 | 0 – 90 | Of the energy left after an obstacle (tumbling, deformation). |
| `ViewpointFixes.VehicleBallistics_DeflectionScale` | Penetration: deflection factor | 1 | 0 – 5 |  |
| `ViewpointFixes.VehicleBallistics_Ricochets` | Ricochets: enable | on |  | Rounds that do not get through hard material at a flat angle can bounce off and fly on. |
| `ViewpointFixes.VehicleBallistics_RicochetScale` | Ricochets: chance factor | 1 | 0 – 5 |  |
| `ViewpointFixes.VehicleBallistics_RicochetAngle` | Ricochets: maximum angle to the surface (degrees) | 20 | 1 – 60 |  |
| `ViewpointFixes.VehicleBallistics_J_Glass` | Penetration energy: Glass (J) | 20 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Plaster` | Penetration energy: Plaster / drywall (J) | 120 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Wood` | Penetration energy: Wood (walls, doors) (J) | 180 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_WoodSolid` | Penetration energy: Solid wood (logs, heavy furniture) (J) | 900 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Tree` | Penetration energy: Tree trunks (J) | 2200 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_MetalLight` | Penetration energy: Sheet metal (J) | 150 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Metal` | Penetration energy: Metal (fences, appliances) (J) | 600 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_MetalLarge` | Penetration energy: Heavy metal (containers) (J) | 1000 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_MetalSolid` | Penetration energy: Hardened metal / armour (J) | 6000 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_GlassArmored` | Penetration energy: Armoured glass (J) | 2500 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Brick` | Penetration energy: Brick (J) | 3500 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Cinderblock` | Penetration energy: Cinder block (J) | 3000 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Stone` | Penetration energy: Stone / concrete (J) | 20000 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Plastic` | Penetration energy: Plastic (J) | 60 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Fabric` | Penetration energy: Fabric / carpet (J) | 20 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_Ceramic` | Penetration energy: Ceramic (J) | 200 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |
| `ViewpointFixes.VehicleBallistics_J_VehicleBody` | Penetration energy: Vehicle body (one door) (J) | 250 | 0 – 50000 | Energy a round needs to get through. For comparison: .22 about 150 J, 9 mm 500, .45 550, .357 750, 5.56 1700, 7.62x39 2000, .308 3400, 12 gauge slug 3000, buckshot pellet 180. |

Bullet Impacts 2D (VFA_VehicleBallistics2D) has the same options as `VehicleBallistics2D.<Name>` (page "Vehicle Ballistics 2D"); there "also in the 2D view" is on by default.
