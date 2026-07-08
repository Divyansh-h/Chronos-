import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getWorkflow, cancelWorkflow, retryWorkflow } from '../services/api';
import useTaskflowEvents from '../hooks/useTaskflowEvents';
import DAGVisualizer from '../components/ui/DAGVisualizer';
import StatusBadge from '../components/ui/StatusBadge';
import TaskDetailPanel from '../components/ui/TaskDetailPanel';
import Skeleton from '../components/ui/Skeleton';
import { Play, XCircle } from 'lucide-react';

export default function WorkflowDetail() {
  const { id } = useParams();
  const [workflow, setWorkflow] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedTask, setSelectedTask] = useState(null);

  useEffect(() => {
    async function loadWorkflow() {
      try {
        setLoading(true);
        const data = await getWorkflow(id);
        setWorkflow(data);
      } catch (err) {
        console.error('Failed to fetch workflow:', err);
        setError('Failed to load workflow details.');
      } finally {
        setLoading(false);
      }
    }
    loadWorkflow();
  }, [id]);

  const { events } = useTaskflowEvents();

  useEffect(() => {
    if (!events || events.length === 0) return;

    const latestEvent = events[0];
    if (latestEvent.workflowId === id) {
      setWorkflow(prev => {
        if (!prev) return prev;
        
        // If it's a task update, update the specific task in the list
        if (latestEvent.taskId && prev.tasks) {
          const updatedTasks = prev.tasks.map(task => 
            task.id === latestEvent.taskId 
              ? { ...task, status: latestEvent.status } 
              : task
          );
          return { ...prev, tasks: updatedTasks };
        }
        
        // If it's a workflow update (no taskId), update the main workflow status
        return { ...prev, status: latestEvent.status };
      });
    }
  }, [events, id]);

  const handleCancel = async () => {
    try {
      await cancelWorkflow(id);
      // Optimistic update
      setWorkflow(prev => ({ ...prev, status: 'FAILED' }));
    } catch (err) {
      alert(err.message);
    }
  };

  const handleRetry = async () => {
    try {
      await retryWorkflow(id);
      // Optimistic update
      setWorkflow(prev => ({ ...prev, status: 'RUNNING' }));
    } catch (err) {
      alert(err.message);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col h-full gap-4 mt-8">
        <Skeleton className="h-10 w-1/3 mb-4" />
        <div className="flex-1 rounded-xl border border-slate-200 bg-slate-100/50 p-4">
          <Skeleton className="h-full w-full opacity-50" />
        </div>
      </div>
    );
  }

  if (error || !workflow) {
    return (
      <div className="flex h-full items-center justify-center mt-20">
        <p className="text-red-400 text-lg">{error || 'Workflow not found.'}</p>
      </div>
    );
  }

  return (
    <div className="animate-[fadeIn_0.3s_ease-in_forwards] flex flex-col h-full">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div className="flex items-center gap-4">
          <h1 className="text-3xl font-bold text-slate-900">{workflow.name || 'Workflow Detail'}</h1>
          <StatusBadge status={workflow.status} />
        </div>
        
        <div className="flex items-center gap-3">
          <button 
            onClick={handleCancel}
            disabled={workflow.status === 'COMPLETED' || workflow.status === 'FAILED'}
            className="flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium border border-red-200 text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-white shadow-sm"
          >
            <XCircle className="w-4 h-4" />
            Cancel Workflow
          </button>
          <button 
            onClick={handleRetry}
            disabled={workflow.status !== 'FAILED'}
            className="flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium border border-slate-200 text-slate-700 hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-white shadow-sm"
          >
            <Play className="w-4 h-4" />
            Retry Failed
          </button>
        </div>
      </div>
      
      <div className="flex-1 relative">
        <DAGVisualizer 
          tasks={
            workflow.tasks?.map(t => {
              const dagNode = workflow.dagDefinition?.find(d => d.name === t.name);
              return { ...t, dependsOn: dagNode?.dependsOn || [] };
            }) || []
          } 
          onNodeClick={(task) => setSelectedTask(task)}
        />
        
        <TaskDetailPanel 
          task={selectedTask} 
          onClose={() => setSelectedTask(null)} 
        />
      </div>
    </div>
  );
}
