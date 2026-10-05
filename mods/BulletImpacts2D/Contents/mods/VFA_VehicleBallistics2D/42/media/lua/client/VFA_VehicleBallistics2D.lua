-- Vehicle Ballistics 2D (Client): zeigt dem Schuetzen, was am Fahrzeug getroffen wurde, und setzt Wunden am eigenen Charakter,
-- wenn eine Kugel durch ein Fahrzeug den eigenen Charakter trifft. Der Java-Teil (VehicleBallistics2D) bestimmt Teil und Schaden.
-- Einzelspieler: Meldungen aus VehicleBallistics2D.poll(); Mehrspieler: Servernachrichten (Modul VehicleBallistics2D).

-- Kopie in Bullet Impacts 2D: tritt zurueck, wenn Project Viewpoint - Fixes (VehicleBallisticsVF) geladen ist
if "VehicleBallistics2D" ~= "VehicleBallisticsVF" and VehicleBallisticsVF ~= nil then return end

local MODULE = "VehicleBallistics2D"
local function bridge() return VehicleBallistics2D end
local options = { info = true, occupant = true }

local function build()
    if not (PZAPI and PZAPI.ModOptions) then return end
    local page = PZAPI.ModOptions:create("VehicleBallistics2D", "UI_VVB_Title")
    local info = page:addTickBox("info", "UI_VVB_ShowInfo", true, "UI_VVB_ShowInfo_tooltip")
    function page:apply()
        options.info = info:getValue() == true
    end
    Events.OnGameStart.Add(function() page:apply() end)
end
build()

local function partName(id)
    if not id then return getText("UI_VVB_Body") end
    local t = getTextOrNull("IGUI_VehiclePart" .. id)
    if t then return t end
    -- CamelCase -> Woerter
    return (string.gsub(id, "(%l)(%u)", "%1 %2"))
end

local function stateText(args)
    local key = "UI_VVB_State_" .. tostring(args.state)
    if args.kind == "fuel" and args.state == "leaking" then key = "UI_VVB_State_fuel" end
    return getTextOrNull(key) or tostring(args.state)
end

local last = { text = nil, time = 0 }

local function show(args)
    if not options.info then return end
    local player = getPlayer()
    if not player then return end
    local name = partName(args.part)
    if args.kind == "armor" and args.behind then name = partName(args.behind) .. " (" .. name .. ")" end
    local text = name .. ": " .. stateText(args)
    local cond = tonumber(args.cond)
    if cond and cond >= 0 and args.kind ~= "open" then text = text .. " (" .. tostring(math.floor(cond)) .. "%)" end
    if args.occupant then text = text .. " - " .. getText("UI_VVB_Occupant") end
    local r, g, b = 230, 230, 160
    if args.state == "flat" or args.state == "shattered" or args.state == "broken" then r, g, b = 255, 120, 90 end
    if args.kind == "armor" or args.state == "scrape" or args.state == "runflat" then r, g, b = 160, 190, 255 end
    local now = getTimestampMs()
    if text == last.text and now - last.time < 400 then return end
    last.text, last.time = text, now
    player:setHaloNote(text, r, g, b, 200)
end

local function wound(args)
    local player = getPlayer()
    local J = bridge()
    if player and J and J.wound then J.wound(player, tonumber(args.part) or 0, tonumber(args.dmg) or 10) end
end

local function handle(args)
    if type(args) ~= "table" then return end
    if args.cmd == "hit" then show(args) elseif args.cmd == "wound" then wound(args) end
end

Events.OnServerCommand.Add(function(module, command, args)
    if module ~= MODULE then return end
    if type(args) == "table" then args.cmd = args.cmd or command end
    handle(args)
end)

-- Einzelspieler: Meldungen direkt aus dem Java-Teil
Events.OnTick.Add(function()
    local J = bridge()
    if isClient() or not J or not J.poll then return end
    for _ = 1, 8 do
        local msg = J.poll()
        if not msg then break end
        handle(msg)
    end
end)

-- Konsole: VehicleBallistics2D_describe() zeigt, wie die Mod das Fahrzeug unter/vor dir sieht
function VehicleBallistics2D_describe()
    local p = getPlayer()
    local v = p and (p:getVehicle() or p:getNearVehicle())
    local J = bridge()
    print(J and J.describe and J.describe(v) or "no bridge")
end

---------------------------------------------------------------------------
-- Aus dem Fahrzeug schiessen: rechte Maustaste halten = zielen, linke = schiessen (Dauerfeuer bei "Auto").
-- Braucht Viewpoint True Ballistics (echte Kugel). 3D-Sicht: Blickrichtung, 2D: zum Mauszeiger.
---------------------------------------------------------------------------

local function sandbox(name, default)
    local t = SandboxVars and SandboxVars["VehicleBallistics2D"]
    local v = t and t["" .. name]
    if v == nil then return default end
    return v
end

local drive = { aiming = false, firing = false, last = 0, warned = false }

local function weaponOf(player)
    local w = player and player:getPrimaryHandItem()
    if w and instanceof(w, "HandWeapon") and w:isAimedFirearm() then return w end
    return nil
end

local function isDriver(player, vehicle)
    return vehicle:getDriver() == player or vehicle:getSeat(player) == 0
end

local function canAim(player)
    if not player or player:isDead() then return false end
    local vehicle = player:getVehicle()
    if not vehicle or not weaponOf(player) then return false end
    if sandbox("DriveBy", true) == false then return false end
    if isDriver(player, vehicle) and sandbox("DriverCanShoot", true) == false then return false end
    local J = bridge()
    if not (J and J.driveByAvailable and J.driveByAvailable()) then
        if not drive.warned then
            drive.warned = true
            player:setHaloNote(getText("UI_VVB_DriveByNeedsVTB"), 255, 180, 120, 300)
        end
        return false
    end
    return true
end

local function view3D()
    if VFA3D and VFA3D.active and VFA3D.active() then return true end
    return VFA_FX_VpOn == true and VFA_FX_Active3D and VFA_FX_Active3D() or false
end

local function noise(player, weapon)
    local mul = 1
    local opt = getSandboxOptions():getOptionByName("FirearmNoiseMultiplier")
    if opt then mul = opt:getValue() end
    local radius = weapon:getSoundRadius() * mul
    if not player:isOutside() then radius = radius * 0.5 end
    player:addWorldSoundUnlessInvisible(radius, weapon:getSoundVolume(), true)
end

local function fire(player)
    local weapon = weaponOf(player)
    local vehicle = player:getVehicle()
    if not weapon or not vehicle then return end
    local now = getTimestampMs()
    local delay = math.max(80, (weapon:getRecoilDelay() or 10) * 1000 / 60)
    if now - drive.last < delay then return end
    drive.last = now
    if not ISReloadWeaponAction.canShoot(player, weapon) then
        player:setRangedWeaponEmpty(true)
        pcall(function()
            local click = weapon:getClickSound()
            if click then player:getEmitter():playSound(click) end
        end)
        drive.firing = false
        return
    end
    local z = player:getZ()
    local tx, ty = screenToIsoX(0, getMouseX(), getMouseY(), z), screenToIsoY(0, getMouseX(), getMouseY(), z)
    local spread = (tonumber(sandbox("DriveBySpread", 1.5)) or 1.5)
    if isDriver(player, vehicle) then spread = spread * (tonumber(sandbox("DriverSpread", 2.0)) or 2.0) end
    local J = bridge()
    local ok = J and J.driveBy(player, tx, ty, z + 0.42, view3D(), spread)
    if not ok then return end
    player:playRangedWeaponShootSound(weapon:getSwingSound())
    noise(player, weapon)
    pcall(function() getEffectsManager():startMuzzleFlash(player, 1) end)
    ISReloadWeaponAction.onShoot(player, weapon)
    if weapon:isRackAfterShoot() then pcall(ISReloadWeaponAction.OnPlayerAttackFinished, player, weapon) end
    if weapon:getFireMode() ~= "Auto" then drive.firing = false end
end

Events.OnRightMouseDown.Add(function()
    if canAim(getPlayer()) then drive.aiming = true end
end)
Events.OnRightMouseUp.Add(function() drive.aiming = false; drive.firing = false end)
Events.OnMouseDown.Add(function()
    if drive.aiming then
        drive.firing = true
        fire(getPlayer())
    end
end)
Events.OnMouseUp.Add(function() drive.firing = false end)

Events.OnTick.Add(function()
    if not drive.aiming then return end
    local player = getPlayer()
    if not canAim(player) then drive.aiming = false; drive.firing = false; return end
    if drive.firing then fire(player) end
end)

-- Fadenkreuz waehrend des Zielens (2D am Mauszeiger, 3D in der Bildmitte)
local Reticle = ISUIElement:derive("VVB_DriveByReticle")
function Reticle:render()
    if not drive.aiming then return end
    local x, y = getMouseX(), getMouseY()
    if view3D() then x, y = getCore():getScreenWidth() / 2, getCore():getScreenHeight() / 2 end
    local c = drive.firing and { 1, 0.5, 0.4 } or { 1, 1, 1 }
    for _, r in ipairs({ { -12, -1, 8, 2 }, { 4, -1, 8, 2 }, { -1, -12, 2, 8 }, { -1, 4, 2, 8 } }) do
        self:drawRect(x + r[1] - 1, y + r[2] - 1, r[3] + 2, r[4] + 2, 0.6, 0, 0, 0)
        self:drawRect(x + r[1], y + r[2], r[3], r[4], 0.9, c[1], c[2], c[3])
    end
end
Events.OnGameStart.Add(function()
    local r = Reticle:new(0, 0, 1, 1)
    r:initialise()
    r:addToUIManager()
    r:setAlwaysOnTop(true)
    r.onMouseDown = function() return false end
end)

---------------------------------------------------------------------------
-- Seitenfenster ohne Kurbel (Mod-Fahrzeuge) kurbelbar machen
---------------------------------------------------------------------------

local openableDone, scanTicks = {}, 0
Events.OnTick.Add(function()
    scanTicks = scanTicks + 1
    if scanTicks < 120 then return end
    scanTicks = 0
    if sandbox("OpenableWindows", true) == false then return end
    local J = bridge()
    local cell = getCell()
    if not (J and J.makeOpenable and cell) then return end
    local list = cell:getVehicles()
    for i = 0, list:size() - 1 do
        local v = list:get(i)
        if v and not openableDone[v] then
            openableDone[v] = true
            J.makeOpenable(v)
        end
    end
end)
