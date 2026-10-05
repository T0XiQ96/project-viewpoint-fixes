-- Mod Options Control: Server (Mehrspieler-Server und Einzelspieler).
-- Haelt die Regeln in der globalen ModData (wird mit der Welt gespeichert), schickt sie jedem Client auf Anfrage
-- und allen nach jeder Aenderung. Aendern darf nur, wer die Sandbox aendern darf (MOC.canConfigure).

if isClient() then return end
require "ModOptionsControl/MOC_Shared"

local function store()
    local d = ModData.getOrCreate(MOC.MODULE)
    if type(d.pages) ~= "table" then d.pages = {} end
    d.rev = tonumber(d.rev) or 0
    return d
end

local function payload()
    local d = store()
    return { pages = d.pages, rev = d.rev, by = d.by }
end

local function send(player)
    if player then
        sendServerCommand(player, MOC.MODULE, "config", payload())
    else
        sendServerCommand(MOC.MODULE, "config", payload())
    end
end

local function onClientCommand(module, command, player, args)
    if module ~= MOC.MODULE then return end
    if command == "get" then
        send(player)
    elseif command == "set" then
        local name = player and player:getUsername() or "?"
        if not MOC.canConfigure(player) then
            print("[ModOptionsControl] " .. tostring(name) .. " tried to change the rules without permission")
            sendServerCommand(player, MOC.MODULE, "denied", {})
            return
        end
        local d = store()
        d.pages = MOC.sanitize(args and args.pages)
        d.rev = d.rev + 1
        d.by = name
        print(string.format("[ModOptionsControl] %s saved the rules (revision %d, %d locks)", name, d.rev, MOC.countLocks(d.pages)))
        send(nil)
    end
end

Events.OnClientCommand.Add(onClientCommand)
