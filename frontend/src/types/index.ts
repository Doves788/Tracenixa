export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
}

export interface Project {
  id: string;
  name: string;
  slug: string;
  description?: string;
  ownerId: string;
  createdAt: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  status: string;
  lastUsedAt?: string;
  createdAt: string;
}

export interface ApiKeyCreated extends ApiKey {
  plaintextKey: string;
}

export interface TraceSummary {
  id: string;
  traceId: string;
  projectId: string;
  rootOperationName: string;
  serviceName: string;
  status: 'OK' | 'ERROR' | 'UNSET';
  startTime: string;
  endTime: string;
  durationMs: number;
  totalSpans: number;
  modelName?: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number;
  inputPreview?: string;
  outputPreview?: string;
  errorMessage?: string;
}

export interface SpanNode {
  spanId: string;
  parentSpanId?: string;
  operationName: string;
  spanKind: string;
  serviceName: string;
  status: 'OK' | 'ERROR' | 'UNSET';
  statusMessage?: string;
  startTime: string;
  endTime: string;
  durationMs: number;
  offsetPercentage: number;
  widthPercentage: number;
  modelName?: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  inputPayload?: string;
  outputPayload?: string;
  attributes?: string;
  children: SpanNode[];
}

export interface TraceDetail {
  trace: TraceSummary;
  spanTree: SpanNode[];
  chronologicalSpans: SpanNode[];
}

export interface ReplayStepComparison {
  stepOrder: number;
  operationName: string;
  eventType: string;
  originalInput?: string;
  originalOutput?: string;
  replayedInput?: string;
  replayedOutput?: string;
  originalDurationMs: number;
  replayedDurationMs: number;
  match: boolean;
  status: 'MATCHED' | 'DIVERGED' | 'INJECTED_FAULT' | 'FAILED';
  diffNote: string;
}

export interface DivergenceAnalysis {
  diverged: boolean;
  similarityScore: number;
  severity: 'NONE' | 'LOW' | 'MEDIUM' | 'CRITICAL';
  firstDivergenceStep?: number;
  rootCauseCategory: string;
  explanation: string;
  sequenceDifferences: string[];
  matchedSteps: number;
  divergedSteps: number;
  totalSteps: number;
}

export interface ReplaySession {
  id: string;
  projectId: string;
  sourceTraceId: string;
  replayedTraceId?: string;
  replayMode: 'RECORDED' | 'LIVE' | 'FAULT_INJECTION';
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  originalDurationMs: number;
  replayDurationMs: number;
  stepsCount: number;
  similarityScore?: number;
  divergenceSeverity?: 'NONE' | 'LOW' | 'MEDIUM' | 'CRITICAL';
  rootCauseCategory?: string;
  diffSummary?: string;
  errorMessage?: string;
  createdAt: string;
  completedAt?: string;
}

export interface ReplayDetail {
  session: ReplaySession;
  divergenceAnalysis?: DivergenceAnalysis;
  stepComparisons: ReplayStepComparison[];
}

export interface TestCase {
  id: string;
  name: string;
  input: string;
  expectedTools?: string[];
  requiredKeywords?: string[];
  expectedResolution?: string;
}

export interface Dataset {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  version: string;
  testCases: TestCase[];
  testCaseCount: number;
  createdAt: string;
}

export interface TestCaseResult {
  testCaseId: string;
  testCaseName: string;
  input: string;
  output: string;
  toolsInvoked: string[];
  toolsMatched: boolean;
  assertionsPassed: boolean;
  deterministicScore: number;
  faithfulnessScore: number;
  relevanceScore: number;
  status: 'PASSED' | 'FAILED';
  failureReason?: string;
}

export interface EvaluationRun {
  id: string;
  projectId: string;
  datasetId: string;
  datasetName: string;
  name: string;
  agentVersion: string;
  modelIdentifier: string;
  status: string;
  totalCases: number;
  passedCases: number;
  failedCases: number;
  averageScore: number;
  faithfulnessScore: number;
  relevanceScore: number;
  toolAccuracyScore: number;
  results: TestCaseResult[];
  createdAt: string;
  completedAt?: string;
}

export interface CiCheckResult {
  passed: boolean;
  exitCode: number;
  message: string;
  actualPassRate: number;
  actualFaithfulness: number;
  runId: string;
  evaluatedAt: string;
}

export interface RegressionTest {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  sourceTraceId?: string;
  inputPayload: string;
  expectedTools?: string[];
  expectedAssertions?: string[];
  toleranceMs: number;
  status: string;
  lastRunAt?: string;
  lastRunStatus?: 'PASSED' | 'FAILED';
  createdAt: string;
}

export interface OverviewStats {
  totalTraces: number;
  successRate: number;
  errorCount: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  avgLatencyMs: number;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  estimatedCostUsd: number;
}

export interface ModelCostBreakdown {
  modelName: string;
  totalTokens: number;
  executionCount: number;
  estimatedCostUsd: number;
}

export interface ToolPerformance {
  toolName: string;
  callCount: number;
  avgDurationMs: number;
  failureCount: number;
  failureRatePct: number;
}

export interface LatencyBucket {
  label: string;
  count: number;
}

export interface DailyTrend {
  date: string;
  executions: number;
  errors: number;
  tokens: number;
  cost: number;
  avgLatencyMs: number;
}

export interface RecentFailure {
  traceId: string;
  rootOperation: string;
  serviceName: string;
  errorMessage: string;
  durationMs: number;
  timestamp: string;
}
