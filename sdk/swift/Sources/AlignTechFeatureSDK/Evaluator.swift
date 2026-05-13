import Foundation

struct Evaluator {
    func evaluate(flag: FlagConfig, user: AlignTechUser) -> EvaluationResult {
        let base = { (enabled: Bool, reason: String) in
            EvaluationResult(
                flagKey: flag.flagKey,
                enabled: enabled,
                reason: reason
            )
        }
        
        guard flag.isActive else {
            return base(false, "flag_inactive")
        }
        
        guard let rollout = flag.rollout, !rollout.isEmpty else {
            return base(false, "no_rollout_config")
        }
        
        let type = (rollout["type"]?.value as? String) ?? "boolean"
        var enabled = false
        
        switch type {
        case "whitelist":
            if let userIds = rollout["userIds"]?.value as? [String],
               !userIds.isEmpty,
               let userId = user.userId {
                enabled = matchesAny(value: userId, patterns: userIds)
            }
            
        case "boolean":
            enabled = (rollout["value"]?.value as? Bool) ?? false
            
        default:
            enabled = false
        }
        
        return base(enabled, "rollout:\(type)")
    }
    
    private func matchesAny(value: String, patterns: [String]) -> Bool {
        for pattern in patterns {
            if pattern == "*" {
                return true
            }
            
            if pattern.hasPrefix("*") && pattern.hasSuffix("*") {
                let substr = String(pattern.dropFirst().dropLast())
                if value.contains(substr) {
                    return true
                }
            } else if pattern.hasPrefix("*") {
                let suffix = String(pattern.dropFirst())
                if value.hasSuffix(suffix) {
                    return true
                }
            } else if pattern.hasSuffix("*") {
                let prefix = String(pattern.dropLast())
                if value.hasPrefix(prefix) {
                    return true
                }
            } else if pattern == value {
                return true
            }
        }
        return false
    }
}
