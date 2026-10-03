# Builds OnlineExaminationPortal.war without Maven.
# Usage:  powershell -ExecutionPolicy Bypass -File build.ps1
# If CATALINA_HOME points at your Tomcat 10.1/11 folder, the WAR is also copied to its webapps folder.

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$name = "OnlineExaminationPortal"
$out = Join-Path $root "build"
$web = Join-Path $out $name

$servletJar = Join-Path $root "lib\jakarta.servlet-api-6.0.0.jar"
$pgJar = Get-ChildItem (Join-Path $root "lib") -Filter "postgresql-*.jar" | Select-Object -First 1
if (-not (Test-Path $servletJar)) { throw "Missing $servletJar" }
if (-not $pgJar) { throw "Missing PostgreSQL JDBC driver (lib\postgresql-*.jar)" }

$props = Join-Path $root "src\main\resources\app.properties"
if (-not (Test-Path $props)) {
    Copy-Item (Join-Path $root "src\main\resources\app.properties.example") $props
    Write-Host "Created src\main\resources\app.properties - put your PostgreSQL password in it, then build again." -ForegroundColor Yellow
    exit 1
}

Write-Host "Cleaning build folder..."
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force (Join-Path $web "WEB-INF\classes"), (Join-Path $web "WEB-INF\lib") | Out-Null

Write-Host "Copying web files..."
Copy-Item (Join-Path $root "src\main\webapp\*") $web -Recurse -Force

Write-Host "Compiling Java sources..."
$sources = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& javac --release 17 -encoding UTF-8 -cp "$servletJar;$($pgJar.FullName)" -d (Join-Path $web "WEB-INF\classes") $sources
if ($LASTEXITCODE -ne 0) { throw "Compilation failed" }

# Copy config files (app.properties) next to the classes so AppConfig can find them
Copy-Item (Join-Path $root "src\main\resources\*") (Join-Path $web "WEB-INF\classes") -Recurse -Force
# The servlet API is provided by Tomcat, so only the JDBC driver goes into WEB-INF/lib
Copy-Item $pgJar.FullName (Join-Path $web "WEB-INF\lib")

Write-Host "Packaging WAR..."
$war = Join-Path $out "$name.war"
& jar --create --file $war -C $web .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }
Write-Host "Built $war" -ForegroundColor Green

if ($env:CATALINA_HOME) {
    Copy-Item $war (Join-Path $env:CATALINA_HOME "webapps") -Force
    Write-Host "Deployed to $env:CATALINA_HOME\webapps - open http://localhost:8080/$name/" -ForegroundColor Green
}
