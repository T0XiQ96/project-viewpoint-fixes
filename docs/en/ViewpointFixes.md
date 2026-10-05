# Project Viewpoint - Fixes

Steam Workshop: [3812510377](https://steamcommunity.com/sharedfiles/filedetails/?id=3812510377) · Build 42.21 · [Deutsch](../de/ViewpointFixes.md)

Fifteen small, independent mods for **Project Viewpoint** (3D view). Enable only the ones you want.

- All need **ZombieBuddy** and are loaded **after Viewpoint**.
- Mods 1–14 are client-side. Mod 12 changes where your own shots go. Mod 15 changes gameplay and has to run on the **server too** (ZombieBuddy on the dedicated server).
- Java source of every mod is in its `source/` folder ([mods/ViewpointFixes](../../mods/ViewpointFixes/Contents/mods)).
- Unofficial, not affiliated with the authors of Viewpoint or of the patched mods. Viewpoint is only accessed at runtime (ZombieBuddy patches and reflection); none of its code is included.

| # | Mod ID | Name | Side |
|---|---|---|---|
| 1 | `Viewpoint2Dto3D` | Viewpoint 2D to 3D | client |
| 2 | `Viewpoint2Dto3D_ExtendedPatch` | 2D to 3D – Extended Patch | client |
| 3 | `ViewpointOpenContainers` | Open All Containers Fix | client |
| 4 | `ViewpointHideCamera` | Hide Camera | client |
| 5 | `ViewpointPlaceItems` | Item Placement 3D | client |
| 6 | `ViewpointMouseRightClick` | Right Click in Mouse Mode | client |
| 7 | `ViewpointCrosshairUse` | Crosshair Use | client |
| 8 | `ViewpointProneKey` | Prone on X | client |
| 9 | `ViewpointInteractList` | Interaction List Cleanup | client |
| 10 | `ViewpointEffects3D` | 3D Gun Effects, Tracers and Fireflies | client |
| 11 | `ViewpointShoulderLook3D` | Over the Shoulder in 3D | client |
| 12 | `ViewpointAimSpread` | Aim Spread | client (gameplay) |
| 13 | `ViewpointReticle` | Crosshair Options | client |
| 14 | `ViewpointZoomHold` | Hold to Zoom | client |
| 15 | `ViewpointVehicleBallistics` | Vehicle Ballistics & Penetration | **server + client** |

---

## Map markers and labels

### 1) Viewpoint 2D to 3D (`Viewpoint2Dto3D`)
Many mods place hints with the old isometric projection (Lua `isoToScreenX/Y`). Under Viewpoint those hints float at the wrong spot.
- While the 3D view is on, `isoToScreenX/Y` return the real screen position from Viewpoint's camera (first person, third person, free camera). Points behind the camera are moved far off screen, so mods hide them.
- Real Vehicle VFX draws flat 2D smoke (exhaust, damage smoke, radiator steam, tyre smoke). It is switched off while the 3D view is on; your settings come back afterwards.
- With the 3D view off, or without Viewpoint, everything behaves like vanilla. Markers can lag one frame behind.
- Lua: `VFA3D.active()` is true while Viewpoint renders the 3D view.

### 2) Viewpoint 2D to 3D – Extended Patch (`Viewpoint2Dto3D_ExtendedPatch`)
Needs mod 1. Does the same for mods that use `IsoUtils.XToScreen/YToScreen`.
- Checked with: Leave A Message Note, Shotgun Trajectory, Home Inventory, Cold Breath, Hide Anywhere.
- Not covered: `renderPoly` (e.g. OffGrid) and mods with their own Java.

## Looks

### 3) Open All Containers Fix (`ViewpointOpenContainers`)
Open All Containers swaps a container for an "open" sprite that Viewpoint has no 3D shape for, so opened containers vanished. Now they use OAC's own geometry, or the closed original's shape if there is none.

### 4) Hide Camera (`ViewpointHideCamera`)
Hiding under a bed (Hide Anywhere) used to leave the camera at standing eye height, above the bed. Now it drops to the floor while you are hidden. The height is one constant in the Lua file (`EYE_HEIGHT`).

### 10) 3D Gun Effects, Tracers and Fireflies (`ViewpointEffects3D`)
Effects that were flat 2D or invisible in 3D are now drawn in Viewpoint's scene and hidden behind walls. Each part has an option.
- **Tracers:** the game's own bullet tracers in 3D, yours and other players'. Colours, length and speed come per ammo type, so weapon mods apply. With Viewpoint True Ballistics every round is drawn where it really flies, without double tracers.
- **Muzzle flash:** a bright flash that lights up the surroundings for a moment, whenever the game itself shows its muzzle flash (depends on weapon and chance; suppressors apply).
- **Gun smoke:** puffs start at the real muzzle and stay in the world, bigger in first person (adjustable). Muzzle Smoke's flat 2D puffs are hidden in 3D.
- **Impacts by material** (the game's own `MaterialType` of the hit tile, the same that picks the impact sound):

  | Material | Effect |
  |---|---|
  | Wood | dark hole, splinters |
  | Metal | small hole, sparks |
  | Hardened metal / armoured glass | only a scrape, frequent ricochets |
  | Stone, concrete, brick | hole, dust, some sparks |
  | Dirt, grass, sand, snow | spray, short-lived mark |

  Ricochets get likelier on hard surfaces at flat angles. Hole size depends on the ammo (mod ammo too). Holes fade after about 2 minutes, adjustable up to about 10.
- **Vehicles** (with mod 15): impacts show the part that was really hit: glass, sheet metal, tyre, armour or lamp, with sparks, glass splinters and ricochets. Bullet holes stay on the vehicle and move with it:

  | Part | Hole |
  |---|---|
  | Glass | hole with a crack halo |
  | Metal | hole with a bare rim |
  | Armour | scrape |

  A hole disappears when the part is replaced, repaired to 100 % or the window shatters. Other players' shots at vehicles get the vehicle material too.
- **JB's Fireflies:** drawn in 3D and hidden behind walls, or switched off in 3D (option).
- The game's impact sound and hits on placed items still happen in 3D. The 2D view is unchanged.
- Options: Options > Mods > Viewpoint 3D Effects. All of them are also in the sandbox (see [Server settings](#server-settings-sandbox--viewpoint-fixes)).

## Controls

### 5) Item Placement 3D (`ViewpointPlaceItems`)
Placing an item (right-click > Place item) showed no preview and did nothing in 3D. Now:
- a 3D preview at the cursor or crosshair, on the floor or on furniture;
- **R** / **Shift+R** rotate;
- the normal "toggle mode" key changes the surface;
- **left click** places, **right click** cancels;
- a hint shows when the spot is invalid.

Items without a 3D model show no preview. The 2D view is unchanged.

### 6) Right Click in Mouse Mode (`ViewpointMouseRightClick`)
In Viewpoint's mouse mode (free cursor: middle click, loot window, settings, pause) right click started the aim stance. Now it opens the normal context menu. In crosshair view right click stays aim. If another mod swallows the click, the menu for the square under the mouse opens anyway.

### 7) Crosshair Use (`ViewpointCrosshairUse`)
Needs mods 6 and 9.
- **Left click** on a light switch you look at switches it, like a left click in the normal view (instant when close, no walking). The game finds the clicked object with its 2D picture, which in 3D only hit when you happened to stand just right. Now the object in the crosshair counts.
- A **short right click** (under 0.25 s) opens the normal right-click menu for exactly the object you look at, with a free mouse pointer. Holding the button stays aim.

### 8) Prone on X (`ViewpointProneKey`)
Lethal Stealth: **X** goes prone, X again gets up. This works in the 3D view only, and not with a firearm in the primary hand, because X is "Rack firearm" there. The Prone entry is removed from the right-click menu; Lethal Stealth's own key still works.

### 11) Over the Shoulder in 3D (`ViewpointShoulderLook3D`)
Needs mod 1 and Over the Shoulder. In the 3D view **hold the look key** (default **Caps Lock**, Options > Key Bindings > "Look around in 3D") and move the mouse:
- the camera looks around while you keep walking where you were going;
- head and upper body turn, and your view widens like Over the Shoulder in 2D;
- releasing the key turns the camera back, and aiming ends the look.

The middle mouse button stays free for Viewpoint's mouse mode. In the 2D view Over the Shoulder works as before.

### 14) Hold to Zoom (`ViewpointZoomHold`)
Needs Project Viewpoint QOL. You zoom only while holding a key (default Left Shift) and turning the wheel, in bigger steps; releasing snaps back to the normal view. The plain wheel no longer zooms. The key is always the player's own choice; the zoom step can be set by the server.

## Shooting

### 12) Aim Spread (`ViewpointAimSpread`)
Needs **Viewpoint True Ballistics** ([3812864848](https://steamcommunity.com/sharedfiles/filedetails/?id=3812864848)), loaded after it. Changes gameplay for your own shots.
- Every shot leaves at a random point inside the game's own aiming circle (the brackets), instead of True Ballistics' own spread.
  - The circle shrinks with Aiming skill, steady aim and focus, and grows with movement and moodles (mods that change it apply).
  - It is used at **1.5x** its size by default. The round then flies normally with True Ballistics.
- Only active with the sandbox option **Firearms Use Damage Chance = Disabled**. With "Zombies only" or "All", shots go exactly to the reticle.
- Bushes do not stop bullets but can deflect them: 10 % chance per bush, up to 10° sideways and up/down (both adjustable). The round flies on and hits whatever is on its new path.
- With Project Viewpoint ADS: ADS's own hip-fire spread is switched off while Aim Spread is on.

### 13) Crosshair Options (`ViewpointReticle`)
Viewpoint's centre dot can be shown always, only with a firearm, with any weapon, only while aiming, or never. You can choose dot, cross or ring, plus size, opacity, colour and outline. The game's aiming brackets can be hidden in 3D too.

### 15) Vehicle Ballistics & Penetration (`ViewpointVehicleBallistics`)
Realistic vehicle hits, penetration through walls and vehicles, ricochets, and shooting from vehicles. Full description: **[VehicleBallistics.md](VehicleBallistics.md)**.

## Interaction list

### 9) Interaction List Cleanup (`ViewpointInteractList`)
Viewpoint builds the list next to the crosshair from the game's right-click menu. Mod entries about yourself therefore appeared for every object and even became the heading ("Wellness" over a door). They are now hidden in Viewpoint's list only:
- Lifestyle: Wellness/Yoga, dancing, sharing knowledge, cleaning;
- the admin debug menu;
- Container Options, Decoholic, Door Bar, Open All Containers auto-close.

Hide Anywhere entries move to the top. Hint texts like "Missing a Hammer..." no longer pop up. The normal right-click menu is untouched.

---

## Server settings (Sandbox > Viewpoint Fixes)

All mod options of mods 10, 12, 13, 14 and 15 are mirrored on the sandbox page **Viewpoint Fixes**. For each of the mods 10, 12, 13 and 14 the admin sets **who sets the options**:

| Value | Meaning |
|---|---|
| 1 = Players choose (default) | Everyone uses Options > Mods; the sandbox values are ignored. |
| 2 = Server values (greyed out) | Everyone uses the sandbox values; the entries in Options > Mods are locked and show a note. |
| 3 = Server values (hidden) | Like 2, but the entries are hidden, so the list stays short. |

- The players' own values stay saved in `ModOptions.ini` and come back in other games.
- Key bindings always stay the player's choice.
- Changes made by an admin during the game apply within one in-game minute.

All options with defaults: [viewpointfixes-sandbox.generated.md](viewpointfixes-sandbox.generated.md). The vehicle mod's options: [vehicle-ballistics-options.generated.md](vehicle-ballistics-options.generated.md).

To control **any other mod's** options in the same way, use [Mod Options Control](ModOptionsControl.md).

## Notes
- Mod IDs are the names in brackets. If you also subscribed the separate items "Viewpoint 2D to 3D" / "– Extended Patch", switch them off: they use the same IDs.
- For door lighting use the original Door Fix by GamerKreep ([3812151347](https://steamcommunity.com/sharedfiles/filedetails/?id=3812151347)).
- Problems: please report the mod name and the matching lines from `console.txt` ([issues](https://github.com/T0XiQ96/project-viewpoint-fixes/issues)).
