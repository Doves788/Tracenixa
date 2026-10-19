import React, { useMemo, useState } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Node,
  Edge,
  MarkerType,
  BackgroundVariant,
  Panel,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { ReplayStepComparison, DivergenceAnalysis } from '../types';
import { GitBranch, Layers, CheckCircle2, AlertTriangle, AlertCircle, Eye, Info } from 'lucide-react';

interface DivergenceGraphViewProps {
  stepComparisons: ReplayStepComparison[];
  divergenceAnalysis?: DivergenceAnalysis;
  replayMode: string;
}

export const DivergenceGraphView: React.FC<DivergenceGraphViewProps> = ({
  stepComparisons,
  divergenceAnalysis,
  replayMode,
}) => {
  const [selectedNodeData, setSelectedNodeData] = useState<ReplayStepComparison | null>(null);
  const [showAlgorithmExplanation, setShowAlgorithmExplanation] = useState(false);

  // Transform step comparisons into a dual-track / branching DAG
  const { nodes, edges } = useMemo(() => {
    const nodesList: Node[] = [];
    const edgesList: Edge[] = [];

    const HORIZONTAL_SPACING = 280;
    const Y_BASELINE = 100;
    const Y_DIVERGED = 260;

    let previousNodeId: string | null = null;
    let branched = false;

    stepComparisons.forEach((step, index) => {
      const isDiverged = step.status === 'DIVERGED' || (!step.match && step.status !== 'INJECTED_FAULT');
      const isFault = step.status === 'INJECTED_FAULT';
      const isMatched = step.status === 'MATCHED';

      if (isDiverged) {
        branched = true;
      }

      const xPos = 40 + index * HORIZONTAL_SPACING;
      const yPos = branched && isDiverged ? Y_DIVERGED : Y_BASELINE;

      const nodeId = `node-step-${step.stepOrder}`;

      // Node background & border colors based on divergence status
      let borderColor = 'border-emerald-500/50';
      let bgColor = 'bg-slate-900/95';
      let glowColor = 'shadow-emerald-500/10';
      let badgeBg = 'bg-emerald-500/20 text-emerald-400';
      let edgeColor = '#10b981';

      if (isFault) {
        borderColor = 'border-amber-500/80';
        bgColor = 'bg-amber-950/40';
        glowColor = 'shadow-amber-500/20';
        badgeBg = 'bg-amber-500/20 text-amber-400';
        edgeColor = '#f59e0b';
      } else if (isDiverged) {
        borderColor = 'border-rose-500';
        bgColor = 'bg-rose-950/60';
        glowColor = 'shadow-rose-500/30';
        badgeBg = 'bg-rose-500/20 text-rose-300';
        edgeColor = '#f43f5e';
      }

      nodesList.push({
        id: nodeId,
        position: { x: xPos, y: yPos },
        data: {
          label: (
            <div
              onClick={() => setSelectedNodeData(step)}
              className={`p-3.5 rounded-xl border-2 ${borderColor} ${bgColor} shadow-lg ${glowColor} font-mono text-xs w-[240px] cursor-pointer transition hover:scale-105 select-none`}
            >
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-[10px] text-slate-400 font-bold">
                  STEP #{step.stepOrder}
                </span>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${badgeBg}`}>
                  {step.status}
                </span>
              </div>

              <div className="font-bold text-white text-xs truncate flex items-center gap-1.5 mb-1">
                {isMatched && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />}
                {isFault && <AlertTriangle className="w-3.5 h-3.5 text-amber-400 shrink-0" />}
                {isDiverged && <AlertCircle className="w-3.5 h-3.5 text-rose-400 shrink-0" />}
                <span className="truncate">{step.operationName}</span>
              </div>

              <div className="text-[10px] text-slate-400 flex items-center justify-between pt-1 border-t border-white/5">
                <span>{step.eventType}</span>
                <span className="text-teal-300 font-bold">
                  {step.replayedDurationMs || step.originalDurationMs}ms
                </span>
              </div>

              {isDiverged && (
                <div className="mt-2 text-[10px] text-rose-300 bg-rose-900/40 p-1.5 rounded border border-rose-500/30 line-clamp-2">
                  ⚠️ {step.diffNote}
                </div>
              )}
            </div>
          ),
        },
        draggable: true,
      });

      // Connect edges between sequential nodes
      if (previousNodeId) {
        edgesList.push({
          id: `edge-${previousNodeId}-${nodeId}`,
          source: previousNodeId,
          target: nodeId,
          animated: isDiverged || isFault,
          style: {
            stroke: edgeColor,
            strokeWidth: isDiverged ? 3 : 2,
            strokeDasharray: isDiverged ? '6,6' : undefined,
          },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: edgeColor,
          },
        });
      }

      previousNodeId = nodeId;
    });

    return { nodes: nodesList, edges: edgesList };
  }, [stepComparisons]);

  return (
    <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm font-mono text-xs">
      {/* Header Bar */}
      <div className="p-4 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-900/50">
        <div className="flex items-center gap-2.5">
          <GitBranch className="w-4 h-4 text-teal-400" />
          <span className="font-bold text-white text-sm">
            CALL-GRAPH EXECUTION DIVERGENCE TREE (DAG)
          </span>
          <span className="text-[10px] px-2 py-0.5 rounded bg-teal-500/20 text-teal-300 font-bold">
            Interactive Node Visualizer
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowAlgorithmExplanation(!showAlgorithmExplanation)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 transition text-[11px]"
          >
            <Info className="w-3.5 h-3.5" />
            <span>{showAlgorithmExplanation ? 'Hide Algorithm' : 'BFS Tree Diffing Algorithm'}</span>
          </button>
        </div>
      </div>

      {/* Algorithm Explanation Callout for Interviews */}
      {showAlgorithmExplanation && (
        <div className="p-4 bg-indigo-950/30 border-b border-indigo-500/20 text-indigo-200 text-xs space-y-2">
          <div className="font-bold text-white flex items-center gap-2">
            <span>🧠 Dual-Graph Level-Order Traversal Algorithm (Interview Defense)</span>
          </div>
          <p className="text-slate-300 leading-relaxed text-[11px]">
            To compare non-deterministic agent executions, Tracenixa implements a <strong>Synchronous Level-Order BFS Tree Diffing Algorithm</strong> in <code className="text-teal-300">ExecutionDivergenceDetector.java</code>.
            It traverses the baseline DAG G(orig) and replayed DAG G(rep) in lockstep:
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5 pt-1 text-[11px]">
            <div className="p-2.5 rounded bg-black/40 border border-white/10">
              <span className="text-teal-300 font-bold block mb-1">1. Sequence Alignment</span>
              Validates call graph parity. Detects omitted tools (<code className="text-amber-300">EXPECTED_TOOL_OMITTED</code>) and unexpected branches (<code className="text-rose-300">TOOL_SEQUENCE_MISMATCH</code>).
            </div>
            <div className="p-2.5 rounded bg-black/40 border border-white/10">
              <span className="text-teal-300 font-bold block mb-1">2. JSON AST Parameter Diff</span>
              Recursively parses input payloads with Jackson <code className="text-teal-300">JsonNode</code>, identifying parameter drift even when keys are reordered.
            </div>
            <div className="p-2.5 rounded bg-black/40 border border-white/10">
              <span className="text-teal-300 font-bold block mb-1">3. Complexity & Similarity</span>
              Evaluates in O(|V| + |E|) linear time complexity, producing a quantitative Path Parity Index (0.00 to 1.00) and pinpointing the exact root-cause step.
            </div>
          </div>
        </div>
      )}

      {/* React Flow Interactive Graph Canvas */}
      <div className="h-[420px] w-full bg-slate-950 relative">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          fitView
          fitViewOptions={{ padding: 0.3 }}
          minZoom={0.2}
          maxZoom={1.5}
          proOptions={{ hideAttribution: true }}
        >
          <Background variant={BackgroundVariant.Dots} gap={16} size={1} color="#334155" />
          <Controls className="bg-slate-900 border border-slate-700 text-white rounded-lg overflow-hidden fill-white" />
          <MiniMap
            nodeColor={(n) => {
              if (n.id.includes('step')) return '#10b981';
              return '#64748b';
            }}
            maskColor="rgba(15, 23, 42, 0.7)"
            className="bg-slate-900 border border-slate-800 rounded-lg overflow-hidden"
          />

          <Panel position="top-right" className="bg-slate-900/90 p-2.5 rounded-lg border border-slate-800 shadow-md">
            <div className="text-[10px] space-y-1.5 font-mono">
              <span className="text-slate-400 font-bold block border-b border-slate-800 pb-1">
                GRAPH LEGEND
              </span>
              <div className="flex items-center gap-2 text-emerald-400">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                <span>Matched Step (100% Parity)</span>
              </div>
              <div className="flex items-center gap-2 text-amber-400">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                <span>Injected Fault / Backoff</span>
              </div>
              <div className="flex items-center gap-2 text-rose-400">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500" />
                <span>Divergence Point / Branch</span>
              </div>
            </div>
          </Panel>
        </ReactFlow>
      </div>

      {/* Node Inspector Drawer */}
      {selectedNodeData && (
        <div className="p-4 border-t border-slate-800 bg-slate-900/95 space-y-3 animate-fadeIn">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Eye className="w-4 h-4 text-teal-400" />
              <span className="font-bold text-white">
                NODE INSPECTOR: Step #{selectedNodeData.stepOrder} ({selectedNodeData.operationName})
              </span>
            </div>
            <button
              onClick={() => setSelectedNodeData(null)}
              className="text-slate-400 hover:text-white text-xs px-2 py-0.5 rounded bg-slate-800"
            >
              Close
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-[11px]">
            <div className="p-3 rounded-lg bg-black/40 border border-slate-800 space-y-1">
              <span className="text-slate-400 font-bold block">ORIGINAL INPUT / OUTPUT</span>
              <pre className="text-slate-300 overflow-x-auto max-h-32 text-[10px]">
                {selectedNodeData.originalInput || 'None'}
              </pre>
              <div className="border-t border-white/5 pt-1 text-slate-400 text-[10px]">
                Duration: <span className="text-slate-200">{selectedNodeData.originalDurationMs}ms</span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-black/40 border border-slate-800 space-y-1">
              <span className="text-teal-400 font-bold block">REPLAYED INPUT / OUTPUT</span>
              <pre className="text-teal-200 overflow-x-auto max-h-32 text-[10px]">
                {selectedNodeData.replayedInput || 'None'}
              </pre>
              <div className="border-t border-white/5 pt-1 text-slate-400 text-[10px]">
                Duration: <span className="text-teal-300">{selectedNodeData.replayedDurationMs}ms</span>
              </div>
            </div>
          </div>

          <div className="p-2.5 rounded bg-slate-950 border border-slate-800 text-[11px] flex items-center justify-between">
            <span className="text-slate-400">Divergence Verdict:</span>
            <span className={`font-bold ${selectedNodeData.match ? 'text-emerald-400' : 'text-rose-400'}`}>
              {selectedNodeData.diffNote}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};
