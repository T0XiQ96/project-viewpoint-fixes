-- ViewpointFixes: Mod-Optionen aus der Sandbox (Seite "Viewpoint Fixes").
-- Dieselbe Datei liegt in jeder ViewpointFixes-Mod mit Optionen (gleicher Pfad, das Spiel laedt sie einmal).
--
-- Pro Mod gibt es die Sandbox-Option ViewpointFixes.<Kuerzel>_Control:
--   1 = Spieler stellen selbst ein (Standard, Sandbox-Werte unbenutzt)
--   2 = Server-Werte gelten, Eintraege in Optionen > Mods ausgegraut
--   3 = Server-Werte gelten, Eintraege ausgeblendet
-- und je Mod-Option ViewpointFixes.<Kuerzel>_<Name> mit demselben Standardwert.
--
-- Erzwungen wird ueber die Werte der PZAPI.ModOptions-Eintraege selbst, danach ruft diese Datei page:apply() der Mod.
-- Die eigenen Werte des Spielers bleiben in ModOptions.ini: beim Speichern werden sie kurz zurueckgesetzt.
-- Tastenbelegungen werden nie erzwungen.

local VERSION = 2
if VF_SandboxLock and (VF_SandboxLock.version or 0) >= VERSION then return end

VF_SandboxLock = { version = VERSION, entries = {} }
local L = VF_SandboxLock

local CHOOSE, GREY, HIDE = 1, 2, 3

local function vars()
    return SandboxVars and SandboxVars.ViewpointFixes
end

function L.mode(prefix)
    local v = vars()
    local m = v and tonumber(v[prefix .. "_Control"])
    if m == GREY or m == HIDE then return m end
    return CHOOSE
end

local function get(option)
    if option.type == "combobox" then return option.selected end
    return option.value
end

local function put(option, value)
    if value == nil then return end
    if option.type == "combobox" then
        value = math.floor(tonumber(value) or 1)
        if value < 1 or value > #option.values then return end
    elseif option.type == "tickbox" then
        value = value == true
    elseif option.type == "slider" then
        value = tonumber(value)
        if not value then return end
        value = math.max(option.min, math.min(option.max, value))
    end
    option:setValue(value)
end

local function sandboxValue(entry, key)
    local v = vars()
    if not v then return nil end
    return v[entry.prefix .. "_" .. key]
end

-- Server-Werte setzen (eigene Werte vorher merken) bzw. freigeben
local function enforce(entry)
    local locked = L.mode(entry.prefix) ~= CHOOSE
    for id, key in pairs(entry.map) do
        local option = entry.page.dict[id]
        if option and option.type ~= "keybind" then
            if locked then
                if entry.own[id] == nil then entry.own[id] = get(option) end
                put(option, sandboxValue(entry, key))
                option:setEnabled(false)
            elseif entry.locked then
                -- nur freigeben, was diese Datei selbst gesperrt hat (Mod Options Control sperrt ggf. weiter)
                if entry.own[id] ~= nil then
                    put(option, entry.own[id])
                    entry.own[id] = nil
                end
                option:setEnabled(true)
            end
        end
    end
    entry.locked = locked
end

local function apply(entry)
    if entry.page.apply then
        local ok, err = pcall(entry.page.apply, entry.page)
        if not ok then print("[ViewpointFixes] apply failed for " .. tostring(entry.page.modOptionsID) .. ": " .. tostring(err)) end
    end
end

local function signature()
    local v = vars()
    if not v then return "" end
    local parts = {}
    for _, entry in ipairs(L.entries) do
        parts[#parts + 1] = tostring(v[entry.prefix .. "_Control"])
        for _, key in pairs(entry.map) do parts[#parts + 1] = tostring(v[entry.prefix .. "_" .. key]) end
    end
    return table.concat(parts, "|")
end

function L.refresh()
    for _, entry in ipairs(L.entries) do
        local was = entry.locked
        enforce(entry)
        if entry.locked or was then apply(entry) end
    end
    L.lastSignature = signature()
end

--- page: PZAPI.ModOptions-Seite; prefix: Kuerzel in der Sandbox; map: { optionId = "SandboxName", ... }
function L.register(page, prefix, map)
    if not page then return end
    local entry = { page = page, prefix = prefix, map = map, own = {}, locked = false }
    table.insert(L.entries, entry)
    if vars() then
        enforce(entry)
        if entry.locked then apply(entry) end
    end
end

---------------------------------------------------------------------------
-- Laden / Speichern von ModOptions.ini
---------------------------------------------------------------------------

local function wrapStorage()
    local api = PZAPI and PZAPI.ModOptions
    if not api or api.vfLockWrapped then return end
    api.vfLockWrapped = true

    local load = api.load
    api.load = function(self, ...)
        -- geladene Werte sind die eigenen Werte des Spielers
        for _, entry in ipairs(L.entries) do entry.own = {} end
        local result = load(self, ...)
        for _, entry in ipairs(L.entries) do
            entry.locked = false
            enforce(entry)
        end
        return result
    end

    local save = api.save
    api.save = function(self, ...)
        local forced = {}
        for _, entry in ipairs(L.entries) do
            for id, value in pairs(entry.own) do
                local option = entry.page.dict[id]
                if option then
                    forced[#forced + 1] = { option, get(option) }
                    if option.type == "combobox" then option.selected = value else option.value = value end
                end
            end
        end
        local ok, err = pcall(save, self, ...)
        for _, f in ipairs(forced) do
            if f[1].type == "combobox" then f[1].selected = f[2] else f[1].value = f[2] end
        end
        if not ok then error(err) end
    end
end

---------------------------------------------------------------------------
-- Optionen > Mods: ausblenden (Stufe 3) und Hinweis
---------------------------------------------------------------------------

local NOTE_GREY = "Some settings of this mod are set by the server (Sandbox > Viewpoint Fixes)."
local NOTE_HIDE = "Settings of this mod are set by the server (Sandbox > Viewpoint Fixes)."

local function note(mode)
    local key = mode == HIDE and "UI_ViewpointFixes_SetByServerHidden" or "UI_ViewpointFixes_SetByServer"
    local text = getTextOrNull and getTextOrNull(key)
    return text or (mode == HIDE and NOTE_HIDE or NOTE_GREY)
end

local function filtered(entry, mode)
    local hidden = {}
    if mode == HIDE then
        for id in pairs(entry.map) do
            local option = entry.page.dict[id]
            if option and option.type ~= "keybind" then hidden[option] = true end
        end
    end
    local out = { { type = "description", text = note(mode) } }
    local pendingTitle
    for _, item in ipairs(entry.page.data) do
        if item.type == "title" then
            pendingTitle = item
        elseif not hidden[item] then
            if pendingTitle then out[#out + 1] = pendingTitle; pendingTitle = nil end
            out[#out + 1] = item
        end
    end
    return out
end

local function wrapPanel()
    if not MainOptions or MainOptions.vfLockWrapped or not MainOptions.addModOptionsPanel then return end
    MainOptions.vfLockWrapped = true
    local original = MainOptions.addModOptionsPanel
    MainOptions.addModOptionsPanel = function(self, ...)
        local saved = {}
        for _, entry in ipairs(L.entries) do
            local mode = L.mode(entry.prefix)
            if mode ~= CHOOSE then
                saved[#saved + 1] = { entry.page, entry.page.data }
                entry.page.data = filtered(entry, mode)
            end
        end
        local ok, err = pcall(original, self, ...)
        for _, s in ipairs(saved) do s[1].data = s[2] end
        if not ok then error(err) end
    end
end

wrapStorage()
wrapPanel()

Events.OnGameStart.Add(L.refresh)
Events.OnMainMenuEnter.Add(L.refresh)
-- Admin aendert die Sandbox im laufenden Spiel: neue Werte uebernehmen
Events.EveryOneMinute.Add(function()
    if signature() ~= L.lastSignature then L.refresh() end
end)
