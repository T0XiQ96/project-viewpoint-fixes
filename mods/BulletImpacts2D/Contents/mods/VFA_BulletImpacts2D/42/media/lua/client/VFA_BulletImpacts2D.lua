-- Bullet Impacts 2D
-- Einschussloecher, Funken, Staub/Splitter und Abpraller in der normalen (isometrischen) Ansicht.
-- Einschlagpunkt: vom Spiel selbst (CombatManager.addTracerEffect, Java-Teil VFABulletImpacts.poll()).
-- Material: die Tile-Eigenschaft "MaterialType" des getroffenen Feldes - dieselbe, nach der das Spiel den
-- Einschlag-Sound waehlt. Munition aus dem Munitionstyp der Waffe (auch Mod-Munition, am Namen erkannt).
-- Ist die 3D-Ansicht von Project Viewpoint an, zeichnet diese Mod nichts (das macht dort ViewpointEffects3D).

VFA_BulletImpacts2D = VFA_BulletImpacts2D or {}
local BI = VFA_BulletImpacts2D

BI.settings = { holes = true, holeTime = 1.0, sparks = true, dust = true, ricochets = true, size = 1.0 }

local tex = {}
local holes, parts = {}, {}
local MAX_HOLES, MAX_PARTS = 250, 300
local options

-- Material: hole = Lochgroesse (Pixel), scrape = helle Schramme statt Loch, spark = Funkenchance,
-- dust = Anzahl Staubwolken, dc = Staubfarbe, hc = Lochfarbe, ric = Abprall-Grundchance, ground = Gelaende
local M = {}
local function mat(hole, scrape, hc, spark, dust, dc, ric, ground)
    return { hole = hole, scrape = scrape, hc = hc, spark = spark, dust = dust, dc = dc, ric = ric, ground = ground }
end
M.Concrete    = mat(7, false, { 0.10, 0.10, 0.10, 0.85 }, 0.35, 3, { 0.70, 0.70, 0.68 }, 0.30, false)
M.Plaster     = mat(6, false, { 0.12, 0.11, 0.10, 0.80 }, 0.00, 3, { 0.88, 0.86, 0.82 }, 0.05, false)
M.Stone       = mat(6, false, { 0.12, 0.12, 0.12, 0.80 }, 0.45, 3, { 0.62, 0.60, 0.58 }, 0.40, false)
M.Brick       = mat(7, false, { 0.14, 0.07, 0.05, 0.85 }, 0.25, 3, { 0.62, 0.33, 0.24 }, 0.25, false)
M.Cinderblock = mat(7, false, { 0.10, 0.10, 0.10, 0.85 }, 0.30, 3, { 0.66, 0.66, 0.64 }, 0.25, false)
M.Wood        = mat(5, false, { 0.06, 0.04, 0.02, 0.95 }, 0.00, 2, { 0.55, 0.40, 0.25 }, 0.03, false)
M.Wood_Solid  = mat(5, false, { 0.05, 0.035, 0.02, 0.95 }, 0.00, 2, { 0.50, 0.36, 0.22 }, 0.06, false)
M.Metal       = mat(4, false, { 0.04, 0.04, 0.05, 0.95 }, 0.90, 1, { 0.35, 0.35, 0.36 }, 0.45, false)
M.Metal_Light = mat(4, false, { 0.03, 0.03, 0.03, 0.95 }, 0.70, 1, { 0.35, 0.35, 0.36 }, 0.20, false)
M.Metal_Large = mat(3, false, { 0.05, 0.05, 0.06, 0.90 }, 0.95, 1, { 0.35, 0.35, 0.36 }, 0.55, false)
M.Metal_Solid = mat(4, true,  { 0.75, 0.75, 0.72, 0.55 }, 1.00, 1, { 0.40, 0.40, 0.40 }, 0.80, false)
M.Glass       = mat(4, false, { 0.85, 0.88, 0.90, 0.45 }, 0.30, 2, { 0.90, 0.95, 1.00 }, 0.05, false)
M.Glass_Solid = mat(4, true,  { 0.90, 0.92, 0.95, 0.50 }, 0.20, 1, { 0.90, 0.95, 1.00 }, 0.50, false)
M.Plastic     = mat(4, false, { 0.08, 0.08, 0.08, 0.85 }, 0.00, 1, { 0.80, 0.80, 0.80 }, 0.05, false)
M.Ceramic     = mat(5, false, { 0.20, 0.20, 0.20, 0.70 }, 0.10, 2, { 0.92, 0.92, 0.90 }, 0.20, false)
M.Soft        = mat(3, false, { 0.08, 0.07, 0.07, 0.80 }, 0.00, 1, { 0.55, 0.52, 0.50 }, 0.00, false)
M.Dirt        = mat(9, false, { 0.10, 0.07, 0.05, 0.60 }, 0.00, 4, { 0.45, 0.35, 0.24 }, 0.02, true)
M.Grass       = mat(8, false, { 0.10, 0.08, 0.05, 0.50 }, 0.00, 3, { 0.36, 0.40, 0.22 }, 0.02, true)
M.Gravel      = mat(7, false, { 0.12, 0.12, 0.12, 0.55 }, 0.35, 3, { 0.55, 0.53, 0.50 }, 0.30, true)
M.Sand        = mat(9, false, { 0.30, 0.26, 0.18, 0.45 }, 0.00, 4, { 0.78, 0.70, 0.52 }, 0.02, true)
M.Snow        = mat(9, false, { 0.55, 0.58, 0.62, 0.40 }, 0.00, 4, { 0.95, 0.96, 1.00 }, 0.00, true)
local ALIAS = { Default = "Concrete", Glass_Light = "Glass", Rubber = "Soft", Fabric = "Soft", Carpet = "Soft" }

local function material(name)
    if name == "Flesh" or name == "Flesh_Hollow" then return nil end
    name = ALIAS[name] or name
    return M[name] or M.Concrete
end

-- Munition: Groesse/Wucht und Abprall-Faktor
local function ammo(key)
    local k = string.lower(key or "")
    if k:find("shotgun") or k:find("shell") or k:find("buck") or k:find("gauge") then return 0.55, 1.3 end
    if k:find("cap") then return 0, 0 end
    for _, w in ipairs({ "308", "762", "3006", "30-06", "50bmg", "338", "3030", "545", "556", "223", "rifle" }) do
        if k:find(w, 1, true) then return 1.35, 0.75 end
    end
    for _, w in ipairs({ "357", "44", "magnum", "454" }) do
        if k:find(w, 1, true) then return 1.15, 0.9 end
    end
    if k:find("22") or k:find("380") then return 0.7, 1.1 end
    return 0.95, 1.0
end

local function viewpoint3D()
    if VFA3D and VFA3D.active and VFA3D.active() then return true end
    return VFA_FX_VpOn == true
end

local function loadTextures()
    if tex.spark then return true end
    tex.hole = { getTexture("media/textures/VFA_BI2D/hole1.png"), getTexture("media/textures/VFA_BI2D/hole2.png"), getTexture("media/textures/VFA_BI2D/hole3.png") }
    tex.dust = { getTexture("media/textures/VFA_BI2D/dust1.png"), getTexture("media/textures/VFA_BI2D/dust2.png"), getTexture("media/textures/VFA_BI2D/dust3.png") }
    tex.spark = getTexture("media/textures/VFA_BI2D/spark.png")
    return tex.spark ~= nil
end

-- Tuer/Fenster auf der Feldkante beim Einschlag (damit das Loch beim Oeffnen verschwindet)
local function doorState(o)
    if not o then return nil end
    local open = (o.IsOpen and o:IsOpen()) or false
    local destroyed = (o.isDestroyed and o:isDestroyed()) or false
    local smashed = (o.isSmashed and o:isSmashed()) or false
    local present = o:getSquare() ~= nil and o:getObjectIndex() >= 0
    return tostring(open) .. tostring(destroyed) .. tostring(smashed) .. tostring(present)
end

local function findDoor(x, y, z)
    local cell = getCell()
    local ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    local best, bestD = nil, 0.15
    for dx = -1, 1 do
        for dy = -1, 1 do
            local sq = cell:getGridSquare(ix + dx, iy + dy, iz)
            if sq then
                local objs = sq:getObjects()
                for i = 0, objs:size() - 1 do
                    local o = objs:get(i)
                    local movable = instanceof(o, "IsoDoor") or instanceof(o, "IsoWindow")
                        or (instanceof(o, "IsoThumpable") and (o:isDoor() or o:isWindow()))
                    if movable then
                        local ox, oy = ix + dx, iy + dy
                        local north = o.getNorth and o:getNorth() or false
                        local d
                        if north then
                            d = math.abs(y - oy) + math.max(0, ox - x, x - (ox + 1))
                        else
                            d = math.abs(x - ox) + math.max(0, oy - y, y - (oy + 1))
                        end
                        if d < bestD then best, bestD = o, d end
                    end
                end
            end
        end
    end
    return best
end

local function addPart(p)
    if #parts >= MAX_PARTS then table.remove(parts, 1) end
    table.insert(parts, p)
end

-- Fahrzeug am Einschlagpunkt? Material vom Fahrzeug-Modell (Vehicle Ballistics: Glas, Blech, Reifen, Panzerung)
local function vehicleMaterial(x, y, z, sx, sy)
    local VB = VehicleBallisticsVF or VehicleBallistics2D
    if not (VB and VB.vehicleMaterial) then return nil end
    local dx, dy = x - sx, y - sy
    local l = math.sqrt(dx * dx + dy * dy)
    if l < 0.001 then return nil end
    local zz = z
    if z - math.floor(z) < 0.05 then zz = math.floor(z) + 0.5 end   -- Endpunkt am Boden: auf Muendungshoehe
    local ok, name = pcall(VB.vehicleMaterial, sx, sy, zz, x + dx / l * 0.5, y + dy / l * 0.5, zz)
    if ok then return name end
    return nil
end

local function impact(x, y, z, sx, sy, ammoKey)
    local cell = getCell()
    local sq = cell:getGridSquare(math.floor(x), math.floor(y), math.floor(z))
    local vname = vehicleMaterial(x, y, z, sx, sy)
    if vname == "open" then return end                                   -- durchs offene Fenster
    if not vname and sq and sq:getMovingObjects():size() > 0 then return end   -- Treffer am Ziel: Blut macht das Spiel
    local props = sq and sq:getProperties()
    local name = vname or (props and props:get("MaterialType")) or nil
    local m = material(name or ((z - math.floor(z)) < 0.05 and "Dirt" or "Concrete"))
    if not m then return end
    local power, ricMul = ammo(ammoKey)
    if power <= 0 then return end
    local s = BI.settings
    local size = s.size or 1

    local dx, dy = x - sx, y - sy
    local len = math.sqrt(dx * dx + dy * dy)
    if len < 0.001 then dx, dy, len = 1, 0, 1 end
    dx, dy = dx / len, dy / len
    -- grob: Wand an Feldkante -> streifender Winkel, wenn die Flugrichtung fast parallel zur Kante ist
    local fx, fy = x - math.floor(x), y - math.floor(y)
    local graze = 0.5
    if math.min(fx, 1 - fx) < 0.12 then graze = math.abs(dy) elseif math.min(fy, 1 - fy) < 0.12 then graze = math.abs(dx) end

    local ricochet = s.ricochets and ZombRandFloat(0, 1) < m.ric * ricMul * (0.25 + 0.75 * graze * graze)
    -- am Fahrzeug keine festen Loecher (das Fahrzeug faehrt weg); Funken und Splitter schon
    if s.holes and m.hole > 0 and not ricochet and not vname then
        if #holes >= MAX_HOLES then table.remove(holes, 1) end
        local door = not m.ground and findDoor(x, y, z) or nil
        table.insert(holes, {
            x = x, y = y, z = z, tex = tex.hole[ZombRand(3) + 1],
            size = m.hole * (m.scrape and 1 or (0.8 + 0.4 * power)) * size * ZombRandFloat(0.85, 1.15),
            c = m.hc, age = 0, life = (m.ground and 25 or 120) * (s.holeTime or 1),
            door = door, state = doorState(door),
        })
    end
    -- Funken
    local sparkCount = 0
    if s.sparks then
        if ricochet then sparkCount = ZombRand(5, 9)
        elseif ZombRandFloat(0, 1) < m.spark then sparkCount = ZombRand(3, 7) end
    end
    for _ = 1, sparkCount do
        local a = ZombRandFloat(0, math.pi * 2)
        local sp = ZombRandFloat(1.5, 4.5)
        addPart({ kind = "spark", x = x, y = y, z = z, vx = math.cos(a) * sp - dx * 1.5, vy = math.sin(a) * sp - dy * 1.5,
            vz = ZombRandFloat(0.5, 2.5), age = 0, life = ZombRandFloat(0.15, 0.35), size = 3 * size })
    end
    -- Querschlaeger: schneller heller Funke in reflektierter Richtung
    if ricochet then
        local rx, ry = -dx + ZombRandFloat(-0.5, 0.5), -dy + ZombRandFloat(-0.5, 0.5)
        if math.min(fx, 1 - fx) < 0.12 then rx, ry = -dx, dy elseif math.min(fy, 1 - fy) < 0.12 then rx, ry = dx, -dy end
        for i = 0, 5 do
            addPart({ kind = "spark", x = x, y = y, z = z, vx = rx * 18, vy = ry * 18, vz = ZombRandFloat(0, 0.6),
                age = -i * 0.012, life = 0.18, size = 2.5 * size })
        end
    end
    -- Staub / Splitter / Rauch
    if s.dust then
        for _ = 1, math.floor(m.dust * (0.6 + 0.5 * power) + 0.5) do
            addPart({ kind = "dust", x = x, y = y, z = z, tex = tex.dust[ZombRand(3) + 1],
                vx = -dx * ZombRandFloat(0.2, 0.9) + ZombRandFloat(-0.3, 0.3), vy = -dy * ZombRandFloat(0.2, 0.9) + ZombRandFloat(-0.3, 0.3),
                vz = ZombRandFloat(0.05, 0.35), age = 0, life = ZombRandFloat(0.4, 0.9),
                size0 = 6 * size, size1 = ZombRandFloat(16, 26) * size * (0.7 + 0.4 * power), c = m.dc })
        end
    end
end

local function lightAt(x, y, z)
    local sq = getCell():getGridSquare(math.floor(x), math.floor(y), math.floor(z))
    if not sq then return 0.5 end
    return math.max(0.08, math.min(1, sq:getLightLevel(0)))
end

local function draw(t, size, r, g, b, a, x, y, z)
    if not t or a <= 0.004 then return end
    local ts = Core.getTileScale() or 1
    IsoSprite.renderTextureWithDepth(t, size * ts, size * ts, r, g, b, a, x, y, z - (size * 0.5) / (96.0 * ts))
end

local lastMs = 0
local function onTick()
    if not VFABulletImpacts or not loadTextures() then return end
    while true do
        local e = VFABulletImpacts.poll()
        if not e then break end
        if not viewpoint3D() then
            local v = {}
            for part in string.gmatch(e, "([^;]*)") do table.insert(v, part) end
            local x, y, z, sx, sy = tonumber(v[1]), tonumber(v[2]), tonumber(v[3]), tonumber(v[4]), tonumber(v[5])
            if x and y and z then
                local ok, err = pcall(impact, x, y, z, sx or x, sy or y, v[6])
                if not ok then print("[BulletImpacts2D] " .. tostring(err)) end
            end
        end
    end
end

local function onPostRender()
    if viewpoint3D() or (#holes == 0 and #parts == 0) then return end
    local now = getTimestampMs()
    local dt = lastMs > 0 and math.min(0.1, (now - lastMs) / 1000) or 0
    lastMs = now
    local player = getSpecificPlayer(0)
    local pz = player and math.floor(player:getZ()) or 0

    for i = #holes, 1, -1 do
        local h = holes[i]
        h.age = h.age + dt
        local gone = h.age >= h.life or (h.door and doorState(h.door) ~= h.state)
        if gone then
            table.remove(holes, i)
        elseif math.floor(h.z) == pz then
            local fade = math.min(1, (h.life - h.age) / (h.life * 0.2))
            local l = lightAt(h.x, h.y, h.z)
            draw(h.tex, h.size, h.c[1] * l, h.c[2] * l, h.c[3] * l, h.c[4] * fade, h.x, h.y, h.z)
        end
    end
    for i = #parts, 1, -1 do
        local p = parts[i]
        p.age = p.age + dt
        if p.age >= p.life then
            table.remove(parts, i)
        elseif p.age >= 0 then
            local t = p.age / p.life
            if p.kind == "spark" then
                p.vz = p.vz - 9.8 * dt
                p.x, p.y, p.z = p.x + p.vx * dt, p.y + p.vy * dt, p.z + p.vz * dt / 2.449
                draw(tex.spark, p.size, 1, 0.75 + 0.2 * (1 - t), 0.35, 1 - t * t, p.x, p.y, p.z)
            else
                local drag = math.exp(-3 * dt)
                p.vx, p.vy, p.vz = p.vx * drag, p.vy * drag, p.vz * drag + 0.05 * dt
                p.x, p.y, p.z = p.x + p.vx * dt, p.y + p.vy * dt, p.z + p.vz * dt / 2.449
                local grow = 1 - (1 - math.min(1, t * 1.4)) ^ 2
                local l = lightAt(p.x, p.y, p.z)
                draw(p.tex, p.size0 + (p.size1 - p.size0) * grow, p.c[1] * l, p.c[2] * l, p.c[3] * l,
                    0.45 * math.min(1, t / 0.05) * (1 - t), p.x, p.y, p.z)
            end
        end
    end
end

-- Optionen ---------------------------------------------------------------------------------------------------

local function apply()
    if not options then return end
    local s = BI.settings
    s.holes = options.holes:getValue() == true
    s.holeTime = tonumber(options.holeTime:getValue()) or 1
    s.sparks = options.sparks:getValue() == true
    s.dust = options.dust:getValue() == true
    s.ricochets = options.ricochets:getValue() == true
    s.size = tonumber(options.size:getValue()) or 1
end

local function build()
    if options or not (PZAPI and PZAPI.ModOptions) then return end
    local page = PZAPI.ModOptions:create("VFA_BulletImpacts2D", "Bullet Impacts 2D")
    options = {}
    options.holes = page:addTickBox("holes", "Bullet holes", true, "Holes on walls and objects; hardened metal and armoured glass only get a scrape; short-lived marks on dirt, grass, sand and snow.")
    options.holeTime = page:addSlider("holeTime", "Bullet hole lifetime", 0.1, 5, 0.1, 1, "1 = about 2 minutes on walls, 25 seconds on the ground.")
    options.sparks = page:addTickBox("sparks", "Sparks", true, "Sparks on metal, stone and concrete.")
    options.dust = page:addTickBox("dust", "Dust, splinters and smoke", true, "Puffs in the colour of the material.")
    options.ricochets = page:addTickBox("ricochets", "Ricochets", true, "Hard surfaces at flat angles make bullets bounce off with sparks instead of a hole.")
    options.size = page:addSlider("size", "Effect size", 0.5, 2, 0.05, 1, "1 = default.")
    function page:apply() apply() end
    Events.OnMainMenuEnter.Add(apply)
end

build()
Events.OnGameStart.Add(apply)
Events.OnTick.Add(onTick)
Events.OnPostRender.Add(onPostRender)
