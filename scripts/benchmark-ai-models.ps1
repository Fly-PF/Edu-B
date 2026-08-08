$ErrorActionPreference = "Stop"

$baseUrl = $env:EDU_AI_BASE_URL
$apiKey = $env:EDU_AI_API_KEY

if ([string]::IsNullOrWhiteSpace($baseUrl)) {
    throw "EDU_AI_BASE_URL is required."
}
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    throw "EDU_AI_API_KEY is required."
}

$models = @(
    "gpt-5.4-mini",
    "gpt-5.5",
    "gpt-5.6-luna",
    "gpt-5.6-sol",
    "gpt-5.6-terra",
    "gpt-5.3-codex-spark"
)

$uri = ($baseUrl.TrimEnd("/") + "/chat/completions")
$systemPrompt = "You are a strict teacher grader. Output JSON only. Grade semantically, require completeness, and keep reasons brief."
$userPrompt = @"
{"question":"Explain the difference between list and tuple in Python.","questionType":"Short answer","referenceAnswer":"Lists are mutable and tuples are immutable.","rubric":[{"criterion":"Correctness","description":"Explain both are ordered sequences and avoid obvious mistakes.","maxScore":3},{"criterion":"Core difference","description":"Explain list mutability vs tuple immutability and element changes.","maxScore":3},{"criterion":"Scenario fit","description":"Match list to dynamic data and tuple to fixed data.","maxScore":3},{"criterion":"Clarity","description":"Clear and logical explanation.","maxScore":1}],"studentAnswer":"Lists can be modified. Tuples usually cannot. Lists fit changing data and tuples fit fixed data.","maxScore":10}
"@

$handler = [System.Net.Http.HttpClientHandler]::new()
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(60)
$client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $apiKey)

foreach ($model in $models) {
    $payload = @{
        model = $model
        temperature = 0
        messages = @(
            @{ role = "system"; content = $systemPrompt },
            @{ role = "user"; content = $userPrompt }
        )
        response_format = @{ type = "json_object" }
        max_tokens = 420
    } | ConvertTo-Json -Depth 8 -Compress

    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, $uri)
    $request.Content = [System.Net.Http.StringContent]::new($payload, [System.Text.Encoding]::UTF8, "application/json")

    $startedAt = Get-Date
    $response = $null
    $content = ""
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $totalMs = [int]((Get-Date) - $startedAt).TotalMilliseconds
        $status = [int]$response.StatusCode
        $contentLength = if ($null -eq $content) { 0 } else { $content.Length }
        $result = if ($status -ge 200 -and $status -lt 300) { "success" } else { "failure" }
        Write-Output ("model={0} httpStatus={1} totalMs={2} contentLength={3} result={4}" -f $model, $status, $totalMs, $contentLength, $result)
    } catch {
        $totalMs = [int]((Get-Date) - $startedAt).TotalMilliseconds
        Write-Output ("model={0} httpStatus=0 totalMs={1} contentLength=0 result=failure" -f $model, $totalMs)
    } finally {
        if ($response) {
            $response.Dispose()
        }
        $request.Dispose()
    }
}

$client.Dispose()
$handler.Dispose()
