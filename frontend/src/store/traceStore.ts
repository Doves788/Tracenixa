import { create } from 'zustand';
import { TraceSummary, TraceDetail } from '../types';

interface TraceState {
  selectedTrace: TraceSummary | null;
  activeTraceDetail: TraceDetail | null;
  timelineScrubberMs: number;
  selectedSpanId: string | null;
  filterService: string;
  filterStatus: string;
  setSelectedTrace: (trace: TraceSummary | null) => void;
  setActiveTraceDetail: (detail: TraceDetail | null) => void;
  setTimelineScrubberMs: (ms: number) => void;
  setSelectedSpanId: (spanId: string | null) => void;
  setFilterService: (service: string) => void;
  setFilterStatus: (status: string) => void;
  resetFilters: () => void;
}

export const useTraceStore = create<TraceState>((set) => ({
  selectedTrace: null,
  activeTraceDetail: null,
  timelineScrubberMs: 0,
  selectedSpanId: null,
  filterService: '',
  filterStatus: '',
  setSelectedTrace: (trace) => set({ selectedTrace: trace }),
  setActiveTraceDetail: (detail) => set({ activeTraceDetail: detail }),
  setTimelineScrubberMs: (ms) => set({ timelineScrubberMs: ms }),
  setSelectedSpanId: (spanId) => set({ selectedSpanId: spanId }),
  setFilterService: (service) => set({ filterService: service }),
  setFilterStatus: (status) => set({ filterStatus: status }),
  resetFilters: () => set({ filterService: '', filterStatus: '', selectedSpanId: null }),
}));

