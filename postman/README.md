# AlignTech Feature Service - Postman Collection

Complete API testing collection for AlignTech Feature Management Service.

## 📦 Files

- **AlignTech_Feature_Service.postman_collection.json** - Main API collection
- **AlignTech_Environments.postman_environment.json** - Development environment
- **AlignTech_Production.postman_environment.json** - Production environment

## 🚀 Quick Start

### 1. Import into Postman

**Option A: Import from File**
1. Open Postman
2. Click "Import" button
3. Select all JSON files from this directory
4. Click "Import"

**Option B: Import via URL** (if hosted on GitHub)
```
https://raw.githubusercontent.com/your-org/aligntech-feature-service/main/postman/AlignTech_Feature_Service.postman_collection.json
```

### 2. Select Environment

1. Click the environment dropdown (top right)
2. Select "AlignTech - Development"
3. Verify `baseUrl` is set to `http://localhost:8080`

### 3. Start the Service

```bash
cd service
mvn spring-boot:run
```

Wait for the service to start (check http://localhost:8080/api/v1/health)

### 4. Run the Collection

**Run All Tests:**
1. Click on "AlignTech Feature Service" collection
2. Click "Run" button
3. Click "Run AlignTech Feature Service"
4. View test results

**Run Individual Requests:**
1. Navigate to a specific request
2. Click "Send"
3. View response

## 📋 Collection Structure

### Health & Monitoring
- ✅ Health Check
- ✅ Actuator Health (detailed)
- ✅ Prometheus Metrics
- ✅ Cache Metrics (hit rate, misses, evictions)
- ✅ Cache Summary (overall performance)

### Admin - Flag Management
- ✅ Create Flag (Boolean)
- ✅ Create Flag (Whitelist)
- ✅ Create Flag (Premium Feature)
- ✅ List All Flags
- ✅ List Active Flags
- ✅ Get Flag by ID
- ✅ Activate Flag
- ✅ Archive Flag

### Admin - Audit
- ✅ Get Flag Audit History

### Evaluation - Data Plane
- ✅ Evaluate Single Flag
- ✅ Evaluate Multiple Flags
- ✅ Evaluate All Active Flags
- ✅ Get Snapshot (SDK Bootstrap)
- ✅ SSE Stream (Real-time Updates)

### Test Scenarios
- ✅ VIP User Gets AI Features
- ✅ Regular User Blocked from Beta
- ✅ Premium Tier Analytics

## 🔑 Authentication

Admin endpoints require the `X-API-Key` header.

**Development:** `dev-secret-key`  
**Production:** Set in environment variable

The collection automatically includes this header for admin requests.

## 🧪 Automated Tests

Most requests include automated tests that verify:
- HTTP status codes
- Response structure
- Data correctness

Tests run automatically when you send a request. Look for the "Test Results" tab.

Example test output:
```
✓ Status is 201 Created
✓ Response has id
✓ Flag key matches
```

## 📊 Variables

### Collection Variables
- `baseUrl` - Service URL (default: http://localhost:8080)
- `apiKey` - Admin API key (default: dev-secret-key)
- `flagId` - Auto-populated after creating a flag

### Environment-Specific
Set different values per environment (dev, staging, prod).

To change:
1. Click the environment dropdown
2. Click the eye icon
3. Edit values

## 🎯 Common Workflows

### Create and Test a New Flag

1. **Create Flag**
   - Run: `Admin - Flag Management > Create Flag - Whitelist`
   - Note: `flagId` is auto-saved to collection variable

2. **Activate Flag**
   - Run: `Admin - Flag Management > Activate Flag`
   - Uses saved `flagId`

3. **Test Evaluation**
   - Run: `Evaluation - Data Plane > Evaluate Single Flag`
   - Modify `userId` in request body to test different users

4. **Check Audit Trail**
   - Run: `Admin - Audit > Get Flag Audit History`
   - See all changes made to the flag

### Test User Targeting

1. **VIP User (should be enabled)**
   ```json
   {
     "context": {
       "userId": "vip-customer-123"
     },
     "flagKeys": ["ai-recommendations"]
   }
   ```

2. **Regular User (should be disabled)**
   ```json
   {
     "context": {
       "userId": "regular-user-456"
     },
     "flagKeys": ["ai-recommendations"]
   }
   ```

Run these in `Test Scenarios` folder.

## 🔄 Real-time Updates (SSE)

The SSE endpoint doesn't work well in Postman. Use curl instead:

```bash
curl -N -H "Accept: text/event-stream" http://localhost:8080/api/v1/stream
```

Then in another terminal, create/update flags and see events in real-time:

```bash
# Events will appear like:
# data: {"event":"flag-changed","flagKey":"ai-search","action":"activated"}
```

## 📝 Example Request Bodies

### Create Boolean Flag
```json
{
  "flagKey": "dark-mode",
  "name": "Dark Mode UI",
  "description": "Toggle dark mode theme",
  "flagType": "boolean",
  "status": "active",
  "createdBy": "ui-team",
  "rollout": {
    "type": "boolean",
    "value": true
  }
}
```

### Create Whitelist Flag
```json
{
  "flagKey": "beta-feature",
  "name": "Beta Feature Access",
  "flagType": "boolean",
  "status": "active",
  "createdBy": "product-team",
  "rollout": {
    "type": "whitelist",
    "userIds": ["user_001", "user_002", "beta-*", "staff-*"]
  }
}
```

### Evaluate Request
```json
{
  "context": {
    "userId": "user_001",
    "region": "us-east-1",
    "platform": "web",
    "appVersion": "2.0.0",
    "customAttributes": {
      "tier": "premium"
    }
  },
  "flagKeys": ["dark-mode", "beta-feature"]
}
```

## 🐛 Troubleshooting

### Connection Refused
- Make sure the service is running: `mvn spring-boot:run`
- Check the health endpoint: http://localhost:8080/api/v1/health

### 401 Unauthorized
- Verify `X-API-Key` header is set
- Check environment variable `apiKey` is correct
- Default dev key: `dev-secret-key`

### 404 Flag Not Found
- Ensure you created a flag first
- Check `flagId` variable is populated
- Run `List All Flags` to see available flags

### Tests Failing
- Check response status code in the response panel
- Verify request body matches expected format
- Look at the "Test Results" tab for specific failures

## 📖 API Documentation

For detailed API documentation, see:
- [Main README](../README.md)
- [Swagger/OpenAPI Spec](../service/src/main/resources/openapi.yaml) _(if available)_

## 🔗 Related Resources

- **Service Repository**: [github.com/aligntech/feature-service](https://github.com/aligntech/feature-service)
- **SDK Documentation**: [../sdk/README.md](../sdk/README.md)
- **Architecture Docs**: Coming soon

## 💡 Tips

- Use **Collection Runner** to run all requests sequentially
- Enable **Auto-follow redirects** in Postman settings
- Use **Pre-request Scripts** for dynamic data generation
- Export test results for CI/CD integration
- Use **Mock Servers** to test without running the real service

## 📞 Support

For issues or questions:
- Open an issue on GitHub
- Contact: dev-team@aligntech.com
- Slack: #feature-flags channel
