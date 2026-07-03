import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, GitMerge, Server } from 'lucide-react';

const NAV_ITEMS = [
  { name: 'Dashboard', path: '/', icon: LayoutDashboard },
  { name: 'Workflows', path: '/workflows', icon: GitMerge },
  { name: 'Workers', path: '/workers', icon: Server },
];

export default function Sidebar() {
  return (
    <aside className="glass-panel w-64 h-screen sticky top-0 flex flex-col p-4 shrink-0 !rounded-none !border-y-0 !border-l-0">
      <div className="mb-8 px-4 py-4">
        <h1 className="text-xl font-bold tracking-wider text-white">TASKFLOW</h1>
      </div>
      
      <nav className="flex flex-col gap-2 flex-1">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) => 
              `flex items-center gap-3 px-4 py-3 rounded-lg transition-colors duration-200 ${
                isActive 
                  ? 'bg-brand-blue/20 text-brand-blue font-medium' 
                  : 'text-slate-400 hover:text-white hover:bg-white/5'
              }`
            }
          >
            <item.icon className="w-5 h-5" />
            {item.name}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
