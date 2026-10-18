import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { RegressionTest, CiCheckResult } from '../types';
import {
  ShieldCheck,
  ShieldAlert,
  Play,
  Terminal,
  Copy,
  Check,
  RefreshCw,
  GitPullRequest,
  CheckCircle2,
  XCircle
} from 'lucide-react';

export const RegressionPage: React.FC = () => {
  const { currentProject } = useAuth();
  const [tests, setTests] = useState<RegressionTest[]>([]);
  const [loading, setLoading] = useState(false);
  const [runningId, setRunningId] = useState<string | null>(null);

  // CI Gate Simulator
  const [minPassRate, setMinPassRate] = useState(80);
  const [minFaithfulness, setMinFaithfulness] = useState(75);
  const [ciResult, setCiResult] = useState<CiCheckResult | null>(null);
  const [ciLoading, setCiLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const fetchTests = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await api.getRegressionTests(currentProject.id);
      setTests(res);
    } catch (err) {
      console.error('Failed to load regression tests:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTests();
  }, [currentProject?.id]);

  const handleRunTest = async (testId: string) => {
    if (!currentProject) return;
    setRunningId(testId);
    try {
      await api.runRegressionTest(currentProject.id, testId);
      await fetchTests();
    } catch (err: any) {
      alert('Failed to execute regression test: ' + err.message);
    } finally {
      setRunningId(null);
    }
  };

  const handleRunCiCheck = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProject) return;
    setCiLoading(true);
    try {
      const res = await api.runCiCheck({
        projectId: currentProject.id,
        minPassRate,
        minFaithfulness,
      });
      setCiResult(res);
    } catch (err: any) {
      // In case of 412 Precondition Failed, err may be thrown by client
      setCiResult({
        passed: false,
        exitCode: 1,
        message: err.message,
        actualPassRate: 0,
        actualFaithfulness: 0,
        runId: 'failed-run',
        evaluatedAt: new Date().toISOString(),
      });
    } finally {
      setCiLoading(false);
    }
  };

  const curlSnippet = `curl -X POST "http://localhost:8080/api/evaluations/ci-check" \\
  -H "Content-Type: application/json" \\
  -d '{"projectId": "${currentProject?.id || 'PROJECT_ID'}", "minPassRate": ${minPassRate}, "minFaithfulness": ${minFaithfulness}}' \\
  --fail-with-body`;

  const copyCurl = () => {
    navigator.clipboard.writeText(curlSnippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
          Regression Test Suites & CI Gates
          <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
            Automated Quality Gates
          </span>
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Lock down edge cases and run regression suites in CI/CD pipelines to prevent behavioral drift before deployment.
        </p>
      </div>

      {/* CI Gate Simulator Box */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-sm space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
          <div>
            <h2 className="text-base font-bold text-white font-mono flex items-center gap-2">
              <GitPullRequest className="w-4 h-4 text-teal-400" />
              CI/CD Pipeline Quality Gate Simulator
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Simulates automated GitHub Actions / GitLab CI regression gate check
            </p>
          </div>

          <button
            onClick={copyCurl}
            className="self-start sm:self-auto flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-mono text-xs border border-slate-700 transition"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-teal-400" /> : <Copy className="w-3.5 h-3.5" />}
            {copied ? 'Copied cURL' : 'Copy CI cURL Command'}
          </button>
        </div>

        <form onSubmit={handleRunCiCheck} className="grid grid-cols-1 sm:grid-cols-3 gap-4 font-mono text-xs">
          <div>
            <label className="block text-slate-400 mb-1">Min Pass Rate (%): {minPassRate}%</label>
            <input
              type="range"
              min="50"
              max="100"
              value={minPassRate}
              onChange={(e) => setMinPassRate(Number(e.target.value))}
              className="w-full accent-teal-500"
            />
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Min Faithfulness (%): {minFaithfulness}%</label>
            <input
              type="range"
              min="50"
              max="100"
              value={minFaithfulness}
              onChange={(e) => setMinFaithfulness(Number(e.target.value))}
              className="w-full accent-teal-500"
            />
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={ciLoading}
              className="w-full py-2.5 px-4 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold transition shadow-lg shadow-teal-500/20 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {ciLoading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  Running CI Evaluation Gate...
                </>
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current" />
                  Trigger CI Gate Evaluation
                </>
              )}
            </button>
          </div>
        </form>

        {/* CI Output Card */}
        {ciResult && (
          <div
            className={`p-4 rounded-xl border font-mono text-xs space-y-2 ${
              ciResult.passed
                ? 'bg-emerald-950/20 border-emerald-500/40 text-emerald-300'
                : 'bg-rose-950/20 border-rose-500/40 text-rose-300'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="font-bold flex items-center gap-2 text-sm">
                {ciResult.passed ? (
                  <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                ) : (
                  <XCircle className="w-5 h-5 text-rose-400" />
                )}
                {ciResult.passed ? 'CI QUALITY GATE PASSED (Exit Code: 0)' : 'CI QUALITY GATE FAILED (Exit Code: 1)'}
              </span>
              <span className="text-[11px] opacity-70">
                Evaluated: {new Date(ciResult.evaluatedAt).toLocaleTimeString()}
              </span>
            </div>

            <p className="text-[11px] leading-relaxed opacity-90">{ciResult.message}</p>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-800 text-[11px]">
              <div>
                <span className="text-slate-400 block">ACTUAL PASS RATE</span>
                <span className="font-bold text-white">{ciResult.actualPassRate}%</span>
              </div>
              <div>
                <span className="text-slate-400 block">ACTUAL FAITHFULNESS</span>
                <span className="font-bold text-white">{ciResult.actualFaithfulness}%</span>
              </div>
              <div>
                <span className="text-slate-400 block">EVALUATION RUN</span>
                <span className="font-bold text-white">{ciResult.runId.slice(0, 8)}...</span>
              </div>
              <div>
                <span className="text-slate-400 block">PIPELINE OUTCOME</span>
                <span className={`font-bold ${ciResult.passed ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {ciResult.passed ? 'Deploy Permitted' : 'Deploy Blocked'}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Regression Suite Table */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between font-mono text-xs text-slate-400 font-semibold bg-slate-900/50">
          <span className="flex items-center gap-2 text-white">
            <ShieldCheck className="w-4 h-4 text-teal-400" />
            ACTIVE REGRESSION SUITE ({tests.length})
          </span>
          <button onClick={fetchTests} className="hover:text-teal-400">
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="divide-y divide-slate-800/60 font-mono text-xs">
          {tests.length === 0 ? (
            <div className="p-8 text-center text-slate-500 font-sans">
              No regression tests created yet. Convert failed traces or evaluation runs to regression tests.
            </div>
          ) : (
            tests.map((t) => (
              <div key={t.id} className="p-4 space-y-2 hover:bg-slate-800/30 transition">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <span className="font-bold text-slate-200">{t.name}</span>
                    {t.lastRunStatus && (
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          t.lastRunStatus === 'PASSED'
                            ? 'bg-emerald-500/20 text-emerald-400'
                            : 'bg-rose-500/20 text-rose-400'
                        }`}
                      >
                        {t.lastRunStatus}
                      </span>
                    )}
                  </div>

                  <button
                    onClick={() => handleRunTest(t.id)}
                    disabled={runningId === t.id}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-teal-300 border border-slate-700 text-xs font-bold transition disabled:opacity-50"
                  >
                    <Play className="w-3 h-3 fill-current" />
                    {runningId === t.id ? 'Verifying...' : 'Run Regression Test'}
                  </button>
                </div>

                <p className="text-slate-400 text-[11px] font-sans">{t.description}</p>

                <div className="bg-slate-950 p-2.5 rounded border border-slate-800 text-[11px] text-slate-300">
                  <span className="text-slate-500 block text-[10px]">TEST INPUT PAYLOAD:</span>
                  {t.inputPayload}
                </div>

                <div className="flex items-center justify-between text-[10px] text-slate-500 pt-1">
                  <span>Tolerance: {t.toleranceMs}ms</span>
                  <span>Last run: {t.lastRunAt ? new Date(t.lastRunAt).toLocaleString() : 'Never'}</span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
