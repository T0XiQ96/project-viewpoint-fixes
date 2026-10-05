-- Mod Options Control: gemeinsamer Teil (Client und Server).
--
-- Regeln (vom Server in der globalen ModData "ModOptionsControl" gespeichert):
--   pages[pageId] = { lock = true|false|nil, opts = { [optionId] = { lock = true|false|nil, value = <Wert>|nil } } }
--   pageId = modOptionsID der PZAPI.ModOptions-Seite, optionId = id der Option.
-- Gesperrt ist eine Option, wenn (in dieser Reihenfolge) ihre eigene Regel, die Regel ihrer Seite, die Sandbox-Listen
-- LockedList/FreeList oder die Sandbox-Grundregel DefaultPolicy es sagen. Gesperrte Optionen bekommen den Admin-Wert,
-- sonst den Standardwert der Mod.

MOC = MOC or {}
MOC.MODULE = "ModOptionsControl"
MOC.PAGE_ID = "ModOptionsControl"
MOC.FREE, MOC.LOCKED = 1, 2

function MOC.sandbox()
    return (SandboxVars and SandboxVars.ModOptionsControl) or {}
end

function MOC.singleplayer()
    return not isClient() and not isServer()
end

--- Darf dieser Spieler die Regeln aendern? Wer die Sandbox aendern darf (Rolle mit SandboxOptions), im Einzelspieler immer.
function MOC.canConfigure(player)
    if MOC.singleplayer() then return true end
    if not player then return false end
    local ok, allowed = pcall(function()
        local role = player:getRole()
        return role ~= nil and Capability ~= nil and role:hasCapability(Capability.SandboxOptions)
    end)
    if ok and allowed then return true end
    local ok2, level = pcall(function() return player:getAccessLevel() end)
    return ok2 and level ~= nil and string.lower(tostring(level)) == "admin"
end

local function copy(v, depth)
    if type(v) ~= "table" then return v end
    depth = (depth or 0) + 1
    if depth > 8 then return nil end
    local out = {}
    for k, x in pairs(v) do out[k] = copy(x, depth) end
    return out
end
MOC.copy = copy

local function cleanValue(v)
    local t = type(v)
    if t == "boolean" or t == "number" then return v end
    if t == "string" then return string.sub(v, 1, 2000) end
    if t == "table" then
        local out, n = {}, 0
        for k, x in pairs(v) do
            n = n + 1
            if n > 64 then break end
            local kt, xt = type(k), type(x)
            if (kt == "string" or kt == "number") and (xt == "boolean" or xt == "number" or xt == "string") then out[k] = x end
        end
        return out
    end
    return nil
end

--- Nur erlaubte Struktur uebernehmen (vom Client geschickte Regeln).
function MOC.sanitize(pages)
    local out = {}
    if type(pages) ~= "table" then return out end
    local count = 0
    for pageId, page in pairs(pages) do
        if type(pageId) == "string" and type(page) == "table" and count < 2000 then
            local p = { opts = {} }
            if type(page.lock) == "boolean" then p.lock = page.lock end
            if type(page.opts) == "table" then
                for optId, opt in pairs(page.opts) do
                    if type(optId) == "string" and type(opt) == "table" and count < 2000 then
                        local o = {}
                        if type(opt.lock) == "boolean" then o.lock = opt.lock end
                        if opt.value ~= nil then o.value = cleanValue(opt.value) end
                        if o.lock ~= nil or o.value ~= nil then
                            p.opts[optId] = o
                            count = count + 1
                        end
                    end
                end
            end
            if p.lock ~= nil or next(p.opts) ~= nil then out[pageId] = p end
        end
    end
    return out
end

--- "Seite;Seite.option;..." -> { pages = {Seite=true}, opts = {["Seite.option"]=true} }
function MOC.parseList(text)
    local list = { pages = {}, opts = {} }
    if type(text) ~= "string" then return list end
    for entry in string.gmatch(text, "[^;,%s]+") do
        local dot = string.find(entry, ".", 1, true)
        if dot then list.opts[entry] = true else list.pages[entry] = true end
    end
    return list
end

function MOC.countLocks(pages)
    local n = 0
    for _, p in pairs(pages or {}) do
        if p.lock == true then n = n + 1 end
        for _, o in pairs(p.opts or {}) do if o.lock == true then n = n + 1 end end
    end
    return n
end

-- Einfache Serialisierung fuer Export/Import (nur Zahlen, Text, Wahrheitswerte, Tabellen)
local function ser(v, indent)
    local t = type(v)
    if t == "string" then return string.format("%q", v) end
    if t == "number" or t == "boolean" then return tostring(v) end
    if t ~= "table" then return "nil" end
    indent = indent or ""
    local inner = indent .. "  "
    local parts = {}
    for k, x in pairs(v) do
        local key = type(k) == "number" and ("[" .. tostring(k) .. "]") or ("[" .. string.format("%q", tostring(k)) .. "]")
        parts[#parts + 1] = inner .. key .. " = " .. ser(x, inner)
    end
    if #parts == 0 then return "{}" end
    return "{\n" .. table.concat(parts, ",\n") .. "\n" .. indent .. "}"
end
MOC.serialize = ser
