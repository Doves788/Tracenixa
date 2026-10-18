import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  BookOpen,
  Terminal,
  Copy,
  Check,
  ExternalLink,
  Code,
  Cpu,
  Layers,
  FileCode
} from 'lucide-react';

export const IntegrationDocsPage: React.FC = () => {
  const { currentProject } = useAuth();
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const copyCode = (text: string, idx: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(idx);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const otlpUrl = `${window.location.protocol}//${window.location.hostname}:8080/v1/traces`;
  const projectId = currentProject?.id || 'YOUR_PROJECT_ID';

  const snippets = [
    {
      title: 'OpenTelemetry OTLP/HTTP Ingestion (cURL)',
      desc: 'Send standard OpenTelemetry JSON spans directly to the Tracenixa OTLP receiver.',
      lang: 'bash',
      code: `curl -X POST "${otlpUrl}" \\
  -H "Content-Type: application/json" \\
  -H "X-Project-Id: ${projectId}" \\
  -H "X-API-Key: trx_live_YOUR_API_KEY" \\
  -d '{
    "resourceSpans": [{
      "resource": {
        "attributes": [
          { "key": "service.name", "value": { "stringValue": "customer-support-agent" } },
          { "key": "project.id", "value": { "stringValue": "${projectId}" } }
        ]
      },
      "scopeSpans": [{
        "spans": [{
          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
          "spanId": "00f067aa0ba902b7",
          "name": "agent.customer_support",
          "kind": 1,
          "startTimeUnixNano": "1700000000000000000",
          "endTimeUnixNano": "1700000000450000000",
          "attributes": [
            { "key": "gen_ai.request.model", "value": { "stringValue": "gpt-4o" } },
            { "key": "gen_ai.usage.prompt_tokens", "value": { "intValue": 180 } },
            { "key": "gen_ai.usage.completion_tokens", "value": { "intValue": 65 } }
          ],
          "status": { "code": 1 }
        }]
      }]
    }]
  }'`,
    },
    {
      title: 'Spring AI Java Agent Instrumentation',
      desc: 'Annotate mock and production business tools with @Tool, using Spring AI and Tracenixa.',
      lang: 'java',
      code: `@Component
public class CustomerSupportTools {

    @Tool(description = "Verify real-time stock levels for warehouse replacement")
    public InventoryResult checkInventory(String productId) {
        // Mock tool returns structured data, records duration & handles faults
        return new InventoryResult(productId, 12, true);
    }

    @Tool(description = "Safely refund customer order with idempotency protection")
    public RefundResult refundOrder(String orderId, double amount, String idempotencyKey) {
        // Destructive operations are simulated by default with strict idempotency
        return new RefundResult("REF-9812", orderId, amount, "SUCCESS_SIMULATED");
    }
}`,
    },
    {
      title: 'OpenTelemetry Collector Configuration (otel-collector-config.yaml)',
      desc: 'Forward telemetry from local collector agents to Tracenixa with project headers.',
      lang: 'yaml',
      code: `receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  otlphttp/tracenixa:
    endpoint: "http://localhost:8080/v1/traces"
    headers:
      X-Project-Id: "${projectId}"
      X-API-Key: "trx_live_YOUR_API_KEY"

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [otlphttp/tracenixa]`,
    },
    {
      title: 'Python OpenTelemetry Exporter Integration',
      desc: 'Export traces from LangChain, LlamaIndex, or AutoGen directly into Tracenixa.',
      lang: 'python',
      code: `from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter

exporter = OTLPSpanExporter(
    endpoint="${otlpUrl}",
    headers={
        "X-Project-Id": "${projectId}",
        "X-API-Key": "trx_live_YOUR_API_KEY"
    }
)

provider = TracerProvider()
provider.add_span_processor(BatchSpanProcessor(exporter))
trace.set_tracer_provider(provider)
tracer = trace.get_tracer("tracenixa-agent")

with tracer.start_as_current_span("agent.run") as span:
    span.set_attribute("gen_ai.request.model", "gpt-4o")
    # Execute agent workflow`,
    },
  ];

  return (
    <div className="p-8 max-w-5xl mx-auto space-y-8 font-mono">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-3">
          Integration Guide & API Reference
          <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
            Developer Docs
          </span>
        </h1>
        <p className="text-sm text-slate-400 mt-1 font-sans">
          Step-by-step guides for connecting external AI agents, OpenTelemetry collectors, and CI/CD pipelines to Tracenixa.
        </p>
      </div>

      {/* Quick Access Badges */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <span className="text-[11px] text-slate-500 uppercase block">OTLP INGESTION ENDPOINT</span>
          <span className="text-sm text-teal-300 font-bold break-all">{otlpUrl}</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <span className="text-[11px] text-slate-500 uppercase block">ACTIVE WORKSPACE ID</span>
          <span className="text-sm text-slate-200 font-bold break-all">{projectId}</span>
        </div>
      </div>

      {/* OpenAPI / Swagger UI Banner */}
      <div className="p-5 rounded-xl bg-gradient-to-r from-teal-950/40 to-slate-900 border border-teal-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <FileCode className="w-5 h-5 text-teal-400" />
            Interactive Swagger UI & OpenAPI Specification
          </h2>
          <p className="text-xs text-slate-300 mt-1 font-sans">
            Inspect all endpoints, schemas, parameters, and test requests interactively.
          </p>
        </div>

        <a
          href="http://localhost:8080/swagger-ui.html"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold text-xs transition shadow-lg shadow-teal-500/20 shrink-0"
        >
          Open Swagger UI <ExternalLink className="w-3.5 h-3.5" />
        </a>
      </div>

      {/* Code Snippets List */}
      <div className="space-y-6">
        {snippets.map((snip, idx) => (
          <div key={idx} className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
            <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-900/50">
              <div>
                <h3 className="text-sm font-bold text-white">{snip.title}</h3>
                <p className="text-xs text-slate-400 font-sans mt-0.5">{snip.desc}</p>
              </div>

              <button
                onClick={() => copyCode(snip.code, idx)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs border border-slate-700 transition"
              >
                {copiedIndex === idx ? <Check className="w-3.5 h-3.5 text-teal-400" /> : <Copy className="w-3.5 h-3.5" />}
                {copiedIndex === idx ? 'Copied' : 'Copy'}
              </button>
            </div>

            <pre className="p-4 bg-slate-950 text-slate-300 text-xs leading-relaxed overflow-x-auto">
              <code>{snip.code}</code>
            </pre>
          </div>
        ))}
      </div>
    </div>
  );
};
