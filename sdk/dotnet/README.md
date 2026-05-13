# AlignTech Feature SDK - .NET

.NET SDK for AlignTech Feature Management Service. Supports .NET 8.0+.

## Requirements

- .NET 8.0 or higher
- C# 12.0+

## Installation

### NuGet Package Manager

```bash
dotnet add package AlignTech.FeatureSDK
```

Or via Package Manager Console:

```powershell
Install-Package AlignTech.FeatureSDK
```

## Usage

### Basic Example

```csharp
using AlignTech.FeatureSDK;
using AlignTech.FeatureSDK.Models;

var config = new AlignTechConfig
{
    BaseUrl = "https://feature-service.example.com",
    AppKey = "your-app-key",
    PollIntervalMs = 30000 // 30 seconds (optional)
};

var client = new AlignTechClient(config);
await client.InitializeAsync();

var user = new AlignTechUser
{
    UserId = "user-123",
    Region = "us-west",
    Platform = "web"
};

if (client.IsEnabled("new-checkout-flow", user))
{
    Console.WriteLine("New checkout flow is enabled");
}
```

### ASP.NET Core Integration

#### Register as a Service

```csharp
// Program.cs
using AlignTech.FeatureSDK;
using AlignTech.FeatureSDK.Models;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton<AlignTechClient>(sp =>
{
    var config = new AlignTechConfig
    {
        BaseUrl = builder.Configuration["AlignTech:BaseUrl"] ?? "http://localhost:8080",
        AppKey = builder.Configuration["AlignTech:AppKey"] ?? throw new InvalidOperationException("AppKey is required"),
        PollIntervalMs = 30000
    };
    
    var client = new AlignTechClient(config);
    client.InitializeAsync().GetAwaiter().GetResult();
    return client;
});

var app = builder.Build();
```

#### Use in Controllers

```csharp
using AlignTech.FeatureSDK;
using AlignTech.FeatureSDK.Models;
using Microsoft.AspNetCore.Mvc;

[ApiController]
[Route("api/[controller]")]
public class ProductsController : ControllerBase
{
    private readonly AlignTechClient _featureClient;

    public ProductsController(AlignTechClient featureClient)
    {
        _featureClient = featureClient;
    }

    [HttpGet]
    public IActionResult GetProducts()
    {
        var user = new AlignTechUser
        {
            UserId = User.Identity?.Name,
            Region = HttpContext.Request.Headers["X-Region"].ToString(),
            Platform = "web"
        };

        var showNewUI = _featureClient.IsEnabled("new-product-ui", user);

        if (showNewUI)
        {
            return Ok(GetProductsV2());
        }

        return Ok(GetProductsV1());
    }
}
```

### Advanced Evaluation

```csharp
var result = client.Evaluate("premium-features", user);

if (result != null)
{
    Console.WriteLine($"Flag: {result.FlagKey}");
    Console.WriteLine($"Enabled: {result.Enabled}");
    Console.WriteLine($"Reason: {result.Reason}");
}
```

### Blazor Integration

```csharp
@page "/dashboard"
@inject AlignTechClient FeatureClient

<h3>Dashboard</h3>

@if (_showNewDashboard)
{
    <NewDashboard />
}
else
{
    <OldDashboard />
}

@code {
    private bool _showNewDashboard = false;

    protected override async Task OnInitializedAsync()
    {
        var user = new AlignTechUser
        {
            UserId = await GetCurrentUserId(),
            Platform = "web"
        };

        _showNewDashboard = FeatureClient.IsEnabled("new-dashboard", user);
    }

    private async Task<string> GetCurrentUserId()
    {
        // Get user ID from auth state
        return "user-123";
    }
}
```

### Background Service Example

```csharp
using AlignTech.FeatureSDK;
using AlignTech.FeatureSDK.Models;

public class FeatureFlagBackgroundService : BackgroundService
{
    private readonly AlignTechClient _featureClient;
    private readonly ILogger<FeatureFlagBackgroundService> _logger;

    public FeatureFlagBackgroundService(
        AlignTechClient featureClient,
        ILogger<FeatureFlagBackgroundService> logger)
    {
        _featureClient = featureClient;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            var user = new AlignTechUser
            {
                TenantId = "system",
                Platform = "background-service"
            };

            if (_featureClient.IsEnabled("advanced-analytics", user))
            {
                _logger.LogInformation("Running advanced analytics...");
                // Run advanced analytics
            }

            await Task.Delay(TimeSpan.FromMinutes(5), stoppingToken);
        }
    }
}
```

### Dependency Injection with IHostedService

```csharp
// FeatureFlagInitializer.cs
using AlignTech.FeatureSDK;

public class FeatureFlagInitializer : IHostedService
{
    private readonly AlignTechClient _client;

    public FeatureFlagInitializer(AlignTechClient client)
    {
        _client = client;
    }

    public async Task StartAsync(CancellationToken cancellationToken)
    {
        await _client.InitializeAsync();
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        _client.Close();
        return Task.CompletedTask;
    }
}

// Program.cs
builder.Services.AddHostedService<FeatureFlagInitializer>();
```

### Cleanup

```csharp
client.Close();
// or
client.Dispose();
```

## Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `BaseUrl` | string | `http://localhost:8080` | Feature service URL |
| `AppKey` | string | *required* | Application API key |
| `PollIntervalMs` | int | `30000` | Polling interval in milliseconds |

### appsettings.json Example

```json
{
  "AlignTech": {
    "BaseUrl": "https://feature-service.example.com",
    "AppKey": "your-app-key-here",
    "PollIntervalMs": 30000
  }
}
```

## User Context

| Field | Type | Description |
|-------|------|-------------|
| `UserId` | string? | User identifier |
| `TenantId` | string? | Tenant/organization ID |
| `Region` | string? | Geographic region |
| `AppVersion` | string? | Application version |
| `Platform` | string? | Platform (web, mobile, etc.) |
| `Custom` | Dictionary<string, object>? | Custom attributes |

## API

### `new AlignTechClient(AlignTechConfig config)`

Creates a new client instance.

### `Task InitializeAsync()`

Initializes the client by fetching initial flag configuration and starting the polling loop.

### `bool IsEnabled(string flagKey, AlignTechUser user)`

Returns `true` if the flag is enabled for the given user, `false` otherwise.

### `EvaluationResult? Evaluate(string flagKey, AlignTechUser user)`

Returns detailed evaluation result or `null` if flag doesn't exist.

### `bool IsReady()`

Returns `true` if the client has successfully loaded flags.

### `void SetTraceParent(string traceParent)`

Sets W3C trace parent for distributed tracing.

### `void Close()`

Stops polling and cleans up resources.

### `void Dispose()`

Disposes the client and releases resources.

## Thread Safety

The SDK is thread-safe and can be used concurrently from multiple threads.

## Error Handling

```csharp
try
{
    var config = new AlignTechConfig
    {
        BaseUrl = "https://feature-service.example.com",
        AppKey = "your-app-key"
    };

    var client = new AlignTechClient(config);
    await client.InitializeAsync();

    if (!client.IsReady())
    {
        Console.WriteLine("Feature client not ready, using defaults");
    }
}
catch (ArgumentException ex)
{
    Console.WriteLine($"Configuration error: {ex.Message}");
}
catch (HttpRequestException ex)
{
    Console.WriteLine($"Network error: {ex.Message}");
}
```

## Best Practices

1. **Singleton Pattern**: Register the client as a singleton in your DI container
2. **Graceful Degradation**: Always handle cases where flags can't be loaded
3. **User Context**: Provide as much user context as possible for accurate targeting
4. **Initialization**: Wait for `InitializeAsync()` to complete before evaluating flags
5. **Cleanup**: Call `Close()` or `Dispose()` when shutting down your application

## License

MIT
