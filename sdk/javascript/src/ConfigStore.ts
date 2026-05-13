import { FlagConfig } from './types';

export class ConfigStore {
  private flags: Map<string, FlagConfig> = new Map();
  private _version: string = '';

  replaceAll(version: string, newFlags: Map<string, FlagConfig>): void {
    this._version = version;
    this.flags = new Map(newFlags);
  }

  get(flagKey: string): FlagConfig | undefined {
    return this.flags.get(flagKey);
  }

  get version(): string {
    return this._version;
  }

  get size(): number {
    return this.flags.size;
  }

  isEmpty(): boolean {
    return this.flags.size === 0;
  }
}
