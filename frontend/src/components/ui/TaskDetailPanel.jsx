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
    <aside className="fixed right-0 top-0 h-full w-96 bg-white border-l border-slate-200 shadow-2xl p-6 z-50 overflow-y-auto animate-[slideInRight_0.3s_ease-out_forwards]">
      <style>{`
        @keyframes slideInRight {
          from { transform: translateX(100%); }
          to { transform: translateX(0); }
        }
      `}</style>
      
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-xl font-bold text-slate-900">Task Details</h2>
        <button 
          onClick={onClose}
          className="p-2 rounded-md text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex flex-col gap-6">
        <div>
          <span className="text-sm text-slate-500 block mb-1">Name</span>
          <span className="text-slate-900 font-medium">{task.name}</span>
        </div>

        <div>
          <span className="text-sm text-slate-500 block mb-2">Status</span>
          <StatusBadge status={task.status} />
        </div>

        <div>
          <span className="text-sm text-slate-500 block mb-1">Retry Count</span>
          <span className="text-slate-900">{task.retryCount || 0} / {task.maxRetries || 3}</span>
        </div>

        <div>
          <span className="text-sm text-slate-500 block mb-1">Started At</span>
          <span className="text-slate-900 font-mono text-sm">{formatDate(task.startedAt)}</span>
        </div>

        <div>
          <span className="text-sm text-slate-500 block mb-1">Completed At</span>
          <span className="text-slate-900 font-mono text-sm">{formatDate(task.completedAt)}</span>
        </div>
        
        <div>
          <span className="text-sm text-slate-500 block">Input Data</span>
          <pre className="bg-slate-50 border border-slate-200 p-3 rounded-md text-xs text-slate-700 overflow-x-auto mt-2 font-mono">
            {task.inputData ? JSON.stringify(task.inputData, null, 2) : '{}'}
          </pre>
        </div>

        <div>
          <span className="text-sm text-slate-500 block">Output Data</span>
          <pre className="bg-slate-50 border border-slate-200 p-3 rounded-md text-xs text-slate-700 overflow-x-auto mt-2 font-mono">
            {task.outputData ? JSON.stringify(task.outputData, null, 2) : '{}'}
          </pre>
        </div>

        {task.errorLog && (
          <div className="mt-2">
            <span className="text-sm text-red-600 block font-bold mb-2">Error Log</span>
            <pre className="bg-red-50 border border-red-200 p-3 rounded-md text-xs text-red-700 overflow-x-auto whitespace-pre-wrap font-mono">
              {task.errorLog}
            </pre>
          </div>
        )}
      </div>
    </aside>
  );
}
