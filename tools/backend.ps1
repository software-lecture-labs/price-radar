function Get-DatabaseSettings {
    param([Parameter(Mandatory)][string]$EnvFile)

    if (-not (Test-Path -LiteralPath $EnvFile)) { throw "No .env file at $EnvFile" }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $EnvFile) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') { $values[$Matches[1]] = $Matches[2].Trim() }
    }

    $url = if ($values['POSTGRES_URL']) { $values['POSTGRES_URL'] } else { $values['POSTGRES_DB'] }
    if ($url -notmatch '^(jdbc:)?postgres(ql)?://') {
        throw 'Set POSTGRES_URL, or POSTGRES_DB, in .env to a PostgreSQL URL.'
    }
    $uri = [Uri]($url -replace '^jdbc:', '' -replace '^postgres(ql)?://', 'postgresql://')
    if (-not $uri.Host -or $uri.AbsolutePath.Length -le 1) {
        throw 'The PostgreSQL URL in .env needs a host and a database name.'
    }

    $user = $values['POSTGRES_USER']
    $password = $values['POSTGRES_PASSWORD']
    if ($uri.UserInfo) {
        $credentials = $uri.UserInfo -split ':', 2
        $user = [Uri]::UnescapeDataString($credentials[0])
        if ($credentials.Count -eq 2) { $password = [Uri]::UnescapeDataString($credentials[1]) }
    }

    $port = if ($uri.Port -gt 0) { $uri.Port } else { 5432 }
    # JDBC calls libpq's channel_binding option channelBinding.
    $query = $uri.Query -replace '([?&])channel_binding=', '$1channelBinding='
    return @{
        POSTGRES_URL      = "jdbc:postgresql://$($uri.Host):$port$($uri.AbsolutePath)$query"
        POSTGRES_USER     = $user
        POSTGRES_PASSWORD = $password
    }
}

function Invoke-Backend {
    param([Parameter(Mandatory)][string[]]$MavenArguments)

    $repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $settings = Get-DatabaseSettings -EnvFile (Join-Path $repoRoot '.env')
    $previous = @{}
    foreach ($name in $settings.Keys) { $previous[$name] = [Environment]::GetEnvironmentVariable($name) }
    try {
        foreach ($name in $settings.Keys) { [Environment]::SetEnvironmentVariable($name, $settings[$name]) }
        Push-Location (Join-Path $repoRoot 'backend')
        try { & .\mvnw.cmd @MavenArguments }
        finally { Pop-Location }
    }
    finally {
        foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name, $previous[$name]) }
    }
}
