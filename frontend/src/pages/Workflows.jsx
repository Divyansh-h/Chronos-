import React, { useState, useEffect } from 'react';
import { Plus, X } from 'lucide-react';
import { getWorkflows, createWorkflow } from '../services/api';

export default function Workflows() {
  const [workflows, setWorkflows] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [dagJson, setDagJson] = useState('[\n  { "name": "Task 1" },\n  { "name": "Task 2", "dependsOn": ["Task 1"] }\n]');
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

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
        <h1 className="text-3xl font-bold text-white">Workflows</h1>
        <button 
          onClick={() => setIsModalOpen(true)}
          className="flex items-center gap-2 px-4 py-2 bg-brand-blue hover:bg-brand-blue/80 text-white rounded-lg transition-colors font-medium shadow-[0_0_15px_rgba(56,189,248,0.3)]"
        >
          <Plus className="w-5 h-5" />
          Create Workflow
        </button>
      </div>

      <div className="glass-panel overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-white/10 text-slate-400 text-sm">
              <th className="p-4 font-medium">ID</th>
              <th className="p-4 font-medium">Name</th>
              <th className="p-4 font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            {workflows.map(wf => (
              <tr key={wf.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                <td className="p-4 font-mono text-sm text-slate-300">{wf.id}</td>
                <td className="p-4 text-white font-medium">{wf.name}</td>
                <td className="p-4">
                  <span className="px-2 py-1 bg-white/10 text-slate-300 rounded text-xs">{wf.status || 'PENDING'}</span>
                </td>
              </tr>
            ))}
            {workflows.length === 0 && (
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
          className="px-4 py-2 bg-white/5 text-slate-300 rounded disabled:opacity-30 hover:bg-white/10"
        >
          Previous
        </button>
        <span className="text-slate-400 text-sm">
          Page {page + 1} of {Math.max(1, totalPages)}
        </span>
        <button 
          onClick={() => setPage(p => p + 1)}
          disabled={page >= totalPages - 1}
          className="px-4 py-2 bg-white/5 text-slate-300 rounded disabled:opacity-30 hover:bg-white/10"
        >
          Next
        </button>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center animate-[fadeIn_0.2s_ease-out_forwards]">
          <div className="glass-panel w-full max-w-lg p-6 flex flex-col">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold text-white">Create Workflow</h2>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div>
                <label className="block text-sm text-slate-300 mb-1">Workflow Name</label>
                <input 
                  type="text" 
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  className="w-full bg-black/30 border border-white/10 rounded-lg p-2 text-white outline-none focus:border-brand-blue transition-colors"
                  placeholder="e.g. Daily Data Export"
                />
              </div>
              
              <div>
                <label className="block text-sm text-slate-300 mb-1">DAG Definition (JSON Array)</label>
                <textarea 
                  value={dagJson}
                  onChange={(e) => setDagJson(e.target.value)}
                  required
                  rows={8}
                  className="w-full bg-black/30 border border-white/10 rounded-lg p-2 text-green-400 font-mono text-sm outline-none focus:border-brand-blue transition-colors"
                />
              </div>

              <div className="flex justify-end gap-3 mt-4">
                <button 
                  type="button" 
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-lg text-slate-300 hover:bg-white/10 transition-colors font-medium"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={loading}
                  className="px-4 py-2 bg-brand-blue hover:bg-brand-blue/80 text-white rounded-lg transition-colors font-medium disabled:opacity-50"
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
