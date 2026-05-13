import Foundation

public actor AlignTechClient {
    private let store: ConfigStore
    private let evaluator: Evaluator
    private let poller: ConfigPoller
    
    public init(config: AlignTechConfig) throws {
        guard !config.appKey.isEmpty else {
            throw AlignTechError.invalidConfig("appKey is required")
        }
        
        self.store = ConfigStore()
        self.evaluator = Evaluator()
        self.poller = ConfigPoller(config: config, store: store)
    }
    
    public func initialize() async {
        await poller.fetchInitial()
        await poller.start()
    }
    
    public func isEnabled(flagKey: String, user: AlignTechUser) async -> Bool {
        guard let flag = await store.get(flagKey) else {
            return false
        }
        return evaluator.evaluate(flag: flag, user: user).enabled
    }
    
    public func evaluate(flagKey: String, user: AlignTechUser) async -> EvaluationResult? {
        guard let flag = await store.get(flagKey) else {
            return nil
        }
        return evaluator.evaluate(flag: flag, user: user)
    }
    
    public func isReady() async -> Bool {
        return await !store.isEmpty()
    }
    
    public func setTraceParent(_ traceParent: String) async {
        await poller.setTraceParent(traceParent)
    }
    
    public func close() async {
        await poller.stop()
    }
}

public enum AlignTechError: Error {
    case invalidConfig(String)
    case networkError(String)
}
