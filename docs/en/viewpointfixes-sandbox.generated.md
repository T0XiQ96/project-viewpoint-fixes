### 3D Gun Effects (ViewpointEffects3D)

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.Effects3D_Control` | Who sets the options | 1 | 1 = Players choose / 2 = Server values (greyed out) / 3 = Server values (hidden) | Players choose: everyone uses Options > Mods, the values below are ignored. Server values: everyone uses the values below; the entries in Options > Mods are greyed out or hidden. Key bindings always stay the player's choice. |
| `ViewpointFixes.Effects3D_Tracers` | Bullet tracers (`tracers`) | on |  | The game's own tracers, drawn in 3D. Look per ammo type. |
| `ViewpointFixes.Effects3D_TracerWidth` | Tracer thickness (`tracerWidth`) | 1.1 | 0.3 – 3 | 1 = as set by the game. |
| `ViewpointFixes.Effects3D_TracerPath` | Faint bullet path line (`tracerPath`) | on |  | The thin fading line from the muzzle to the bullet. |
| `ViewpointFixes.Effects3D_Flash` | Muzzle flash glow (`flash`) | on |  | A bright flash at the real muzzle whenever the game shows its own muzzle flash. |
| `ViewpointFixes.Effects3D_FlashSize` | Muzzle flash size (`flashSize`) | 1.25 | 0.3 – 2 | 1.25 = default size. |
| `ViewpointFixes.Effects3D_Light` | Muzzle flash lights the surroundings (`light`) | on |  | A short warm light at the muzzle for every shot. |
| `ViewpointFixes.Effects3D_LightStrength` | Muzzle light strength (`lightStrength`) | 1.1 | 0.2 – 2 | 1.1 = default. |
| `ViewpointFixes.Effects3D_Smoke` | Gun smoke (`smoke`) | on |  | Smoke puffs that start at the muzzle and stay in the world. |
| `ViewpointFixes.Effects3D_SmokeAmount` | Smoke amount (`smokeAmount`) | 0.5 | 0 – 2 | 0.5 = default. |
| `ViewpointFixes.Effects3D_SmokeFP` | Smoke size in first person (`smokeFP`) | 0.8 | 0.5 – 3 | Factor on the puff size in first person (third person always 1). |
| `ViewpointFixes.Effects3D_Impacts` | Bullet impacts (`impacts`) | on |  | Dust, splinters, sparks and ricochets by material. Size depends on the ammo. |
| `ViewpointFixes.Effects3D_Holes` | Bullet holes (`holes`) | on |  | Holes stay on walls and objects, short-lived marks on dirt, grass, sand and snow. |
| `ViewpointFixes.Effects3D_HoleTime` | Bullet hole lifetime (`holeTime`) | 5 | 0.1 – 5 | 1 = about 2 minutes on walls, 25 seconds on the ground; 5 = about 10 / 2 minutes. |
| `ViewpointFixes.Effects3D_Fireflies` | JB's Fireflies in 3D (`fireflies`) | on |  | On: fireflies drawn in 3D, hidden behind walls. Off: no fireflies in the 3D view. |

### Aim Spread (ViewpointAimSpread)

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.AimSpread_Control` | Who sets the options | 1 | 1 = Players choose / 2 = Server values (greyed out) / 3 = Server values (hidden) | Players choose: everyone uses Options > Mods, the values below are ignored. Server values: everyone uses the values below; the entries in Options > Mods are greyed out or hidden. Key bindings always stay the player's choice. |
| `ViewpointFixes.AimSpread_Spread` | Spread inside the game's aiming circle (`spread`) | on |  | Every shot leaves at a random point inside the game's aiming circle. Only with Firearms Use Damage Chance = Disabled. |
| `ViewpointFixes.AimSpread_Scale` | Circle size factor (`scale`) | 1.5 | 0.25 – 2 | 1 = exactly the brackets, 1.5 = default. |
| `ViewpointFixes.AimSpread_Cover` | Bushes deflect bullets (`cover`) | on |  | Bushes do not stop bullets but can deflect them. |
| `ViewpointFixes.AimSpread_BushChance` | Deflection chance per bush (%) (`bushChance`) | 10 | 0 – 50 | Chance that a bullet flying through a bush is deflected. |
| `ViewpointFixes.AimSpread_DeflectDeg` | Deflection angle (degrees) (`deflectDeg`) | 10 | 0 – 30 | How far a deflected bullet can turn away, sideways and up/down. |

### Crosshair Options (ViewpointReticle)

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.Reticle_Control` | Who sets the options | 1 | 1 = Players choose / 2 = Server values (greyed out) / 3 = Server values (hidden) | Players choose: everyone uses Options > Mods, the values below are ignored. Server values: everyone uses the values below; the entries in Options > Mods are greyed out or hidden. Key bindings always stay the player's choice. |
| `ViewpointFixes.Reticle_Mode` | Show the dot (`mode`) | 1 | 1 = Always / 2 = Only with a firearm / 3 = With any weapon / 4 = Only while aiming / 5 = Never | When Viewpoint's centre dot is drawn. |
| `ViewpointFixes.Reticle_Style` | Style (`style`) | 1 | 1 = Dot / 2 = Cross / 3 = Ring | Shape of the crosshair. |
| `ViewpointFixes.Reticle_Size` | Size (pixels) (`size`) | 2 | 1 – 12 | 2 = Viewpoint's default dot. |
| `ViewpointFixes.Reticle_Opacity` | Opacity (`opacity`) | 1 | 0.05 – 1 | 1 = fully opaque. |
| `ViewpointFixes.Reticle_Color` | Colour (`color`) | 1 | 1 = White / 2 = Green / 3 = Red / 4 = Yellow / 5 = Cyan / 6 = Pink |  |
| `ViewpointFixes.Reticle_Outline` | Dark outline (`outline`) | on |  | A thin dark edge so the crosshair stays visible on bright backgrounds. |
| `ViewpointFixes.Reticle_Brackets` | Show the game's aiming brackets (`brackets`) | on |  | The brackets that show how steady your aim is (3D view). |

### Hold to Zoom (ViewpointZoomHold)

| Sandbox option | Label | Default | Range | Description |
|---|---|---|---|---|
| `ViewpointFixes.ZoomHold_Control` | Who sets the options | 1 | 1 = Players choose / 2 = Server values (greyed out) / 3 = Server values (hidden) | Players choose: everyone uses Options > Mods, the values below are ignored. Server values: everyone uses the values below; the entries in Options > Mods are greyed out or hidden. Key bindings always stay the player's choice. |
| `ViewpointFixes.ZoomHold_Step` | Zoom step per wheel notch (`step`) | 15 | 5 – 40 | Field-of-view change per notch. The zoom key itself always stays the player's choice. |

