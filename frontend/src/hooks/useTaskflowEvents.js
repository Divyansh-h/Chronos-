import { useState, useEffect } from 'react';
import SockJS from 'sockjs-client/dist/sockjs';
import { Client } from '@stomp/stompjs';

export default function useTaskflowEvents() {
  const [events, setEvents] = useState([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_BASE_URL || ''}/ws-endpoint`),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.debug = function (str) {
      console.log(str);
    };

    stompClient.onConnect = function (frame) {
      setIsConnected(true);
      stompClient.subscribe('/topic/workflow-events', (message) => {
        if (message.body) {
          const eventData = JSON.parse(message.body);
          setEvents(prevEvents => [eventData, ...prevEvents].slice(0, 50));
        }
      });
    };

    // E2E Testing Backdoor: Allows Cypress to inject simulated STOMP payloads
    const handleCypressEvent = (e) => {
      if (e.detail) {
        setEvents(prevEvents => [e.detail, ...prevEvents].slice(0, 50));
      }
    };
    if (window.Cypress) {
      window.addEventListener('Cypress-STOMP', handleCypressEvent);
    }

    stompClient.onWebSocketError = function (evt) {
      console.error('WebSocketError', evt);
      setIsConnected(false);
    };

    stompClient.onWebSocketClose = function () {
      setIsConnected(false);
    };

    stompClient.activate();

    return () => {
      if (window.Cypress) {
        window.removeEventListener('Cypress-STOMP', handleCypressEvent);
      }
      stompClient.deactivate();
    };
  }, []);

  return { events, isConnected };
}
