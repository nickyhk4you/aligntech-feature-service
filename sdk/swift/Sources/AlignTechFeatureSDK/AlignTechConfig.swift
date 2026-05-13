import Foundation

public struct AlignTechConfig {
    public let baseUrl: String
    public let appKey: String
    public let pollInterval: TimeInterval
    
    public init(
        baseUrl: String = "http://localhost:8080",
        appKey: String,
        pollInterval: TimeInterval = 30.0
    ) {
        self.baseUrl = baseUrl
        self.appKey = appKey
        self.pollInterval = pollInterval
    }
}
