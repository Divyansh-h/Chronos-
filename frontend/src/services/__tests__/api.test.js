import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getWorkflows } from '../api';

describe('API Service - getWorkflows', () => {
  let fetchSpy;

  beforeEach(() => {
    // Intercept all global fetch calls
    fetchSpy = vi.spyOn(global, 'fetch');
  });

  afterEach(() => {
    // Clean up mocks after each test to prevent test bleeding
    vi.restoreAllMocks();
  });

  it('should successfully parse and return a JSON payload on HTTP 200', async () => {
    // Arrange
    const mockData = [
      { id: '1', name: 'Data Pipeline', status: 'RUNNING' }
    ];
    
    // Simulate a successful HTTP Response object
    fetchSpy.mockResolvedValueOnce({
      ok: true,
      json: async () => mockData,
    });

    // Act
    const result = await getWorkflows();

    // Assert
    expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/v1/workflows');
    expect(result).toEqual(mockData);
  });

  it('should throw a strict Error if the HTTP response is not ok', async () => {
    // Arrange
    // Simulate a 500 Internal Server Error
    fetchSpy.mockResolvedValueOnce({
      ok: false,
      statusText: 'Internal Server Error',
    });

    // Act & Assert
    await expect(getWorkflows()).rejects.toThrow('Failed to fetch workflows: Internal Server Error');
  });
});
