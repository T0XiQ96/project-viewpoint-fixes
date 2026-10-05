-- Mod Options Control: Admin-Fenster und eigene Seite unter Optionen > Mods.
--
-- Fenster: alle Mod-Optionen-Seiten mit ihren Optionen. Spalte links = Regel:
--   "-" erbt (Seite bzw. Sandbox-Grundregel), "Gesperrt", "Frei". Klick auf die Spalte schaltet weiter.
-- Werte: "Meinen Wert nehmen" uebernimmt den eigenen aktuellen Wert des Admins als Server-Wert,
-- "Standard" loescht ihn (dann gilt der Standardwert der Mod). Erst "Speichern & senden" schickt alles an den Server.

require "ISUI/ISCollapsableWindow"
require "ISUI/ISScrollingListBox"
require "ModOptionsControl/MOC_Shared"
require "ModOptionsControl/MOC_Client"

local C = MOC.Client
local txt = C.txt
local FONT_HGT_SMALL = getTextManager():getFontHeight(UIFont.Small)
local BUTTON_HGT = FONT_HGT_SMALL + 6
local PAD = 8
local STATE_W = 90
local EXPORT_FILE = "ModOptionsControl_rules.txt"

MOC.AdminUI = ISCollapsableWindow:derive("MOC_AdminUI")
local UI = MOC.AdminUI

local function optionName(option)
    local ok, s = pcall(getText, option.name or option.id or "?")
    return ok and s or tostring(option.name or option.id)
end

local function valueText(option, v)
    if v == nil then return txt("UI_MOC_ValueDefault", "(mod default)") end
    local t = option.type
    if t == "combobox" then return tostring(option.values[tonumber(v) or 0] or v) end
    if t == "keybind" then return getKeyName(tonumber(v) or 0) end
    if t == "tickbox" then return v and txt("UI_MOC_On", "on") or txt("UI_MOC_Off", "off") end
    if t == "colorpicker" and type(v) == "table" then
        return string.format("%.2f %.2f %.2f", tonumber(v.r) or 0, tonumber(v.g) or 0, tonumber(v.b) or 0)
    end
    if type(v) == "table" then
        local parts = {}
        for i, x in ipairs(v) do parts[#parts + 1] = x and "x" or "-" end
        return table.concat(parts, "")
    end
    if type(v) == "number" then return string.format("%g", v) end
    return tostring(v)
end

--- eigener aktueller Wert des Admins (auch wenn er selbst gesperrt ist)
local function myValue(page, option)
    local own = C.own[C.key(page.modOptionsID, option.id)]
    if own then return MOC.copy(own.v) end
    return MOC.copy(C.getValue(option))
end

function UI:createChildren()
    ISCollapsableWindow.createChildren(self)
    local th = self:titleBarHeight()
    local y = th + PAD

    self.search = ISTextEntryBox:new("", PAD, y, 260, BUTTON_HGT)
    self.search:initialise()
    self.search:instantiate()
    self.search.onTextChange = function() self:refreshList() end
    self:addChild(self.search)
    self.searchLabel = txt("UI_MOC_Search", "Search")

    self.info = ISLabel:new(PAD + 270, y + 2, FONT_HGT_SMALL, "", 1, 1, 1, 0.8, UIFont.Small, true)
    self.info:initialise()
    self:addChild(self.info)
    y = y + BUTTON_HGT + PAD

    local listH = self.height - y - (BUTTON_HGT + PAD) * 3 - PAD
    self.list = ISScrollingListBox:new(PAD, y, self.width - PAD * 2, listH)
    self.list:initialise()
    self.list:instantiate()
    self.list.itemheight = FONT_HGT_SMALL + 6
    self.list.font = UIFont.Small
    self.list.drawBorder = true
    self.list.doDrawItem = UI.drawItem
    self.list.onMouseDown = UI.listMouseDown
    self.list.owner = self
    self:addChild(self.list)
    y = y + listH + PAD

    local buttons = {
        { "lockAll", txt("UI_MOC_LockAll", "Lock everything") },
        { "freeAll", txt("UI_MOC_FreeAll", "Free everything") },
        { "clear", txt("UI_MOC_ClearRules", "Clear all rules") },
        { "takeMine", txt("UI_MOC_TakeAllMine", "My values for all locked") },
        { "export", txt("UI_MOC_Export", "Export") },
        { "import", txt("UI_MOC_Import", "Import") },
        { "row:mine", txt("UI_MOC_TakeMine", "Use my value (selected)") },
        { "row:default", txt("UI_MOC_UseDefault", "Mod default (selected)") },
        { "row:cycle", txt("UI_MOC_Cycle", "Change rule (selected)") },
        { "expand", txt("UI_MOC_ExpandAll", "Expand / collapse all") },
        { "save", txt("UI_MOC_Save", "Save & send") },
        { "close", txt("UI_btn_close", "Close") },
    }
    local perRow = 6
    local bw = math.floor((self.width - PAD * (perRow + 1)) / perRow)
    for i, b in ipairs(buttons) do
        local col = (i - 1) % perRow
        local row = math.floor((i - 1) / perRow)
        local btn = ISButton:new(PAD + col * (bw + PAD), y + row * (BUTTON_HGT + PAD), bw, BUTTON_HGT, b[2], self, UI.onButton)
        btn.internal = b[1]
        btn:initialise()
        btn:instantiate()
        if b[1] == "save" then btn:enableAcceptColor() end
        self:addChild(btn)
    end
end

function UI:initialise()
    ISCollapsableWindow.initialise(self)
    self.work = MOC.copy(C.config.pages) or {}
    self.expanded = {}
    self.dirty = false
end

function UI:onConfig()
    if not self.dirty then
        self.work = MOC.copy(C.config.pages) or {}
        self:refreshList()
    end
end

function UI:rule(pageId, optId)
    local p = self.work[pageId]
    if not p then
        p = { opts = {} }
        self.work[pageId] = p
    end
    p.opts = p.opts or {}
    if optId == nil then return p end
    local o = p.opts[optId]
    if not o then
        o = {}
        p.opts[optId] = o
    end
    return p, o
end

-- Regel anzeigen/weiterschalten: nil -> true (gesperrt) -> false (frei) -> nil
local function nextState(v)
    if v == nil then return true end
    if v == true then return false end
    return nil
end

local function stateText(v, effective)
    local s = v == true and txt("UI_MOC_Locked", "Locked") or v == false and txt("UI_MOC_Free", "Free") or "-"
    if v == nil then s = s .. (effective and (" (" .. txt("UI_MOC_Locked", "Locked") .. ")") or "") end
    return s
end

--- wirksam gesperrt nach den Arbeitsregeln (ohne Admin-Ausnahme)
function UI:effective(pageId, optId)
    local saved = C.config.pages
    C.config.pages = self.work
    local r = C.lockedByRules(pageId, optId)
    C.config.pages = saved
    return r
end

function UI:refreshList()
    local selected = self.list.items[self.list.selected] and self.list.items[self.list.selected].item
    self.list:clear()
    self.list:setScrollHeight(0)
    local filter = string.lower(self.search:getInternalText() or "")
    local api = PZAPI and PZAPI.ModOptions
    if not api then return end
    local locks = 0
    for _, page in ipairs(api.Data) do
        if page.modOptionsID ~= MOC.PAGE_ID then
            local rows = {}
            local pageName = getText(page.name or page.modOptionsID)
            local pageMatch = filter == "" or string.find(string.lower(pageName), filter, 1, true)
                or string.find(string.lower(page.modOptionsID), filter, 1, true)
            for _, option in ipairs(page.data) do
                if C.controllable(option) then
                    local name = optionName(option)
                    if pageMatch or string.find(string.lower(name), filter, 1, true) then
                        rows[#rows + 1] = { kind = "option", page = page, option = option, name = name }
                    end
                    if self:effective(page.modOptionsID, option.id) then locks = locks + 1 end
                end
            end
            if #rows > 0 then
                local head = { kind = "page", page = page, name = pageName, count = #rows }
                self.list:addItem(pageName, head)
                if self.expanded[page.modOptionsID] or filter ~= "" then
                    for _, r in ipairs(rows) do self.list:addItem(r.name, r) end
                end
            end
        end
    end
    for i, it in ipairs(self.list.items) do
        if selected and it.item.page == selected.page and it.item.option == selected.option then self.list.selected = i end
    end
    local sb = MOC.sandbox()
    self.info:setName(txt("UI_MOC_Info", "%1 options locked for players. Default rule: %2. %3",
        locks, tonumber(sb.DefaultPolicy) == MOC.LOCKED and txt("UI_MOC_Locked", "Locked") or txt("UI_MOC_Free", "Free"),
        self.dirty and txt("UI_MOC_Unsaved", "(not saved yet)") or ""))
end

function UI.drawItem(list, y, item, alt)
    local self = list.owner
    local it = item.item
    local h = list.itemheight
    if list.selected == item.itemindex then
        list:drawRect(0, y, list:getWidth(), h, 0.3, 0.7, 0.35, 0.15)
    end
    local pageId = it.page.modOptionsID
    if it.kind == "page" then
        local p = self.work[pageId]
        local v = p and p.lock
        list:drawRect(0, y, list:getWidth(), h, 0.25, 0.3, 0.3, 0.3)
        local r, g, b = 1, 1, 1
        if v == true then r, g, b = 1, 0.45, 0.4 elseif v == false then r, g, b = 0.5, 1, 0.5 end
        list:drawText(stateText(v, false), 6, y + 3, r, g, b, 1, UIFont.Small)
        local arrow = self.expanded[pageId] and "v " or "> "
        list:drawText(arrow .. it.name .. "  (" .. pageId .. ")", STATE_W + 6, y + 3, 1, 1, 0.8, 1, UIFont.Small)
    else
        local _, o = nil, nil
        local p = self.work[pageId]
        o = p and p.opts and p.opts[it.option.id]
        local v = o and o.lock
        local eff = self:effective(pageId, it.option.id)
        local r, g, b = 0.8, 0.8, 0.8
        if eff then r, g, b = 1, 0.45, 0.4 elseif v == false then r, g, b = 0.5, 1, 0.5 end
        list:drawText(stateText(v, eff), 16, y + 3, r, g, b, 1, UIFont.Small)
        list:drawText(it.name, STATE_W + 22, y + 3, 1, 1, 1, 0.95, UIFont.Small)
        local value = valueText(it.option, o and o.value)
        local w = getTextManager():MeasureStringX(UIFont.Small, value)
        list:drawText(value, list:getWidth() - w - 22, y + 3, 0.7, 0.85, 1, 1, UIFont.Small)
    end
    return y + h
end

function UI.listMouseDown(list, x, y)
    ISScrollingListBox.onMouseDown(list, x, y)
    local self = list.owner
    local row = list.items[list.selected]
    if not row then return end
    local it = row.item
    if x < STATE_W + 10 then
        self:cycle(it)
    elseif it.kind == "page" then
        self.expanded[it.page.modOptionsID] = not self.expanded[it.page.modOptionsID]
        self:refreshList()
    end
end

function UI:cycle(it)
    if it.kind == "page" then
        local p = self:rule(it.page.modOptionsID)
        p.lock = nextState(p.lock)
    else
        local _, o = self:rule(it.page.modOptionsID, it.option.id)
        o.lock = nextState(o.lock)
    end
    self.dirty = true
    self:refreshList()
end

function UI:setAll(lock)
    for _, page in ipairs(PZAPI.ModOptions.Data) do
        if page.modOptionsID ~= MOC.PAGE_ID then
            local p = self:rule(page.modOptionsID)
            p.lock = lock
            for _, o in pairs(p.opts) do o.lock = nil end
        end
    end
    self.dirty = true
    self:refreshList()
end

function UI:takeAllMine()
    C.eachOption(function(page, option)
        if self:effective(page.modOptionsID, option.id) then
            local _, o = self:rule(page.modOptionsID, option.id)
            o.value = myValue(page, option)
        end
    end)
    self.dirty = true
    self:refreshList()
end

function UI:export()
    local w = getFileWriter(EXPORT_FILE, true, false)
    if not w then return end
    w:write("return " .. MOC.serialize(MOC.sanitize(self.work)) .. "\n")
    w:close()
    self:note(txt("UI_MOC_Exported", "Saved to Zomboid/Lua/%1", EXPORT_FILE))
end

function UI:import()
    local r = getFileReader(EXPORT_FILE, false)
    if not r then
        self:note(txt("UI_MOC_NoFile", "File Zomboid/Lua/%1 not found", EXPORT_FILE))
        return
    end
    local lines = {}
    while true do
        local line = r:readLine()
        if line == nil then break end
        lines[#lines + 1] = line
    end
    r:close()
    local fn = loadstring(table.concat(lines, "\n"))
    local ok, data = false, nil
    if fn then ok, data = pcall(fn) end
    if ok and type(data) == "table" then
        self.work = MOC.sanitize(data)
        self.dirty = true
        self:refreshList()
        self:note(txt("UI_MOC_Imported", "Rules loaded - press Save & send"))
    else
        self:note(txt("UI_MOC_ImportFailed", "Could not read the file"))
    end
end

function UI:note(text)
    local p = getPlayer()
    if p then p:setHaloNote(text, 220, 220, 120, 300) end
    print("[ModOptionsControl] " .. text)
end

function UI:onButton(button)
    local id = button.internal
    local row = self.list.items[self.list.selected]
    local it = row and row.item
    if id == "lockAll" then self:setAll(true)
    elseif id == "freeAll" then self:setAll(false)
    elseif id == "clear" then self.work = {}; self.dirty = true; self:refreshList()
    elseif id == "takeMine" then self:takeAllMine()
    elseif id == "export" then self:export()
    elseif id == "import" then self:import()
    elseif id == "expand" then
        local any = next(self.expanded) ~= nil
        self.expanded = {}
        if not any then
            for _, page in ipairs(PZAPI.ModOptions.Data) do self.expanded[page.modOptionsID] = true end
        end
        self:refreshList()
    elseif id == "row:cycle" and it then self:cycle(it)
    elseif id == "row:mine" and it and it.kind == "option" then
        local _, o = self:rule(it.page.modOptionsID, it.option.id)
        o.value = myValue(it.page, it.option)
        self.dirty = true
        self:refreshList()
    elseif id == "row:default" and it and it.kind == "option" then
        local _, o = self:rule(it.page.modOptionsID, it.option.id)
        o.value = nil
        self.dirty = true
        self:refreshList()
    elseif id == "save" then
        C.send(MOC.sanitize(self.work))
        self.dirty = false
        self:refreshList()
        self:note(txt("UI_MOC_Sent", "Rules sent to the server"))
    elseif id == "close" then
        self:close()
    end
end

function UI:prerender()
    ISCollapsableWindow.prerender(self)
end

function UI:render()
    ISCollapsableWindow.render(self)
    if (self.search:getInternalText() or "") == "" then
        self:drawText(self.searchLabel .. "...", self.search:getX() + 6, self.search:getY() + 3, 0.6, 0.6, 0.6, 1, UIFont.Small)
    end
end

function UI:close()
    self:setVisible(false)
    self:removeFromUIManager()
    UI.instance = nil
end

function UI:new(x, y, w, h)
    local o = ISCollapsableWindow.new(self, x, y, w, h)
    o.title = txt("UI_MOC_Title", "Mod Options Control")
    o.resizable = false
    return o
end

function UI.open()
    if not MOC.canConfigure(getPlayer()) then
        local p = getPlayer()
        if p then p:setHaloNote(txt("UI_MOC_Denied", "Mod Options Control: no permission"), 255, 80, 80, 300) end
        return
    end
    if UI.instance then
        UI.instance:bringToTop()
        return
    end
    local w = math.min(1100, getCore():getScreenWidth() - 80)
    local h = math.min(760, getCore():getScreenHeight() - 80)
    local ui = UI:new((getCore():getScreenWidth() - w) / 2, (getCore():getScreenHeight() - h) / 2, w, h)
    ui:initialise()
    ui:addToUIManager()
    ui:setAlwaysOnTop(true)
    UI.instance = ui
    ui:refreshList()
    if not MOC.singleplayer() then C.request() end
end

---------------------------------------------------------------------------
-- Eigene Seite unter Optionen > Mods
---------------------------------------------------------------------------

local own = {}

local function buildOwnPage()
    if not (PZAPI and PZAPI.ModOptions) or own.page then return end
    local page = PZAPI.ModOptions:create(MOC.PAGE_ID, "UI_MOC_Title")
    own.page = page
    page:addDescription("UI_MOC_PlayerHelp")
    own.status = { type = "description", text = "" }
    table.insert(page.data, own.status)
    own.show = page:addTickBox("showLocked", "UI_MOC_ShowLocked", false, "UI_MOC_ShowLocked_tooltip")
    own.button = page:addButton("openAdmin", "UI_MOC_OpenAdmin", "UI_MOC_OpenAdmin_tooltip", function()
        if MainScreen and MainScreen.instance and MainScreen.instance.mainOptions and MainScreen.instance.mainOptions:isVisible() then
            MainScreen.instance.mainOptions:close()
        end
        UI.open()
    end)
    function page:apply()
        local before = C.showLocked
        C.showLocked = own.show:getValue() == true
        if before ~= C.showLocked then C.panelDirty = true end
    end
end

function C.updateOwnPage()
    if not own.page then return end
    local n, pages = C.lockedCount()
    if not getPlayer() then
        own.status.text = txt("UI_MOC_StatusMenu", "Only active in a game.")
    elseif n == 0 then
        own.status.text = txt("UI_MOC_StatusNone", "The server does not set any mod options.")
    else
        own.status.text = txt("UI_MOC_Status", "The server sets %1 options of %2 mods.", n, pages)
    end
    own.button:setEnabled(getPlayer() ~= nil and MOC.canConfigure(getPlayer()))
end

buildOwnPage()

---------------------------------------------------------------------------
-- Knopf im Admin-Panel
---------------------------------------------------------------------------

local function patchAdminPanel()
    if not ISAdminPanelUI or ISAdminPanelUI.mocPatched then return end
    ISAdminPanelUI.mocPatched = true
    local create = ISAdminPanelUI.create
    ISAdminPanelUI.create = function(self, ...)
        create(self, ...)
        if not self.cancel then return end
        local btn = ISButton:new(self.cancel:getX(), self.cancel:getY(), self.cancel:getWidth(), self.cancel:getHeight(),
            txt("UI_MOC_Title", "Mod Options Control"), self, function() UI.open() end)
        btn:initialise()
        btn:instantiate()
        btn.borderColor = self.buttonBorderColor
        btn.tooltip = txt("UI_MOC_OpenAdmin_tooltip", "Lock mod options for players and set their values.")
        btn.enable = MOC.canConfigure(getPlayer())
        self:addChild(btn)
        self.mocButton = btn
        self.cancel:setY(btn:getBottom() + 8)
        self:setHeight(self.cancel:getBottom() + 9)
    end
end

patchAdminPanel()
