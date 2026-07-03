import React from 'react';

const STATUS_CONFIG = {
  PENDING: 'bg-gray-500/20 text-gray-300 border-gray-500/30',
  QUEUED: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  RUNNING: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
  COMPLETED: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
  FAILED: 'bg-red-500/20 text-red-400 border-red-500/30'
};

export default function StatusBadge({ status }) {
  const normalizedStatus = status ? status.toUpperCase() : 'PENDING';
  const colorClass = STATUS_CONFIG[normalizedStatus] || STATUS_CONFIG.PENDING;
  
  return (
    <span className={`px-3 py-1 rounded-full text-xs font-medium border flex items-center gap-1.5 w-fit ${colorClass}`}>
      {normalizedStatus === 'RUNNING' && (
        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
      )}
      {status}
    </span>
  );
}
