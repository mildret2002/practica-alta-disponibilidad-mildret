param(
    [Parameter(Mandatory=$true)]
    [string]$OutputFile
)

Set-Content -Path $OutputFile -Value "" -NoNewline

Write-Host "Ejecutando 200 solicitudes..." -ForegroundColor Cyan

for ($i = 1; $i -le 200; $i++) {
    $tempFile = [System.IO.Path]::GetTempFileName()

    try {
        $metadata = curl.exe -sS -o $tempFile -w "%{http_code} %{time_total}" http://localhost:8080/v1/products

        $body = Get-Content -Raw -Path $tempFile
        $body = $body -replace '\r?\n', ' '

        $line = "{0:D3} {1} {2}" -f $i, $metadata, $body

        Add-Content -Path $OutputFile -Value $line
        Write-Host $line
    }
    finally {
        Remove-Item -Force $tempFile -ErrorAction SilentlyContinue
    }
}

$lines = @(Get-Content -Path $OutputFile)
$total = $lines.Count

$success = @($lines | Where-Object {
    $_ -match '"status"\s*:\s*"SUCCESS"'
}).Count

$http200 = @($lines | Where-Object {
    $_ -match '^\d+\s+200\s+'
}).Count

$errors = $total - $http200

Write-Host "`nRESULTADOS" -ForegroundColor Cyan
Write-Host "Respuestas SUCCESS: $success"
Write-Host "Respuestas HTTP 200: $http200"
Write-Host "Respuestas HTTP distintas de 200: $errors"
Write-Host "Solicitudes totales: $total"

if ($total -gt 0) {
    $availability = [math]::Round($http200 / $total, 4)
    $percentage = [math]::Round(100 * $http200 / $total, 2)

    Write-Host "Disponibilidad observada: $availability ($percentage%)"
}

Write-Host "Resultados guardados en: $OutputFile"