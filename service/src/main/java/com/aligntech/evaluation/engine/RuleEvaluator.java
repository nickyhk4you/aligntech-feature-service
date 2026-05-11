package com.aligntech.evaluation.engine;

import com.aligntech.domain.FeatureFlag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RuleEvaluator {

    @Cacheable(value = "flagEvaluations", key = "#flag.flagKey + ':' + #context.userId")
    public EvaluationResult evaluate(FeatureFlag flag, EvaluationContext context) {
        EvaluationResult.EvaluationResultBuilder base = EvaluationResult.builder()
                .flagKey(flag.getFlagKey())
                .userId(context.getUserId())
                .region(context.getRegion())
                .releaseVersion(flag.getReleaseVersion());

        if (!flag.isActive()) {
            return base.enabled(false).reason("flag_inactive").build();
        }

        Map<String, Object> rollout = flag.getRollout();
        if (rollout == null || rollout.isEmpty()) {
            return base.enabled(false).reason("no_rollout_config").build();
        }

        String type = (String) rollout.getOrDefault("type", "boolean");

        boolean enabled = switch (type) {
            case "whitelist" -> {
                @SuppressWarnings("unchecked")
                List<String> userIds = toStrList(rollout.get("userIds"));
                yield userIds == null || userIds.isEmpty()
                        || (context.getUserId() != null && matchesAny(context.getUserId(), userIds));
            }
            case "boolean" -> Boolean.TRUE.equals(rollout.get("value"));
            default -> false;
        };

        return base.enabled(enabled).reason("rollout:" + type).build();
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
}
