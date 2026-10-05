-- Project Viewpoint, Fadenkreuz-Ansicht: direkt benutzen, was man ansieht.
--
-- 1) Linksklick auf einen angesehenen Lichtschalter = genau das, was ein Linksklick darauf in der normalen Ansicht
--    macht (ISObjectClickHandler): schaltet sofort, wenn man nah genug dran steht, ohne dass der Charakter erst zum
--    Objekt laeuft. Das Spiel selbst sucht das Objekt unter dem Mauszeiger mit der alten 2D-Bilderkennung - in 3D
--    trifft die den Schalter nur, wenn man zufaellig "richtig" steht. Hier zaehlt stattdessen das Objekt im
--    Fadenkreuz (dasselbe, fuer das Viewpoint "Licht an/aus" anzeigt); die 2D-Erkennung schaltet in der
--    Fadenkreuz-Ansicht keine Lichtschalter mehr (sonst doppelt = an und gleich wieder aus).
-- 2) Kurzer Rechtsklick (unter einer Viertelsekunde) auf das angesehene Objekt = das normale Rechtsklick-Menue des
--    Spiels fuer genau dieses Objekt, in der Mitte des Bildes, mit freiem Mauszeiger (Viewpoints Mausmodus).
--    Rechtsklick gedrueckt halten bleibt Zielen.
--
-- Braucht von ViewpointInteractList das angesehene Objekt (VFA_VpTarget) und von ViewpointMouseRightClick den
-- Zustand der Ansicht (VFA_VpView / VFA_VpMouse) und den Mausmodus-Wunsch (VFA_WantMouseMode).

local TAP_MS = 250
local TARGET_MS = 300

local function crosshair()
    return VFA_VpView == true and VFA_VpMouse == false
end

local function target()
    local object = VFA_VpTarget
    if object and VFA_VpTargetTime and getTimestampMs() - VFA_VpTargetTime < TARGET_MS then
        if object:getSquare() then return object end
    end
    return nil
end

local function typing()
    local chat = ISChat and ISChat.instance
    return chat and chat.textEntry and chat.textEntry.isFocused and chat.textEntry:isFocused()
end

local function player()
    local p = getSpecificPlayer(0)
    if p and not p:isDead() then return p end
    return nil
end

-- 1) Linksklick ------------------------------------------------------------------------------------------------

local originalLightSwitch = nil
local ownClick = false

local function wrapLightSwitch()
    if originalLightSwitch or not (ISObjectClickHandler and ISObjectClickHandler.doClickLightSwitch) then return end
    originalLightSwitch = ISObjectClickHandler.doClickLightSwitch
    ISObjectClickHandler.doClickLightSwitch = function(object, playerNum, playerObj)
        -- Klick aus der 2D-Bilderkennung in der Fadenkreuz-Ansicht: erledigt onLeftClick, hier nur schlucken
        if crosshair() and not ownClick then return true end
        return originalLightSwitch(object, playerNum, playerObj)
    end
end

local function onLeftClick()
    local p, object = player(), target()
    if not (p and object and instanceof(object, "IsoLightSwitch")) then return end
    if not (ISObjectClickHandler and ISObjectClickHandler.doClickSpecificObject) then return end
    ownClick = true
    local ok, err = pcall(ISObjectClickHandler.doClickSpecificObject, object, 0, p)
    ownClick = false
    if not ok then print("[ViewpointCrosshairUse] " .. tostring(err)) end
end

-- 2) Kurzer Rechtsklick ----------------------------------------------------------------------------------------

local function openMenu()
    local p, object = player(), target()
    if not (p and object) then return end
    local core = getCore()
    VFA_WantMouseMode = true
    ISContextManager.getInstance().createWorldMenu(0, nil, { object }, core:getScreenWidth() / 2, core:getScreenHeight() / 2)
end

-- Maustasten abfragen ------------------------------------------------------------------------------------------

local leftDown = false
local rightDown, rightSince = false, 0

local function onTick()
    wrapLightSwitch()

    local left = isMouseButtonDown(0)
    if left and not leftDown and crosshair() and isIngameState() and not typing() then
        onLeftClick()
    end
    leftDown = left

    local right = isMouseButtonDown(1)
    if right and not rightDown then
        rightDown, rightSince = true, getTimestampMs()
    elseif not right and rightDown then
        rightDown = false
        if getTimestampMs() - rightSince < TAP_MS and crosshair() and isIngameState() and not typing() then
            local ok, err = pcall(openMenu)
            if not ok then print("[ViewpointCrosshairUse] " .. tostring(err)) end
        end
    end
end

Events.OnTick.Add(onTick)
