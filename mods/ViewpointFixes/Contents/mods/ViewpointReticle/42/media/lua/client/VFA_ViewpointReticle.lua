-- Project Viewpoint - Crosshair Options (Optionen > Mods > Viewpoint Crosshair).
-- Der Java-Teil (ViewpointReticle.jar) liest diese Globalen:
--   VFA_RET_Mode     1 immer, 2 nur mit Schusswaffe, 3 mit jeder Waffe, 4 nur beim Zielen, 5 nie
--   VFA_RET_Style    1 Punkt, 2 Kreuz, 3 Ring
--   VFA_RET_Size     Groesse in Pixeln (2 = wie Viewpoint)
--   VFA_RET_Opacity  Deckkraft 0.05..1
--   VFA_RET_Color    1 weiss, 2 gruen, 3 rot, 4 gelb, 5 cyan, 6 pink
--   VFA_RET_Outline  dunkler Rand
--   VFA_RET_Brackets Zielkreis-Klammern des Spiels in 3D zeigen

VFA_RET_Mode = 1
VFA_RET_Style = 1
VFA_RET_Size = 2
VFA_RET_Opacity = 1
VFA_RET_Color = 1
VFA_RET_Outline = true
VFA_RET_Brackets = true

local o

local function apply()
    if not o then return end
    VFA_RET_Mode = o.mode:getValue() or 1
    VFA_RET_Style = o.style:getValue() or 1
    VFA_RET_Size = tonumber(o.size:getValue()) or 2
    VFA_RET_Opacity = tonumber(o.opacity:getValue()) or 1
    VFA_RET_Color = o.color:getValue() or 1
    VFA_RET_Outline = o.outline:getValue() == true
    VFA_RET_Brackets = o.brackets:getValue() == true
end

local function build()
    if o or not (PZAPI and PZAPI.ModOptions) then return end
    local page = PZAPI.ModOptions:create("ViewpointReticle", "Viewpoint Crosshair")
    o = {}
    page:addTitle("Centre dot (3D view)")
    o.mode = page:addComboBox("mode", "Show the dot", "When Viewpoint's centre dot is drawn.")
    o.mode:addItem("Always", true)
    o.mode:addItem("Only with a firearm", false)
    o.mode:addItem("With any weapon (not empty hands)", false)
    o.mode:addItem("Only while aiming", false)
    o.mode:addItem("Never", false)
    o.style = page:addComboBox("style", "Style", "Shape of the crosshair.")
    o.style:addItem("Dot", true)
    o.style:addItem("Cross", false)
    o.style:addItem("Ring", false)
    o.size = page:addSlider("size", "Size (pixels)", 1, 12, 1, 2, "2 = Viewpoint's default dot.")
    o.opacity = page:addSlider("opacity", "Opacity", 0.05, 1, 0.05, 1, "1 = fully opaque.")
    o.color = page:addComboBox("color", "Colour", "")
    for i, name in ipairs({ "White", "Green", "Red", "Yellow", "Cyan", "Pink" }) do o.color:addItem(name, i == 1) end
    o.outline = page:addTickBox("outline", "Dark outline", true, "A thin dark edge so the crosshair stays visible on bright backgrounds.")
    page:addTitle("Aiming brackets (3D view)")
    o.brackets = page:addTickBox("brackets", "Show the game's aiming brackets", true,
        "The \"( o )\" brackets that show how steady your aim is. Untick to hide them in 3D (2D unchanged).")
    function page:apply() apply() end
    Events.OnMainMenuEnter.Add(apply)
end

build()

-- Sandbox > Viewpoint Fixes: Server kann diese Optionen festlegen (siehe ViewpointFixes/VF_SandboxLock.lua)
require "ViewpointFixes/VF_SandboxLock"
if VF_SandboxLock and PZAPI and PZAPI.ModOptions then
    VF_SandboxLock.register(PZAPI.ModOptions:getOptions("ViewpointReticle"), "Reticle", { mode='Mode', style='Style', size='Size', opacity='Opacity', color='Color', outline='Outline', brackets='Brackets' })
end

Events.OnGameStart.Add(apply)
