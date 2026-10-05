-- Gegenstaende platzieren unter Project Viewpoint (3D).
--
-- Problem: Der Platzier-Cursor (ISPlace3DItemCursor) rechnet die Mausposition mit der alten isometrischen
-- Projektion um und zeichnet die Vorschau ueber die iso-Welt, die Viewpoint abschaltet. Im 3D-Bild sieht man
-- deshalb keine Vorschau, und der Cursor merkt nie, ob die Stelle gueltig ist -> es wird nichts platziert.
--
-- Loesung: Solange Viewpoint 3D rendert und der Platzier-Cursor aktiv ist, laeuft hier pro Tick dieselbe Logik
-- wie im Original (renderOpaqueObjectsInWorld: Position, Drehen mit R / Shift+R, Oberflaechen), nur mit dem Punkt,
-- auf den Viewpoint mit der Maus/dem Fadenkreuz zeigt. Die Vorschau zeichnet ViewpointPlaceItems.jar in die
-- 3D-Szene (Tabelle VFA_PlaceGhost). Linksklick platziert, Rechtsklick bricht ab.
-- Ist Viewpoint aus (normales 2D-Bild), passiert hier nichts.

VFA_PlaceGhost = nil

local ghost = {}
local lastCursor = nil
local wasLeft, wasRight = false, false

local function clearGhost()
    VFA_PlaceGhost = nil
    lastCursor = nil
end

local function pointer()
    local m = Viewpoint and Viewpoint.Mouse
    if not (m and m.worldX and m.worldY) then return nil end
    local x, y = m.worldX(), m.worldY()
    if x == nil or y == nil then return nil end
    return x, y
end

local function cursorOf(playerNum)
    local cell = getCell()
    if not cell then return nil end
    local drag = cell:getDrag(playerNum)
    if type(drag) == "table" and drag.isPlace3DCursor and drag.items and drag.items[1] and drag.chr then
        return drag
    end
    return nil
end

-- Originallogik des Cursors laufen lassen, aber mit der 3D-Mausposition; die Vorschau wird nur mitgeschrieben.
local function update(cursor, wx, wy)
    local rec
    local isoX, isoY, render = screenToIsoX, screenToIsoY, Render3DItem
    screenToIsoX = function() return wx end
    screenToIsoY = function() return wy end
    Render3DItem = function(item, sq, x, y, z, rot)
        rec = { item = item, sq = sq, x = x, y = y, z = z, rot = rot }
    end
    local floorX, floorY = math.floor(wx), math.floor(wy)
    local floorZ = math.floor(cursor.chr:getZ())
    local square = getCell():getGridSquare(floorX, floorY, floorZ)
    local ok, err = pcall(cursor.renderOpaqueObjectsInWorld, cursor, floorX, floorY, floorZ, square)
    screenToIsoX, screenToIsoY, Render3DItem = isoX, isoY, render
    if not ok then
        print("[ViewpointPlaceItems] " .. tostring(err))
        return nil
    end
    return rec
end

local function place(cursor)
    local sq = cursor.selectedSqDrop
    if not sq then return end
    cursor:tryBuild(sq:getX(), sq:getY(), sq:getZ())
end

local function step()
    local cursor = cursorOf(0)
    if not cursor then
        if lastCursor then clearGhost() end
        return
    end
    local wx, wy = pointer()
    if not wx then
        clearGhost()
        return
    end

    local left = isMouseButtonDown(0)
    local right = isMouseButtonDown(1)
    if cursor ~= lastCursor then
        lastCursor = cursor
        wasLeft, wasRight = left, right
    end

    local rec = update(cursor, wx, wy)
    -- Das Original soll im 3D-Bild nie selbst platzieren (es kennt die richtige Stelle nicht): dafuer sorgen wir.
    cursor.canBeBuild = false
    if not rec or not rec.sq then
        VFA_PlaceGhost = nil
        wasLeft, wasRight = left, right
        return
    end

    ghost.item, ghost.sq, ghost.x, ghost.y, ghost.z, ghost.rot = rec.item, rec.sq, rec.x, rec.y, rec.z, rec.rot
    VFA_PlaceGhost = ghost

    local sq = cursor.selectedSqDrop
    local valid = sq ~= nil and cursor:isValid(sq) or false
    cursor.vfaValid = valid

    if left and not wasLeft and valid then
        wasLeft, wasRight = left, right
        place(cursor)
        return
    end
    if right and not wasRight then
        wasLeft, wasRight = left, right
        getCell():setDrag(nil, 0)
        clearGhost()
        return
    end
    wasLeft, wasRight = left, right
end

local function onTick()
    local ok, err = pcall(step)
    if not ok then
        print("[ViewpointPlaceItems] " .. tostring(err))
        VFA_PlaceGhost = nil
    end
end

-- Hinweis, wenn die Stelle nicht passt (im Original zeigt das die rote Bodenkachel, die im 3D-Bild fehlt).
local function onUiDraw()
    local cursor = lastCursor
    if not (cursor and VFA_PlaceGhost and cursor.vfaValid == false) then return end
    local core = getCore()
    local ok, lang = pcall(function() return Translator.getLanguage():name() end)
    local text = (ok and lang == "DE") and "Hier kann nichts abgelegt werden" or "Can't place it here"
    getTextManager():DrawStringCentre(UIFont.Medium, core:getScreenWidth() / 2, core:getScreenHeight() * 0.62,
        text, 1.0, 0.35, 0.3, 1.0)
end

Events.OnTick.Add(onTick)
Events.OnPostUIDraw.Add(onUiDraw)
