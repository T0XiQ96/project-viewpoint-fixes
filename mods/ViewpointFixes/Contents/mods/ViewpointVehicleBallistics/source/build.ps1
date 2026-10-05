# Baut aus derselben Quelle (vpveh) zwei Jars:
#   ..\42\media\java\ViewpointVehicleBallistics.jar                      (Project Viewpoint - Fixes, Paket vpveh)
#   BulletImpacts2D\...\VFA_VehicleBallistics2D\42\media\java\VehicleBallistics2D.jar  (Kopie, Paket vbi2d)
# Jar liegt bewusst NICHT unter java\client: ZombieBuddy laedt es auch auf dem Server (Schaden macht der Server).
# Aufruf:  powershell -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$game  = 'G:\SteamLibrary\steamapps\common\ProjectZomboid'
$zb    = 'G:\SteamLibrary\steamapps\workshop\content\108600\3619862853\mods\ZombieBuddy\libs\ZombieBuddy.jar'
$vtb   = 'G:\SteamLibrary\steamapps\workshop\content\108600\3812864848\mods\ViewpointTrueBallistics\42\media\java\ViewpointTrueBallistics.jar'
$jdk   = 'G:\Zomboid Claude\VR-Mod\tools\jdk25\bin'   # Spielklassen sind Java 25
$bi2d  = Join-Path $root '..\..\..\..\..\BulletImpacts2D\Contents\mods\VFA_VehicleBallistics2D\42\media\java\VehicleBallistics2D.jar'
$cp    = "$game\projectzomboid.jar;$zb;$vtb"

function Build-Jar($pkg, $buildJava, $out) {
    $tmp = Join-Path $env:TEMP ("vpveh_build_" + $pkg)
    $src = Join-Path $tmp 'src'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    New-Item -ItemType Directory -Force (Join-Path $src $pkg) | Out-Null
    Get-ChildItem (Join-Path $root 'vpveh') -Filter *.java | ForEach-Object {
        $text = Get-Content $_.FullName -Raw -Encoding UTF8
        if ($_.Name -eq 'Build.java' -and $buildJava) { $text = $buildJava }
        $text = $text -replace 'package vpveh;', "package $pkg;" -replace '\bvpveh\.', "$pkg."
        [IO.File]::WriteAllText((Join-Path (Join-Path $src $pkg) $_.Name), $text, (New-Object Text.UTF8Encoding $false))
    }
    $classes = Join-Path $tmp 'classes'
    New-Item -ItemType Directory -Force $classes | Out-Null
    $sources = Get-ChildItem (Join-Path $src $pkg) -Filter *.java | ForEach-Object { $_.FullName }
    & "$jdk\javac.exe" --release 25 -encoding UTF-8 -nowarn -cp $cp -d $classes $sources
    if ($LASTEXITCODE -ne 0) { throw "javac fehlgeschlagen ($pkg)" }
    New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
    & "$jdk\jar.exe" cf $out -C $classes .
    if ($LASTEXITCODE -ne 0) { throw "jar fehlgeschlagen ($pkg)" }
    Remove-Item $tmp -Recurse -Force
    Write-Host "Fertig: $out"
}

Build-Jar 'vpveh' $null (Join-Path $root '..\42\media\java\ViewpointVehicleBallistics.jar')

$bi2dBuild = @'
package vpveh;

/** Fassung dieses Jars: Kopie in Bullet Impacts 2D (laeuft nur ohne Project Viewpoint - Fixes). */
final class Build {
    static final boolean PRIMARY = false;
    static final String SANDBOX = "VehicleBallistics2D";
    static final String PREFIX = "";
    static final String LUA = "VehicleBallistics2D";
    static final String MODULE = "VehicleBallistics2D";
    static final String TAG = "[VehicleBallistics2D]";

    private Build() {}
}
'@
if ($args -notcontains '-OnlyVF') { Build-Jar 'vbi2d' $bi2dBuild $bi2d }
