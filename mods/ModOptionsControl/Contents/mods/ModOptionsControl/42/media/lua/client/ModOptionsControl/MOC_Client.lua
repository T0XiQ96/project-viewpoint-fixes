-- Mod Options Control: Client.
-- * holt die Regeln vom Server (OnGameStart) und bekommt Aenderungen automatisch
-- * gesperrte Optionen: Wert vom Admin (sonst Standardwert der Mod), Eintrag ausgegraut oder ausgeblendet
-- * die eigenen Werte des Spielers bleiben in ModOptions.ini (beim Speichern kurz zurueckgesetzt)
-- * Optionen > Mods wird neu aufgebaut, wenn sich etwas aendert (beim naechsten Oeffnen)

require "ModOptionsControl/MOC_Shared"

local C = {
    config = { pages = {} },
    received = false,
    defaults = {},     -- [key] = Standardwert der Mod (vor dem ersten Laden von ModOptions.ini)
    own = {},          -- [key] = eigener Wert des Spielers, solange gesperrt
    locked = {},       -- [key] = true
    panelDirty = false,
    showLocked = false, -- Spieler-Option: vom Server festgelegte Eintraege trotzdem (ausgegraut) zeigen
}
MOC.Client = C

local CONTROLLABLE = { tickbox = true, slider = true, combobox = true, textentry = true, multipletickbox = true,
    colorpicker = true, keybind = true }

local function key(pageId, optId) return tostring(pageId) .. "\1" .. tostring(optId) end
C.key = key

function C.controllable(option)
    if not option or not option.id or not CONTROLLABLE[option.type] then return false end
    if option.type == "keybind" and not MOC.sandbox().KeybindsLockable then return false end
    return true
end

function C.eachOption(fn)
    local api = PZAPI and PZAPI.ModOptions
    if not api then return end
    for _, page in ipairs(api.Data) do
        if page.modOptionsID ~= MOC.PAGE_ID then
            for _, option in ipairs(page.data) do
                if C.controllable(option) then fn(page, option) end
            end
        end
    end
end

---------------------------------------------------------------------------
-- Werte lesen / setzen je Typ
---------------------------------------------------------------------------

function C.getValue(o)
    local t = o.type
    if t == "combobox" then return o.selected end
    if t == "multipletickbox" then
        local r = {}
        for i, v in ipairs(o.values) do r[i] = v.value == true end
        return r
    end
    if t == "colorpicker" then
        local c = o.color or {}
        return { r = c.r, g = c.g, b = c.b, a = c.a }
    end
    if t == "keybind" then return o.key end
    return o.value
end

local function same(a, b)
    if type(a) ~= "table" or type(b) ~= "table" then return a == b end
    for k, v in pairs(a) do if b[k] ~= v then return false end end
    for k, v in pairs(b) do if a[k] ~= v then return false end end
    return true
end
C.same = same

function C.setValue(o, v)
    if v == nil then return end
    local t = o.type
    if t == "combobox" then
        v = tonumber(v)
        if v and v >= 1 and v <= #o.values then o:setValue(math.floor(v)) end
    elseif t == "tickbox" then
        o:setValue(v == true)
    elseif t == "slider" then
        v = tonumber(v)
        if v then o:setValue(math.max(o.min, math.min(o.max, v))) end
    elseif t == "textentry" then
        o:setValue(tostring(v))
    elseif t == "multipletickbox" then
        if type(v) == "table" then
            for i = 1, #o.values do
                if v[i] ~= nil then o:setValue(i, v[i] == true) end
            end
        end
    elseif t == "colorpicker" then
        if type(v) == "table" then
            o:setValue({ r = tonumber(v.r) or 1, g = tonumber(v.g) or 1, b = tonumber(v.b) or 1, a = tonumber(v.a) or 1 })
        end
    elseif t == "keybind" then
        v = tonumber(v)
        if v then o:setValue(v) end
    end
end

local function setEnabled(o, enabled)
    if o.type == "multipletickbox" then
        for _, v in ipairs(o.values) do o:setEnabled(v.name, enabled) end
    elseif o.setEnabled then
        o:setEnabled(enabled)
    end
end

-- direkt in die Tabelle schreiben (ohne Oberflaeche), fuer das Speichern
local function rawSet(o, v)
    local t = o.type
    if t == "combobox" then o.selected = v
    elseif t == "multipletickbox" then
        if type(v) == "table" then for i, x in ipairs(o.values) do if v[i] ~= nil then x.value = v[i] end end end
    elseif t == "colorpicker" then o.color = MOC.copy(v)
    elseif t == "keybind" then o.key = v
    else o.value = v end
end

---------------------------------------------------------------------------
-- Regeln auswerten
---------------------------------------------------------------------------

function C.rule(pageId, optId)
    local p = C.config.pages[pageId]
    local o = p and p.opts and p.opts[optId]
    return p, o
end

--- Gesperrt fuer alle Spieler (ohne Admin-Ausnahme)?
function C.lockedByRules(pageId, optId)
    local sb = MOC.sandbox()
    if sb.Enabled == false then return false end
    local p, o = C.rule(pageId, optId)
    if o and o.lock ~= nil then return o.lock end
    if p and p.lock ~= nil then return p.lock end
    local full = tostring(pageId) .. "." .. tostring(optId)
    local free, locked = MOC.parseList(sb.FreeList), MOC.parseList(sb.LockedList)
    if free.opts[full] then return false end
    if locked.opts[full] then return true end
    if free.pages[pageId] then return false end
    if locked.pages[pageId] then return true end
    return tonumber(sb.DefaultPolicy) == MOC.LOCKED
end

function C.bypass()
    return MOC.sandbox().AdminsBypass ~= false and MOC.canConfigure(getPlayer())
end

function C.isLocked(page, option)
    if C.bypass() then return false end
    return C.lockedByRules(page.modOptionsID, option.id)
end

function C.serverValue(page, option)
    local _, o = C.rule(page.modOptionsID, option.id)
    if o and o.value ~= nil then return o.value end
    return C.defaults[key(page.modOptionsID, option.id)]
end

local function snapshotDefaults()
    C.eachOption(function(page, option)
        local k = key(page.modOptionsID, option.id)
        if C.defaults[k] == nil then C.defaults[k] = MOC.copy(C.getValue(option)) end
    end)
end

local function applyPage(page)
    if page.apply then
        local ok, err = pcall(page.apply, page)
        if not ok then print("[ModOptionsControl] apply failed for " .. tostring(page.modOptionsID) .. ": " .. tostring(err)) end
    end
end

local function setAndNotify(option, value)
    local before = MOC.copy(C.getValue(option))
    C.setValue(option, value)
    local after = C.getValue(option)
    if not same(before, after) then
        if option.onChangeApply then pcall(option.onChangeApply, option, after) end
        return true
    end
    return false
end

--- Alle Regeln anwenden. Gibt zurueck, ob sich gesperrte/freie Eintraege geaendert haben (Seite neu aufbauen).
function C.enforce()
    snapshotDefaults()
    local changedPages, layout = {}, false
    C.eachOption(function(page, option)
        local k = key(page.modOptionsID, option.id)
        if C.isLocked(page, option) then
            if not C.locked[k] then
                C.own[k] = { v = MOC.copy(C.getValue(option)) }
                C.locked[k] = true
                layout = true
            end
            if setAndNotify(option, C.serverValue(page, option)) then changedPages[page] = true end
            setEnabled(option, false)
        elseif C.locked[k] then
            if C.own[k] and setAndNotify(option, C.own[k].v) then changedPages[page] = true end
            C.own[k] = nil
            C.locked[k] = nil
            setEnabled(option, true)
            layout = true
        end
    end)
    for page in pairs(changedPages) do applyPage(page) end
    if layout then C.panelDirty = true end
    return layout
end

function C.lockedCount()
    local n, pages = 0, {}
    for k in pairs(C.locked) do
        n = n + 1
        pages[string.match(k, "^(.-)\1")] = true
    end
    local p = 0
    for _ in pairs(pages) do p = p + 1 end
    return n, p
end

---------------------------------------------------------------------------
-- ModOptions.ini: Laden / Speichern
---------------------------------------------------------------------------

local function wrapStorage()
    local api = PZAPI and PZAPI.ModOptions
    if not api or api.mocWrapped then return end
    api.mocWrapped = true

    local load = api.load
    api.load = function(self, ...)
        snapshotDefaults()
        local result = load(self, ...)
        -- frisch geladene Werte sind die eigenen Werte des Spielers
        C.eachOption(function(page, option)
            local k = key(page.modOptionsID, option.id)
            if C.locked[k] then C.own[k] = { v = MOC.copy(C.getValue(option)) } end
        end)
        C.eachOption(function(page, option)
            local k = key(page.modOptionsID, option.id)
            if C.locked[k] then C.setValue(option, C.serverValue(page, option)) end
        end)
        C.enforce()
        return result
    end

    local save = api.save
    api.save = function(self, ...)
        local forced = {}
        C.eachOption(function(page, option)
            local own = C.own[key(page.modOptionsID, option.id)]
            if own then
                forced[#forced + 1] = { option, MOC.copy(C.getValue(option)) }
                rawSet(option, MOC.copy(own.v))
            end
        end)
        local ok, err = pcall(save, self, ...)
        for _, f in ipairs(forced) do rawSet(f[1], f[2]) end
        if not ok then error(err) end
    end
end

---------------------------------------------------------------------------
-- Optionen > Mods: Hinweis, Kennzeichen, Ausblenden, Neuaufbau
---------------------------------------------------------------------------

local function txt(k, fallback, ...)
    local s = getTextOrNull and getTextOrNull(k, ...)
    if s then return s end
    local args = { ... }
    return (string.gsub(fallback, "%%(%d)", function(i) return tostring(args[tonumber(i)] or "") end))
end
C.txt = txt

local function hiding()
    return MOC.sandbox().HideLocked ~= false and not C.showLocked
end

local function pageView(page)
    local lockedHere, interactive = 0, 0
    local out, pendingTitle = {}, nil
    local tag = " " .. txt("UI_MOC_ServerTag", "[Server]")
    local renamed = {}
    for _, item in ipairs(page.data) do
        local k = item.id and key(page.modOptionsID, item.id)
        local locked = k and C.locked[k]
        if item.type == "title" then
            pendingTitle = item
        elseif locked and hiding() then
            lockedHere = lockedHere + 1
        else
            if locked then
                lockedHere = lockedHere + 1
                renamed[#renamed + 1] = { item, item.name }
                item.name = getText(item.name) .. tag
            end
            if item.type ~= "description" and item.type ~= "separator" then interactive = interactive + 1 end
            if pendingTitle then out[#out + 1] = pendingTitle; pendingTitle = nil end
            out[#out + 1] = item
        end
    end
    if lockedHere > 0 then
        local note = hiding() and txt("UI_MOC_PageNoteHidden", "%1 settings of this mod are set by the server and hidden.", lockedHere)
            or txt("UI_MOC_PageNote", "%1 settings of this mod are set by the server (marked [Server]).", lockedHere)
        table.insert(out, 1, { type = "description", text = note })
    end
    return out, interactive, renamed
end

local function wrapPanel()
    if not MainOptions or MainOptions.mocWrapped or not MainOptions.addModOptionsPanel then return end
    MainOptions.mocWrapped = true
    local original = MainOptions.addModOptionsPanel
    MainOptions.addModOptionsPanel = function(self, ...)
        local api = PZAPI.ModOptions
        local savedData, savedList, renamedAll = {}, api.Data, {}
        C.enforce()
        if C.updateOwnPage then C.updateOwnPage() end
        local list = {}
        for _, page in ipairs(api.Data) do
            if page.modOptionsID == MOC.PAGE_ID then
                list[#list + 1] = page
            else
                -- ModOptions.ini laedt das Original selbst; Sperren erst danach (load-Wrapper) -> hier nur Anzeige
                local data, interactive, renamed = pageView(page)
                for _, r in ipairs(renamed) do renamedAll[#renamedAll + 1] = r end
                savedData[#savedData + 1] = { page, page.data }
                page.data = data
                if interactive > 0 or not hiding() then list[#list + 1] = page end
            end
        end
        api.Data = list
        local ok, err = pcall(original, self, ...)
        api.Data = savedList
        for _, s in ipairs(savedData) do s[1].data = s[2] end
        for _, r in ipairs(renamedAll) do r[1].name = r[2] end
        C.panelDirty = false
        if not ok then error(err) end
    end
end

--- Seite "Mods" in einem bestehenden Optionsfenster neu aufbauen.
function C.rebuildPanel(mo)
    mo = mo or (MainScreen and MainScreen.instance and MainScreen.instance.mainOptions) or MainOptions.instance
    if not mo or not mo.tabs or not mo.tabs.viewList then return false end
    local title = getText("UI_mainscreen_mods")
    local old
    for _, v in ipairs(mo.tabs.viewList) do
        if v.name == title then old = v.view end
    end
    local ids = {}
    for _, page in ipairs(PZAPI.ModOptions.Data) do ids[#ids + 1] = page.modOptionsID .. "." end
    if mo.gameOptions and mo.gameOptions.options then
        local list = mo.gameOptions.options
        for i = #list, 1, -1 do
            local name = tostring(list[i].name)
            for _, prefix in ipairs(ids) do
                if string.sub(name, 1, #prefix) == prefix then table.remove(list, i); break end
            end
        end
    end
    if MainOptions.keyText then
        for i = #MainOptions.keyText, 1, -1 do
            if MainOptions.keyText[i].isModBind then table.remove(MainOptions.keyText, i) end
        end
    end
    if old then mo.tabs:removeView(old) end
    local ok, err = pcall(mo.addModOptionsPanel, mo)
    if not ok then print("[ModOptionsControl] rebuilding Options > Mods failed: " .. tostring(err)) end
    return ok
end

local function wrapToUI()
    if not MainOptions or MainOptions.mocToUIWrapped then return end
    MainOptions.mocToUIWrapped = true
    local original = MainOptions.toUI
    MainOptions.toUI = function(self, ...)
        if C.panelDirty and not self:isVisible() then C.rebuildPanel(self) end
        return original(self, ...)
    end
end

---------------------------------------------------------------------------
-- Netzwerk
---------------------------------------------------------------------------

function C.request()
    sendClientCommand(getPlayer(), MOC.MODULE, "get", {})
end

function C.send(pages)
    sendClientCommand(getPlayer(), MOC.MODULE, "set", { pages = pages })
end

local function onServerCommand(module, command, args)
    if module ~= MOC.MODULE then return end
    if command == "config" then
        C.config = { pages = MOC.sanitize(args and args.pages), rev = args and args.rev, by = args and args.by }
        C.received = true
        C.enforce()
        if MOC.AdminUI and MOC.AdminUI.instance then MOC.AdminUI.instance:onConfig() end
    elseif command == "denied" then
        local p = getPlayer()
        if p then p:setHaloNote(txt("UI_MOC_Denied", "Mod Options Control: no permission"), 255, 80, 80, 300) end
    end
end

local lastSandbox
local function sandboxSignature()
    local sb = MOC.sandbox()
    return table.concat({ tostring(sb.Enabled), tostring(sb.DefaultPolicy), tostring(sb.HideLocked), tostring(sb.AdminsBypass),
        tostring(sb.KeybindsLockable), tostring(sb.LockedList), tostring(sb.FreeList) }, "|")
end

local function onGameStart()
    lastSandbox = sandboxSignature()
    C.enforce()
    C.request()
end

local function everyMinute()
    local s = sandboxSignature()
    if s ~= lastSandbox then
        lastSandbox = s
        C.panelDirty = true
        C.enforce()
    end
end

wrapStorage()
wrapPanel()
wrapToUI()
Events.OnServerCommand.Add(onServerCommand)
Events.OnGameStart.Add(onGameStart)
Events.EveryOneMinute.Add(everyMinute)
