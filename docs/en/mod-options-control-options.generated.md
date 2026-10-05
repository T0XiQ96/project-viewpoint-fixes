| Sandbox option | Label | Default | Description |
|---|---|---|---|
| `ModOptionsControl.Enabled` | Enable Mod Options Control | on | Off: every player sets all mod options freely, all rules are ignored. |
| `ModOptionsControl.DefaultPolicy` | Default rule | 1 = Free | For options without their own rule. Free (default): players may change everything until an admin locks something. Locked: everything uses the admin's values unless freed. |
| `ModOptionsControl.HideLocked` | Hide locked options | on | On: locked options disappear from Options > Mods, so players only see what they may change (they can still show them greyed out on the Mod Options Control page). Off: they stay visible, greyed out and marked [Server]. |
| `ModOptionsControl.AdminsBypass` | Admins keep their own options | on | On: players who may change the sandbox are not locked, so they can set their values and copy them for everyone in the admin window. Off: admins get the same values as everyone (also in single player). |
| `ModOptionsControl.KeybindsLockable` | Key bindings can be locked | off | Off (recommended): key bindings in mod options always stay the player's choice. |
| `ModOptionsControl.LockedList` | Always locked | – | Optional list separated by ; - a page ID locks all options of that mod, PageID.optionID one option. Page IDs are shown in brackets in the admin window. Rules set in the admin window win. |
| `ModOptionsControl.FreeList` | Always free | – | Like “Always locked”, for exceptions when the default rule is Locked. |
