namespace AlignTech.FeatureSDK.Models;

public class EvaluationResult
{
    public required string FlagKey { get; set; }
    public bool Enabled { get; set; }
    public string? Variant { get; set; }
    public object? Payload { get; set; }
    public required string Reason { get; set; }
}
