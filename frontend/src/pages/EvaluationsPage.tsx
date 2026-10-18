import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { Dataset, EvaluationRun, TestCaseResult } from '../types';
import {
  CheckCircle2,
  XCircle,
  Play,
  Layers,
  Sparkles,
  RefreshCw,
  Plus,
  ShieldAlert,
  Sliders,
  ChevronDown,
  ChevronUp
} from 'lucide-react';

interface EvaluationsPageProps {
  onNavigateToRegression?: () => void;
}

export const EvaluationsPage: React.FC<EvaluationsPageProps> = ({ onNavigateToRegression }) => {
  const { currentProject } = useAuth();
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [runs, setRuns] = useState<EvaluationRun[]>([]);
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);
  const [runDetail, setRunDetail] = useState<EvaluationRun | null>(null);

  // Trigger modal
  const [showTriggerModal, setShowTriggerModal] = useState(false);
  const [selectedDatasetId, setSelectedDatasetId] = useState('');
  const [agentVersion, setAgentVersion] = useState('v1.0-prod');
  const [modelIdentifier, setModelIdentifier] = useState('gpt-4o');
  const [evaluating, setEvaluating] = useState(false);

  // Promote to regression test state
  const [promotingId, setPromotingId] = useState<string | null>(null);

  const fetchData = async () => {
    if (!currentProject) return;
    try {
      const [ds, r] = await Promise.all([
        api.getDatasets(currentProject.id),
        api.getEvaluationRuns(currentProject.id),
      ]);
      setDatasets(ds);
      setRuns(r.content);
      if (ds.length > 0 && !selectedDatasetId) {
        setSelectedDatasetId(ds[0].id);
      }
      if (r.content.length > 0 && !selectedRunId) {
        setSelectedRunId(r.content[0].id);
      }
    } catch (err) {
      console.error('Failed to load evaluation data:', err);
    }
  };

  useEffect(() => {
    fetchData();
  }, [currentProject?.id]);

  useEffect(() => {
    if (!currentProject || !selectedRunId) return;
    const fetchRun = async () => {
      try {
        const detail = await api.getEvaluationRunDetail(currentProject.id, selectedRunId);
        setRunDetail(detail);
      } catch (err) {
        console.error('Failed to load run detail:', err);
      }
    };
    fetchRun();
  }, [currentProject?.id, selectedRunId]);

  const handleTriggerRun = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProject || !selectedDatasetId) return;

    setEvaluating(true);
    try {
      const run = await api.runEvaluation(currentProject.id, {
        datasetId: selectedDatasetId,
        name: `Eval Run (${agentVersion})`,
        agentVersion,
        modelIdentifier,
      });
      setShowTriggerModal(false);
      await fetchData();
      setSelectedRunId(run.id);
    } catch (err: any) {
      alert('Failed to execute evaluation: ' + err.message);
    } finally {
      setEvaluating(false);
    }
  };

  const handlePromoteToRegression = async (tc: TestCaseResult) => {
    if (!currentProject) return;
    setPromotingId(tc.testCaseId);
    try {
      await api.createRegressionTest(currentProject.id, {
        name: `Regression: ${tc.testCaseName}`,
        description: `Auto-promoted from failed evaluation case: ${tc.failureReason || 'Behavioral mismatch'}`,
        inputPayload: tc.input,
        expectedTools: tc.toolsInvoked,
        toleranceMs: 6000,
      });
      alert(`Test case "${tc.testCaseName}" successfully promoted to Regression Suite!`);
      if (onNavigateToRegression) {
        onNavigateToRegression();
      }
    } catch (err: any) {
      alert('Failed to promote: ' + err.message);
    } finally {
      setPromotingId(null);
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
            AI Evaluation & LLM-as-a-Judge
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
              Reliability Benchmarking
            </span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Deterministic assertions for tool selection and LLM judge scoring for faithfulness and answer relevance.
          </p>
        </div>

        <button
          onClick={() => setShowTriggerModal(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gradient-to-r from-teal-500 to-cyan-500 hover:from-teal-400 hover:to-cyan-400 text-slate-950 font-semibold text-xs font-mono transition shadow-lg shadow-teal-500/20"
        >
          <Play className="w-4 h-4 fill-current" />
          Run Evaluation Suite
        </button>
      </div>

      {/* Main Grid: Runs List & Detailed Score Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left: Runs List */}
        <div className="lg:col-span-4 bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm flex flex-col max-h-[820px]">
          <div className="p-3.5 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 font-semibold bg-slate-900/50">
            <span>EVALUATION RUNS ({runs.length})</span>
            <button onClick={fetchData} className="hover:text-teal-400">
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="overflow-y-auto divide-y divide-slate-800/60 flex-1">
            {runs.length === 0 ? (
              <div className="p-8 text-center text-slate-500 font-mono text-xs">
                No evaluation runs yet. Click Run Evaluation Suite to evaluate your agent.
              </div>
            ) : (
              runs.map((r) => {
                const isSelected = selectedRunId === r.id;
                return (
                  <div
                    key={r.id}
                    onClick={() => setSelectedRunId(r.id)}
                    className={`p-3.5 cursor-pointer transition ${
                      isSelected
                        ? 'bg-teal-500/10 border-l-4 border-teal-400'
                        : 'hover:bg-slate-800/40 border-l-4 border-transparent'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-1">
                      <span className="font-mono text-xs font-bold text-slate-200 truncate">
                        {r.name}
                      </span>
                      <span className="text-xs font-mono text-emerald-400 font-bold">
                        {r.averageScore}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-[11px] font-mono text-slate-400">
                      <span>{r.agentVersion}</span>
                      <span>{r.passedCases}/{r.totalCases} passed</span>
                    </div>

                    <div className="flex items-center gap-3 text-[10px] font-mono text-slate-500 mt-1">
                      <span>Faith: {r.faithfulnessScore}%</span>
                      <span>Rel: {r.relevanceScore}%</span>
                      <span>Acc: {r.toolAccuracyScore}%</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right: Detailed Run Inspection & Case Table */}
        <div className="lg:col-span-8 space-y-6">
          {!runDetail ? (
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-12 text-center text-slate-500 font-mono text-xs">
              Select an evaluation run to inspect test case scores and ground-truth evaluations.
            </div>
          ) : (
            <>
              {/* Aggregate Score Card */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-bold text-white font-mono">{runDetail.name}</h2>
                    <div className="text-xs font-mono text-slate-400 mt-0.5">
                      Dataset: <span className="text-teal-400">{runDetail.datasetName}</span> | Model:{' '}
                      <span className="text-indigo-400">{runDetail.modelIdentifier}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="px-3 py-1 rounded-full bg-slate-950 border border-slate-700 font-mono text-xs font-bold text-emerald-400">
                      Composite Score: {runDetail.averageScore}%
                    </span>
                  </div>
                </div>

                {/* Score Breakdown Pills */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-2 border-t border-slate-800/80 font-mono text-xs">
                  <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">FAITHFULNESS</span>
                    <span className="font-bold text-emerald-400 text-sm">{runDetail.faithfulnessScore}%</span>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">RELEVANCE</span>
                    <span className="font-bold text-cyan-400 text-sm">{runDetail.relevanceScore}%</span>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">TOOL ACCURACY</span>
                    <span className="font-bold text-amber-400 text-sm">{runDetail.toolAccuracyScore}%</span>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">PASSED / TOTAL</span>
                    <span className="font-bold text-slate-200 text-sm">
                      {runDetail.passedCases} / {runDetail.totalCases}
                    </span>
                  </div>
                </div>
              </div>

              {/* Test Cases Table */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div className="p-3.5 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 font-semibold bg-slate-900/50">
                  <span className="flex items-center gap-2">
                    <Layers className="w-4 h-4 text-teal-400" />
                    TEST CASE RESULTS ({runDetail.results?.length || 0})
                  </span>
                  <span>DETERMINISTIC & JUDGE GRADES</span>
                </div>

                <div className="divide-y divide-slate-800/60">
                  {runDetail.results?.map((tc) => (
                    <div key={tc.testCaseId} className="p-4 space-y-3 font-mono text-xs hover:bg-slate-800/30 transition">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          {tc.status === 'PASSED' ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                          ) : (
                            <XCircle className="w-4 h-4 text-rose-400" />
                          )}
                          <span className="font-bold text-slate-200">{tc.testCaseName}</span>
                        </div>

                        <div className="flex items-center gap-3">
                          <span className="text-slate-400 text-[11px]">
                            Faith: <strong className="text-teal-300">{tc.faithfulnessScore}%</strong> | Rel:{' '}
                            <strong className="text-cyan-300">{tc.relevanceScore}%</strong>
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              tc.status === 'PASSED'
                                ? 'bg-emerald-500/20 text-emerald-400'
                                : 'bg-rose-500/20 text-rose-400'
                            }`}
                          >
                            {tc.status}
                          </span>
                        </div>
                      </div>

                      {/* Prompt Input */}
                      <div className="text-[11px] text-slate-400 bg-slate-950 p-2.5 rounded-lg border border-slate-800/80">
                        <span className="text-slate-500 block text-[10px] font-bold">USER INPUT:</span>
                        {tc.input}
                      </div>

                      {/* Agent Response */}
                      <div className="text-[11px] text-slate-300 bg-slate-950 p-2.5 rounded-lg border border-slate-800/80">
                        <span className="text-teal-400 block text-[10px] font-bold">AGENT OUTPUT:</span>
                        {tc.output}
                      </div>

                      {/* Tool selection verification */}
                      <div className="flex items-center gap-2 text-[11px]">
                        <span className="text-slate-500">Tools Invoked:</span>
                        {tc.toolsInvoked.map((t, i) => (
                          <span key={i} className="px-1.5 py-0.5 rounded bg-slate-800 text-teal-300 text-[10px]">
                            {t}
                          </span>
                        ))}
                      </div>

                      {/* Failure reason and promote button if failed */}
                      {tc.status === 'FAILED' && (
                        <div className="flex items-center justify-between p-2.5 rounded-lg bg-rose-950/20 border border-rose-500/30 text-rose-300 text-[11px]">
                          <span>Reason: {tc.failureReason || 'Failed deterministic threshold'}</span>
                          <button
                            onClick={() => handlePromoteToRegression(tc)}
                            disabled={promotingId === tc.testCaseId}
                            className="px-2.5 py-1 rounded bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/40 text-[10px] font-bold transition flex items-center gap-1 shrink-0 ml-2"
                          >
                            <ShieldAlert className="w-3.5 h-3.5" />
                            {promotingId === tc.testCaseId ? 'Promoting...' : 'Promote to Regression Test'}
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Trigger Modal */}
      {showTriggerModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-white font-mono flex items-center gap-2">
              <Play className="w-5 h-5 text-teal-400 fill-current" />
              Trigger Evaluation Benchmark
            </h2>

            <form onSubmit={handleTriggerRun} className="space-y-4 font-mono text-xs">
              <div>
                <label className="block text-slate-400 mb-1">Target Benchmark Dataset</label>
                <select
                  value={selectedDatasetId}
                  onChange={(e) => setSelectedDatasetId(e.target.value)}
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none"
                >
                  {datasets.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name} ({d.testCaseCount} cases)
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Agent Version / Prompt Tag</label>
                <input
                  type="text"
                  required
                  value={agentVersion}
                  onChange={(e) => setAgentVersion(e.target.value)}
                  placeholder="e.g. v1.1-prompt-update"
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Model Architecture</label>
                <select
                  value={modelIdentifier}
                  onChange={(e) => setModelIdentifier(e.target.value)}
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none"
                >
                  <option value="gpt-4o">gpt-4o (Default)</option>
                  <option value="gpt-4-turbo">gpt-4-turbo</option>
                  <option value="claude-3-5-sonnet">claude-3-5-sonnet</option>
                  <option value="gpt-3.5-turbo">gpt-3.5-turbo</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowTriggerModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={evaluating}
                  className="px-4 py-2 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold transition disabled:opacity-50"
                >
                  {evaluating ? 'Running Tests...' : 'Run Benchmark'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
