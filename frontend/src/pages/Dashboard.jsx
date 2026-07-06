import React, { useState, useEffect } from 'react';
import { getWorkflows } from '../services/api';
import StatCard from '../components/ui/StatCard';
import useTaskflowEvents from '../hooks/useTaskflowEvents';
import { Layers, Activity, AlertCircle, Wifi, WifiOff } from 'lucide-react';
import ActivityFeed from '../components/ui/ActivityFeed';

export default function Dashboard() {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchData() {
      try {
        const data = await getWorkflows(0, 100);
        setWorkflows(data.content || []);
        setError(null);
      } catch (err) {
        console.error('Failed to load workflows:', err);
        setError('Failed to fetch dashboard data. Please check your network connection.');
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  const totalWorkflows = workflows.length;
  const activeWorkflows = workflows.filter(w => w.status === 'RUNNING').length;
  const failedWorkflows = workflows.filter(w => w.status === 'FAILED').length;

  const { events, isConnected } = useTaskflowEvents();

  return (
    <div className="animate-[fadeIn_0.3s_ease-in_forwards]">
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
      <h1 className="text-3xl font-bold text-white mb-6">Dashboard</h1>
      
      {error && (
        <div className="mb-6 bg-red-500/20 border border-red-500/50 rounded-lg p-4 flex items-center gap-3 text-red-400">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <p>{error}</p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <StatCard 
          title="Total Workflows (Recent)" 
          value={loading ? "..." : totalWorkflows} 
          icon={<Layers className="w-6 h-6" />} 
        />
        <StatCard 
          title="Active Workflows" 
          value={loading ? "..." : activeWorkflows} 
          icon={<Activity className="w-6 h-6" />} 
        />
        <StatCard 
          title="Failed Workflows" 
          value={loading ? "..." : failedWorkflows} 
          icon={<AlertCircle className="w-6 h-6" />} 
        />
        <StatCard 
          title="Live Server Status" 
          value={isConnected ? "Connected" : "Disconnected"} 
          icon={isConnected ? <Wifi className="w-6 h-6" /> : <WifiOff className="w-6 h-6" />} 
        />
      </div>
      
      <div className="mt-8">
        <ActivityFeed events={events} />
      </div>
    </div>
  );
}
