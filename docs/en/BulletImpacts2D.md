# Bullet Impacts 2D

Steam Workshop: [3813696499](https://steamcommunity.com/sharedfiles/filedetails/?id=3813696499) · Build 42.21 · [Deutsch](../de/BulletImpacts2D.md)

The Workshop item contains two mods.

## VFA_BulletImpacts2D – effects (client)

Bullet holes, sparks, dust, splinters and ricochets in the normal isometric view.

- **Impact point:** the game's own (where the tracer ends: wall, door, window, vehicle, obstacle), read by a small Java hook.
- **Material:** from the hit tile's `MaterialType`, the same property the game uses for the impact sound.

  | Material | Effect |
  |---|---|
  | Wood | dark hole, splinters |
  | Metal | small hole, sparks |
  | Hardened metal / armoured glass | scrape, frequent ricochets |
  | Stone, brick, concrete | hole, coloured dust |
  | Dirt, grass, sand, snow | spray, short-lived mark |

- **Vehicles:** if Vehicle Ballistics is loaded (either version), impacts use the vehicle's material (glass, sheet metal, rubber, armour) instead of the ground under it. There are no fixed holes on vehicles, because they drive away.
- **Ricochets** get likelier on hard surfaces at flat angles. **Size** depends on the ammo.
- **Doors and windows:** holes vanish when the door or window is opened or broken.
- **Light:** effects get darker in the dark.
- **Options:** Options > Mods > Bullet Impacts 2D (holes, lifetime, sparks, dust, ricochets, size).
- **Requirements:** ZombieBuddy. Every player who wants the effects needs the mod.
- **3D view:** with Project Viewpoint's 3D view on, this mod steps aside; ViewpointEffects3D draws the 3D effects.

## VFA_VehicleBallistics2D – vehicle hits (server + client, optional)

The same vehicle model as [Vehicle Ballistics & Penetration](VehicleBallistics.md), as its own mod for servers without Project Viewpoint - Fixes.
- Sandbox page *Vehicle Ballistics 2D*, options `VehicleBallistics2D.*`.
- *Also in the 2D view* is on by default here.
- If `ViewpointVehicleBallistics` is enabled too, that one takes over and this copy stays idle (Java and Lua).
- Needs ZombieBuddy on server and clients.
