# Mod Options Control (Server)

Mod ID `ModOptionsControl` · Build 42.13+ · no Java, no ZombieBuddy · [Deutsch](../de/ModOptionsControl.md)

Server admins decide which **Mod Options** (Options > Mods) players may change, and set the values for everyone. It works with every mod that uses the game's own Mod Options (`PZAPI.ModOptions`); no mod needs to be changed.

## Quick start

1. Add `ModOptionsControl` to the server (`Mods=` and `WorkshopItems=`); clients get it automatically.
2. As an admin, set your own values in Options > Mods as usual.
3. Open the **admin window**: Admin Panel > *Mod Options Control*, or Options > Mods > *Mod Options Control* > *Open admin window*.
4. Click the left column of a mod (page row) or of single options until it says **Locked**.
5. Press **My values for all locked** (or *Use my value (selected)* for one option), then **Save & send**.

Players get the values immediately. Locked options disappear from their Options > Mods.

## Rules

Every page (mod) and every option has a rule: **–** (inherit), **Locked** or **Free**. Whether an option is locked is decided in this order:
1. the option's own rule;
2. the rule of its page;
3. the sandbox lists *Always free* / *Always locked* (single options first, then pages);
4. the sandbox *Default rule* (Free by default).

A locked option gets the admin's saved value. Without one it gets **the mod's default value**.

**Admin window buttons:**

| Button | Effect |
|---|---|
| Search | filter mods and options |
| Expand / collapse all | |
| Change rule (selected) | |
| Lock everything / Free everything | |
| Clear all rules | |
| My values for all locked | |
| Use my value (selected) | |
| Mod default (selected) | removes the server value |
| Export / Import | `Zomboid/Lua/ModOptionsControl_rules.txt`, to copy rules to another server |
| Save & send | |

Permission: everyone whose role may change the sandbox (`Capability.SandboxOptions`), plus access level admin; in single player always. Unauthorised changes are rejected and logged on the server.

## What players see

- **Locked options hidden** (default): only what they may change is listed. A note at the top of the mod says how many settings the server sets. Mods with nothing left to change disappear from the list.
- **Not hidden:** the options stay visible, greyed out and marked **[Server]**.
- On the page *Mod Options Control* players see how many options the server sets and can show hidden ones greyed out.
- The players' own values stay saved in `ModOptions.ini`. Forced values are never written there, so other games and servers keep the player's settings.
- Changes from the admin, and sandbox changes during the game, apply right away; the options page is rebuilt the next time it is opened.

## Sandbox (page *Mod Options Control*)

[mod-options-control-options.generated.md](mod-options-control-options.generated.md)

## How it works

- `PZAPI.ModOptions` is plain Lua. Every option has a value and an *enabled* flag, and every page has an `apply()` function.
- Mod Options Control sets the value, calls the option's `onChangeApply` and the page's `apply()`, exactly like pressing *Apply*, and disables the entry.
- Loading and saving `ModOptions.ini` is wrapped, so forced values never replace the player's own.
- Hiding works by filtering the entries while the game builds the Mods tab, and by rebuilding that tab when the rules change.
- The rules are stored on the server in the global mod data (`ModOptionsControl`, saved with the world) and sent to each client when it joins and after every change.

## Good to know

- When a player joins a server, the game reloads the server's Lua mods. Every mod a player sees in Options > Mods in multiplayer is therefore a server mod; Lua mods that only the player enabled are not running there.
- Mods with their own settings systems (the older "Mod Options" library, own files) cannot be controlled.
- Key bindings are not locked unless *Key bindings can be locked* is on.
- For Project Viewpoint - Fixes there is also a built-in per-mod switch in Sandbox > Viewpoint Fixes; both work together.

## Files

| File | Purpose |
|---|---|
| `shared/ModOptionsControl/MOC_Shared.lua` | rules, permission, checks |
| `server/ModOptionsControl/MOC_Server.lua` | storage, sending |
| `client/ModOptionsControl/MOC_Client.lua` | applying, hiding, rebuilding |
| `client/ModOptionsControl/MOC_AdminUI.lua` | admin window, own options page, admin panel button |
