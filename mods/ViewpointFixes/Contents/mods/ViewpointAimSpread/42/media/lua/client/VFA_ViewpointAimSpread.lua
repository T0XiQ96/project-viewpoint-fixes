-- Project Viewpoint - Aim Spread (Zusatz fuer Viewpoint True Ballistics): Optionen (Optionen > Mods > Viewpoint Aim Spread).
-- Der Java-Teil (ViewpointAimSpread.jar) liest diese Globalen:
--   VFA_SpreadOn        Streuung im Zielkreis des Spiels statt der eigenen Streuung von True Ballistics
--   VFA_SpreadScale     Faktor auf den Kreisradius des Spiels (1 = genau die Klammern, Standard 1.5)
--   VFA_CoverOn         Buesche koennen Kugeln ablenken
--   VFA_CoverBushChance Ablenkchance pro Busch auf der Flugbahn (0..1, Standard 0.10)
--   VFA_CoverDeflectDeg Groesste Abweichung der abgelenkten Kugel in Grad (seitlich und Hoehe, Standard 10)
-- Gestreut wird nur bei Sandbox "Firearms Use Damage Chance" = 1 (Disabled); sonst trifft der Schuss genau das Fadenkreuz.

VFA_SpreadOn = true
VFA_SpreadScale = 1.5
VFA_CoverOn = true
VFA_CoverBushChance = 0.10
VFA_CoverDeflectDeg = 10

local options

local function apply()
    if not options then return end
    VFA_SpreadOn = options.spread:getValue() == true
    VFA_SpreadScale = tonumber(options.scale:getValue()) or 1.5
    VFA_CoverOn = options.cover:getValue() == true
    VFA_CoverBushChance = (tonumber(options.bushChance:getValue()) or 10) / 100
    VFA_CoverDeflectDeg = tonumber(options.deflectDeg:getValue()) or 10
end

local function build()
    if options or not (PZAPI and PZAPI.ModOptions) then return end
    local page = PZAPI.ModOptions:create("ViewpointAimSpread", "Viewpoint Aim Spread")
    options = {}
    options.spread = page:addTickBox("spread", "Spread inside the game's aiming circle", true,
        "Every shot leaves at a random point inside the game's own aiming circle (it shrinks with Aiming skill, steady aim and focus). Replaces True Ballistics' own spread. Only when the sandbox option Firearms Use Damage Chance is Disabled; otherwise shots go exactly to the reticle.")
    options.scale = page:addSlider("scale", "Circle size factor", 0.25, 2, 0.05, 1.5,
        "Factor on the game's circle. 1 = exactly the brackets, 1.5 = default.")
    options.cover = page:addTickBox("cover", "Bushes deflect bullets", true,
        "Bushes do not stop bullets but can deflect them; the bullet flies on and can hit something else. Tree trunks stop bullets (True Ballistics).")
    options.bushChance = page:addSlider("bushChance", "Deflection chance per bush (%)", 0, 50, 1, 10,
        "Chance that a bullet flying through a bush is deflected.")
    options.deflectDeg = page:addSlider("deflectDeg", "Deflection angle (degrees)", 0, 30, 1, 10,
        "How far a deflected bullet can turn away, sideways and up/down.")
    function page:apply() apply() end
    Events.OnMainMenuEnter.Add(apply)
end

build()

-- Sandbox > Viewpoint Fixes: Server kann diese Optionen festlegen (siehe ViewpointFixes/VF_SandboxLock.lua)
require "ViewpointFixes/VF_SandboxLock"
if VF_SandboxLock and PZAPI and PZAPI.ModOptions then
    VF_SandboxLock.register(PZAPI.ModOptions:getOptions("ViewpointAimSpread"), "AimSpread", { spread='Spread', scale='Scale', cover='Cover', bushChance='BushChance', deflectDeg='DeflectDeg' })
end

Events.OnGameStart.Add(apply)

---------------------------------------------------------------------------
-- Project Viewpoint ADS: Zusammenspiel
--   * ADS' eigene Hueftfeuer-Streuung ("Hip-fire spread") wuerde zusaetzlich zur Streuung im Zielkreis wirken:
--     solange VFA_SpreadOn an ist, wird sie abgeschaltet.
--   * Laeuft nur ADS' Java-Teil (ADS nicht in der Mod-Liste des Servers, dann fehlt sein Optionsmenue), nutzt ADS
--     seine eingebauten Standardwerte mit vollem Kamera-Rueckstoss. Dann setzt diese Datei den Rueckstoss auf 30 %
--     (in 3D deutlich weniger Wackeln als in 2D) und schaltet die Hueftfeuer-Streuung aus.
---------------------------------------------------------------------------

local ADS_RECOIL_WITHOUT_OPTIONS = 0.3

local function patchADS()
    if type(PVADS) == "table" and type(PVADS.applySettings) == "function" and not PVADS.vfaSpreadPatched then
        PVADS.vfaSpreadPatched = true
        local original = PVADS.applySettings
        PVADS.applySettings = function(...)
            if VFA_SpreadOn and PVADS.Settings then PVADS.Settings.hipSpread = false end
            return original(...)
        end
        PVADS.applySettings()
        return
    end
    if PVADS == nil and PVADS_setSettings then
        PVADS_setSettings({
            enabled = true, hideCrosshair = true, adsSharp = true, thirdAds = true, closeClip = true, sway = true,
            cameraRecoil = true, recoilAmount = ADS_RECOIL_WITHOUT_OPTIONS, hipSpread = not VFA_SpreadOn,
            presence = true,
        })
        print("[ViewpointAimSpread] ADS without its options menu: recoil " .. ADS_RECOIL_WITHOUT_OPTIONS .. ", hip-fire spread " .. tostring(not VFA_SpreadOn))
    end
end

Events.OnGameStart.Add(patchADS)
