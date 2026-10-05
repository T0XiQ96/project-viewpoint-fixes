-- Bauchlage (Lethal Stealth) nur noch per Taste X, im Viewpoint-3D-Bild.
--
-- 1) X legt den Charakter in Bauchlage, das naechste X steht wieder auf (Lethal Stealth entscheidet selbst, was gilt).
--    Nicht, solange eine Schusswaffe in der Hand ist: X ist im Spiel "Waffe durchladen".
-- 2) Der Eintrag "Bauchlage" / "Aufstehen" verschwindet aus dem Rechtsklick-Menue. Lethal Stealths eigene Taste
--    (Mod-Optionen, Standard U) funktioniert weiter. Waehlt man in den Lethal-Stealth-Optionen selbst X, wird
--    zweimal umgeschaltet - dann eine der beiden Tasten aendern.

local function active3D()
    local m = Viewpoint and Viewpoint.Mouse
    if not (m and m.worldX) then return false end
    return m.worldX() ~= nil
end

local function hideMenuEntry()
    if RET_LTS and RET_LTS.ShowProneContextualOption then
        RET_LTS.ShowProneContextualOption = function() return false end
    end
end

hideMenuEntry()
Events.OnGameStart.Add(hideMenuEntry)

local function typing()
    local chat = ISChat and ISChat.instance
    return chat and chat.textEntry and chat.textEntry.isFocused and chat.textEntry:isFocused()
end

local function onKey(key)
    if key ~= Keyboard.KEY_X then return end
    if not isIngameState() or typing() or not active3D() then return end
    if not (RET_LTS and RET_LTS.TriggerProneAction) then return end
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return end
    local weapon = player:getPrimaryHandItem()
    if weapon and instanceof(weapon, "HandWeapon") and weapon:isRanged() then return end
    RET_LTS.TriggerProneAction(player)
end

Events.OnKeyPressed.Add(onKey)
