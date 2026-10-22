param(
    [Parameter(Mandatory = $true)][string]$File,
    [switch]$DryRun,
    [switch]$Commit,
    [switch]$Batch,
    [string]$ExpectedSha256 = ''
)

$ErrorActionPreference = 'Stop'
if ($DryRun -and $Commit) { throw 'Choose either -DryRun or -Commit.' }
if ($Commit -and $ExpectedSha256 -cnotmatch '^[0-9a-f]{64}$') {
    throw '-Commit requires -ExpectedSha256 from the reviewed preview.'
}
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$sourcePath = if ([IO.Path]::IsPathRooted($File)) { $File } else { Join-Path $repoRoot $File }
$classes = Join-Path $repoRoot 'backend/target/classes'
$jar = Join-Path $repoRoot 'backend/target/backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath (Join-Path $classes 'com/priceradar/ingestion/ReefImportCommand.class'))) {
    throw 'Compile the importer first: run .\mvnw.cmd -Dtest=ReefSearchImporterTest test from backend.'
}
if (-not (Test-Path -LiteralPath $jar)) {
    throw 'Build existing runtime libraries first: run .\mvnw.cmd -DskipTests package from backend.'
}
$mode = if ($Commit) { 'commit' } elseif ($DryRun) { 'dry-run' } else { 'preview' }
if ($Batch) { $mode = 'batch-' + $mode }
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$temporaryName = 'price-radar-import-' + [guid]::NewGuid().ToString('N')
$temporaryDir = Join-Path $tempRoot $temporaryName
New-Item -ItemType Directory -Path $temporaryDir | Out-Null
try {
    # Reuse the application's pinned dependencies; no packages are installed.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -match '^BOOT-INF/lib/[^/]+\.jar$') {
                [IO.Compression.ZipFileExtensions]::ExtractToFile(
                    $entry, (Join-Path $temporaryDir $entry.Name))
            }
        }
    }
    finally { $archive.Dispose() }
    $classPath = $classes + [IO.Path]::PathSeparator + (Join-Path $temporaryDir '*')
    $expectedArgument = if ($ExpectedSha256) { $ExpectedSha256 } else { '-' }
    & java '-Dfile.encoding=UTF-8' -cp $classPath com.priceradar.ingestion.ReefImportCommand `
        $repoRoot $sourcePath $mode $expectedArgument
    $importExit = $LASTEXITCODE
}
finally {
    $resolved = (Resolve-Path -LiteralPath $temporaryDir).ProviderPath
    if ($resolved -ne $temporaryDir -or
        -not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolved) -ne $temporaryName) {
        throw 'Unexpected temporary cleanup target.'
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
exit $importExit
