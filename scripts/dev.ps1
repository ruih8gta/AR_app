[CmdletBinding()]
param(
    [ValidateSet('doctor', 'build', 'check', 'test-build', 'devices', 'install', 'device-test')]
    [string]$Action = 'doctor',
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$studioJava = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
$sdkDirectory = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:USERPROFILE 'AppData/Local/Android/Sdk' }
if (-not (Test-Path (Join-Path $studioJava 'bin/java.exe'))) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        $studioJava = $env:JAVA_HOME
    } else { throw 'Android Studio JBR or a valid JAVA_HOME is required.' }
}
if (-not (Test-Path (Join-Path $sdkDirectory 'platform-tools/adb.exe'))) {
    throw 'Android SDK platform-tools were not found. Set ANDROID_HOME to your SDK directory.'
}
# Process-local settings only.
$previousJava = $env:JAVA_HOME
$previousSdk = $env:ANDROID_HOME
try {
    $env:JAVA_HOME = $studioJava
    $env:ANDROID_HOME = $sdkDirectory
    Push-Location $projectDirectory
    try {
        if ($Action -eq 'doctor') {
            Write-Output "Project: $projectDirectory"
            Write-Output "JDK: $studioJava"
            Write-Output "SDK: $sdkDirectory"
            & (Join-Path $studioJava 'bin/java.exe') -version
            if ($LASTEXITCODE -ne 0) { throw 'Java check failed.' }
            & (Join-Path $sdkDirectory 'platform-tools/adb.exe') version
            if ($LASTEXITCODE -ne 0) { throw 'ADB version check failed.' }
            git --version
            if ($LASTEXITCODE -ne 0) { throw 'Git check failed.' }
            $driveName = [IO.Path]::GetPathRoot($projectDirectory).Substring(0, 1)
            Write-Output ("Free disk space (GB): " + [Math]::Round((Get-PSDrive $driveName).Free / 1GB, 1))
            return
        }
        if ($Action -eq 'devices') {
            & (Join-Path $sdkDirectory 'platform-tools/adb.exe') devices -l
            if ($LASTEXITCODE -ne 0) { throw 'ADB could not query devices.' }
            return
        }
        $gradleTasks = switch ($Action) {
            'build'       { @('assembleDebug') }
            'check'       { @('assembleDebug', 'testDebugUnitTest', 'lintDebug') }
            'test-build'  { @('assembleDebugAndroidTest') }
            'install'     { @('installDebug') }
            'device-test' { @('connectedDebugAndroidTest') }
        }
        $gradleArguments = @($gradleTasks) + @('--console=plain')
        if ($Offline) { $gradleArguments += '--offline' }
        & (Join-Path $projectDirectory 'gradlew.bat') @gradleArguments
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE." }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:ANDROID_HOME = $previousSdk
}
