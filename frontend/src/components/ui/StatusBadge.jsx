import React from 'react';

const STATUS_CONFIG = {
  'PENDING': 'bg-amber-100 text-amber-700 border-amber-200',
  'RUNNING': 'bg-blue-100 text-blue-700 border-blue-200',
  'COMPLETED': 'bg-green-100 text-green-700 border-green-200',
  'FAILED': 'bg-red-100 text-red-700 border-red-200',
  'BLOCKED': 'bg-slate-100 text-slate-700 border-slate-200'
};

export default function StatusBadge({ status }) {
  const normalizedStatus = status ? status.toUpperCase() : 'PENDING';
  const colorClass = STATUS_CONFIG[normalizedStatus] || 'bg-slate-100 text-slate-700 border-slate-200';
  
  return (
    <span className={`px-3 py-1 rounded-full text-xs font-medium border flex items-center gap-1.5 w-fit ${colorClass}`}>
      {normalizedStatus === 'RUNNING' && (
        <span className="w-1.5 h-1.5 rounded-full bg-blue-700 animate-pulse" />
      )}
      {status}
    </span>
  );
}
