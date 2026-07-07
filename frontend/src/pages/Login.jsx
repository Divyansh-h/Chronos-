import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Key } from 'lucide-react';

export default function Login() {
  const [apiKey, setApiKey] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!apiKey.trim()) {
      setError('API Key is required');
      return;
    }
    
    // In a real app we might validate the key against the backend here,
    // but for now we'll just save it and let the backend reject requests if it's invalid.
    localStorage.setItem('API_KEY', apiKey.trim());
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-xl p-8 shadow-2xl">
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 bg-brand-blue/10 rounded-2xl flex items-center justify-center mb-4 border border-brand-blue/20 shadow-[0_0_15px_rgba(56,189,248,0.15)]">
            <Key className="w-8 h-8 text-brand-blue" />
          </div>
          <h1 className="text-2xl font-bold text-white">Welcome to Chronos</h1>
          <p className="text-slate-400 text-sm mt-2">Enter your API key to continue</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="apiKey" className="block text-sm font-medium text-slate-300 mb-2">
              API Key
            </label>
            <input
              id="apiKey"
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:border-brand-blue focus:ring-1 focus:ring-brand-blue transition-colors"
              placeholder="Enter your API key"
            />
            {error && <p className="text-red-400 text-sm mt-2">{error}</p>}
          </div>

          <button
            type="submit"
            className="w-full bg-brand-blue text-white rounded-lg px-4 py-3 font-medium hover:bg-sky-400 transition-colors shadow-[0_0_15px_rgba(56,189,248,0.2)] hover:shadow-[0_0_20px_rgba(56,189,248,0.4)]"
          >
            Access Dashboard
          </button>
        </form>
      </div>
    </div>
  );
}
