# AlignTech Feature Management Service


---


##  API

### Management API

```bash
# Create flag
POST /api/v1/admin/flags

# List flags (with pagination & filtering)
GET /api/v1/admin/flags?status=active&page=0&size=50

# Get single flag
GET /api/v1/admin/flags/{id}

# Activate flag
POST /api/v1/admin/flags/{id}/activate

# Archive flag
DELETE /api/v1/admin/flags/{id}

# Get audit history
GET /api/v1/admin/audit/{flagId}
```

### Evaluation API

```bash
# Evaluate flags
POST /api/v1/evaluate

# Get snapshot (for SDK initialization)
GET /api/v1/snapshot

# Real-time updates (SSE)
GET /api/v1/stream

# Health check
GET /api/v1/health
```

### Explainability API

```bash
# Explain why flag is enabled/disabled
POST /api/v1/explain/{flagKey}

# GET version with query params
GET /api/v1/explain/{flagKey}?userId=user-123&region=us-west
```

### Observability API

```bash
# Health checks
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness

# Metrics
GET /actuator/metrics
GET /actuator/prometheus

# Cache metrics
GET /api/v1/admin/metrics/cache
```

---

##  Client SDKs

### JavaScript/TypeScript

```typescript
import { AlignTechClient } from '@aligntech/feature-sdk';

const client = new AlignTechClient({
  baseUrl: 'http://localhost:8080',
  appKey: 'your-app-key',
  pollInterval: 30000, // 30 seconds
});

await client.initialize();

const user = {
  userId: 'user-123',
  region: 'us-west',
  platform: 'web',
};

// Simple check
if (client.isEnabled('new-checkout', user)) {
  console.log('New checkout is enabled!');
}

// Detailed evaluation
const result = client.evaluate('premium-features', user);
console.log(`Flag: ${result.flagKey}, Enabled: ${result.enabled}, Reason: ${result.reason}`);
```

### Java

```java
import com.aligntech.featuresdk.AlignTechClient;
import com.aligntech.featuresdk.AlignTechConfig;
import com.aligntech.featuresdk.AlignTechUser;

AlignTechConfig config = AlignTechConfig.builder()
    .baseUrl("http://localhost:8080")
    .appKey("your-app-key")
    .pollInterval(30000)
    .build();

AlignTechClient client = new AlignTechClient(config);
client.initialize();

AlignTechUser user = AlignTechUser.builder()
    .userId("user-123")
    .region("us-west")
    .platform("web")
    .build();

if (client.isEnabled("new-checkout", user)) {
    System.out.println("New checkout is enabled!");
}
```

### Swift

```swift
import AlignTechFeatureSDK

let config = AlignTechConfig(
    baseUrl: "http://localhost:8080",
    appKey: "your-app-key",
    pollInterval: 30.0
)

let client = try await AlignTechClient(config: config)
await client.initialize()

let user = AlignTechUser(
    userId: "user-123",
    region: "us-west",
    platform: "iOS"
)

if await client.isEnabled(flagKey: "new-checkout", user: user) {
    print("New checkout is enabled!")
}
```

### .NET / C#

```csharp
using AlignTech.FeatureSDK;
using AlignTech.FeatureSDK.Models;

var config = new AlignTechConfig
{
    BaseUrl = "http://localhost:8080",
    AppKey = "your-app-key",
    PollIntervalMs = 30000
};

var client = new AlignTechClient(config);
await client.InitializeAsync();

var user = new AlignTechUser
{
    UserId = "user-123",
    Region = "us-west",
    Platform = "web"
};

if (client.IsEnabled("new-checkout", user))
{
    Console.WriteLine("New checkout is enabled!");
}
```




##  Tech Stack

### Backend

- **Java 17** - Language
- **Spring Boot 3.2** - Framework
- **PostgreSQL 15** - Database
- **Redis 7** - Cache & Pub/Sub
- **Flyway** - Schema migrations

### Security

- **Spring Security** - Authentication & Authorization
- **OAuth2 Resource Server** - JWT validation

### Observability

- **Micrometer** - Metrics
- **Prometheus** - Metrics storage
- **Brave** - Distributed tracing
- **Grafana** - Dashboards
- **Logback** - Logging

### DevOps

- **Docker** - Containerization
- **Kubernetes** - Orchestration
- **GitHub Actions** - CI/CD
- **Trivy** - Security scanning

```
