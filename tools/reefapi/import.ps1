<#
.SYNOPSIS
Imports saved ReefAPI search responses into the database configured in .env.

.DESCRIPTION
Runs the backend with the "import" profile (SearchImportRunner). Every file is
validated before anything is written, and each file is its own transaction.
Without -Commit nothing is written: the run only reports what would happen.
Files that were already imported are skipped, so a run can always be repeated.

.EXAMPLE
.\tools\reefapi\import.ps1
Dry run over everything under data/reefapi/raw.

.EXAMPLE
.\tools\reefapi\import.ps1 -Source 2026-10-22 -Commit
Imports one fetch.
#>
param(
    # A file or folder under data/reefapi/raw, relative to it. Can be given more than once.
    [string[]]$Source = @(),
    # Write to the database.
    [switch]$Commit
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '..\backend.ps1')

$applicationArguments = @($Source | ForEach-Object { "--source=$_" })
if ($Commit) { $applicationArguments += '--commit' }

Invoke-Backend @(
    '-q', 'spring-boot:run',
    '-Dspring-boot.run.profiles=import',
    "-Dspring-boot.run.arguments=$($applicationArguments -join ' ')"
)
exit $LASTEXITCODE
