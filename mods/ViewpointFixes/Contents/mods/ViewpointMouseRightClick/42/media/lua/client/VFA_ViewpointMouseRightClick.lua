-- Project Viewpoint, Mausmodus: Rechtsklick oeffnet das normale Rechtsklick-Menue.
--
-- Normalerweise oeffnet das Spiel das Menue selbst (Viewpoint lenkt dafuer ISCoordConversion.ToWorld auf das Feld
-- unter dem Mauszeiger um, der Java-Teil verhindert, dass der Rechtsklick als Zielen zaehlt). Klappt das einmal nicht
-- (z. B. auf einem Server, wenn ein anderer Mod den Klick schluckt), oeffnet diese Datei das Menue selbst: kurzer
-- Rechtsklick im Mausmodus und nach zwei Ticks noch kein Menue offen -> Menue fuer alles auf dem Feld unter dem
-- Mauszeiger (Viewpoint.Mouse.worldX/Y), inklusive Spieler, NPCs und Zombies dort.

local TAP_MS = 350
local WAIT_TICKS = 2

local rightDown, rightSince = false, 0
local pending = -1
local logged = false

local function mouseMode()
    if VFA_VpMouse ~= nil then return VFA_VpView == true and VFA_VpMouse == true end
    -- Java-Teil nicht geladen: Mauszeiger ueber der 3D-Welt
    local m = Viewpoint and Viewpoint.Mouse
    return m ~= nil and m.worldX() ~= nil
end

local function menuOpen()
    local menu = getPlayerContextMenu(0)
    return menu ~= nil and menu:isAnyVisible()
end

local function typing()
    local chat = ISChat and ISChat.instance
    return chat and chat.textEntry and chat.textEntry.isFocused and chat.textEntry:isFocused()
end

local function overUI()
    local uis = UIManager.getUI()
    for i = 0, uis:size() - 1 do
        local ui = uis:get(i)
        if ui:isMouseOver() then return true end
    end
    return false
end

local function openMenu()
    local player = getSpecificPlayer(0)
    local m = Viewpoint and Viewpoint.Mouse
    if not (player and m) then return end
    local wx, wy = m.worldX(), m.worldY()
    if not (wx and wy) then return end
    local sq = getCell():getGridSquare(math.floor(wx), math.floor(wy), math.floor(player:getZ()))
    if not sq then return end
    local worldobjects = {}
    for i = 0, sq:getObjects():size() - 1 do table.insert(worldobjects, sq:getObjects():get(i)) end
    for i = 0, sq:getMovingObjects():size() - 1 do table.insert(worldobjects, sq:getMovingObjects():get(i)) end
    if #worldobjects == 0 then return end
    ISContextManager.getInstance().createWorldMenu(0, nil, worldobjects, getMouseX(), getMouseY())
    if not logged then
        logged = true
        print("[ViewpointMouseRightClick] opened the right-click menu for the square under the mouse (game did not)")
    end
end

local function onTick()
    local right = isMouseButtonDown(1)
    if right and not rightDown then
        rightDown, rightSince = true, getTimestampMs()
    elseif not right and rightDown then
        rightDown = false
        if getTimestampMs() - rightSince < TAP_MS and isIngameState() and mouseMode() and not typing()
                and not menuOpen() and not overUI() then
            pending = WAIT_TICKS
        end
    end
    if pending >= 0 then
        if menuOpen() then
            pending = -1            -- das Spiel hat das Menue selbst geoeffnet
        elseif pending == 0 then
            pending = -1
            local ok, err = pcall(openMenu)
            if not ok then print("[ViewpointMouseRightClick] " .. tostring(err)) end
        else
            pending = pending - 1
        end
    end
end

Events.OnTick.Add(onTick)
