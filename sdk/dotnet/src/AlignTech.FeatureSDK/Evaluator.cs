using System.Text.Json;
using AlignTech.FeatureSDK.Models;

namespace AlignTech.FeatureSDK;

internal class Evaluator
{
    public EvaluationResult Evaluate(FlagConfig flag, AlignTechUser user)
    {
        var baseResult = new EvaluationResult
        {
            FlagKey = flag.FlagKey,
            Enabled = false,
            Reason = string.Empty
        };

        if (flag.Status != "active")
        {
            baseResult.Reason = "flag_inactive";
            return baseResult;
        }

        if (flag.Rollout == null || flag.Rollout.Count == 0)
        {
            baseResult.Reason = "no_rollout_config";
            return baseResult;
        }

        var rollout = flag.Rollout;
        var type = rollout.ContainsKey("type") ? rollout["type"]?.ToString() ?? "boolean" : "boolean";
        var enabled = false;

        switch (type)
        {
            case "whitelist":
                if (rollout.ContainsKey("userIds") && user.UserId != null)
                {
                    var userIds = GetStringList(rollout["userIds"]);
                    if (userIds.Count > 0)
                    {
                        enabled = MatchesAny(user.UserId, userIds);
                    }
                }
                break;

            case "boolean":
                if (rollout.ContainsKey("value"))
                {
                    enabled = rollout["value"] is JsonElement element
                        ? element.ValueKind == JsonValueKind.True
                        : Convert.ToBoolean(rollout["value"]);
                }
                break;

            default:
                enabled = false;
                break;
        }

        baseResult.Enabled = enabled;
        baseResult.Reason = $"rollout:{type}";
        return baseResult;
    }

    private static List<string> GetStringList(object? value)
    {
        if (value is JsonElement element && element.ValueKind == JsonValueKind.Array)
        {
            return element.EnumerateArray()
                .Where(e => e.ValueKind == JsonValueKind.String)
                .Select(e => e.GetString() ?? string.Empty)
                .Where(s => !string.IsNullOrEmpty(s))
                .ToList();
        }

        if (value is IEnumerable<object> enumerable)
        {
            return enumerable.Select(o => o.ToString() ?? string.Empty)
                .Where(s => !string.IsNullOrEmpty(s))
                .ToList();
        }

        return new List<string>();
    }

    private static bool MatchesAny(string value, List<string> patterns)
    {
        foreach (var pattern in patterns)
        {
            if (pattern == "*") return true;

            if (pattern.StartsWith("*") && pattern.EndsWith("*"))
            {
                var substr = pattern[1..^1];
                if (value.Contains(substr)) return true;
            }
            else if (pattern.StartsWith("*"))
            {
                if (value.EndsWith(pattern[1..])) return true;
            }
            else if (pattern.EndsWith("*"))
            {
                if (value.StartsWith(pattern[..^1])) return true;
            }
            else if (pattern == value)
            {
                return true;
            }
        }

        return false;
    }
}
