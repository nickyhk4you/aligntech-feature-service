package com.aligntech.sdk;

import java.util.Collection;
import java.util.List;
import java.util.Map;


class Evaluator {

    EvaluationResult evaluate(FlagConfig flag, AlignTechUser user) {
        if (!flag.isActive()) {
            return new EvaluationResult(flag.getFlagKey(), false, null, null, "flag_inactive");
        }

        Map<String, Object> rollout = flag.getRollout();
        if (rollout.isEmpty()) {
            return new EvaluationResult(flag.getFlagKey(), false, null, null, "no_rollout_config");
        }

        String type = (String) rollout.getOrDefault("type", "boolean");

        boolean enabled = switch (type) {
            case "whitelist" -> {
                List<String> userIds = toStrList(rollout.get("userIds"));
                yield userIds != null && !userIds.isEmpty()
                        && user.getUserId() != null
                        && matchesAny(user.getUserId(), userIds);
            }
            case "boolean" -> Boolean.TRUE.equals(rollout.get("value"));
            default -> false;
        };

        return new EvaluationResult(flag.getFlagKey(), enabled, null, null, "rollout:" + type);
    }

    private boolean matchesAny(String value, List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern.equals("*")) return true;
            if (pattern.startsWith("*") && pattern.endsWith("*"))
                return value.contains(pattern.substring(1, pattern.length() - 1));
            else if (pattern.startsWith("*"))
                return value.endsWith(pattern.substring(1));
            else if (pattern.endsWith("*"))
                return value.startsWith(pattern.substring(0, pattern.length() - 1));
            else if (pattern.equals(value)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStrList(Object obj) {
        if (obj instanceof List<?> list) return (List<String>) list;
        if (obj instanceof Collection<?> col) return List.copyOf((Collection<String>) col);
        return null;
    }

    record EvaluationResult(String flagKey, boolean enabled, String variant, Object payload, String reason) {}
}
