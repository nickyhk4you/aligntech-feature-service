import Foundation

public struct EvaluationResult {
    public let flagKey: String
    public let enabled: Bool
    public let variant: String?
    public let payload: Any?
    public let reason: String
    
    init(
        flagKey: String,
        enabled: Bool,
        variant: String? = nil,
        payload: Any? = nil,
        reason: String
    ) {
        self.flagKey = flagKey
        self.enabled = enabled
        self.variant = variant
        self.payload = payload
        self.reason = reason
    }
}
