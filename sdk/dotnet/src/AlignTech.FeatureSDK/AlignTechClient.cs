using AlignTech.FeatureSDK.Models;

namespace AlignTech.FeatureSDK;

public class AlignTechClient : IDisposable
{
    private readonly ConfigStore _store;
    private readonly Evaluator _evaluator;
    private readonly ConfigPoller _poller;

    public AlignTechClient(AlignTechConfig config)
    {
        if (string.IsNullOrEmpty(config.AppKey))
        {
            throw new ArgumentException("AppKey is required", nameof(config));
        }

        _store = new ConfigStore();
        _evaluator = new Evaluator();
        _poller = new ConfigPoller(config, _store);
    }

    public async Task InitializeAsync()
    {
        await _poller.FetchInitial();
        _poller.Start();
    }

    public bool IsEnabled(string flagKey, AlignTechUser user)
    {
        var flag = _store.Get(flagKey);
        if (flag == null) return false;

        return _evaluator.Evaluate(flag, user).Enabled;
    }

    public EvaluationResult? Evaluate(string flagKey, AlignTechUser user)
    {
        var flag = _store.Get(flagKey);
        if (flag == null) return null;

        return _evaluator.Evaluate(flag, user);
    }

    public bool IsReady()
    {
        return !_store.IsEmpty();
    }

    public void SetTraceParent(string traceParent)
    {
        _poller.SetTraceParent(traceParent);
    }

    public void Close()
    {
        _poller.Stop();
    }

    public void Dispose()
    {
        _poller.Dispose();
        GC.SuppressFinalize(this);
    }
}
