-- Project Viewpoint: Interaktionsliste neben dem Fadenkreuz aufraeumen.
--
-- Viewpoint baut die Liste aus dem normalen Rechtsklick-Menue des Spiels. Mods, die dort Eintraege einfuegen,
-- die mit dem angesehenen Objekt wenig zu tun haben, erscheinen dadurch bei jedem Objekt - und weil Eintraege ganz oben
-- auch die Ueberschrift bestimmen, steht dann z. B. "Wellness" ueber einer Tuer. Lifestyle haengt ausserdem alles
-- "ueber sich selbst" (Yoga, Tanzen, Wissen teilen, Admin-Debugmenue ...) an jedes Objekt in Spielernaehe.
--
-- Hier werden solche Eintraege nur aus Viewpoints Liste genommen. Das normale Rechtsklick-Menue des Spiels
-- (auch im Viewpoint-Mausmodus) bleibt unveraendert. Ausserdem:
--   - alles zum Verstecken (Hide Anywhere) steht ganz oben,
--   - Hinweistexte ueber dem Charakter ("Missing a Hammer...") werden unterdrueckt, waehrend die Liste gebaut wird,
--   - das aktuell angesehene Objekt steht in VFA_VpTarget (fuer ViewpointCrosshairUse).

-- Was aus der Liste soll ------------------------------------------------------------------------------------------

-- Untermenues (Name des Untermenues als Text)
local function hiddenGroups()
    return {
        ["Container Options"] = true,                                  -- Customizable Containers
        [getText("ContextMenu_P4Decoholic_ChangeOverlay")] = true,    -- Decoholic: Moebelanzeige aendern
        ["Door Bar"] = true,                                           -- Door Bar
        ["Door Bar - Garage"] = true,
        ["Door Bar - Double Door"] = true,
        [getText("ContextMenu_LSBody")] = true,                       -- Lifestyle: Wellness (Yoga, Meditation)
        [getText("ContextMenu_LSDebug_Main")] = true,                 -- Lifestyle: Admin-Debugmenue
    }
end

-- einzelne Eintraege ohne Untermenue
local function hiddenOptions()
    return {
        [getText("ContextMenu_EnableAutoClose")] = true,               -- Open All Containers
        [getText("ContextMenu_DisableAutoClose")] = true,
    }
end

-- Verstecken: Texte von Hide Anywhere ("In %1 verstecken", "Unter dem Bett verstecken", "Drinnen verstecken")
local MARK = "@@@"

local function hideMatcher()
    local named = getText("UI_DHCS_Action_HideInsideNamed", MARK)
    local before, after = "", ""
    local cut = string.find(named, MARK, 1, true)
    if cut then
        before, after = string.sub(named, 1, cut - 1), string.sub(named, cut + #MARK)
    end
    local exact = {
        [getText("UI_DHCS_Action_HideUnderBed")] = true,
        [getText("UI_DHCS_Action_HideInside")] = true,
    }
    return function(name)
        if exact[name] then return true end
        if cut and before ~= "" or after ~= "" then
            if (before == "" or string.sub(name, 1, #before) == before)
                and (after == "" or string.sub(name, -#after) == after) and #name > #before + #after then
                return true
            end
        end
        return false
    end
end

-- Eintraege, die ein Mod ueber seine eigene Pruefung ausblendet, solange Viewpoint die Liste baut ("Wellness" von
-- Lifestyle: sonst Ueberschrift der Liste) ------------------------------------------------------------------------

local function switchOff()
    local undo = {}
    local zen = ZenWellnessContextMenu
    if zen and zen.isValid then
        local original = zen.isValid
        zen.isValid = function() return false end
        undo[#undo + 1] = function() zen.isValid = original end
    end
    -- Lifestyle: alle Eintraege "ueber sich selbst" (Yoga/Meditation, Tanzen, Wissen teilen, Raum putzen, auf den
    -- Boden pinkeln, Admin-Debugmenue) haengt Lifestyle an jedes Objekt in der Naehe des Spielers.
    if LSGetContextOptions then
        local original = LSGetContextOptions
        LSGetContextOptions = function(player, context)
            if context == "contextSelfTable" then return {} end
            return original(player, context)
        end
        undo[#undo + 1] = function() LSGetContextOptions = original end
    end
    -- Lifestyle-Debugmenue fuer Admins (Skills setzen, u. a. "Yoga (x)")
    if LSUtil and LSUtil.hasAdminRights then
        local original = LSUtil.hasAdminRights
        LSUtil.hasAdminRights = function() return false end
        undo[#undo + 1] = function() LSUtil.hasAdminRights = original end
    end
    return function()
        for _, f in ipairs(undo) do f() end
    end
end

-- Liste filtern ---------------------------------------------------------------------------------------------------

local function stripGroup(label, group)
    local prefix = group .. ": "
    if string.sub(label, 1, #prefix) == prefix then return string.sub(label, #prefix + 1) end
    return label
end

local function filter(result)
    local actions = ViewpointInteract.actions
    if type(result) ~= "table" or type(result.labels) ~= "table" or type(actions) ~= "table" then return result end
    local groups, options, isHide = hiddenGroups(), hiddenOptions(), hideMatcher()
    local top, rest, removedTitle = {}, {}, false
    for _, a in ipairs(actions) do
        local hide = (a.group and groups[a.group]) or (not a.group and options[a.name])
        if hide then
            if a.group and a.group == result.title then removedTitle = true end
        elseif not a.group and isHide(a.name) then
            top[#top + 1] = a
        else
            rest[#rest + 1] = a
        end
    end
    local kept = {}
    for _, a in ipairs(top) do kept[#kept + 1] = a end
    for _, a in ipairs(rest) do kept[#kept + 1] = a end

    local changed = #kept ~= #actions or #top > 0
    if not changed then return result end

    local title = result.title
    if removedTitle then
        title = nil
        for _, a in ipairs(kept) do
            if a.group then title = a.group break end
        end
    end
    local labels, enabled = {}, {}
    for i, a in ipairs(kept) do
        local label = a.name
        if removedTitle and title and a.group == title then label = stripGroup(label, title) end
        labels[i], enabled[i] = label, a.enabled
    end
    ViewpointInteract.actions = kept
    result.title, result.labels, result.enabled = title, labels, enabled
    return result
end

local wrapped = false

local function wrap()
    if wrapped or not (ViewpointInteract and ViewpointInteract.harvest) then return end
    wrapped = true
    local harvest = ViewpointInteract.harvest

    ViewpointInteract.harvest = function(player, object, ...)
        VFA_VpTarget = object
        VFA_VpTargetTime = getTimestampMs()
        local restore = switchOff()
        VFA_InHarvest = true
        local ok, result = pcall(harvest, player, object, ...)
        VFA_InHarvest = false
        restore()
        if not ok then error(result, 0) end
        local good, filtered = pcall(filter, result)
        if not good then
            print("[ViewpointInteractList] " .. tostring(filtered))
            return result
        end
        return filtered
    end
end

wrap()
Events.OnGameStart.Add(wrap)
Events.OnTick.Add(wrap)
