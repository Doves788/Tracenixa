import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Folder, LogOut, ChevronDown, Check } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, projects, currentProject, setCurrentProject, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = React.useState(false);

  return (
    <header className="h-16 bg-slate-900/90 backdrop-blur border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Project Switcher */}
      <div className="relative">
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="flex items-center gap-2.5 px-3 py-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 text-slate-200 text-sm font-medium transition"
        >
          <Folder className="w-4 h-4 text-teal-400" />
          <span className="max-w-[200px] truncate">{currentProject?.name || 'Select Project'}</span>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
        </button>

        {dropdownOpen && (
          <div className="absolute left-0 mt-2 w-64 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl py-1.5 z-50">
            <div className="px-3 py-1.5 text-[11px] font-semibold text-slate-400 uppercase tracking-wider font-mono">
              Workspaces ({projects.length})
            </div>
            {projects.map((p) => {
              const isSelected = p.id === currentProject?.id;
              return (
                <button
                  key={p.id}
                  onClick={() => {
                    setCurrentProject(p);
                    setDropdownOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-left text-sm hover:bg-slate-800 transition ${
                    isSelected ? 'text-teal-300 font-medium bg-teal-500/10' : 'text-slate-300'
                  }`}
                >
                  <div className="truncate pr-2">
                    <div className="truncate">{p.name}</div>
                    <div className="text-[11px] text-slate-400 font-mono truncate">{p.slug}</div>
                  </div>
                  {isSelected && <Check className="w-4 h-4 text-teal-400 shrink-0" />}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Right pill: Status + User Profile */}
      <div className="flex items-center gap-4">
        <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-mono">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          OTLP Ingestion Active
        </div>

        <div className="flex items-center gap-3 pl-3 border-l border-slate-800">
          <div className="text-right hidden sm:block">
            <div className="text-xs font-medium text-slate-200 leading-tight">{user?.fullName || 'Developer'}</div>
            <div className="text-[11px] text-slate-400 font-mono">{user?.email}</div>
          </div>
          <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 text-teal-300 font-bold flex items-center justify-center text-xs">
            {user?.fullName?.charAt(0) || 'D'}
          </div>
          <button
            onClick={logout}
            title="Sign out"
            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800/80 transition"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
