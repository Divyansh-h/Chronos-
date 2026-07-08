import React from 'react';

export default function StatCard({ title, value, icon }) {
  return (
    <div className="card p-6 flex items-center justify-between hover:-translate-y-1 transition-transform duration-300">
      <div>
        <h3 className="text-sm font-medium text-slate-500">{title}</h3>
        <p className="text-2xl font-bold text-slate-900 mt-1">{value}</p>
      </div>
      <div className="w-12 h-12 rounded-full bg-brand-light flex items-center justify-center text-brand-blue border border-brand-border">
        {icon}
      </div>
    </div>
  );
}
