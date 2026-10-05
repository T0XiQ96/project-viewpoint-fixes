-- @NAME@ (Server und Einzelspieler): leitet die Treffer-Angaben der Clients an den Java-Teil weiter und laesst
-- Luft- und Spritverlust laufen. Im Mehrspieler macht nur der Server Schaden.

if isClient() then return end
-- Kopie in Bullet Impacts 2D: tritt zurueck, wenn Project Viewpoint - Fixes (VehicleBallisticsVF) geladen ist
if "@LUA@" ~= "VehicleBallisticsVF" and VehicleBallisticsVF ~= nil then return end


local MODULE = "@MODULE@"
local function bridge() return @LUA@ end

Events.OnClientCommand.Add(function(module, command, player, args)
    local J = bridge()
    if module ~= MODULE or not J then return end
    if command == "detail" and type(args) == "table" then J.detail(player, args) end
    if command == "selfhit" and type(args) == "table" and J.selfHit then J.selfHit(player, args) end
end)

Events.OnTick.Add(function()
    local J = bridge()
    if J and J.tick then J.tick() end
end)

-- Seitenfenster ohne Kurbel kurbelbar machen (auch der Server muss es wissen, er fuehrt das Oeffnen aus)
local openableDone, scanTicks = {}, 0
Events.OnTick.Add(function()
    scanTicks = scanTicks + 1
    if scanTicks < 300 then return end
    scanTicks = 0
    local t = SandboxVars and SandboxVars["@NS@"]
    if t and t["@PREFIX@OpenableWindows"] == false then return end
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
