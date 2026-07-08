import React from 'react';
import { Activity } from 'lucide-react';
import StatusBadge from './StatusBadge';

export default function ActivityFeed({ events = [] }) {
  return (
    <div className="card p-6">
      <h3 className="text-lg font-bold text-slate-900 mb-4">Live Activity Feed</h3>
      
      {events.length === 0 ? (
        <p className="text-slate-500 text-sm">No recent activity.</p>
      ) : (
        <div className="flex flex-col">
          {events.map((event, index) => (
            <div key={index} className="flex items-center justify-between py-3 border-b border-slate-100 last:border-0">
              <div className="flex items-center gap-3">
                <Activity className="w-4 h-4 text-brand-blue" />
                <div className="flex flex-col">
                  <span className="text-sm text-slate-800 font-medium">{event.eventType}</span>
                  <span className="text-xs text-slate-500 font-mono">
                    {event.workflowId?.substring(0, 8)}...
                  </span>
                </div>
              </div>
              <StatusBadge status={event.status} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
