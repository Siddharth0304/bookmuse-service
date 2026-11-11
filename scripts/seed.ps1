param([string]$BaseUrl = 'http://localhost:8080/api')
$ErrorActionPreference = 'Stop'
if (-not $env:ADMIN_EMAIL -or -not $env:ADMIN_PASSWORD) {
    throw 'Export ADMIN_EMAIL and ADMIN_PASSWORD before seeding.'
}
$loginBody = @{ email = $env:ADMIN_EMAIL; password = $env:ADMIN_PASSWORD } | ConvertTo-Json
$session = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($session.accessToken)" }
$books = @(
    @{title='Pride and Prejudice';author='Jane Austen';genres=@('classics','romance');summary='Manners, marriage, and mistaken impressions.'},
    @{title='Frankenstein';author='Mary Shelley';genres=@('classics','science fiction');summary='A scientist confronts the consequences of his creation.'},
    @{title='The Time Machine';author='H. G. Wells';genres=@('science fiction');summary='A journey into the distant future.'}
)
foreach ($book in $books) {
    $search = [Uri]::EscapeDataString($book.title)
    $existing = Invoke-RestMethod -Uri "$BaseUrl/books?search=$search&size=100" -Headers $headers
    if (-not ($existing.content | Where-Object { $_.title -eq $book.title -and $_.author -eq $book.author })) {
        $null = Invoke-RestMethod -Method Post -Uri "$BaseUrl/books" -Headers $headers -ContentType 'application/json' -Body ($book | ConvertTo-Json)
        Write-Output "Created book: $($book.title)"
    }
}
if ($env:DEMO_EMAIL -and $env:DEMO_PASSWORD) {
    $body = @{email=$env:DEMO_EMAIL;password=$env:DEMO_PASSWORD;name='Sample Reader'} | ConvertTo-Json
    $null = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/register" -ContentType 'application/json' -Body $body
    Write-Output 'Created sample reader.'
}
