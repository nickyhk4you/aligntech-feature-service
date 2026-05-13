import { FlagConfig, AlignTechUser, EvaluationResult } from './types';

export class Evaluator {
  evaluate(flag: FlagConfig, user: AlignTechUser): EvaluationResult {
    const base = {
      flagKey: flag.flagKey,
      enabled: false,
      reason: '',
    };

    if (flag.status !== 'active') {
      return { ...base, reason: 'flag_inactive' };
    }

    const rollout = flag.rollout;
    if (!rollout || Object.keys(rollout).length === 0) {
      return { ...base, reason: 'no_rollout_config' };
    }

    const type = rollout.type || 'boolean';
    let enabled = false;

    switch (type) {
      case 'whitelist': {
        const userIds = rollout.userIds as string[] | undefined;
        if (userIds && userIds.length > 0 && user.userId) {
          enabled = this.matchesAny(user.userId, userIds);
        }
        break;
      }
      case 'boolean': {
        enabled = rollout.value === true;
        break;
      }
      default:
        enabled = false;
    }

    return { ...base, enabled, reason: `rollout:${type}` };
  }

  private matchesAny(value: string, patterns: string[]): boolean {
    for (const pattern of patterns) {
      if (pattern === '*') return true;
      if (pattern.startsWith('*') && pattern.endsWith('*')) {
        const substr = pattern.substring(1, pattern.length - 1);
        if (value.includes(substr)) return true;
      } else if (pattern.startsWith('*')) {
        if (value.endsWith(pattern.substring(1))) return true;
      } else if (pattern.endsWith('*')) {
        if (value.startsWith(pattern.substring(0, pattern.length - 1))) return true;
      } else if (pattern === value) {
        return true;
      }
    }
    return false;
  }
}
