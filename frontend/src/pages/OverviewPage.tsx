import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { OverviewStats, ModelCostBreakdown, ToolPerformance, DailyTrend, RecentFailure } from '../types';
import {
  Activity,
  CheckCircle,
  AlertTriangle,
  Clock,
  Cpu,
  DollarSign,
  ArrowUpRight,
  RefreshCw,
  ExternalLink
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  BarChart,
  Bar,
  Cell
} from 'recharts';

interface OverviewPageProps {
  onNavigateToTrace: (traceId: string) => void;
}

export const OverviewPage: React.FC<OverviewPageProps> = ({ onNavigateToTrace }) => {
  const { currentProject } = useAuth();
  const [stats, setStats] = useState<OverviewStats | null>(null);
  const [models, setModels] = useState<ModelCostBreakdown[]>([]);
  const [tools, setTools] = useState<ToolPerformance[]>([]);
  const [timeline, setTimeline] = useState<DailyTrend[]>([]);
  const [failures, setFailures] = useState<RecentFailure[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const [s, m, t, tl, f] = await Promise.all([
        api.getOverviewAnalytics(currentProject.id),
        api.getModelCosts(currentProject.id),
        api.getToolPerformance(currentProject.id),
        api.getDailyTimeline(currentProject.id),
        api.getRecentFailures(currentProject.id),
      ]);
      setStats(s);
      setModels(m);
      setTools(t);
      setTimeline(tl);
      setFailures(f);
    } catch (err) {
      console.error('Failed to load overview data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [currentProject?.id]);

  if (loading && !stats) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[400px]">
        <div className="flex items-center gap-3 text-slate-400 font-mono text-sm">
          <RefreshCw className="w-5 h-5 animate-spin text-teal-400" />
          Loading project telemetry...
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
            System Overview
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
              Live Telemetry
            </span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time execution metrics, p50/p95 latency percentiles, and operational agent costs.
          </p>
        </div>

        <button
          onClick={fetchData}
          className="self-start sm:self-auto flex items-center gap-2 px-3.5 py-2 rounded-lg bg-slate-900 border border-slate-700/80 hover:bg-slate-800 text-slate-300 text-xs font-mono transition"
        >
          <RefreshCw className="w-3.5 h-3.5 text-teal-400" />
          Refresh Metrics
        </button>
      </div>

      {/* KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4">
        {/* Card 1: Executions */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-mono uppercase tracking-wider">Total Traces</span>
            <Activity className="w-4 h-4 text-teal-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{stats?.totalTraces ?? 0}</div>
          <div className="text-[11px] text-slate-400 mt-1">Captured agent runs</div>
        </div>

        {/* Card 2: Success Rate */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-mono uppercase tracking-wider">Success Rate</span>
            <CheckCircle className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-400 font-mono">
            {stats?.successRate ?? 100}%
          </div>
          <div className="text-[11px] text-slate-400 mt-1">{stats?.errorCount ?? 0} total errors</div>
        </div>

        {/* Card 3: p50 Latency */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-mono uppercase tracking-wider">p50 Latency</span>
            <Clock className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{stats?.p50LatencyMs ?? 0}<span className="text-xs text-slate-400 font-normal">ms</span></div>
          <div className="text-[11px] text-slate-400 mt-1">Median agent duration</div>
        </div>

        {/* Card 4: p95 Latency */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-mono uppercase tracking-wider">p95 Latency</span>
            <AlertTriangle className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-amber-400 font-mono">{stats?.p95LatencyMs ?? 0}<span className="text-xs text-slate-400 font-normal">ms</span></div>
          <div className="text-[11px] text-slate-400 mt-1">95th percentile latency</div>
        </div>

        {/* Card 5: Tokens */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-mono uppercase tracking-wider">Total Tokens</span>
            <Cpu className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">
            {stats ? (stats.totalTokens > 1000 ? `${(stats.totalTokens / 1000).toFixed(1)}k` : stats.totalTokens) : 0}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Prompt + completion</div>
        </div>

        {/* Card 6: Estimated Cost */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-mono uppercase tracking-wider">Est. Cost</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-400 font-mono">
            ${stats?.estimatedCostUsd ? Number(stats.estimatedCostUsd).toFixed(4) : '0.0000'}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Configurable pricing</div>
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Execution & Latency Timeline */}
        <div className="lg:col-span-2 bg-slate-900/90 border border-slate-800 rounded-xl p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-semibold text-white font-mono">Execution Volume & Latency Trend</h2>
              <p className="text-xs text-slate-400 mt-0.5">7-day rolling window of agent executions</p>
            </div>
            <div className="flex items-center gap-4 text-xs font-mono">
              <span className="flex items-center gap-1.5 text-teal-400">
                <span className="w-2.5 h-2.5 rounded-full bg-teal-400 inline-block"></span>
                Executions
              </span>
              <span className="flex items-center gap-1.5 text-cyan-400">
                <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 inline-block"></span>
                Avg Latency (ms)
              </span>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={timeline} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="execGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#14b8a6" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#14b8a6" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickFormatter={(v) => v.slice(5)} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="executions" stroke="#14b8a6" strokeWidth={2} fillOpacity={1} fill="url(#execGradient)" />
                <Area type="monotone" dataKey="avgLatencyMs" stroke="#06b6d4" strokeWidth={2} fillOpacity={0} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Model Cost & Token Breakdown */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6">
          <h2 className="text-base font-semibold text-white font-mono">Tokens by Model</h2>
          <p className="text-xs text-slate-400 mt-0.5 mb-6">Distribution across LLM architectures</p>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={models} layout="vertical" margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis type="number" stroke="#64748b" fontSize={11} />
                <YAxis dataKey="modelName" type="category" stroke="#94a3b8" fontSize={11} width={80} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }}
                  formatter={(val: any) => [`${val} tokens`, 'Usage']}
                />
                <Bar dataKey="totalTokens" radius={[0, 4, 4, 0]}>
                  {models.map((_, idx) => (
                    <Cell key={idx} fill={idx === 0 ? '#14b8a6' : '#6366f1'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Tables Row: Tool Performance & Recent Failures */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Tool Call Metrics */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-semibold text-white font-mono">Tool Call Performance</h2>
              <p className="text-xs text-slate-400 mt-0.5">Execution volume and failure rates by tool</p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400">
                  <th className="pb-3 font-semibold">TOOL NAME</th>
                  <th className="pb-3 font-semibold text-right">INVOCATIONS</th>
                  <th className="pb-3 font-semibold text-right">AVG TIME</th>
                  <th className="pb-3 font-semibold text-right">FAIL RATE</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {tools.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="py-6 text-center text-slate-500 font-sans">
                      No tool executions captured yet.
                    </td>
                  </tr>
                ) : (
                  tools.map((t) => (
                    <tr key={t.toolName} className="hover:bg-slate-800/40">
                      <td className="py-3 font-medium text-teal-300">{t.toolName}</td>
                      <td className="py-3 text-right text-slate-300">{t.callCount}</td>
                      <td className="py-3 text-right text-slate-300">{t.avgDurationMs}ms</td>
                      <td className="py-3 text-right">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            t.failureRatePct > 0
                              ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                              : 'bg-emerald-500/20 text-emerald-400'
                          }`}
                        >
                          {t.failureRatePct}%
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent Failures Table */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-semibold text-white font-mono flex items-center gap-2">
                Recent Failures
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping"></span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">Click any failed trace to inspect in Trace Explorer</p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400">
                  <th className="pb-3 font-semibold">TRACE ID</th>
                  <th className="pb-3 font-semibold">OPERATION</th>
                  <th className="pb-3 font-semibold">ERROR DETAILS</th>
                  <th className="pb-3 font-semibold text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {failures.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="py-6 text-center text-slate-500 font-sans">
                      All recent traces succeeded with zero errors.
                    </td>
                  </tr>
                ) : (
                  failures.map((f) => (
                    <tr key={f.traceId} className="hover:bg-slate-800/40">
                      <td className="py-3 text-slate-300 font-medium max-w-[100px] truncate">{f.traceId.slice(0, 8)}...</td>
                      <td className="py-3 text-slate-300 max-w-[120px] truncate">{f.rootOperation}</td>
                      <td className="py-3 text-rose-400 max-w-[160px] truncate" title={f.errorMessage}>
                        {f.errorMessage}
                      </td>
                      <td className="py-3 text-right">
                        <button
                          onClick={() => onNavigateToTrace(f.traceId)}
                          className="inline-flex items-center gap-1 text-teal-400 hover:text-teal-300 font-mono text-[11px] underline"
                        >
                          Inspect <ArrowUpRight className="w-3 h-3" />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
