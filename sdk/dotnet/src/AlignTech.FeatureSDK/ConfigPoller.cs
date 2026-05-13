using System.Net.Http.Headers;
using System.Text.Json;
using AlignTech.FeatureSDK.Models;

namespace AlignTech.FeatureSDK;

internal class ConfigPoller : IDisposable
{
    private readonly AlignTechConfig _config;
    private readonly ConfigStore _store;
    private readonly HttpClient _httpClient;
    private readonly Timer? _timer;
    private string? _traceParent;

    public ConfigPoller(AlignTechConfig config, ConfigStore store)
    {
        _config = config;
        _store = store;
        _httpClient = new HttpClient
        {
            BaseAddress = new Uri(config.BaseUrl)
        };
        _httpClient.DefaultRequestHeaders.Add("X-App-Key", config.AppKey);

        if (config.PollIntervalMs > 0)
        {
            _timer = new Timer(
                async _ => await FetchSnapshot(),
                null,
                Timeout.Infinite,
                Timeout.Infinite
            );
        }
    }

    public async Task FetchInitial()
    {
        await FetchSnapshot();
    }

    public void Start()
    {
        if (_timer != null && _config.PollIntervalMs > 0)
        {
            _timer.Change(_config.PollIntervalMs, _config.PollIntervalMs);
        }
    }

    public void Stop()
    {
        _timer?.Change(Timeout.Infinite, Timeout.Infinite);
    }

    public void SetTraceParent(string traceParent)
    {
        _traceParent = traceParent;
    }

    private async Task FetchSnapshot()
    {
        try
        {
            var request = new HttpRequestMessage(HttpMethod.Get, "/api/v1/snapshot");

            if (!string.IsNullOrEmpty(_traceParent))
            {
                request.Headers.Add("traceparent", _traceParent);
            }

            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();

            var content = await response.Content.ReadAsStringAsync();
            var snapshot = JsonSerializer.Deserialize<SnapshotResponse>(content);

            if (snapshot?.Flags != null)
            {
                _store.UpdateFlags(snapshot.Flags);
            }
        }
        catch (Exception)
        {
        }
    }

    public void Dispose()
    {
        _timer?.Dispose();
        _httpClient.Dispose();
        GC.SuppressFinalize(this);
    }
}
