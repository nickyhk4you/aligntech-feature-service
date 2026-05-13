using System.Text.Json.Serialization;

namespace AlignTech.FeatureSDK.Models;

public class SnapshotResponse
{
    [JsonPropertyName("version")]
    public required string Version { get; set; }

    [JsonPropertyName("flags")]
    public required List<FlagConfig> Flags { get; set; }
}
