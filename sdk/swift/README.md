# AlignTech Feature SDK - Swift

Swift SDK for AlignTech Feature Management Service. Supports iOS, macOS, tvOS, and watchOS.

## Requirements

- iOS 15.0+ / macOS 12.0+ / tvOS 15.0+ / watchOS 8.0+
- Swift 5.9+
- Xcode 15.0+

## Installation

### Swift Package Manager

Add the following to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/aligntech/feature-sdk-swift.git", from: "1.0.0")
]
```

Or in Xcode:
1. File → Add Packages...
2. Enter package URL
3. Select version

## Usage

### Basic Example

```swift
import AlignTechFeatureSDK

let config = AlignTechConfig(
    baseUrl: "https://feature-service.example.com",
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

if await client.isEnabled(flagKey: "new-onboarding-flow", user: user) {
    print("New onboarding flow is enabled")
}
```

### SwiftUI Integration

```swift
import SwiftUI
import AlignTechFeatureSDK

@MainActor
class FeatureFlagService: ObservableObject {
    private var client: AlignTechClient?
    
    func initialize() async {
        let config = AlignTechConfig(
            baseUrl: "https://feature-service.example.com",
            appKey: "ios-app-key"
        )
        
        do {
            client = try await AlignTechClient(config: config)
            await client?.initialize()
        } catch {
            print("Failed to initialize: \(error)")
        }
    }
    
    func isEnabled(_ flagKey: String, for user: AlignTechUser) async -> Bool {
        guard let client = client else { return false }
        return await client.isEnabled(flagKey: flagKey, user: user)
    }
}

struct ContentView: View {
    @StateObject private var featureFlags = FeatureFlagService()
    @State private var showNewFeature = false
    
    var body: some View {
        VStack {
            if showNewFeature {
                NewFeatureView()
            } else {
                OldFeatureView()
            }
        }
        .task {
            await featureFlags.initialize()
            
            let user = AlignTechUser(
                userId: UserDefaults.standard.string(forKey: "userId"),
                region: Locale.current.region?.identifier
            )
            
            showNewFeature = await featureFlags.isEnabled(
                "new-ui-redesign",
                for: user
            )
        }
    }
}
```

### Advanced Evaluation

```swift
let result = await client.evaluate(flagKey: "premium-features", user: user)

if let result = result {
    print("Flag: \(result.flagKey)")
    print("Enabled: \(result.enabled)")
    print("Reason: \(result.reason)")
}
```

### UIKit Integration

```swift
class ViewController: UIViewController {
    var client: AlignTechClient?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        Task {
            let config = AlignTechConfig(
                baseUrl: "https://feature-service.example.com",
                appKey: "ios-app-key"
            )
            
            client = try await AlignTechClient(config: config)
            await client?.initialize()
            
            await checkFeatureFlags()
        }
    }
    
    func checkFeatureFlags() async {
        guard let client = client else { return }
        
        let user = AlignTechUser(
            userId: getCurrentUserId(),
            platform: "iOS",
            appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        )
        
        let isDarkModeEnabled = await client.isEnabled(
            flagKey: "dark-mode",
            user: user
        )
        
        await MainActor.run {
            if isDarkModeEnabled {
                overrideUserInterfaceStyle = .dark
            }
        }
    }
}
```

### Cleanup

```swift
await client.close()
```

## Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `baseUrl` | String | `http://localhost:8080` | Feature service URL |
| `appKey` | String | *required* | Application API key |
| `pollInterval` | TimeInterval | `30.0` | Polling interval in seconds |

## User Context

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String? | User identifier |
| `tenantId` | String? | Tenant/organization ID |
| `region` | String? | Geographic region |
| `appVersion` | String? | Application version |
| `platform` | String? | Platform (iOS, macOS, etc.) |
| `custom` | [String: Any] | Custom attributes |

## API

### `init(config: AlignTechConfig) async throws`

Creates a new client instance.

### `func initialize() async`

Initializes the client by fetching initial flag configuration and starting the polling loop.

### `func isEnabled(flagKey: String, user: AlignTechUser) async -> Bool`

Returns `true` if the flag is enabled for the given user, `false` otherwise.

### `func evaluate(flagKey: String, user: AlignTechUser) async -> EvaluationResult?`

Returns detailed evaluation result or `nil` if flag doesn't exist.

### `func isReady() async -> Bool`

Returns `true` if the client has successfully loaded flags.

### `func setTraceParent(_ traceParent: String) async`

Sets W3C trace parent for distributed tracing.

### `func close() async`

Stops polling and cleans up resources.

## Concurrency

This SDK uses Swift's modern concurrency features (`async`/`await`). All methods are marked with `async` and should be called within an asynchronous context.

## License

MIT
