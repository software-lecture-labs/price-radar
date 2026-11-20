<#
.SYNOPSIS
Starts the backend against the database configured in .env.

.EXAMPLE
.\tools\run-backend.ps1
#>
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'backend.ps1')

Invoke-Backend @('spring-boot:run')
exit $LASTEXITCODE
