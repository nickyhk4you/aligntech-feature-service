import { AlignTechClient } from './AlignTechClient';
import { AlignTechUser } from './types';

// Mock fetch
global.fetch = jest.fn();

describe('AlignTechClient', () => {
  let client: AlignTechClient;
  const mockFetch = global.fetch as jest.MockedFunction<typeof fetch>;

  beforeEach(() => {
    client = new AlignTechClient({
      baseUrl: 'http://localhost:8080',
      appKey: 'test-key',
      pollInterval: 0, // Disable polling for tests
    });
    jest.clearAllMocks();
  });

  afterEach(() => {
    client.close();
  });

  describe('initialization', () => {
    it('should throw error if appKey is missing', () => {
      expect(() => {
        new AlignTechClient({ baseUrl: 'http://localhost:8080', appKey: '' });
      }).toThrow('appKey is required');
    });

    it('should fetch initial snapshot on initialize', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          version: '1',
          flags: [
            {
              flagKey: 'test-flag',
              flagType: 'boolean',
              status: 'active',
              rollout: { type: 'boolean', value: true },
            },
          ],
        }),
      } as Response);

      await client.initialize();

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/snapshot',
        expect.objectContaining({
          headers: expect.objectContaining({ 'X-App-Key': 'test-key' }),
        })
      );
      expect(client.isReady()).toBe(true);
    });
  });

  describe('isEnabled', () => {
    beforeEach(async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          version: '1',
          flags: [
            {
              flagKey: 'enabled-flag',
              flagType: 'boolean',
              status: 'active',
              rollout: { type: 'boolean', value: true },
            },
            {
              flagKey: 'disabled-flag',
              flagType: 'boolean',
              status: 'active',
              rollout: { type: 'boolean', value: false },
            },
            {
              flagKey: 'whitelist-flag',
              flagType: 'whitelist',
              status: 'active',
              rollout: { type: 'whitelist', userIds: ['user-123', 'admin-*'] },
            },
          ],
        }),
      } as Response);

      await client.initialize();
    });

    it('should return true for enabled boolean flag', () => {
      const user: AlignTechUser = { userId: 'user-123' };
      expect(client.isEnabled('enabled-flag', user)).toBe(true);
    });

    it('should return false for disabled boolean flag', () => {
      const user: AlignTechUser = { userId: 'user-123' };
      expect(client.isEnabled('disabled-flag', user)).toBe(false);
    });

    it('should return false for non-existent flag', () => {
      const user: AlignTechUser = { userId: 'user-123' };
      expect(client.isEnabled('non-existent', user)).toBe(false);
    });

    it('should handle whitelist with exact match', () => {
      const user: AlignTechUser = { userId: 'user-123' };
      expect(client.isEnabled('whitelist-flag', user)).toBe(true);
    });

    it('should handle whitelist with wildcard match', () => {
      const user: AlignTechUser = { userId: 'admin-john' };
      expect(client.isEnabled('whitelist-flag', user)).toBe(true);
    });

    it('should return false for whitelist without match', () => {
      const user: AlignTechUser = { userId: 'user-456' };
      expect(client.isEnabled('whitelist-flag', user)).toBe(false);
    });
  });

  describe('evaluate', () => {
    beforeEach(async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          version: '1',
          flags: [
            {
              flagKey: 'test-flag',
              flagType: 'boolean',
              status: 'active',
              rollout: { type: 'boolean', value: true },
            },
          ],
        }),
      } as Response);

      await client.initialize();
    });

    it('should return evaluation result for existing flag', () => {
      const user: AlignTechUser = { userId: 'user-123' };
      const result = client.evaluate('test-flag', user);

      expect(result).not.toBeNull();
      expect(result?.flagKey).toBe('test-flag');
      expect(result?.enabled).toBe(true);
      expect(result?.reason).toBe('rollout:boolean');
    });

    it('should return null for non-existent flag', () => {
      const user: AlignTechUser = { userId: 'user-123' };
      const result = client.evaluate('non-existent', user);

      expect(result).toBeNull();
    });
  });

  describe('tracing', () => {
    it('should set trace parent header', async () => {
      const traceParent = '00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01';
      client.setTraceParent(traceParent);

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ version: '1', flags: [] }),
      } as Response);

      await client.initialize();

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({ traceparent: traceParent }),
        })
      );
    });
  });

  describe('isReady', () => {
    it('should return false before initialization', () => {
      expect(client.isReady()).toBe(false);
    });

    it('should return true after successful initialization', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ version: '1', flags: [] }),
      } as Response);

      await client.initialize();
      expect(client.isReady()).toBe(true);
    });
  });
});
