import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { OverviewStats, ModelCostBreakdown, ToolPerformance, LatencyBucket, DailyTrend } from '../types';
import {
  BarChart3,
  Clock,
  Cpu,
  DollarSign,
  TrendingUp,
  RefreshCw,
  Layers,
  Info
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Cell,
  LineChart,
  Line
} from 'recharts';

export const AnalyticsPage: React.FC = () => {
  const { currentProject } = useAuth();
  const [stats, setStats] = useState<OverviewStats | null>(null);
  const [models, setModels] = useState<ModelCostBreakdown[]>([]);
  const [tools, setTools] = useState<ToolPerformance[]>([]);
  const [latencyHist, setLatencyHist] = useState<LatencyBucket[]>([]);
  const [timeline, setTimeline] = useState<DailyTrend[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const [s, m, t, lh, tl] = await Promise.all([
        api.getOverviewAnalytics(currentProject.id),
        api.getModelCosts(currentProject.id),
        api.getToolPerformance(currentProject.id),
        api.getLatencyHistogram(currentProject.id),
        api.getDailyTimeline(currentProject.id),
      ]);
      setStats(s);
      setModels(m);
      setTools(t);
      setLatencyHist(lh);
      setTimeline(tl);
    } catch (err) {
      console.error('Failed to load analytics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [currentProject?.id]);

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
            Analytics & Cost Monitoring
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
              Aggregated PostgreSQL Metrics
            </span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            p50/p95 latency percentiles, latency distribution, model token costs, and tool failure analytics.
          </p>
        </div>

        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-slate-900 border border-slate-700/80 hover:bg-slate-800 text-slate-300 text-xs font-mono transition"
        >
          <RefreshCw className="w-3.5 h-3.5 text-teal-400" />
          Refresh Aggregates
        </button>
      </div>

      {/* Latency & Cost KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 font-mono">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs uppercase tracking-wider">Median Latency (p50)</span>
            <Clock className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-3xl font-bold text-white">{stats?.p50LatencyMs ?? 0}<span className="text-sm font-normal text-slate-500">ms</span></div>
          <div className="text-[11px] text-slate-400 mt-1">50% of runs complete within this time</div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs uppercase tracking-wider">Tail Latency (p95)</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-3xl font-bold text-amber-400">{stats?.p95LatencyMs ?? 0}<span className="text-sm font-normal text-slate-500">ms</span></div>
          <div className="text-[11px] text-slate-400 mt-1">95% of runs complete within this time</div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs uppercase tracking-wider">Total Token Volume</span>
            <Cpu className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-3xl font-bold text-white">
            {stats?.totalTokens ? `${(stats.totalTokens / 1000).toFixed(1)}k` : '0k'}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Prompt: {stats?.promptTokens ?? 0} | Comp: {stats?.completionTokens ?? 0}</div>
        </div>

        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs uppercase tracking-wider">Total Incurred Cost</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-3xl font-bold text-emerald-400">
            ${stats?.estimatedCostUsd ? Number(stats.estimatedCostUsd).toFixed(4) : '0.0000'}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">Estimated provider cost</div>
        </div>
      </div>

      {/* Latency Distribution Histogram */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm">
          <h2 className="text-base font-bold text-white font-mono mb-1">Latency Distribution Histogram</h2>
          <p className="text-xs text-slate-400 mb-6 font-mono">Bucket distribution of execution runtimes</p>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={latencyHist} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="label" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }}
                  formatter={(val: any) => [`${val} traces`, 'Count']}
                />
                <Bar dataKey="count" fill="#14b8a6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Cost Accumulation Trend */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm">
          <h2 className="text-base font-bold text-white font-mono mb-1">7-Day Cost Accumulation</h2>
          <p className="text-xs text-slate-400 mb-6 font-mono">Estimated operational cost ($) per day</p>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={timeline} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickFormatter={(v) => v.slice(5)} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }}
                  formatter={(val: any) => [`$${Number(val).toFixed(4)}`, 'Cost']}
                />
                <Line type="monotone" dataKey="cost" stroke="#10b981" strokeWidth={2.5} dot={{ fill: '#10b981', r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Model Pricing & Cost Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between font-mono text-xs text-slate-400 font-semibold bg-slate-900/50">
          <span className="text-white">AI MODEL COST & TOKEN VOLUME BREAKDOWN</span>
          <span>ESTIMATED SPEND</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 bg-slate-950/40">
                <th className="py-3 px-4">MODEL IDENTIFIER</th>
                <th className="py-3 px-4 text-right">TOTAL TOKENS</th>
                <th className="py-3 px-4 text-right">EXECUTIONS</th>
                <th className="py-3 px-4 text-right">ESTIMATED COST</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {models.map((m) => (
                <tr key={m.modelName} className="hover:bg-slate-800/30">
                  <td className="py-3 px-4 font-bold text-teal-300">{m.modelName}</td>
                  <td className="py-3 px-4 text-right text-slate-200">{m.totalTokens} tokens</td>
                  <td className="py-3 px-4 text-right text-slate-300">{m.executionCount} runs</td>
                  <td className="py-3 px-4 text-right font-bold text-emerald-400">
                    ${Number(m.estimatedCostUsd).toFixed(4)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
