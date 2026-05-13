import Foundation

actor ConfigStore {
    private var flags: [String: FlagConfig] = [:]
    private var version: String = ""
    
    func replaceAll(version: String, flags: [String: FlagConfig]) {
        self.version = version
        self.flags = flags
    }
    
    func get(_ flagKey: String) -> FlagConfig? {
        return flags[flagKey]
    }
    
    func getVersion() -> String {
        return version
    }
    
    func getSize() -> Int {
        return flags.count
    }
    
    func isEmpty() -> Bool {
        return flags.isEmpty
    }
}
