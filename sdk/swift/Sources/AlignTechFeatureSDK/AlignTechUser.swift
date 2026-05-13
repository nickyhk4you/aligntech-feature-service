import Foundation

public struct AlignTechUser {
    public let userId: String?
    public let tenantId: String?
    public let region: String?
    public let appVersion: String?
    public let platform: String?
    public let custom: [String: Any]
    
    public init(
        userId: String? = nil,
        tenantId: String? = nil,
        region: String? = nil,
        appVersion: String? = nil,
        platform: String? = nil,
        custom: [String: Any] = [:]
    ) {
        self.userId = userId
        self.tenantId = tenantId
        self.region = region
        self.appVersion = appVersion
        self.platform = platform
        self.custom = custom
    }
    
    public func getAttribute(_ key: String) -> Any? {
        switch key {
        case "user_id": return userId
        case "tenant_id": return tenantId
        case "region": return region
        case "app_version": return appVersion
        case "platform": return platform
        default: return custom[key]
        }
    }
}
