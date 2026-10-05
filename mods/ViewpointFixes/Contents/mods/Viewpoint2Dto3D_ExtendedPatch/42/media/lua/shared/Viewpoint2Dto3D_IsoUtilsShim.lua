-- Viewpoint2Dto3D_ExtendedPatch
-- Mods wie LeaveMessageNote, ShotgunTrajectory, HomeInventory, ColdBreath und HideAnywhere rechnen mit
--   IsoUtils.XToScreen(x, y, z, 0) - IsoCamera.getOffX()   und danach / Zoom
-- Das ist die alte isometrische Projektion. Solange Viewpoint 3D rendert, liefern wir hier einen Rohwert,
-- aus dem die Mods nach ihrer eigenen Rechnung genau die 3D-Bildschirmposition erhalten:
--   roh = pixel3D * zoom + getOffX()
-- Pixel3D kommt aus isoToScreenX/Y (vom Java-Teil in Viewpoint2Dto3D auf die 3D-Kamera umgestellt).
--
-- Vanilla ruft XToScreen mit winzigen Offsets auf (Foraging-Icons: 0.0 / 0.5). Solche Aufrufe bleiben unveraendert:
-- nur Weltkoordinaten (|x| oder |y| > 16) werden umgerechnet.
-- Liegt diese Datei in shared/, ist sie vor den Client-Dateien geladen, auch wenn ein Mod
-- IsoUtils.XToScreen beim Laden in eine lokale Variable kopiert (HideAnywhere).

if not IsoUtils or IsoUtils.VFA3D_shim then return end
IsoUtils.VFA3D_shim = true

local origX  = IsoUtils.XToScreen
local origY  = IsoUtils.YToScreen
local origXE = IsoUtils.XToScreenExact
local origYE = IsoUtils.YToScreenExact

local function active()
    return VFA3D ~= nil and VFA3D.active ~= nil and VFA3D.active() == true
end

local function isWorld(x, y)
    return x ~= nil and y ~= nil and (math.abs(x) > 16 or math.abs(y) > 16)
end

local function wrap(orig, isX)
    if type(orig) ~= "function" then return orig end
    return function(x, y, z, extra)
        local nx, ny, nz = tonumber(x), tonumber(y), tonumber(z)
        if nx and ny and nz and isWorld(nx, ny) and active() then
            local ok, result = pcall(function()
                local zoom = getCore():getZoom(0)
                if isX then
                    return isoToScreenX(0, nx, ny, nz) * zoom + IsoCamera.getOffX()
                end
                return isoToScreenY(0, nx, ny, nz) * zoom + IsoCamera.getOffY()
            end)
            if ok and result ~= nil then return result end
        end
        return orig(x, y, z, extra or 0)
    end
end

IsoUtils.XToScreen      = wrap(origX,  true)
IsoUtils.YToScreen      = wrap(origY,  false)
IsoUtils.XToScreenExact = wrap(origXE, true)
IsoUtils.YToScreenExact = wrap(origYE, false)
