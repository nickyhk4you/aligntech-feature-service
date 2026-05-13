namespace AlignTech.FeatureSDK.Models;

public class AlignTechConfig
{
    public string BaseUrl { get; set; } = "http://localhost:8080";
    public required string AppKey { get; set; }
    public int PollIntervalMs { get; set; } = 30000;
}
