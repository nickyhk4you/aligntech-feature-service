using System.Text.Json.Serialization;

namespace AlignTech.FeatureSDK.Models;

public class FlagConfig
{
    [JsonPropertyName("flagKey")]
    public required string FlagKey { get; set; }

    [JsonPropertyName("flagType")]
    public required string FlagType { get; set; }

    [JsonPropertyName("status")]
    public required string Status { get; set; }

    [JsonPropertyName("rollout")]
    public Dictionary<string, object>? Rollout { get; set; }
}
