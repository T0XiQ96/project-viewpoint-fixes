-- Open All Containers unter Project Viewpoint (3D).
-- OAC tauscht das Sprite eines Behaelters gegen ein "offen"-Sprite (ct_oac_*). Viewpoint kennt deren Kachel-Geometrie
-- nicht und zeigt den Behaelter dann gar nicht. ViewpointOpenContainers.jar liefert die Geometrie nach; fuer
-- Sprites ohne eigene Geometrie nimmt es die des geschlossenen Original-Sprites.
-- Diese Datei schreibt dafuer die Zuordnung "offenes Sprite -> Original-Sprite" in die Tabelle VFA_OacOriginal.

VFA_OacOriginal = VFA_OacOriginal or {}

local function fill()
    local ok, data = pcall(require, "OAC_SpriteData")
    if not ok or type(data) ~= "table" then return end
    local fields = { "openSprite", "openSprite2", "openSprite3", "openSprite4PostFlag" }
    for _, group in pairs(data) do
        if type(group) == "table" then
            for _, sprite in pairs(group) do
                if type(sprite) == "table" and type(sprite.originalSprite) == "string" then
                    for _, key in ipairs(fields) do
                        local open = sprite[key]
                        if type(open) == "string" then VFA_OacOriginal[open] = sprite.originalSprite end
                    end
                end
            end
        end
    end
end

pcall(fill)
Events.OnGameBoot.Add(function() pcall(fill) end)
Events.OnGameStart.Add(function() pcall(fill) end)
