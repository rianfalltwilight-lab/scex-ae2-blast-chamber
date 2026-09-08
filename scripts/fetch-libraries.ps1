param([string]$CacheDirectory = '', [switch]$UseProxy)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$manifest = Get-Content (Join-Path $PSScriptRoot 'libraries.json') -Raw | ConvertFrom-Json
$libDir = Join-Path $root 'libs'
New-Item -ItemType Directory -Path $libDir -Force | Out-Null
foreach ($entry in $manifest) {
    if ($entry.file -ne [IO.Path]::GetFileName($entry.file)) { throw 'Invalid dependency filename' }
    $target = Join-Path $libDir $entry.file
    if (Test-Path -LiteralPath $target) {
        if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Existing dependency differs: $($entry.file)" }
        Write-Output "Verified existing $($entry.file)"
        continue
    }
    $cached = if ($CacheDirectory) { Join-Path $CacheDirectory $entry.file } else { '' }
    if ($cached -and (Test-Path -LiteralPath $cached)) {
        if ((Get-FileHash -LiteralPath $cached -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Cache digest differs: $($entry.file)" }
        Copy-Item -LiteralPath $cached -Destination $target
    } else {
        $part = "$target.part"
        if (Test-Path -LiteralPath $part) { throw "Incomplete download exists: $part; inspect before retrying" }
        $argsList = @('--disable')
        if (!$UseProxy) { $argsList += @('--noproxy=*') }
        $argsList += @('--silent', '--show-error', '--fail', '--location', '--max-redirs', '5', '--connect-timeout', '10', '--speed-limit', '1024', '--speed-time', '30', '--max-time', '300', '--retry', '0', '--proto', '=https', '--proto-redir', '=https', '--output', $part, '--url', $entry.url)
        $curl = if ($IsWindows) { 'curl.exe' } else { 'curl' }
        $start = [Diagnostics.ProcessStartInfo]::new()
        $start.FileName = (Get-Command $curl -CommandType Application).Source
        $start.UseShellExecute = $false
        foreach ($argument in $argsList) { $start.ArgumentList.Add([string]$argument) }
        $process = [Diagnostics.Process]::Start($start)
        $process.WaitForExit()
        $downloadExit = $process.ExitCode
        $process.Dispose()
        if ($downloadExit -ne 0) { throw "Download failed: $($entry.file). Partial file retained; proxy fallback requires -UseProxy." }
        if ((Get-FileHash -LiteralPath $part -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Downloaded digest differs: $($entry.file)" }
        Move-Item -LiteralPath $part -Destination $target
    }
    Write-Output "Prepared $($entry.file)"
}
