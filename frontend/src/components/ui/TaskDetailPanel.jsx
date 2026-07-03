import React from 'react';
import { X } from 'lucide-react';
import StatusBadge from './StatusBadge';

export default function TaskDetailPanel({ task, onClose }) {
  if (!task) return null;

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString();
  };

  return (
    <aside className="fixed right-0 top-0 h-full w-96 bg-brand-navy border-l border-white/10 shadow-2xl p-6 z-50 overflow-y-auto animate-[slideInRight_0.3s_ease-out_forwards]">
      <style>{`
        @keyframes slideInRight {
          from { transform: translateX(100%); }
          to { transform: translateX(0); }
        }
      `}</style>
      
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-xl font-bold text-white">Task Details</h2>
        <button 
          onClick={onClose}
          className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex flex-col gap-6">
        <div>
          <span className="text-sm text-slate-400 block mb-1">Name</span>
          <span className="text-white font-medium">{task.name}</span>
        </div>

        <div>
          <span className="text-sm text-slate-400 block mb-2">Status</span>
          <StatusBadge status={task.status} />
        </div>

        <div>
          <span className="text-sm text-slate-400 block mb-1">Retry Count</span>
          <span className="text-white">{task.retryCount || 0} / {task.maxRetries || 3}</span>
        </div>

        <div>
          <span className="text-sm text-slate-400 block mb-1">Started At</span>
          <span className="text-white font-mono text-sm">{formatDate(task.startedAt)}</span>
        </div>

        <div>
          <span className="text-sm text-slate-400 block mb-1">Completed At</span>
          <span className="text-white font-mono text-sm">{formatDate(task.completedAt)}</span>
        </div>
        
        <div>
          <span className="text-sm text-slate-400 block">Input Data</span>
          <pre className="bg-black/50 p-3 rounded text-xs text-green-400 overflow-x-auto mt-2">
            {task.inputData ? JSON.stringify(task.inputData, null, 2) : '{}'}
          </pre>
        </div>

        <div>
          <span className="text-sm text-slate-400 block">Output Data</span>
          <pre className="bg-black/50 p-3 rounded text-xs text-green-400 overflow-x-auto mt-2">
            {task.outputData ? JSON.stringify(task.outputData, null, 2) : '{}'}
          </pre>
        </div>

        {task.errorLog && (
          <div className="mt-2">
            <span className="text-sm text-red-400 block font-bold mb-2">Error Log</span>
            <pre className="bg-red-500/20 border border-red-500/30 p-3 rounded text-xs text-white overflow-x-auto whitespace-pre-wrap">
              {task.errorLog}
            </pre>
          </div>
        )}
      </div>
    </aside>
  );
}
