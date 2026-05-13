namespace AlignTech.FeatureSDK.Models;

public class AlignTechUser
{
    public string? UserId { get; set; }
    public string? TenantId { get; set; }
    public string? Region { get; set; }
    public string? AppVersion { get; set; }
    public string? Platform { get; set; }
    public Dictionary<string, object>? Custom { get; set; }
}
