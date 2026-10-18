import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import {
  Zap,
  Send,
  AlertTriangle,
  CheckCircle,
  ExternalLink,
  ShieldCheck,
  RefreshCw,
  Terminal,
  Layers
} from 'lucide-react';

interface FaultPlaygroundPageProps {
  onNavigateToTrace: (traceId: string) => void;
}

export const FaultPlaygroundPage: React.FC<FaultPlaygroundPageProps> = ({ onNavigateToTrace }) => {
  const { currentProject } = useAuth();
  const [prompt, setPrompt] = useState(
    'I received order ORD-9821, but the wireless headphones arrived damaged. Please check stock and issue a refund.'
  );
  const [enableFault, setEnableFault] = useState(false);
  const [targetTool, setTargetTool] = useState('checkInventory');
  const [faultType, setFaultType] = useState('TIMEOUT');
  const [maxRetries, setMaxRetries] = useState(1);
  const [delayMs, setDelayMs] = useState(1200);

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<any | null>(null);

  const samplePrompts = [
    {
      label: 'Damaged Item Refund Workflow',
      text: 'I received order ORD-9821, but the wireless headphones arrived damaged. Please check stock and issue a refund.',
    },
    {
      label: 'Order Status Inquiry',
      text: 'Could you please verify the current delivery status of order ORD-1044 for me?',
    },
    {
      label: 'Inventory & Replacement Check',
      text: 'Verify if PROD-4K-MONITOR is available in the warehouse for order ORD-7732.',
    },
  ];

  const handleExecute = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProject || !prompt.trim()) return;

    setLoading(true);
    setResult(null);
    try {
      if (enableFault) {
        const res = await api.simulateAgentFault(currentProject.id, {
          prompt,
          targetTool,
          faultType,
          delayMs,
          maxRetries,
        });
        setResult(res);
      } else {
        const res = await api.chatAgent(currentProject.id, prompt, maxRetries);
        setResult(res);
      }
    } catch (err: any) {
      setResult({
        status: 'ERROR',
        errorMessage: err.message,
        finalResponse: 'Platform exception: ' + err.message,
        toolCallsMade: [],
        totalDurationMs: 0,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
          Fault Injection & Agent Playground
          <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
            Resilience Testing
          </span>
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Execute customer support agent workflows, inject controlled tool errors, and verify bounded retry and idempotency behavior.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left: Input & Injection Controls */}
        <div className="lg:col-span-7 space-y-5">
          <form onSubmit={handleExecute} className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm space-y-4">
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-mono uppercase tracking-wider text-slate-400 font-bold">
                  User Prompt / Inquiry
                </label>
                <div className="text-[11px] text-teal-400 font-mono">Agent: Customer Support v1.0</div>
              </div>
              <textarea
                rows={3}
                required
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="Type customer inquiry..."
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-950 border border-slate-700/80 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500 leading-relaxed"
              />

              {/* Sample Prompts Pills */}
              <div className="flex flex-wrap gap-2 mt-2">
                {samplePrompts.map((sp) => (
                  <button
                    key={sp.label}
                    type="button"
                    onClick={() => setPrompt(sp.text)}
                    className="text-[11px] px-2.5 py-1 rounded bg-slate-800/80 hover:bg-slate-800 text-slate-300 font-mono border border-slate-700/50 transition truncate max-w-[280px]"
                  >
                    {sp.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Fault Injection Toggle Box */}
            <div className={`p-4 rounded-xl border transition ${enableFault ? 'bg-rose-950/20 border-rose-500/40' : 'bg-slate-950 border-slate-800'}`}>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <Zap className={`w-4 h-4 ${enableFault ? 'text-rose-400 animate-pulse' : 'text-slate-400'}`} />
                  <span className="text-xs font-mono font-bold text-slate-200">Simulate Controlled Fault Injection</span>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={enableFault}
                    onChange={(e) => setEnableFault(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-9 h-5 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-rose-500"></div>
                </label>
              </div>

              {enableFault && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-rose-500/20 font-mono text-xs">
                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1">Target Tool</label>
                    <select
                      value={targetTool}
                      onChange={(e) => setTargetTool(e.target.value)}
                      className="w-full p-2 rounded bg-slate-900 border border-slate-700 text-slate-200 text-xs focus:outline-none"
                    >
                      <option value="checkInventory">checkInventory (Stock validation)</option>
                      <option value="getOrderStatus">getOrderStatus (Order DB)</option>
                      <option value="refundOrder">refundOrder (Simulated refund)</option>
                      <option value="*">All Tools (*)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1">Fault Type</label>
                    <select
                      value={faultType}
                      onChange={(e) => setFaultType(e.target.value)}
                      className="w-full p-2 rounded bg-slate-900 border border-slate-700 text-slate-200 text-xs focus:outline-none"
                    >
                      <option value="TIMEOUT">TIMEOUT (Simulated Network Lag)</option>
                      <option value="RATE_LIMIT">RATE_LIMIT (HTTP 429 Too Many Requests)</option>
                      <option value="TRANSIENT_500">TRANSIENT_500 (Upstream Error)</option>
                      <option value="MALFORMED_JSON">MALFORMED_JSON (Corrupted Payload)</option>
                    </select>
                  </div>
                </div>
              )}
            </div>

            {/* Retry Configuration */}
            <div className="flex items-center justify-between text-xs font-mono text-slate-400 pt-1">
              <span>Agent Retry Policy:</span>
              <div className="flex items-center gap-2">
                {[0, 1, 2].map((r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() => setMaxRetries(r)}
                    className={`px-2.5 py-1 rounded text-xs transition ${
                      maxRetries === r
                        ? 'bg-teal-500 text-slate-950 font-bold'
                        : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                    }`}
                  >
                    {r === 0 ? '0 (Fail immediately)' : `${r} ${r === 1 ? 'Retry' : 'Retries'}`}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className={`w-full py-2.5 px-4 rounded-lg font-mono text-xs font-bold transition flex items-center justify-center gap-2 shadow-lg ${
                enableFault
                  ? 'bg-rose-500 hover:bg-rose-400 text-white shadow-rose-500/20'
                  : 'bg-teal-500 hover:bg-teal-400 text-slate-950 shadow-teal-500/20'
              } disabled:opacity-50`}
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  Executing Agent Workflow & Recording Distributed Trace...
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  {enableFault ? 'Execute with Injected Fault' : 'Run Agent Workflow'}
                </>
              )}
            </button>
          </form>
        </div>

        {/* Right: Real-time Execution Output & Telemetry Summary */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm min-h-[360px] flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between pb-3 border-b border-slate-800 text-xs font-mono">
                <span className="font-bold text-white flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-teal-400" />
                  Execution Response
                </span>
                {result && (
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      result.status === 'OK' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'
                    }`}
                  >
                    {result.status}
                  </span>
                )}
              </div>

              {!result && !loading && (
                <div className="py-16 text-center text-slate-500 font-mono text-xs">
                  Run a workflow or inject a fault to observe live telemetry.
                </div>
              )}

              {loading && (
                <div className="py-16 text-center text-teal-400 font-mono text-xs flex flex-col items-center gap-3">
                  <RefreshCw className="w-6 h-6 animate-spin" />
                  <span>Invoking tools and recording OpenTelemetry spans...</span>
                </div>
              )}

              {result && !loading && (
                <div className="mt-4 space-y-4">
                  {/* Final Response Bubble */}
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 text-xs leading-relaxed font-sans">
                    {result.finalResponse}
                  </div>

                  {/* Tool Invocations Made */}
                  <div>
                    <span className="block text-[11px] font-mono text-slate-400 mb-1.5 font-bold uppercase">
                      Tools Invoked in Chain
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {result.toolCallsMade?.map((tool: string, idx: number) => (
                        <span
                          key={idx}
                          className="px-2 py-1 rounded bg-slate-800 border border-slate-700 text-teal-300 font-mono text-[11px] flex items-center gap-1"
                        >
                          <span className="text-slate-500 text-[9px]">#{idx + 1}</span> {tool}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* Error breakdown if any */}
                  {result.errorMessage && (
                    <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 font-mono text-xs">
                      <span className="font-bold block">Injected Fault Triggered:</span>
                      {result.errorMessage}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Bottom Actions & Trace Link */}
            {result?.traceId && (
              <div className="pt-4 border-t border-slate-800 flex items-center justify-between text-xs font-mono">
                <span className="text-slate-400 truncate max-w-[180px]">
                  Trace: <span className="text-teal-300">{result.traceId.slice(0, 10)}...</span>
                </span>
                <button
                  onClick={() => onNavigateToTrace(result.traceId)}
                  className="flex items-center gap-1 text-teal-400 hover:text-teal-300 font-bold underline"
                >
                  View Span Tree <ExternalLink className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
