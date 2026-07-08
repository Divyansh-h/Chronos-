import React, { useEffect, useState } from 'react';
import { getWorkers } from '../services/api';
import { Server, Activity, CheckCircle, Cpu, Clock } from 'lucide-react';
import StatusBadge from '../components/ui/StatusBadge';

export default function Workers() {
  const [workers, setWorkers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;
    
    const fetchWorkers = async () => {
      try {
        const data = await getWorkers(0, 100);
        if (mounted) {
          setWorkers(data.content || []);
          setLoading(false);
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          console.error('Failed to fetch workers:', err);
          setError('Failed to fetch worker nodes.');
          setLoading(false);
        }
      }
    };

    fetchWorkers();
    const intervalId = setInterval(fetchWorkers, 2000);

    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, []);

  const formatRelativeTime = (timestamp) => {
    if (!timestamp) return 'Never';
    const date = new Date(timestamp);
    const now = new Date();
    const diffSeconds = Math.floor((now - date) / 1000);
    
    if (diffSeconds < 5) return 'Just now';
    if (diffSeconds < 60) return `${diffSeconds}s ago`;
    if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)}m ago`;
    return date.toLocaleTimeString();
  };

  const isOnline = (lastHeartbeat) => {
    if (!lastHeartbeat) return false;
    const date = new Date(lastHeartbeat);
    const now = new Date();
    // Consider online if heartbeat was within the last 15 seconds
    return (now - date) < 15000;
  };

  if (loading && workers.length === 0) {
    return (
      <div className="animate-[fadeIn_0.3s_ease-in_forwards]">
        <h1 className="text-3xl font-bold text-slate-900 mb-6">Worker Nodes</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <div key={i} className="card p-6 rounded-xl animate-pulse">
              <div className="h-6 bg-slate-200 rounded w-1/3 mb-4"></div>
              <div className="h-4 bg-slate-100 rounded w-1/2 mb-2"></div>
              <div className="h-4 bg-slate-100 rounded w-1/2 mb-2"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="animate-[fadeIn_0.3s_ease-in_forwards]">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-slate-900">Worker Nodes</h1>
        <div className="flex items-center text-slate-500 text-sm font-medium">
          <Activity className="w-4 h-4 mr-2 animate-pulse text-brand-blue" />
          Live updating
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg mb-6">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {workers.map((worker) => {
          const online = isOnline(worker.lastHeartbeat);
          return (
            <div key={worker.id} className="card p-6 rounded-xl flex flex-col relative overflow-hidden transition-all duration-300 hover:border-slate-300">
              {/* Online Indicator Glow */}
              {online && (
                <div className="absolute -top-4 -right-4 w-12 h-12 bg-green-500/10 blur-xl rounded-full"></div>
              )}
              
              <div className="flex justify-between items-start mb-6">
                <div className="flex items-center">
                  <div className="w-10 h-10 rounded-lg bg-slate-100 flex items-center justify-center mr-3 border border-slate-200">
                    <Server className="w-5 h-5 text-brand-blue" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{worker.hostname}</h3>
                    <div className="flex items-center text-xs mt-1 text-slate-500 font-medium">
                      <span className={`w-2 h-2 rounded-full mr-2 ${online ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.4)]' : 'bg-slate-300'}`}></span>
                      {online ? 'Online' : 'Offline'}
                    </div>
                  </div>
                </div>
                <StatusBadge status={worker.status} />
              </div>
              
              <div className="grid grid-cols-2 gap-4 mt-auto">
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                  <div className="flex items-center text-slate-500 text-xs mb-1 font-medium">
                    <Cpu className="w-3 h-3 mr-1" /> Load
                  </div>
                  <div className="text-xl font-bold text-slate-900">
                    {worker.currentLoad}
                  </div>
                </div>
                
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                  <div className="flex items-center text-slate-500 text-xs mb-1 font-medium">
                    <CheckCircle className="w-3 h-3 mr-1" /> Completed
                  </div>
                  <div className="text-xl font-bold text-slate-900">
                    {worker.tasksCompleted}
                  </div>
                </div>
                
                <div className="col-span-2 bg-slate-50 p-3 rounded-lg border border-slate-100">
                  <div className="flex items-center text-slate-500 text-xs mb-1 font-medium">
                    <Clock className="w-3 h-3 mr-1" /> Last Heartbeat
                  </div>
                  <div className="text-sm text-slate-900 flex justify-between items-center font-medium">
                    <span>{worker.lastHeartbeat ? new Date(worker.lastHeartbeat).toLocaleTimeString() : 'N/A'}</span>
                    <span className="text-xs text-slate-500">{formatRelativeTime(worker.lastHeartbeat)}</span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
        
        {workers.length === 0 && !loading && (
          <div className="col-span-full card p-8 text-center text-slate-500 rounded-xl">
            <Server className="w-12 h-12 text-slate-300 mx-auto mb-4" />
            <p className="text-lg font-medium text-slate-700">No worker nodes found.</p>
            <p className="text-sm mt-2">Workers will appear here once they register with the cluster.</p>
          </div>
        )}
      </div>
    </div>
  );
}
