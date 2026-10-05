-- VFA_Viewpoint3D: 2D-Effekte anderer Mods abschalten, solange Viewpoint das 3D-Bild rendert.
-- Die Umrechnung von isoToScreenX/Y auf die 3D-Kamera macht der Java-Teil (vfa3d.jar).

VFA3D_Compat = VFA3D_Compat or {}

-- Real Vehicle VFX (RVVFX): Motor-/Reifen-/Schadensrauch ist 2D und passt nicht zur 3D-Ansicht.
local RVV_SMOKE_FLAGS = {
    "EngineExhaustVfxEnabled",
    "ExhaustVfxEnabled",
    "EngineDamageSmokeEnabled",
    "EngineRadiatorSteamEnabled",
    "TireSmokeVfxEnabled",
}

local saved = nil

local function viewpointActive()
    return VFA3D ~= nil and VFA3D.active ~= nil and VFA3D.active() == true
end

local function rvvConfig()
    return RVS and RVS.Config or nil
end

local function tick()
    local C = rvvConfig()
    if not C then return end

    if viewpointActive() then
        if not saved then
            saved = {}
            for _, k in ipairs(RVV_SMOKE_FLAGS) do saved[k] = C[k] end
        end
        for _, k in ipairs(RVV_SMOKE_FLAGS) do C[k] = false end
    elseif saved then
        -- zurueck in den 2D-Modus: Original-Einstellungen wiederherstellen
        for _, k in ipairs(RVV_SMOKE_FLAGS) do
            if saved[k] ~= nil then C[k] = saved[k] end
        end
        saved = nil
    end
end

Events.OnTick.Add(tick)
