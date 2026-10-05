-- Project Viewpoint - Hold to Zoom (braucht Project Viewpoint QOL).
-- Zoomen nur, solange die Zoom-Taste (Standard: linke Umschalttaste) gehalten und das Mausrad gedreht wird, in
-- groesseren Schritten; beim Loslassen springt die Sicht auf das normale Sichtfeld zurueck.
-- QOLs eigener Mausrad-Zoom (Strg + Rad fuers Sichtfeld, Rad allein fuer die Kameraentfernung) wird dafuer abgeschaltet.
-- Benutzt QOLs Java-Bruecke ViewpointQOL (zoom / resetFov).

local settings = { key = (Keyboard and Keyboard.KEY_LSHIFT) or 42, step = 15 }
local zoomed = false
local options

local function apply()
    if not options then return end
    settings.key = options.key:getValue() or settings.key
    settings.step = tonumber(options.step:getValue()) or 15
end

local function build()
    if options or not (PZAPI and PZAPI.ModOptions) then return end
    local page = PZAPI.ModOptions:create("ViewpointZoomHold", "Viewpoint Hold to Zoom")
    options = {}
    options.key = page:addKeyBind("key", "Zoom key (hold)", settings.key, "Hold this key and turn the mouse wheel to zoom. Releasing it returns to the normal view.")
    options.step = page:addSlider("step", "Zoom step per wheel notch", 5, 40, 1, 15, "Field-of-view change per notch (QOL's default was 5).")
    function page:apply() apply() end
    Events.OnMainMenuEnter.Add(apply)
end

local function bridge() return ViewpointQOL end

local function active()
    local b = bridge()
    return b and b.isAvailable and b.isAvailable() and b.isViewpointActive and b.isViewpointActive()
end

local function keyDown()
    local k = settings.key
    if not k or k == 0 then return false end
    if isKeyDown(k) then return true end
    -- linke und rechte Umschalt-/Strg-Taste gleich behandeln
    local ls, rs = Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT
    if k == ls then return isKeyDown(rs) end
    if k == rs then return isKeyDown(ls) end
    return false
end

-- QOLs eigenen Rad-Zoom aus (seine Optionen-Tabelle wird bei jedem Optionen-Anwenden neu gefuellt)
local function silenceQOL()
    local o = ProjectViewpointQOLOptions
    if type(o) ~= "table" then return end
    o.enableMouseWheelZoom = false
    o.enableThirdPersonZoom = false
end

local function onMouseWheel(del)
    if not active() or not keyDown() then return end
    local b = bridge()
    if not (b and b.zoom) then return end
    if b.isCursorMode and b.isCursorMode() then return end
    b.zoom(del > 0 and -settings.step or settings.step)
    zoomed = true
end

local function onTick()
    silenceQOL()
    if zoomed and not keyDown() then
        zoomed = false
        local b = bridge()
        if b and b.resetFov then b.resetFov() end
    end
end

build()

-- Sandbox > Viewpoint Fixes: Server kann diese Optionen festlegen (siehe ViewpointFixes/VF_SandboxLock.lua)
require "ViewpointFixes/VF_SandboxLock"
if VF_SandboxLock and PZAPI and PZAPI.ModOptions then
    VF_SandboxLock.register(PZAPI.ModOptions:getOptions("ViewpointZoomHold"), "ZoomHold", { step='Step' })
end

Events.OnGameStart.Add(apply)
Events.OnMouseWheel.Add(onMouseWheel)
Events.OnTick.Add(onTick)
