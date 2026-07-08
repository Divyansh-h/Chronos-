import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, X } from 'lucide-react';
import { getWorkflows, createWorkflow } from '../services/api';
import Skeleton from '../components/ui/Skeleton';

export default function Workflows() {
  const [workflows, setWorkflows] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [dagJson, setDagJson] = useState('[\n  { "name": "Task 1" },\n  { "name": "Task 2", "dependsOn": ["Task 1"] }\n]');
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [initialLoading, setInitialLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchWorkflows(page);
  }, [page]);

  const fetchWorkflows = async (pageIndex = 0) => {
    try {
      const data = await getWorkflows(pageIndex, 10);
      setWorkflows(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      console.error(err);
    } finally {
      setInitialLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = {
        name,
        dagDefinition: JSON.parse(dagJson)
      };
      const newWorkflow = await createWorkflow(payload);
      setWorkflows([newWorkflow, ...workflows]);
      setIsModalOpen(false);
      setName('');
    } catch (err) {
      console.error(err);
      alert('Failed to parse JSON or create workflow');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="animate-[fadeIn_0.3s_ease-in_forwards]">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-slate-900">Workflows</h1>
        <button 
          onClick={() => setIsModalOpen(true)}
          className="flex items-center gap-2 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg transition-colors font-medium shadow-sm"
        >
          <Plus className="w-5 h-5" />
          Create Workflow
        </button>
      </div>

      <div className="card">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-slate-200 text-slate-500 text-sm bg-slate-50/50">
              <th className="p-4 font-medium">ID</th>
              <th className="p-4 font-medium">Name</th>
              <th className="p-4 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {initialLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}>
                  <td className="p-4"><Skeleton className="h-4 w-48" /></td>
                  <td className="p-4"><Skeleton className="h-4 w-32" /></td>
                  <td className="p-4"><Skeleton className="h-6 w-20 rounded" /></td>
                </tr>
              ))
            ) : workflows.map(wf => (
              <tr 
                key={wf.id} 
                onClick={() => navigate(`/workflows/${wf.id}`)}
                className="hover:bg-slate-50 transition-colors cursor-pointer"
              >
                <td className="p-4 font-mono text-sm text-slate-500">{wf.id}</td>
                <td className="p-4 text-slate-900 font-medium">{wf.name}</td>
                <td className="p-4">
                  <span className="px-2.5 py-1 bg-slate-100 text-slate-600 rounded-md text-xs font-medium border border-slate-200">{wf.status || 'PENDING'}</span>
                </td>
              </tr>
            ))}
            {!initialLoading && workflows.length === 0 && (
              <tr>
                <td colSpan="3" className="p-8 text-center text-slate-500">No workflows found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex justify-between items-center mt-4">
        <button 
          onClick={() => setPage(p => Math.max(0, p - 1))}
          disabled={page === 0}
          className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-md disabled:opacity-50 hover:bg-slate-50 font-medium shadow-sm"
        >
          Previous
        </button>
        <span className="text-slate-500 text-sm font-medium">
          Page {page + 1} of {Math.max(1, totalPages)}
        </span>
        <button 
          onClick={() => setPage(p => p + 1)}
          disabled={page >= totalPages - 1}
          className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-md disabled:opacity-50 hover:bg-slate-50 font-medium shadow-sm"
        >
          Next
        </button>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center animate-[fadeIn_0.2s_ease-out_forwards]">
          <div className="card w-full max-w-lg p-6 flex flex-col shadow-xl">
            <div className="flex justify-between items-center mb-5">
              <h2 className="text-xl font-bold text-slate-900">Create Workflow</h2>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Workflow Name</label>
                <input 
                  type="text" 
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  className="w-full bg-white border border-slate-300 rounded-md p-2.5 text-slate-900 outline-none focus:border-slate-400 focus:ring-1 focus:ring-slate-400 transition-shadow"
                  placeholder="e.g. Daily Data Export"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">DAG Definition (JSON Array)</label>
                <textarea 
                  value={dagJson}
                  onChange={(e) => setDagJson(e.target.value)}
                  required
                  rows={8}
                  className="w-full bg-slate-50 border border-slate-300 rounded-md p-2.5 text-slate-800 font-mono text-sm outline-none focus:border-slate-400 focus:ring-1 focus:ring-slate-400 transition-shadow"
                />
              </div>

              <div className="flex justify-end gap-3 mt-4 pt-4 border-t border-slate-100">
                <button 
                  type="button" 
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors font-medium"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={loading}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-md transition-colors font-medium disabled:opacity-50"
                >
                  {loading ? 'Submitting...' : 'Submit'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
