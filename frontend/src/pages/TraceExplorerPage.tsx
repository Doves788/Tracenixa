import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { TraceSummary, TraceDetail, SpanNode } from '../types';
import {
  Search,
  Filter,
  CheckCircle,
  AlertCircle,
  ChevronRight,
  ChevronDown,
  Clock,
  Cpu,
  Layers,
  ArrowRight,
  RotateCcw,
  Sparkles,
  Terminal,
  Code
} from 'lucide-react';

interface TraceExplorerPageProps {
  initialTraceId?: string | null;
  onLaunchReplay?: (traceId: string) => void;
}

export const TraceExplorerPage: React.FC<TraceExplorerPageProps> = ({
  initialTraceId,
  onLaunchReplay,
}) => {
  const { currentProject } = useAuth();
  const [traces, setTraces] = useState<TraceSummary[]>([]);
  const [selectedTraceId, setSelectedTraceId] = useState<string | null>(initialTraceId || null);
  const [traceDetail, setTraceDetail] = useState<TraceDetail | null>(null);
  const [selectedSpan, setSelectedSpan] = useState<SpanNode | null>(null);
  const [collapsedSpans, setCollapsedSpans] = useState<Set<string>>(new Set());

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchTraces = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await api.getTraces(currentProject.id, {
        search,
        status: statusFilter,
        page,
        size: 15,
      });
      setTraces(res.content);
      setTotalPages(res.totalPages || 1);
      if (res.content.length > 0 && !selectedTraceId) {
        setSelectedTraceId(res.content[0].traceId);
      }
    } catch (err) {
      console.error('Failed to load traces:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTraces();
  }, [currentProject?.id, search, statusFilter, page]);

  useEffect(() => {
    if (initialTraceId) {
      setSelectedTraceId(initialTraceId);
    }
  }, [initialTraceId]);

  useEffect(() => {
    if (!currentProject || !selectedTraceId) return;
    const fetchDetail = async () => {
      setDetailLoading(true);
      try {
        const detail = await api.getTraceDetail(currentProject.id, selectedTraceId);
        setTraceDetail(detail);
        if (detail.chronologicalSpans.length > 0) {
          setSelectedSpan(detail.chronologicalSpans[0]);
        }
      } catch (err) {
        console.error('Failed to load trace detail:', err);
      } finally {
        setDetailLoading(false);
      }
    };
    fetchDetail();
  }, [currentProject?.id, selectedTraceId]);

  const toggleCollapse = (spanId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setCollapsedSpans((prev) => {
      const next = new Set(prev);
      if (next.has(spanId)) next.delete(spanId);
      else next.add(spanId);
      return next;
    });
  };

  const renderSpanTreeNode = (node: SpanNode, depth = 0) => {
    const isCollapsed = collapsedSpans.has(node.spanId);
    const isSelected = selectedSpan?.spanId === node.spanId;
    const hasChildren = node.children && node.children.length > 0;

    return (
      <div key={node.spanId} className="flex flex-col">
        <div
          onClick={() => setSelectedSpan(node)}
          style={{ paddingLeft: `${depth * 18 + 12}px` }}
          className={`flex items-center justify-between py-2 pr-3 cursor-pointer border-l-2 transition ${
            isSelected
              ? 'bg-teal-500/10 border-teal-400 text-teal-200'
              : 'hover:bg-slate-800/60 border-transparent text-slate-300'
          }`}
        >
          <div className="flex items-center gap-2 truncate">
            {hasChildren ? (
              <button
                onClick={(e) => toggleCollapse(node.spanId, e)}
                className="p-0.5 text-slate-400 hover:text-white"
              >
                {isCollapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
              </button>
            ) : (
              <span className="w-3.5 h-3.5 inline-block" />
            )}

            <span
              className={`text-[10px] font-mono px-1.5 py-0.5 rounded font-bold uppercase tracking-wider ${
                node.spanKind === 'AGENT'
                  ? 'bg-teal-500/20 text-teal-300 border border-teal-500/30'
                  : node.spanKind === 'MODEL'
                  ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                  : node.spanKind === 'TOOL'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                  : 'bg-slate-700 text-slate-300'
              }`}
            >
              {node.spanKind}
            </span>

            <span className="font-mono text-xs font-medium truncate">{node.operationName}</span>
          </div>

          <div className="flex items-center gap-2 shrink-0 font-mono text-[11px] text-slate-400">
            <span>{node.durationMs}ms</span>
            {node.status === 'ERROR' ? (
              <AlertCircle className="w-3.5 h-3.5 text-rose-400" />
            ) : (
              <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
            )}
          </div>
        </div>

        {hasChildren && !isCollapsed && (
          <div>
            {node.children.map((child) => renderSpanTreeNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      {/* Page Title & Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
            Trace Explorer
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
              Interactive Inspector
            </span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Collapsible execution trees, Gantt timelines, model latency, and sanitized I/O payloads.
          </p>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search trace or operation..."
              className="pl-9 pr-3.5 py-1.5 rounded-lg bg-slate-900 border border-slate-700/80 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500 w-56"
            />
          </div>

          <div className="flex items-center rounded-lg bg-slate-900 border border-slate-700/80 p-0.5 text-xs font-mono">
            {['ALL', 'OK', 'ERROR'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 rounded-md transition ${
                  statusFilter === st ? 'bg-teal-500 text-slate-950 font-bold' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {st}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Split Layout: Left Trace List, Right Detailed Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Traces List */}
        <div className="lg:col-span-5 bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm flex flex-col max-h-[820px]">
          <div className="p-3.5 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 font-semibold bg-slate-900/50">
            <span>EXECUTIONS ({traces.length})</span>
            <span>DURATION / STATUS</span>
          </div>

          <div className="overflow-y-auto divide-y divide-slate-800/60 flex-1">
            {traces.length === 0 ? (
              <div className="p-8 text-center text-slate-500 font-mono text-xs">
                No traces found matching criteria.
              </div>
            ) : (
              traces.map((t) => {
                const isSelected = selectedTraceId === t.traceId;
                return (
                  <div
                    key={t.traceId}
                    onClick={() => setSelectedTraceId(t.traceId)}
                    className={`p-3.5 cursor-pointer transition ${
                      isSelected
                        ? 'bg-teal-500/10 border-l-4 border-teal-400'
                        : 'hover:bg-slate-800/40 border-l-4 border-transparent'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-1">
                      <span className="font-mono text-xs font-semibold text-slate-200 truncate">
                        {t.rootOperationName}
                      </span>
                      <span
                        className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                          t.status === 'ERROR'
                            ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                            : 'bg-emerald-500/20 text-emerald-400'
                        }`}
                      >
                        {t.status}
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-[11px] font-mono text-slate-400">
                      <span className="truncate max-w-[150px]">{t.traceId}</span>
                      <div className="flex items-center gap-2">
                        <span>{t.durationMs}ms</span>
                        <span>•</span>
                        <span>{t.totalTokens} tok</span>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Pagination */}
          <div className="p-3 border-t border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 bg-slate-900/50">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40"
            >
              Prev
            </button>
            <span>Page {page + 1} of {totalPages}</span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>

        {/* Right Column: Detailed Trace Tree, Gantt Timeline & Span Inspector */}
        <div className="lg:col-span-7 space-y-6">
          {detailLoading ? (
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-12 text-center text-slate-400 font-mono text-xs">
              Loading trace telemetry tree...
            </div>
          ) : !traceDetail ? (
            <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-12 text-center text-slate-500 font-mono text-xs">
              Select an execution from the left list to inspect spans.
            </div>
          ) : (
            <>
              {/* Trace Summary Bar */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2.5">
                      <h2 className="text-lg font-bold text-white font-mono">{traceDetail.trace.rootOperationName}</h2>
                      <span
                        className={`text-xs font-mono px-2 py-0.5 rounded font-bold ${
                          traceDetail.trace.status === 'ERROR'
                            ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                            : 'bg-emerald-500/20 text-emerald-400'
                        }`}
                      >
                        {traceDetail.trace.status}
                      </span>
                    </div>
                    <div className="text-xs font-mono text-slate-400 mt-0.5 truncate">
                      Trace ID: <span className="text-teal-400">{traceDetail.trace.traceId}</span>
                    </div>
                  </div>

                  {onLaunchReplay && (
                    <button
                      onClick={() => onLaunchReplay(traceDetail.trace.traceId)}
                      className="self-start sm:self-auto flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-semibold text-xs font-mono transition shadow-lg shadow-teal-500/20"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      Replay in Sandbox
                    </button>
                  )}
                </div>

                {/* Metrics Badges */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-2 border-t border-slate-800/80 font-mono text-xs">
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">TOTAL DURATION</span>
                    <span className="font-bold text-slate-200">{traceDetail.trace.durationMs}ms</span>
                  </div>
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">TOTAL SPANS</span>
                    <span className="font-bold text-slate-200">{traceDetail.trace.totalSpans} nodes</span>
                  </div>
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">TOKEN USAGE</span>
                    <span className="font-bold text-slate-200">{traceDetail.trace.totalTokens} tokens</span>
                  </div>
                  <div className="p-2 rounded-lg bg-slate-950 border border-slate-800/80">
                    <span className="text-[10px] text-slate-400 block">ESTIMATED COST</span>
                    <span className="font-bold text-emerald-400">${Number(traceDetail.trace.estimatedCost).toFixed(4)}</span>
                  </div>
                </div>

                {traceDetail.trace.errorMessage && (
                  <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-mono flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold block">Fatal Execution Error:</span>
                      {traceDetail.trace.errorMessage}
                    </div>
                  </div>
                )}
              </div>

              {/* Gantt Execution Timeline & Tree */}
              <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div className="p-3.5 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-400 font-semibold bg-slate-900/50">
                  <span className="flex items-center gap-2">
                    <Layers className="w-4 h-4 text-teal-400" />
                    SPAN HIERARCHY & TIMELINE
                  </span>
                  <span>TOTAL: {traceDetail.trace.durationMs}ms</span>
                </div>

                {/* Timeline Header Scale */}
                <div className="px-4 py-2 border-b border-slate-800/60 bg-slate-950/40 grid grid-cols-12 text-[10px] font-mono text-slate-500">
                  <div className="col-span-6">OPERATION / NODE</div>
                  <div className="col-span-6 flex justify-between pr-2">
                    <span>0ms</span>
                    <span>{Math.round(traceDetail.trace.durationMs * 0.5)}ms</span>
                    <span>{traceDetail.trace.durationMs}ms</span>
                  </div>
                </div>

                {/* Combined Tree & Gantt Bar Rows */}
                <div className="divide-y divide-slate-800/40">
                  {traceDetail.chronologicalSpans.map((span) => {
                    const isSelected = selectedSpan?.spanId === span.spanId;
                    return (
                      <div
                        key={span.spanId}
                        onClick={() => setSelectedSpan(span)}
                        className={`grid grid-cols-12 items-center py-2 px-3 cursor-pointer text-xs transition ${
                          isSelected ? 'bg-teal-500/10' : 'hover:bg-slate-800/40'
                        }`}
                      >
                        {/* Left: Operation Name & Kind */}
                        <div className="col-span-6 flex items-center gap-2 truncate pr-2">
                          <span
                            className={`text-[9px] font-mono px-1 py-0.5 rounded font-bold uppercase ${
                              span.spanKind === 'AGENT'
                                ? 'bg-teal-500/20 text-teal-300'
                                : span.spanKind === 'MODEL'
                                ? 'bg-indigo-500/20 text-indigo-300'
                                : 'bg-amber-500/20 text-amber-300'
                            }`}
                          >
                            {span.spanKind}
                          </span>
                          <span className="font-mono text-slate-200 truncate">{span.operationName}</span>
                        </div>

                        {/* Right: Gantt Timeline Bar */}
                        <div className="col-span-6 relative h-5 bg-slate-950/80 rounded flex items-center px-1">
                          <div
                            style={{
                              marginLeft: `${span.offsetPercentage}%`,
                              width: `${Math.max(2, span.widthPercentage)}%`,
                            }}
                            className={`h-3 rounded text-[10px] font-mono flex items-center justify-center font-bold text-slate-950 transition-all ${
                              span.status === 'ERROR'
                                ? 'bg-rose-500'
                                : span.spanKind === 'MODEL'
                                ? 'bg-indigo-400'
                                : span.spanKind === 'TOOL'
                                ? 'bg-amber-400'
                                : 'bg-teal-400'
                            }`}
                            title={`${span.operationName}: ${span.durationMs}ms`}
                          />
                          <span className="absolute right-2 text-[10px] font-mono text-slate-400">
                            {span.durationMs}ms
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Selected Span Inspector Drawer */}
              {selectedSpan && (
                <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4 font-mono text-xs">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                    <div className="flex items-center gap-2">
                      <Terminal className="w-4 h-4 text-teal-400" />
                      <span className="font-bold text-white text-sm">Span Inspector: {selectedSpan.operationName}</span>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        selectedSpan.status === 'ERROR' ? 'bg-rose-500/20 text-rose-400' : 'bg-emerald-500/20 text-emerald-400'
                      }`}
                    >
                      {selectedSpan.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-slate-400 text-[11px]">
                    <div>
                      <span className="block text-slate-500">SPAN ID</span>
                      <span className="text-slate-200">{selectedSpan.spanId}</span>
                    </div>
                    <div>
                      <span className="block text-slate-500">PARENT ID</span>
                      <span className="text-slate-200">{selectedSpan.parentSpanId || 'root'}</span>
                    </div>
                    <div>
                      <span className="block text-slate-500">KIND</span>
                      <span className="text-slate-200">{selectedSpan.spanKind}</span>
                    </div>
                    <div>
                      <span className="block text-slate-500">DURATION</span>
                      <span className="text-slate-200">{selectedSpan.durationMs}ms</span>
                    </div>
                  </div>

                  {selectedSpan.modelName && (
                    <div className="p-3 rounded-lg bg-indigo-950/20 border border-indigo-500/30 text-indigo-300 text-xs">
                      <span className="font-bold block mb-1">Model Telemetry: {selectedSpan.modelName}</span>
                      <div className="grid grid-cols-3 gap-2">
                        <span>Prompt: {selectedSpan.promptTokens} tok</span>
                        <span>Completion: {selectedSpan.completionTokens} tok</span>
                        <span>Total: {selectedSpan.totalTokens} tok</span>
                      </div>
                    </div>
                  )}

                  {/* Sanitized Input Payload */}
                  {selectedSpan.inputPayload && (
                    <div>
                      <span className="block text-slate-400 text-[11px] mb-1 font-semibold">INPUT PAYLOAD</span>
                      <pre className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-slate-300 overflow-x-auto text-[11px] leading-relaxed">
                        {selectedSpan.inputPayload}
                      </pre>
                    </div>
                  )}

                  {/* Sanitized Output Payload */}
                  {selectedSpan.outputPayload && (
                    <div>
                      <span className="block text-slate-400 text-[11px] mb-1 font-semibold">OUTPUT PAYLOAD</span>
                      <pre className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-slate-300 overflow-x-auto text-[11px] leading-relaxed">
                        {selectedSpan.outputPayload}
                      </pre>
                    </div>
                  )}

                  {/* Attributes JSON */}
                  {selectedSpan.attributes && (
                    <div>
                      <span className="block text-slate-400 text-[11px] mb-1 font-semibold">SANITIZED OPENTELEMETRY ATTRIBUTES</span>
                      <pre className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-slate-400 overflow-x-auto text-[10px] leading-relaxed">
                        {selectedSpan.attributes}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
