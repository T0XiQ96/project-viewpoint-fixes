# Holt den aktuellen Stand aus den lokalen Quellordnern in dieses Repo und erzeugt die Optionstabellen neu.
# Aufruf (im Repo-Ordner):  powershell -ExecutionPolicy Bypass -File sync.ps1
# Danach: git add -A; git commit; git push
$ErrorActionPreference = 'Stop'
$repo = $PSScriptRoot
$ws   = 'G:\Zomboid Claude\Eigene Mods - Viewpoint Server\Workshop'
$moc  = 'G:\Zomboid Claude\Workshop-Upload\ModOptionsControl'

$items = @(
    @{ src = "$ws\ViewpointFixes";  dst = "$repo\mods\ViewpointFixes" },
    @{ src = "$ws\BulletImpacts2D"; dst = "$repo\mods\BulletImpacts2D" },
    @{ src = $moc;                  dst = "$repo\mods\ModOptionsControl" }
)
foreach ($i in $items) {
    # tools/backup (alte Jars) und kompilierte Hilfsklassen nicht ins Repo
    robocopy $i.src $i.dst /MIR /XD backup __pycache__ /XF *.class /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy fehlgeschlagen: $($i.src)" }
}
python "$repo\mods\ViewpointFixes\tools\docgen.py" "$repo\docs"
if ($LASTEXITCODE -ne 0) { throw "docgen fehlgeschlagen" }
Write-Host "Fertig. Jetzt: git add -A; git commit -m '...'; git push"
