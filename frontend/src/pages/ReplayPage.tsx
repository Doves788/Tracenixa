import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { ReplaySession, ReplayDetail, ReplayStepComparison } from '../types';
import {
  RotateCcw,
  Play,
  CheckCircle,
  AlertTriangle,
  Clock,
  Layers,
  Zap,
  ArrowRight,
  RefreshCw,
  XCircle,
  Plus
} from 'lucide-react';

interface ReplayPageProps {
  initialTraceId?: string | null;
}

export const ReplayPage: React.FC<ReplayPageProps> = ({ initialTraceId }) => {
  const { currentProject } = useAuth();
  const [replays, setReplays] = useState<ReplaySession[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [replayDetail, setReplayDetail] = useState<ReplayDetail | null>(null);
  const [loading, setLoading] = useState(false);

  // New Replay Modal
  const [showModal, setShowModal] = useState(Boolean(initialTraceId));
  const [sourceTraceId, setSourceTraceId] = useState(initialTraceId || '');
  const [replayMode, setReplayMode] = useState<'RECORDED' | 'LIVE' | 'FAULT_INJECTION'>('RECORDED');
  const [targetTool, setTargetTool] = useState('checkInventory');
  const [faultType, setFaultType] = useState('TIMEOUT');
  const [delayMs, setDelayMs] = useState(1500);
  const [launching, setLaunching] = useState(false);

  const fetchReplays = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await api.getReplays(currentProject.id);
      setReplays(res.content);
      if (res.content.length > 0 && !selectedSessionId) {
        setSelectedSessionId(res.content[0].id);
      }
    } catch (err) {
      console.error('Failed to load replays:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReplays();
  }, [currentProject?.id]);

  useEffect(() => {
    if (initialTraceId) {
      setSourceTraceId(initialTraceId);
      setShowModal(true);
    }
  }, [initialTraceId]);

  useEffect(() => {
    if (!currentProject || !selectedSessionId) return;
    const fetchDetail = async () => {
      try {
        const detail = await api.getReplayDetail(currentProject.id, selectedSessionId);
        setReplayDetail(detail);
      } catch (err) {
        console.error('Failed to load replay detail:', err);
      }
    };
    fetchDetail();
    // Auto-poll if session is running
    const interval = setInterval(() => {
      if (replayDetail?.session.status === 'RUNNING' || replayDetail?.session.status === 'PENDING') {
        fetchDetail();
      }
    }, 2000);
    return () => clearInterval(interval);
  }, [currentProject?.id, selectedSessionId]);

  const handleLaunchReplay = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProject || !sourceTraceId.trim()) return;

    setLaunching(true);
    try {
      let faultConfig = undefined;
      if (replayMode === 'FAULT_INJECTION') {
        faultConfig = {
          targetTool,
          faultType,
          delayMs,
          failureRatePercentage: 100,
        };
      }

      const session = await api.createReplay(currentProject.id, {
        sourceTraceId: sourceTraceId.trim(),
        replayMode,
        faultConfig,
      });

      setShowModal(false);
      await fetchReplays();
      setSelectedSessionId(session.id);
    } catch (err: any) {
      alert('Failed to launch replay: ' + err.message);
    } finally {
      setLaunching(false);
    }
  };

  const handleCancel = async (sessionId: string) => {
    if (!currentProject) return;
    try {
      await api.cancelReplay(currentProject.id, sessionId);
      await fetchReplays();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
            Replay & Sandbox Studio
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
              Deterministic Reproduction
            </span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Replay historical agent executions with recorded responses, live reruns, or sandbox fault injection.
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gradient-to-r from-teal-500 to-cyan-500 hover:from-teal-400 hover:to-cyan-400 text-slate-950 font-semibold text-xs font-mono transition shadow-lg shadow-teal-500/20"
        >
          <Plus className="w-4 h-4" />
          Launch Replay Session
        </button>
      </div>

      {/* Main Grid: Sessions List & Step Comparison View */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left: Replay Sessions List */}
        <div className="lg:col-span-4 bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm flex flex-col max-h-[800px]">
          <div className="p-3.5 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 font-semibold bg-slate-900/50">
            <span>REPLAY SESSIONS ({replays.length})</span>
            <button onClick={fetchReplays} className="hover:text-teal-400">
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="overflow-y-auto divide-y divide-slate-800/60 flex-1">
            {replays.length === 0 ? (
              <div className="p-8 text-center text-slate-500 font-mono text-xs">
                No replay sessions recorded yet. Click Launch Replay Session to begin.
              </div>
            ) : (
              replays.map((r) => {
                const isSelected = selectedSessionId === r.id;
                return (
                  <div
                    key={r.id}
                    onClick={() => setSelectedSessionId(r.id)}
                    className={`p-3.5 cursor-pointer transition ${
                      isSelected
                        ? 'bg-teal-500/10 border-l-4 border-teal-400'
                        : 'hover:bg-slate-800/40 border-l-4 border-transparent'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-1">
                      <span className="font-mono text-xs font-bold text-slate-200 uppercase">
                        {r.replayMode}
                      </span>
                      <span
                        className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                          r.status === 'COMPLETED'
                            ? 'bg-emerald-500/20 text-emerald-400'
                            : r.status === 'RUNNING'
                            ? 'bg-cyan-500/20 text-cyan-400 animate-pulse'
                            : r.status === 'FAILED'
                            ? 'bg-rose-500/20 text-rose-400'
                            : 'bg-slate-800 text-slate-400'
                        }`}
                      >
                        {r.status}
                      </span>
                    </div>

                    <div className="text-[11px] font-mono text-slate-400 truncate">
                      Src: <span className="text-teal-300">{r.sourceTraceId.slice(0, 12)}...</span>
                    </div>

                    <div className="flex items-center justify-between text-[10px] font-mono text-slate-500 mt-1">
                      <span>Orig: {r.originalDurationMs}ms</span>
                      <span>Replay: {r.replayDurationMs || 0}ms</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right: Detailed Comparison View */}
        <div className="lg:col-span-8 space-y-6">
          {!replayDetail ? (
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-12 text-center text-slate-500 font-mono text-xs">
              Select a replay session from the left to view step-by-step diff comparison.
            </div>
          ) : (
            <>
              {/* Session Overview Card */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2.5">
                      <h2 className="text-lg font-bold text-white font-mono">
                        {replayDetail.session.replayMode} REPLAY SESSION
                      </h2>
                      <span
                        className={`text-xs font-mono px-2.5 py-0.5 rounded font-bold ${
                          replayDetail.session.status === 'COMPLETED'
                            ? 'bg-emerald-500/20 text-emerald-400'
                            : replayDetail.session.status === 'FAILED'
                            ? 'bg-rose-500/20 text-rose-400'
                            : 'bg-cyan-500/20 text-cyan-400'
                        }`}
                      >
                        {replayDetail.session.status}
                      </span>
                    </div>
                    <div className="text-xs font-mono text-slate-400 mt-0.5">
                      Source Trace: <span className="text-teal-400">{replayDetail.session.sourceTraceId}</span>
                      {replayDetail.session.replayedTraceId && (
                        <span className="ml-3">Replayed Trace: <span className="text-cyan-400">{replayDetail.session.replayedTraceId}</span></span>
                      )}
                    </div>
                  </div>

                  {replayDetail.session.status === 'RUNNING' && (
                    <button
                      onClick={() => handleCancel(replayDetail.session.id)}
                      className="px-3 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 font-mono text-xs border border-rose-500/30 transition"
                    >
                      Cancel Execution
                    </button>
                  )}
                </div>

                {/* Duration & Fidelity Comparison Bar */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-2 border-t border-slate-800/80 font-mono text-xs">
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">ORIGINAL DURATION</span>
                    <span className="font-bold text-slate-200">{replayDetail.session.originalDurationMs}ms</span>
                  </div>
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">REPLAY DURATION</span>
                    <span className="font-bold text-teal-300">{replayDetail.session.replayDurationMs || 0}ms</span>
                  </div>
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">RECORDED STEPS</span>
                    <span className="font-bold text-slate-200">{replayDetail.stepComparisons.length} steps</span>
                  </div>
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">SIDE EFFECTS</span>
                    <span className="font-bold text-emerald-400">0 (Safe Sandbox)</span>
                  </div>
                </div>

                {replayDetail.session.errorMessage && (
                  <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-mono">
                    <span className="font-bold block">Replay Failure Note:</span>
                    {replayDetail.session.errorMessage}
                  </div>
                )}
              </div>

              {/* Step-by-Step Side-by-Side Diff Table */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div className="p-3.5 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 font-semibold bg-slate-900/50">
                  <span className="flex items-center gap-2">
                    <Layers className="w-4 h-4 text-teal-400" />
                    STEP-BY-STEP REPLAY COMPARISON
                  </span>
                  <span>FIDELITY INSPECTION</span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs font-mono">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 bg-slate-950/40">
                        <th className="py-2.5 px-3">STEP</th>
                        <th className="py-2.5 px-3">OPERATION</th>
                        <th className="py-2.5 px-3">ORIGINAL TIME</th>
                        <th className="py-2.5 px-3">REPLAY TIME</th>
                        <th className="py-2.5 px-3 text-center">MATCH STATUS</th>
                        <th className="py-2.5 px-3">NOTES & DIFF</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60">
                      {replayDetail.stepComparisons.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-6 text-center text-slate-500 font-sans">
                            Awaiting replay completion...
                          </td>
                        </tr>
                      ) : (
                        replayDetail.stepComparisons.map((sc) => (
                          <tr key={sc.stepOrder} className="hover:bg-slate-800/30">
                            <td className="py-3 px-3 text-slate-400">#{sc.stepOrder}</td>
                            <td className="py-3 px-3 font-semibold text-slate-200">
                              <span className="text-[10px] text-teal-400 block">{sc.eventType}</span>
                              {sc.operationName}
                            </td>
                            <td className="py-3 px-3 text-slate-300">{sc.originalDurationMs}ms</td>
                            <td className="py-3 px-3 text-teal-300">{sc.replayedDurationMs}ms</td>
                            <td className="py-3 px-3 text-center">
                              <span
                                className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  sc.status === 'MATCHED'
                                    ? 'bg-emerald-500/20 text-emerald-400'
                                    : sc.status === 'INJECTED_FAULT'
                                    ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                                    : 'bg-amber-500/20 text-amber-400'
                                }`}
                              >
                                {sc.status}
                              </span>
                            </td>
                            <td className="py-3 px-3 text-slate-400 text-[11px] max-w-[220px]">
                              {sc.diffNote}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Launch Replay Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h2 className="text-lg font-bold text-white font-mono flex items-center gap-2">
                <RotateCcw className="w-5 h-5 text-teal-400" />
                Launch Replay Session
              </h2>
              <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <form onSubmit={handleLaunchReplay} className="space-y-4">
              <div>
                <label className="block text-xs font-mono text-slate-400 mb-1.5 uppercase">Source Trace ID</label>
                <input
                  type="text"
                  required
                  value={sourceTraceId}
                  onChange={(e) => setSourceTraceId(e.target.value)}
                  placeholder="Paste recorded trace ID..."
                  className="w-full px-3.5 py-2.5 rounded-lg bg-slate-950 border border-slate-700/80 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-xs font-mono text-slate-400 mb-1.5 uppercase">Replay Mode</label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'RECORDED', label: 'Recorded Replay', desc: 'Cached responses, 0 side effects' },
                    { id: 'LIVE', label: 'Live Rerun', desc: 'Live execution against models/tools' },
                    { id: 'FAULT_INJECTION', label: 'Fault Injection', desc: 'Isolated sandbox failure' },
                  ].map((m) => (
                    <button
                      type="button"
                      key={m.id}
                      onClick={() => setReplayMode(m.id as any)}
                      className={`p-2.5 rounded-lg border text-left transition ${
                        replayMode === m.id
                          ? 'bg-teal-500/10 border-teal-500 text-teal-300'
                          : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                      }`}
                    >
                      <div className="font-mono text-xs font-bold">{m.label}</div>
                      <div className="text-[10px] mt-1 opacity-70 leading-tight">{m.desc}</div>
                    </button>
                  ))}
                </div>
              </div>

              {replayMode === 'FAULT_INJECTION' && (
                <div className="p-4 rounded-xl bg-slate-950 border border-rose-500/30 space-y-3 font-mono text-xs">
                  <div className="flex items-center gap-2 text-rose-400 font-bold">
                    <Zap className="w-4 h-4" />
                    <span>Fault Injection Settings</span>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Target Tool</label>
                      <select
                        value={targetTool}
                        onChange={(e) => setTargetTool(e.target.value)}
                        className="w-full p-2 rounded bg-slate-900 border border-slate-700 text-slate-200 text-xs focus:outline-none"
                      >
                        <option value="checkInventory">checkInventory</option>
                        <option value="getOrderStatus">getOrderStatus</option>
                        <option value="refundOrder">refundOrder</option>
                        <option value="*">All Tools (*)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Failure Type</label>
                      <select
                        value={faultType}
                        onChange={(e) => setFaultType(e.target.value)}
                        className="w-full p-2 rounded bg-slate-900 border border-slate-700 text-slate-200 text-xs focus:outline-none"
                      >
                        <option value="TIMEOUT">TIMEOUT (Simulated Network Lag)</option>
                        <option value="RATE_LIMIT">RATE_LIMIT (HTTP 429 Too Many Requests)</option>
                        <option value="TRANSIENT_500">TRANSIENT_500 (Upstream 500 Glitch)</option>
                        <option value="MALFORMED_JSON">MALFORMED_JSON (Corrupted Payload)</option>
                      </select>
                    </div>
                  </div>

                  {faultType === 'TIMEOUT' && (
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Timeout Delay: {delayMs}ms</label>
                      <input
                        type="range"
                        min="500"
                        max="5000"
                        step="500"
                        value={delayMs}
                        onChange={(e) => setDelayMs(Number(e.target.value))}
                        className="w-full accent-teal-500"
                      />
                    </div>
                  )}
                </div>
              )}

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={launching}
                  className="px-4 py-2 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold text-xs font-mono transition shadow-lg shadow-teal-500/20 disabled:opacity-50"
                >
                  {launching ? 'Initializing Sandbox...' : 'Run Sandbox Replay'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
