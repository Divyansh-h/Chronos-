import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import useTaskflowEvents from '../useTaskflowEvents';

// 1. Intercept and mock the SockJS websocket transport layer
vi.mock('sockjs-client', () => {
  return {
    default: vi.fn().mockImplementation(() => ({}))
  };
});

// Capture references to the internal callbacks so we can trigger them manually during the test
let mockSubscribeCallback = null;

// 2. Intercept and mock the STOMP protocol client
vi.mock('@stomp/stompjs', () => {
  return {
    Client: vi.fn().mockImplementation(function() {
      this.activate = vi.fn(() => {
        // Simulate immediate connection success
        if (this.onConnect) {
          setTimeout(() => this.onConnect({}), 0);
        }
      });
      this.deactivate = vi.fn();
      this.subscribe = vi.fn((topic, callback) => {
        mockSubscribeCallback = callback;
      });
      return this;
    })
  };
});

describe('useTaskflowEvents Hook', () => {
  beforeEach(() => {
    mockSubscribeCallback = null;
    vi.clearAllMocks();
  });

  it('successfully negotiates STOMP connection and ingests incoming WebSocket payloads', async () => {
    // Act 1: Mount the hook into a virtual test component
    const { result, unmount } = renderHook(() => useTaskflowEvents());

    // Assert Initial State
    expect(result.current.events).toEqual([]);
    expect(result.current.isConnected).toBe(false);
    
    // Act 2: Wait for the simulated async STOMP connection to resolve
    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    // Assert Connection State
    expect(result.current.isConnected).toBe(true);

    // Act 3: Force the mock broker to push a simulated WebSocket message down to the client
    const mockTelemetryPayload = {
      eventType: 'TASK_UPDATE',
      taskId: 'uuid-1234',
      status: 'RUNNING'
    };

    act(() => {
      // Simulate STOMP Message frame
      if (mockSubscribeCallback) {
        mockSubscribeCallback({ body: JSON.stringify(mockTelemetryPayload) });
      }
    });

    // Assert Array Mutation: The React state should have pushed the new event to the top of the stack
    expect(result.current.events.length).toBe(1);
    expect(result.current.events[0]).toEqual(mockTelemetryPayload);

    // Act 4: Push a second event to prove accumulation works
    act(() => {
      mockSubscribeCallback({ body: JSON.stringify({ ...mockTelemetryPayload, status: 'COMPLETED' }) });
    });

    expect(result.current.events.length).toBe(2);
    expect(result.current.events[0].status).toBe('COMPLETED'); // Verifies it was unshifted to the top

    // Teardown
    unmount();
  });
});
