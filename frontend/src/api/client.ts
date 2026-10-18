import {
  User, Project, ApiKey, ApiKeyCreated, TraceSummary, TraceDetail,
  ReplaySession, ReplayDetail, Dataset, EvaluationRun, CiCheckResult,
  RegressionTest, OverviewStats, ModelCostBreakdown, ToolPerformance,
  LatencyBucket, DailyTrend, RecentFailure
} from '../types';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('trx_jwt_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = {
    'Content-Type': 'application/json',
    ...getAuthHeader(),
    ...(options.headers as Record<string, string> || {}),
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Only redirect if not already on login
    if (!window.location.pathname.includes('/login')) {
      localStorage.removeItem('trx_jwt_token');
      localStorage.removeItem('trx_user');
    }
  }

  if (!response.ok) {
    let errorDetail = response.statusText;
    try {
      const errorJson = await response.json();
      errorDetail = errorJson.message || errorJson.error || JSON.stringify(errorJson);
    } catch (ignored) {}
    throw new Error(errorDetail || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export const api = {
  // --- Auth ---
  register: (body: { email: string; password: string; fullName: string }) =>
    request<{ token: string; userId: string; email: string; fullName: string; role: string }>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  login: (body: { email: string; password: string }) =>
    request<{ token: string; userId: string; email: string; fullName: string; role: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getMe: () => request<User>('/api/auth/me'),

  // --- Projects ---
  getProjects: () => request<Project[]>('/api/projects'),
  createProject: (body: { name: string; slug?: string; description?: string }) =>
    request<Project>('/api/projects', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  getProject: (id: string) => request<Project>(`/api/projects/${id}`),

  // --- API Keys ---
  getApiKeys: (projectId: string) => request<ApiKey[]>(`/api/projects/${projectId}/api-keys`),
  createApiKey: (projectId: string, name: string) =>
    request<ApiKeyCreated>(`/api/projects/${projectId}/api-keys`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  revokeApiKey: (projectId: string, keyId: string) =>
    request<void>(`/api/projects/${projectId}/api-keys/${keyId}`, {
      method: 'DELETE',
    }),

  // --- Traces ---
  getTraces: (projectId: string, params: { status?: string; serviceName?: string; search?: string; page?: number; size?: number } = {}) => {
    const q = new URLSearchParams();
    if (params.status && params.status !== 'ALL') q.set('status', params.status);
    if (params.serviceName && params.serviceName !== 'ALL') q.set('serviceName', params.serviceName);
    if (params.search) q.set('search', params.search);
    if (params.page !== undefined) q.set('page', String(params.page));
    if (params.size !== undefined) q.set('size', String(params.size));
    return request<{ content: TraceSummary[]; totalElements: number; totalPages: number }>(`/api/projects/${projectId}/traces?${q.toString()}`);
  },

  getTraceDetail: (projectId: string, traceId: string) =>
    request<TraceDetail>(`/api/projects/${projectId}/traces/${traceId}`),

  // --- Sample Agent & Playground ---
  chatAgent: (projectId: string, prompt: string, maxRetries: number = 1) =>
    request<{ traceId: string; finalResponse: string; status: string; toolCallsMade: string[]; totalDurationMs: number; errorMessage?: string }>(
      `/api/projects/${projectId}/agent/chat`,
      {
        method: 'POST',
        body: JSON.stringify({ prompt, maxRetries }),
      }
    ),

  simulateAgentFault: (projectId: string, body: { prompt: string; targetTool: string; faultType: string; delayMs?: number; maxRetries?: number }) =>
    request<{ traceId: string; finalResponse: string; status: string; toolCallsMade: string[]; totalDurationMs: number; errorMessage?: string }>(
      `/api/projects/${projectId}/agent/simulate-fault`,
      {
        method: 'POST',
        body: JSON.stringify(body),
      }
    ),

  // --- Record & Replay ---
  getReplays: (projectId: string, page = 0, size = 20) =>
    request<{ content: ReplaySession[]; totalElements: number; totalPages: number }>(`/api/projects/${projectId}/replays?page=${page}&size=${size}`),

  createReplay: (projectId: string, body: { sourceTraceId: string; replayMode: string; faultConfig?: any }) =>
    request<ReplaySession>(`/api/projects/${projectId}/replays`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getReplayDetail: (projectId: string, sessionId: string) =>
    request<ReplayDetail>(`/api/projects/${projectId}/replays/${sessionId}`),

  cancelReplay: (projectId: string, sessionId: string) =>
    request<void>(`/api/projects/${projectId}/replays/${sessionId}/cancel`, { method: 'POST' }),

  // --- Evaluations & LLM-as-a-Judge ---
  getDatasets: (projectId: string) => request<Dataset[]>(`/api/projects/${projectId}/evaluations/datasets`),
  createDataset: (projectId: string, body: any) =>
    request<Dataset>(`/api/projects/${projectId}/evaluations/datasets`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  runEvaluation: (projectId: string, body: { datasetId: string; name?: string; agentVersion?: string; modelIdentifier?: string }) =>
    request<EvaluationRun>(`/api/projects/${projectId}/evaluations/runs`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getEvaluationRuns: (projectId: string, page = 0, size = 20) =>
    request<{ content: EvaluationRun[]; totalElements: number }>(`/api/projects/${projectId}/evaluations/runs?page=${page}&size=${size}`),

  getEvaluationRunDetail: (projectId: string, runId: string) =>
    request<EvaluationRun>(`/api/projects/${projectId}/evaluations/runs/${runId}`),

  runCiCheck: (body: { projectId: string; datasetId?: string; minPassRate?: number; minFaithfulness?: number }) =>
    request<CiCheckResult>('/api/evaluations/ci-check', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  // --- Regression Tests ---
  getRegressionTests: (projectId: string) =>
    request<RegressionTest[]>(`/api/projects/${projectId}/regression-tests`),

  createRegressionTest: (projectId: string, body: any) =>
    request<RegressionTest>(`/api/projects/${projectId}/regression-tests`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  runRegressionTest: (projectId: string, testId: string) =>
    request<RegressionTest>(`/api/projects/${projectId}/regression-tests/${testId}/run`, {
      method: 'POST',
    }),

  // --- Analytics ---
  getOverviewAnalytics: (projectId: string) =>
    request<OverviewStats>(`/api/projects/${projectId}/analytics/overview`),

  getModelCosts: (projectId: string) =>
    request<ModelCostBreakdown[]>(`/api/projects/${projectId}/analytics/models`),

  getToolPerformance: (projectId: string) =>
    request<ToolPerformance[]>(`/api/projects/${projectId}/analytics/tools`),

  getLatencyHistogram: (projectId: string) =>
    request<LatencyBucket[]>(`/api/projects/${projectId}/analytics/latency`),

  getRecentFailures: (projectId: string) =>
    request<RecentFailure[]>(`/api/projects/${projectId}/analytics/recent-failures`),

  getDailyTimeline: (projectId: string) =>
    request<DailyTrend[]>(`/api/projects/${projectId}/analytics/timeline`),
};
