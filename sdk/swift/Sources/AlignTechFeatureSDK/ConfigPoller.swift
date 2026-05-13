import Foundation

struct SnapshotResponse: Codable {
    let version: String
    let flags: [FlagConfig]
}

actor ConfigPoller {
    private let baseUrl: String
    private let appKey: String
    private let pollInterval: TimeInterval
    private let store: ConfigStore
    private var timer: Timer?
    private var traceParent: String?
    
    init(config: AlignTechConfig, store: ConfigStore) {
        self.baseUrl = config.baseUrl
        self.appKey = config.appKey
        self.pollInterval = config.pollInterval
        self.store = store
    }
    
    func setTraceParent(_ traceParent: String) {
        self.traceParent = traceParent
    }
    
    func fetchInitial() async {
        await fetchSnapshot()
    }
    
    func start() {
        Task {
            while !Task.isCancelled {
                await fetchSnapshot()
                try? await Task.sleep(nanoseconds: UInt64(pollInterval * 1_000_000_000))
            }
        }
    }
    
    func stop() {
        timer?.invalidate()
        timer = nil
    }
    
    private func fetchSnapshot() async {
        guard let url = URL(string: "\(baseUrl)/api/v1/snapshot") else {
            print("AlignTech SDK: Invalid URL")
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(appKey, forHTTPHeaderField: "X-App-Key")
        request.timeoutInterval = 10.0
        
        if let traceParent = traceParent {
            request.setValue(traceParent, forHTTPHeaderField: "traceparent")
        }
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                print("AlignTech SDK: HTTP error")
                return
            }
            
            let decoder = JSONDecoder()
            let snapshot = try decoder.decode(SnapshotResponse.self, from: data)
            
            var newFlags: [String: FlagConfig] = [:]
            for flag in snapshot.flags {
                newFlags[flag.flagKey] = flag
            }
            
            await store.replaceAll(version: snapshot.version, flags: newFlags)
            
        } catch {
            print("AlignTech SDK: failed fetching snapshot: \(error.localizedDescription)")
        }
    }
}
