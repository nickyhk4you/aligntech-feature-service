import { AlignTechConfig, FlagConfig, SnapshotResponse } from './types';
import { ConfigStore } from './ConfigStore';

export class ConfigPoller {
  private baseUrl: string;
  private appKey: string;
  private pollInterval: number;
  private store: ConfigStore;
  private timerId: NodeJS.Timeout | null = null;
  private traceParent: string | null = null;

  constructor(config: AlignTechConfig, store: ConfigStore) {
    this.baseUrl = config.baseUrl || 'http://localhost:8080';
    this.appKey = config.appKey;
    this.pollInterval = config.pollInterval || 30000;
    this.store = store;
  }

  setTraceParent(traceParent: string): void {
    this.traceParent = traceParent;
  }

  async fetchInitial(): Promise<void> {
    await this.fetchSnapshot();
  }

  start(): void {
    if (this.timerId) return;
    
    this.timerId = setInterval(() => {
      this.fetchSnapshot().catch(err => {
        console.error('AlignTech SDK: failed fetching snapshot:', err.message);
      });
    }, this.pollInterval);
  }

  stop(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  private async fetchSnapshot(): Promise<void> {
    try {
      const headers: Record<string, string> = {
        'X-App-Key': this.appKey,
      };

      if (this.traceParent) {
        headers['traceparent'] = this.traceParent;
      }

      const response = await fetch(`${this.baseUrl}/api/v1/snapshot`, {
        method: 'GET',
        headers,
        signal: AbortSignal.timeout(10000),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data: SnapshotResponse = await response.json();
      const newFlags = new Map<string, FlagConfig>();

      for (const flag of data.flags) {
        newFlags.set(flag.flagKey, flag);
      }

      this.store.replaceAll(data.version, newFlags);
    } catch (error: any) {
      console.error('AlignTech SDK: failed fetching snapshot:', error.message);
    }
  }
}
