# AlignTech Feature SDK - JavaScript/TypeScript

JavaScript/TypeScript SDK for AlignTech Feature Management Service.

## Installation

```bash
npm install @aligntech/feature-sdk
```

## Usage

### Basic Example

```typescript
import { AlignTechClient, AlignTechUser } from '@aligntech/feature-sdk';

const client = new AlignTechClient({
  baseUrl: 'http://localhost:8080',
  appKey: 'your-app-key',
  pollInterval: 30000, // 30 seconds (optional)
});

await client.initialize();

const user: AlignTechUser = {
  userId: 'user-123',
  region: 'us-west',
  platform: 'web',
};

if (client.isEnabled('new-checkout-flow', user)) {
  console.log('New checkout flow is enabled');
}
```

### Advanced Evaluation

```typescript
const result = client.evaluate('premium-features', user);

if (result) {
  console.log(`Flag: ${result.flagKey}`);
  console.log(`Enabled: ${result.enabled}`);
  console.log(`Reason: ${result.reason}`);
}
```

### Browser Usage

```html
<script type="module">
  import { AlignTechClient } from './dist/index.js';

  const client = new AlignTechClient({
    baseUrl: 'https://feature-service.example.com',
    appKey: 'web-app-key',
  });

  await client.initialize();

  const user = {
    userId: window.currentUserId,
    region: 'us-east',
  };

  if (client.isEnabled('dark-mode', user)) {
    document.body.classList.add('dark');
  }
</script>
```

### Cleanup

```typescript
client.close();
```

## Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `baseUrl` | string | `http://localhost:8080` | Feature service URL |
| `appKey` | string | *required* | Application API key |
| `pollInterval` | number | `30000` | Polling interval in milliseconds |

## User Context

| Field | Type | Description |
|-------|------|-------------|
| `userId` | string | User identifier |
| `tenantId` | string | Tenant/organization ID |
| `region` | string | Geographic region |
| `appVersion` | string | Application version |
| `platform` | string | Platform (web, mobile, etc.) |
| `custom` | object | Custom attributes |

## API

### `new AlignTechClient(config)`

Creates a new client instance.

### `async initialize()`

Initializes the client by fetching initial flag configuration and starting the polling loop.

### `isEnabled(flagKey, user): boolean`

Returns `true` if the flag is enabled for the given user, `false` otherwise.

### `evaluate(flagKey, user): EvaluationResult | null`

Returns detailed evaluation result or `null` if flag doesn't exist.

### `isReady(): boolean`

Returns `true` if the client has successfully loaded flags.

### `setTraceParent(traceParent: string)`

Sets W3C trace parent for distributed tracing.

### `close()`

Stops polling and cleans up resources.

## TypeScript Support

Full TypeScript support with type definitions included.

## License

MIT
