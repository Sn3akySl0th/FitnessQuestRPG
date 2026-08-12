param(
    [Parameter(Mandatory = $true)]
    [string] $Username,

    [string] $Project = "fitquest-1f082"
)

$ErrorActionPreference = "Stop"

$key = $Username.Trim().ToLowerInvariant()
if ($key -notmatch '^[a-z][a-z0-9_]{2,19}$') {
    throw "Invalid username '$Username'. Expected 3-20 chars: letters, numbers, underscore, starting with a letter."
}

if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    throw "Firebase CLI not found. Install it with: npm install -g firebase-tools"
}

Write-Host "Freeing username '$key' in project '$Project'..."

$docs = @(
    "usernamePointers/$key",
    "usernames/$key"
)

foreach ($doc in $docs) {
    Write-Host "Deleting $doc"
    firebase firestore:delete $doc --project $Project --yes
}

Write-Host "Done. Relaunch the app and claim '$key' again."
