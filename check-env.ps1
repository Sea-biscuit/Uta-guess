param(
    [switch]$Strict
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "== $Title ==" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Get-VersionNumbers {
    param([string]$VersionText)
    $match = [regex]::Match($VersionText, "(\d+)(?:\.(\d+))?(?:\.(\d+))?")
    if (-not $match.Success) { return $null }

    $major = [int]$match.Groups[1].Value
    $minor = if ($match.Groups[2].Success) { [int]$match.Groups[2].Value } else { 0 }
    $patch = if ($match.Groups[3].Success) { [int]$match.Groups[3].Value } else { 0 }
    return [pscustomobject]@{
        Major = $major
        Minor = $minor
        Patch = $patch
    }
}

function Test-Command {
    param([string]$Name)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $cmd
}

$failedChecks = 0
$warnChecks = 0

Write-Host "Guess Song environment check" -ForegroundColor Magenta
Write-Host "Required: JDK 17, Maven, Node.js 18+, npm"

Write-Section "Java / JDK"
if (-not (Test-Command "java")) {
    Write-Fail "java not found. Install JDK 17 and set PATH/JAVA_HOME."
    $failedChecks++
} else {
    $javaVersionText = (& cmd /c "java -version 2>&1" | Select-Object -First 1)
    $javaVersion = Get-VersionNumbers -VersionText $javaVersionText
    if ($null -eq $javaVersion) {
        Write-Fail "Unable to parse java version output: $javaVersionText"
        $failedChecks++
    } else {
        if ($javaVersion.Major -eq 17) {
            Write-Ok "Java version is valid: $javaVersionText"
        } elseif ($javaVersion.Major -gt 17) {
            Write-Warn "Java version is higher than 17: $javaVersionText (usually OK)"
            $warnChecks++
        } else {
            Write-Fail "Java version is too low: $javaVersionText (need JDK 17)"
            $failedChecks++
        }
    }
}

Write-Section "Maven"
if (-not (Test-Command "mvn")) {
    Write-Fail "mvn not found. Install Maven and set PATH."
    $failedChecks++
} else {
    $mvnVersionText = (& cmd /c "mvn -v 2>&1" | Select-Object -First 1)
    Write-Ok "Maven detected: $mvnVersionText"
}

Write-Section "Node.js"
if (-not (Test-Command "node")) {
    Write-Fail "node not found. Install Node.js 18+."
    $failedChecks++
} else {
    $nodeVersionText = (& cmd /c "node -v 2>&1")
    $nodeVersion = Get-VersionNumbers -VersionText $nodeVersionText
    if ($null -eq $nodeVersion) {
        Write-Fail "Unable to parse Node.js version output: $nodeVersionText"
        $failedChecks++
    } elseif ($nodeVersion.Major -ge 18) {
        Write-Ok "Node.js version is valid: $nodeVersionText"
    } else {
        Write-Fail "Node.js version is too low: $nodeVersionText (need 18+)"
        $failedChecks++
    }
}

Write-Section "npm"
if (-not (Test-Command "npm")) {
    Write-Fail "npm not found. Confirm Node.js installation."
    $failedChecks++
} else {
    $npmVersionText = (& cmd /c "npm -v 2>&1")
    Write-Ok "npm detected: $npmVersionText"
}

Write-Section "whether-backend\music-exists"
if (Test-Path ".\backend\music") {
    Write-Ok "backend/music directory exists."
} else {
    Write-Warn "backend/music directory not found. Backend may scan no audio files."
    $warnChecks++
}

Write-Host ""
if ($failedChecks -eq 0) {
    Write-Host "Environment check passed. Project can start." -ForegroundColor Green
} else {
    Write-Host "Environment check failed: $failedChecks failed, $warnChecks warnings." -ForegroundColor Red
}

if ($Strict -and ($failedChecks -gt 0 -or $warnChecks -gt 0)) {
    exit 1
}

if (-not $Strict -and $failedChecks -gt 0) {
    exit 1
}

exit 0
