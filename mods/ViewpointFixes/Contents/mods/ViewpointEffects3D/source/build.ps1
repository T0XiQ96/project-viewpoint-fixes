# Baut ViewpointEffects3D.jar nach ..\42\media\java\client.
# Aufruf:  powershell -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$game  = 'G:\SteamLibrary\steamapps\common\ProjectZomboid'
$zb    = 'G:\SteamLibrary\steamapps\workshop\content\108600\3619862853\mods\ZombieBuddy\libs\ZombieBuddy.jar'
$jdk   = 'G:\Zomboid Claude\VR-Mod\tools\jdk25\bin'   # Spielklassen sind Java 25
$tmp   = Join-Path $env:TEMP 'vpfx_build'
$out   = Join-Path $root '..\42\media\java\client\ViewpointEffects3D.jar'

if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
New-Item -ItemType Directory -Force $tmp | Out-Null

$sources = Get-ChildItem (Join-Path $root 'vpfx') -Filter *.java | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" --release 25 -encoding UTF-8 -nowarn -cp "$game\projectzomboid.jar;$zb" -d $tmp $sources
if ($LASTEXITCODE -ne 0) { throw "javac fehlgeschlagen" }

& "$jdk\jar.exe" cf $out -C $tmp .
if ($LASTEXITCODE -ne 0) { throw "jar fehlgeschlagen" }
Remove-Item $tmp -Recurse -Force
Write-Host "Fertig: $out"
