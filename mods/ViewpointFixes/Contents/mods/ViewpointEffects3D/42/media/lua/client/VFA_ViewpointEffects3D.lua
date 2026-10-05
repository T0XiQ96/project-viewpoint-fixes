-- Project Viewpoint - 3D Gun Effects and Fireflies (Lua-Teil)
-- Der Java-Teil (ViewpointEffects3D.jar) zeichnet Tracer, Muendungsfeuer, Licht, Rauch, Einschlaege und Gluehwuermchen in
-- Viewpoints 3D-Szene. Diese Datei:
--   * Optionen (Optionen > Mods > Viewpoint 3D Effects) -> Globale VFA_FX_*
--   * Muzzle Smoke: seine flachen 2D-Wolken in 3D aus (der Rauch kommt dann aus dem Java-Teil)
--   * JB's Fireflies: in 3D statt 2D-Punkten die Weltpositionen in VFA_FX_Fireflies schreiben (x, y, z, alpha, ...)
-- Der Java-Teil meldet VFA_FX_VpOn (3D an) und VFA_FX_Frame (steigt pro Bild). Steht der Zaehler, gilt 3D als aus.

VFA_FX_Flash = true
VFA_FX_FlashSize = 1.25
VFA_FX_Light = true
VFA_FX_LightStrength = 1.1
VFA_FX_Smoke = true
VFA_FX_SmokeAmount = 0.5
VFA_FX_SmokeFP = 0.8
VFA_FX_Impacts = true
VFA_FX_Holes = true
VFA_FX_HoleTime = 5.0
VFA_FX_Tracers = true
VFA_FX_TracerWidth = 1.1
VFA_FX_TracerPath = true
VFA_FX_Fireflies3D = true
VFA_FX_Fireflies = VFA_FX_Fireflies or {}

local STALE_TICKS = 30
local MAX_FIREFLY_VALUES = 4 * 400

local lastFrame, idleTicks = nil, STALE_TICKS

local function active3D()
    return VFA_FX_VpOn == true and idleTicks < STALE_TICKS
end
VFA_FX_Active3D = active3D

local function watch()
    local f = VFA_FX_Frame
    if f ~= nil and f ~= lastFrame then
        lastFrame = f
        idleTicks = 0
    else
        idleTicks = idleTicks + 1
    end
end

---------------------------------------------------------------------------
-- Optionen
---------------------------------------------------------------------------

local options

local function apply()
    if not options then return end
    VFA_FX_Flash = options.flash:getValue() == true
    VFA_FX_FlashSize = tonumber(options.flashSize:getValue()) or 1.25
    VFA_FX_Light = options.light:getValue() == true
    VFA_FX_LightStrength = tonumber(options.lightStrength:getValue()) or 1.1
    VFA_FX_Smoke = options.smoke:getValue() == true
    VFA_FX_SmokeAmount = tonumber(options.smokeAmount:getValue()) or 0.5
    VFA_FX_SmokeFP = tonumber(options.smokeFP:getValue()) or 0.8
    VFA_FX_Impacts = options.impacts:getValue() == true
    VFA_FX_Holes = options.holes:getValue() == true
    VFA_FX_HoleTime = tonumber(options.holeTime:getValue()) or 5.0
    VFA_FX_Tracers = options.tracers:getValue() == true
    VFA_FX_TracerWidth = tonumber(options.tracerWidth:getValue()) or 1.1
    VFA_FX_TracerPath = options.tracerPath:getValue() == true
    VFA_FX_Fireflies3D = options.fireflies:getValue() == true
end

local function build()
    if options or not (PZAPI and PZAPI.ModOptions) then return end
    local page = PZAPI.ModOptions:create("ViewpointEffects3D", "Viewpoint 3D Effects")
    options = {}
    page:addTitle("Gunfire (3D view)")
    options.tracers = page:addTickBox("tracers", "Bullet tracers", true,
        "The game's own tracers (yours and other players'), drawn in 3D. Colour, length and speed come from the ammo type, so weapon mods apply.")
    options.tracerWidth = page:addSlider("tracerWidth", "Tracer thickness", 0.3, 3, 0.05, 1.1, "1 = as set by the game.")
    options.tracerPath = page:addTickBox("tracerPath", "Faint bullet path line", true, "The thin fading line from the muzzle to the bullet.")
    options.flash = page:addTickBox("flash", "Muzzle flash glow", true,
        "A bright flash at the real muzzle whenever the game shows its own muzzle flash (depends on weapon and chance, other players too).")
    options.flashSize = page:addSlider("flashSize", "Muzzle flash size", 0.3, 2, 0.05, 1.25, "1.25 = default size.")
    options.light = page:addTickBox("light", "Muzzle flash lights the surroundings", true,
        "A short warm light at the muzzle for every shot.")
    options.lightStrength = page:addSlider("lightStrength", "Muzzle light strength", 0.2, 2, 0.05, 1.1, "1.1 = default.")
    options.smoke = page:addTickBox("smoke", "Gun smoke", true,
        "Smoke puffs that start at the muzzle and stay in the world. Muzzle Smoke's flat 2D puffs are hidden in 3D.")
    options.smokeAmount = page:addSlider("smokeAmount", "Smoke amount", 0, 2, 0.05, 0.5, "0.5 = default.")
    options.smokeFP = page:addSlider("smokeFP", "Smoke size in first person", 0.5, 3, 0.05, 0.8,
        "Factor on the puff size while you look through your character's eyes (third person always 1).")
    options.impacts = page:addTickBox("impacts", "Bullet impacts", true,
        "Impacts by material (the game's own MaterialType, the same that picks the impact sound): dust, splinters, sparks on metal and stone, ricochets on hard surfaces at flat angles. Size depends on the ammo.")
    options.holes = page:addTickBox("holes", "Bullet holes", true,
        "Holes stay on walls and objects (hardened metal and armoured glass only get a scrape), short-lived marks on dirt, grass, sand and snow.")
    options.holeTime = page:addSlider("holeTime", "Bullet hole lifetime", 0.1, 5, 0.1, 5, "1 = about 2 minutes on walls, 25 seconds on the ground; 5 (default) = about 10 minutes / 2 minutes.")
    page:addTitle("JB's Fireflies (3D view)")
    options.fireflies = page:addTickBox("fireflies", "Fireflies in 3D", true,
        "On: fireflies are drawn in 3D and hidden behind walls. Off: no fireflies while the 3D view is on. 2D view unchanged.")
    function page:apply() apply() end
    Events.OnMainMenuEnter.Add(apply)
end

build()

-- Sandbox > Viewpoint Fixes: Server kann diese Optionen festlegen (siehe ViewpointFixes/VF_SandboxLock.lua)
require "ViewpointFixes/VF_SandboxLock"
if VF_SandboxLock and PZAPI and PZAPI.ModOptions then
    VF_SandboxLock.register(PZAPI.ModOptions:getOptions("ViewpointEffects3D"), "Effects3D", { tracers='Tracers', tracerWidth='TracerWidth', tracerPath='TracerPath', flash='Flash', flashSize='FlashSize', light='Light', lightStrength='LightStrength', smoke='Smoke', smokeAmount='SmokeAmount', smokeFP='SmokeFP', impacts='Impacts', holes='Holes', holeTime='HoleTime', fireflies='Fireflies' })
end

Events.OnGameStart.Add(apply)

---------------------------------------------------------------------------
-- Muzzle Smoke: 2D-Wolken in 3D aus
---------------------------------------------------------------------------

local function patchMuzzleSmoke()
    local ms = MuzzleSmoke
    if type(ms) ~= "table" or ms.vfaFx3D or type(ms.spawnBurstInto) ~= "function" then return end
    ms.vfaFx3D = true
    local original = ms.spawnBurstInto
    ms.spawnBurstInto = function(list, ...)
        if active3D() and list == ms.particles then return end
        return original(list, ...)
    end
end

local function clearMuzzleSmoke()
    local ms = MuzzleSmoke
    if active3D() and type(ms) == "table" and type(ms.particles) == "table" and #ms.particles > 0 then
        for i = #ms.particles, 1, -1 do ms.particles[i] = nil end
    end
end

---------------------------------------------------------------------------
-- JB's Fireflies: in 3D Positionen an Java statt 2D-Zeichnen
---------------------------------------------------------------------------

local fireflyRand = newrandom()

local function patchFireflies()
    if not JBFireflies then return end
    local ok, FireflyUI = pcall(require, "jb_firefly_ui")
    if not ok or type(FireflyUI) ~= "table" or FireflyUI.vfaFx3D or type(FireflyUI.render) ~= "function" then return end
    FireflyUI.vfaFx3D = true
    local original = FireflyUI.render

    FireflyUI.render = function(self, px, py, ...)
        if not active3D() then return original(self, px, py, ...) end

        self.frameCount = self.frameCount + 1
        if self.frameCount > self.maxFrames then
            self.doneFlashing = true
            return
        end
        if not VFA_FX_Fireflies3D then return end      -- in 3D ausgeblendet, laeuft aber normal ab

        local square = self.square
        if not square then
            self.doneFlashing = true
            return
        end
        if not self.vfaHeight then
            self.vfaHeight = 0.12 + fireflyRand:random() * 0.45   -- Stockwerke (ca. 0.3 bis 1.4 m)
        end

        self.angle = self.angle + self.rotationRate
        local rad = math.rad(self.angle)
        self.offsetX = self.offsetX + math.cos(rad) * self.speed
        self.offsetY = self.offsetY + math.sin(rad) * self.speed

        local wx, wy = self.sqx + self.offsetX, self.sqy + self.offsetY
        local dx, dy = wx - px, wy - py
        local dist = math.sqrt(dx * dx + dy * dy)
        local distFactor = math.max(0.25, 1 - dist / 20)

        local lightLevel = square:getLightLevel(self.playerNum) or 0
        local lightFactor = math.min(1, (1 - math.min(lightLevel, 1)) * 25)

        local t = self.frameCount / self.maxFrames
        local blink = 1 - t ^ 0.3
        local alpha = self.baseAlpha * blink * distFactor * lightFactor
        if alpha < 0.05 then
            if t > 0.5 then self.doneFlashing = true end
            return
        end

        local list = VFA_FX_Fireflies
        local n = #list
        if n + 4 > MAX_FIREFLY_VALUES then return end
        list[n + 1] = wx
        list[n + 2] = wy
        list[n + 3] = self.sqz + self.vfaHeight
        list[n + 4] = alpha
    end
end

local function onTick()
    watch()
    clearMuzzleSmoke()
end

patchMuzzleSmoke()
patchFireflies()
Events.OnGameBoot.Add(patchMuzzleSmoke)
Events.OnGameBoot.Add(patchFireflies)
Events.OnGameStart.Add(patchMuzzleSmoke)
Events.OnGameStart.Add(patchFireflies)
Events.OnTick.Add(onTick)
