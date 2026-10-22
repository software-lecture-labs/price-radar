<#
.SYNOPSIS
Imports saved ReefAPI search responses into the database configured in .env.

.DESCRIPTION
Dry run unless -Commit is supplied.

.EXAMPLE
.\tools\reefapi\import.ps1

.EXAMPLE
.\tools\reefapi\import.ps1 -Source 2026-10-22 -Commit
#>
param(
    [string[]]$Source = @(),
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
