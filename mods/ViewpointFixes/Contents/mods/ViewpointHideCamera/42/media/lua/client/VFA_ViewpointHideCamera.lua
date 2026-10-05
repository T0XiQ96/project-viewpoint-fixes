-- Hide Anywhere + Project Viewpoint: Versteckt man sich unter einem Bett, sitzt die Kamera auf Augenhoehe des
-- stehenden Charakters, ueber dem Bett. Hier: solange man unter dem Bett versteckt ist, meldet diese Datei eine
-- niedrige Augenhoehe in VFA_HideEye; ViewpointHideCamera.jar senkt damit die Kamera.
-- Augenhoehe in Viewpoint-Einheiten (ein Stockwerk = 2.45, Stehender ca. 1.4). Zu hoch: Kamera sieht ueber das
-- Bett; zu niedrig: Kamera steckt im Boden.

local EYE_HEIGHT = 0.22

local function underBed()
    local player = getSpecificPlayer(0)
    if not player or player:isDead() then return false end
    local hiding = HidingMod
    if not (hiding and hiding.State and hiding.State.findByPlayer) then return false end
    local ok, state = pcall(hiding.State.findByPlayer, player)
    return ok and state ~= nil and state.kind == "bed" and state.unregistered ~= true
end

local function onTick()
    local ok, result = pcall(underBed)
    if ok and result then
        VFA_HideEye = EYE_HEIGHT
    else
        VFA_HideEye = nil
    end
end

Events.OnTick.Add(onTick)
