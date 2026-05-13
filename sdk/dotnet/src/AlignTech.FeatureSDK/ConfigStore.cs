using System.Collections.Concurrent;
using AlignTech.FeatureSDK.Models;

namespace AlignTech.FeatureSDK;

internal class ConfigStore
{
    private readonly ConcurrentDictionary<string, FlagConfig> _flags = new();

    public void UpdateFlags(IEnumerable<FlagConfig> flags)
    {
        _flags.Clear();
        foreach (var flag in flags)
        {
            _flags[flag.FlagKey] = flag;
        }
    }

    public FlagConfig? Get(string flagKey)
    {
        _flags.TryGetValue(flagKey, out var flag);
        return flag;
    }

    public bool IsEmpty() => _flags.IsEmpty;
}
