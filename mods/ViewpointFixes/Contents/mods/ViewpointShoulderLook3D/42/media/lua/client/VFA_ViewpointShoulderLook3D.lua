-- Over the Shoulder + Project Viewpoint: Umschauen in der 3D-Ansicht.
--
-- In 2D bleibt Over the Shoulder (OTS) unveraendert: mittlere Maustaste, Blick folgt der Maus.
-- In 3D gehoert die mittlere Maustaste Viewpoint (Mausmodus). Dort gilt stattdessen eine eigene Taste
-- (Optionen > Tastenbelegung > "Umschauen in 3D", Standard Feststelltaste): gedrueckt halten und die Maus bewegen.
--   - Die Kamera dreht frei, Koerper und Laufrichtung bleiben, wo sie waren (Java-Teil, VFA_FreeLook).
--   - OTS dreht Kopf und Oberkoerper in Blickrichtung und weitet das Sichtfeld wie in 2D: OTS bekommt die Taste
--     als "OverTheShoulderLook" gemeldet und die Blickrichtung der Kamera statt der Mausrichtung (Look.pad.angle).
--   - Loslassen: die Kamera dreht zurueck nach vorn. Zielen beendet das Umschauen.
-- Kurzes Antippen (OTS: Schulterblick) gibt es in 3D nicht, die Kamera wuerde nicht mitgehen.
-- 3D-Status kommt von Viewpoint2Dto3D (VFA3D.active()), Mausmodus von ViewpointMouseRightClick (VFA_VpMouse).

require "OTS_Look"

local OTS_BIND = "OverTheShoulderLook"
local OWN_BIND = "VFA_ShoulderLook3D"

table.insert(keyBinding, { value = OWN_BIND, key = Keyboard.KEY_CAPITAL })

local function active3D()
    return VFA3D ~= nil and VFA3D.active ~= nil and VFA3D.active() == true
end

local function typing()
    local chat = ISChat and ISChat.instance
    return chat and chat.textEntry and chat.textEntry.isFocused and chat.textEntry:isFocused()
end

local originalIsKeyDown = isKeyDown
local looking = false

local function ownKeyDown()
    local code = getCore():getKey(OWN_BIND)
    return code ~= nil and code > 0 and originalIsKeyDown(OWN_BIND) == true
end

local function wantLook(player)
    if not (player and not player:isDead() and active3D()) then return false end
    if VFA_VpMouse == true or typing() then return false end
    if player:isAiming() or player:getVehicle() then return false end
    return ownKeyDown()
end

-- OTS fragt isKeyDown("OverTheShoulderLook") global ab: in 3D zaehlt die eigene Taste
if not VFA_ShoulderLook3DKeyWrapped then
    VFA_ShoulderLook3DKeyWrapped = true
    isKeyDown = function(key, ...)
        if key == OTS_BIND and active3D() then return looking end
        return originalIsKeyDown(key, ...)
    end
end

local Look = require "OTS_Look"
local savedFollow = nil
local ownAngle = false

-- Schulterblick (Antippen) in 3D aus
local function patchLook()
    if type(Look) ~= "table" or Look.vfaShoulderLook3D then return end
    Look.vfaShoulderLook3D = true
    local originalGlance = Look.glance
    if originalGlance then
        Look.glance = function(...)
            if active3D() then return end
            return originalGlance(...)
        end
    end
end

local function onTick()
    local player = getSpecificPlayer(0)
    looking = wantLook(player)
    VFA_FreeLook = looking

    if type(Look) ~= "table" then return end
    local pad = Look.pad
    if pad then
        if looking then
            -- Blickrichtung der Kamera (Viewpoint liefert sie ueber getLookAngleRadians)
            pad.angle = player:getLookAngleRadians()
            ownAngle = true
        elseif ownAngle then
            pad.angle = nil
            ownAngle = false
        end
    end

    -- "Immer folgen" in 3D aus, beim Wechsel zurueck nach 2D wiederherstellen
    local settings = Look.settings
    if active3D() then
        if settings and savedFollow == nil then
            savedFollow = settings.follow
            settings.follow = false
        end
    elseif savedFollow ~= nil then
        if settings then settings.follow = savedFollow end
        savedFollow = nil
    end
end

patchLook()
Events.OnTickEvenPaused.Add(onTick)
