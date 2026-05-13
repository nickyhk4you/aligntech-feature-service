import { AlignTechConfig, AlignTechUser, EvaluationResult } from './types';
import { ConfigStore } from './ConfigStore';
import { Evaluator } from './Evaluator';
import { ConfigPoller } from './ConfigPoller';

export class AlignTechClient {
  private store: ConfigStore;
  private evaluator: Evaluator;
  private poller: ConfigPoller;

  constructor(config: AlignTechConfig) {
    if (!config.appKey) {
      throw new Error('appKey is required');
    }

    this.store = new ConfigStore();
    this.evaluator = new Evaluator();
    this.poller = new ConfigPoller(config, this.store);
  }

  async initialize(): Promise<void> {
    await this.poller.fetchInitial();
    this.poller.start();
  }

  isEnabled(flagKey: string, user: AlignTechUser): boolean {
    const flag = this.store.get(flagKey);
    if (!flag) return false;
    return this.evaluator.evaluate(flag, user).enabled;
  }

  evaluate(flagKey: string, user: AlignTechUser): EvaluationResult | null {
    const flag = this.store.get(flagKey);
    if (!flag) return null;
    return this.evaluator.evaluate(flag, user);
  }

  isReady(): boolean {
    return !this.store.isEmpty();
  }

  setTraceParent(traceParent: string): void {
    this.poller.setTraceParent(traceParent);
  }

  close(): void {
    this.poller.stop();
  }
}
