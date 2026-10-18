import React from 'react';
import {
  LayoutDashboard,
  Activity,
  RotateCcw,
  Zap,
  CheckCircle2,
  ShieldCheck,
  BarChart3,
  FolderGit2,
  BookOpen
} from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onSelectTab }) => {
  const navItems = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'traces', label: 'Trace Explorer', icon: Activity },
    { id: 'replays', label: 'Replay Studio', icon: RotateCcw },
    { id: 'playground', label: 'Fault & Playground', icon: Zap },
    { id: 'evaluations', label: 'LLM Evaluations', icon: CheckCircle2 },
    { id: 'regression', label: 'Regression & CI', icon: ShieldCheck },
    { id: 'analytics', label: 'Analytics & Costs', icon: BarChart3 },
    { id: 'projects', label: 'Projects & Keys', icon: FolderGit2 },
    { id: 'docs', label: 'Integration Guide', icon: BookOpen },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col shrink-0 h-screen sticky top-0">
      {/* Brand Header */}
      <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800/80">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-teal-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-teal-500/20 font-black text-slate-950 text-lg">
          T
        </div>
        <div>
          <span className="font-bold tracking-tight text-white text-lg font-mono">TRACENIXA</span>
          <span className="text-[10px] block text-teal-400 font-mono tracking-wider -mt-1 font-medium">OBSERVABILITY</span>
        </div>
      </div>

      {/* Navigation List */}
      <nav className="flex-1 py-4 px-3 space-y-1 overflow-y-auto">
        <div className="px-3 pb-2 text-[11px] font-semibold tracking-wider text-slate-400 uppercase font-mono">
          Observability Platform
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                isActive
                  ? 'bg-teal-500/10 text-teal-300 border border-teal-500/30 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 border border-transparent'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-teal-400' : 'text-slate-400'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Tagline Footer */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-900/50">
        <div className="text-[11px] text-slate-400 font-mono leading-tight">
          <span className="text-teal-400">Trace every step.</span><br />
          <span className="text-slate-300">Reproduce every failure.</span>
        </div>
      </div>
    </aside>
  );
};
