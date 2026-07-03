import React from 'react';

export default function StatCard({ title, value, icon }) {
  return (
    <div className="glass-panel p-6 flex items-center justify-between hover:-translate-y-1 transition-transform duration-300">
      <div>
        <h3 className="text-sm font-medium text-slate-400">{title}</h3>
        <p className="text-2xl font-bold text-white mt-1">{value}</p>
      </div>
      <div className="w-12 h-12 rounded-full bg-brand-blue/20 flex items-center justify-center text-brand-blue">
        {icon}
      </div>
    </div>
  );
}
