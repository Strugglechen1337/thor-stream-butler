[CmdletBinding()]
param(
    [string]$KeyAlias = "thor-stream-butler",
    [switch]$SeparateKeyPassword,
    [switch]$ConfigureGitHubSecrets
)

$ErrorActionPreference = "Stop"

# Creates the permanent Android update key locally. Nothing is uploaded unless
# -ConfigureGitHubSecrets is explicitly supplied.
# Erstellt den dauerhaften Android-Update-Schlüssel lokal. Ohne den expliziten
# Schalter -ConfigureGitHubSecrets wird nichts hochgeladen.

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$signingDirectory = [IO.Path]::GetFullPath((Join-Path $projectRoot "signing"))
$keystorePath = Join-Path $signingDirectory "thor-stream-release.jks"
$propertiesPath = Join-Path $signingDirectory "keystore.properties"

if ($signingDirectory -notlike "$projectRoot*") {
    throw "Refusing to write outside the project / Schreiben außerhalb des Projekts verweigert."
}
if ((Test-Path -LiteralPath $keystorePath) -or (Test-Path -LiteralPath $propertiesPath)) {
    throw "Signing files already exist. They were not overwritten. / Signing-Dateien existieren bereits und wurden nicht überschrieben."
}

$keytool = if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\keytool.exe"))) {
    Join-Path $env:JAVA_HOME "bin\keytool.exe"
} else {
    (Get-Command keytool -ErrorAction Stop).Source
}

Write-Host "Create the one permanent release password and store it in a password manager."
Write-Host "Erstelle das eine dauerhafte Release-Passwort und sichere es in einem Passwortmanager."
$storePasswordSecure = Read-Host "Keystore password / Keystore-Passwort" -AsSecureString
$keyPasswordSecure = if ($SeparateKeyPassword) {
    Read-Host "Key password / Schlüssel-Passwort" -AsSecureString
} else {
    $storePasswordSecure
}

$storePointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePasswordSecure)
$keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPasswordSecure)
try {
    $storePassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($storePointer)
    $keyPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
    if ($storePassword.Length -lt 12 -or $keyPassword.Length -lt 12) {
        throw "Use passwords with at least 12 characters. / Verwende Passwörter mit mindestens 12 Zeichen."
    }

    New-Item -ItemType Directory -Path $signingDirectory | Out-Null
    # Keep passwords out of the process command line. keytool reads these two
    # task-specific environment variables only for the lifetime of this script.
    $env:THOR_KEYTOOL_STORE_PASSWORD = $storePassword
    $env:THOR_KEYTOOL_KEY_PASSWORD = $keyPassword
    $arguments = @(
        "-genkeypair",
        "-keystore", $keystorePath,
        "-storetype", "JKS",
        "-storepass:env", "THOR_KEYTOOL_STORE_PASSWORD",
        "-alias", $KeyAlias,
        "-keypass:env", "THOR_KEYTOOL_KEY_PASSWORD",
        "-keyalg", "RSA",
        "-keysize", "4096",
        "-sigalg", "SHA256withRSA",
        "-validity", "10000",
        "-dname", "CN=Thor Stream Butler, OU=Android Release, O=Thor Stream Butler, C=DE"
    )
    & $keytool @arguments
    if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }

    $properties = @(
        "storeFile=thor-stream-release.jks",
        "storePassword=$storePassword",
        "keyAlias=$KeyAlias",
        "keyPassword=$keyPassword",
        ""
    ) -join [Environment]::NewLine
    [IO.File]::WriteAllText($propertiesPath, $properties, [Text.UTF8Encoding]::new($false))

    if ($ConfigureGitHubSecrets) {
        $null = Get-Command gh -ErrorAction Stop
        $secretValues = [ordered]@{
            SIGNING_KEYSTORE_BASE64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
            SIGNING_STORE_PASSWORD = $storePassword
            SIGNING_KEY_ALIAS = $KeyAlias
            SIGNING_KEY_PASSWORD = $keyPassword
        }
        foreach ($secret in $secretValues.GetEnumerator()) {
            $secret.Value | gh secret set $secret.Key
            if ($LASTEXITCODE -ne 0) {
                throw "GitHub secret setup failed for $($secret.Key) / GitHub-Secret-Einrichtung für $($secret.Key) fehlgeschlagen."
            }
        }
        Write-Host "GitHub signing secrets configured. / GitHub-Signing-Secrets eingerichtet."
    }

    Write-Host "Release signing created in signing/. Back up the JKS and passwords separately now."
    Write-Host "Release-Signing in signing/ erstellt. Sichere JKS und Passwörter jetzt getrennt."
    Write-Host "Verification / Prüfung: .\gradlew.bat :app:assembleRelease"
} finally {
    Remove-Item Env:\THOR_KEYTOOL_STORE_PASSWORD, Env:\THOR_KEYTOOL_KEY_PASSWORD -ErrorAction SilentlyContinue
    if ($storePointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($storePointer) }
    if ($keyPointer -ne [IntPtr]::Zero -and $keyPointer -ne $storePointer) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer) }
    Remove-Variable storePassword, keyPassword -ErrorAction SilentlyContinue
}
