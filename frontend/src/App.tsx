import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Sidebar } from './components/Sidebar';
import { Navbar } from './components/Navbar';
import { LoginPage } from './pages/LoginPage';
import { OverviewPage } from './pages/OverviewPage';
import { TraceExplorerPage } from './pages/TraceExplorerPage';
import { ReplayPage } from './pages/ReplayPage';
import { FaultPlaygroundPage } from './pages/FaultPlaygroundPage';
import { EvaluationsPage } from './pages/EvaluationsPage';
import { RegressionPage } from './pages/RegressionPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { IntegrationDocsPage } from './pages/IntegrationDocsPage';

const DashboardLayout: React.FC = () => {
  const { user, isLoading } = useAuth();
  const [currentTab, setCurrentTab] = useState('overview');
  const [targetTraceId, setTargetTraceId] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center font-mono text-sm text-slate-400">
        <span className="w-3 h-3 rounded-full bg-teal-400 animate-ping mr-3" />
        Initializing Tracenixa Observability Platform...
      </div>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  const navigateToTrace = (traceId: string) => {
    setTargetTraceId(traceId);
    setCurrentTab('traces');
  };

  const navigateToReplay = (traceId: string) => {
    setTargetTraceId(traceId);
    setCurrentTab('replays');
  };

  return (
    <div className="flex h-screen bg-slate-950 overflow-hidden">
      {/* Sidebar Navigation */}
      <Sidebar currentTab={currentTab} onSelectTab={(tab) => setCurrentTab(tab)} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Navbar />

        <main className="flex-1 overflow-y-auto">
          {currentTab === 'overview' && (
            <OverviewPage onNavigateToTrace={navigateToTrace} />
          )}
          {currentTab === 'traces' && (
            <TraceExplorerPage
              initialTraceId={targetTraceId}
              onLaunchReplay={navigateToReplay}
            />
          )}
          {currentTab === 'replays' && (
            <ReplayPage initialTraceId={targetTraceId} />
          )}
          {currentTab === 'playground' && (
            <FaultPlaygroundPage onNavigateToTrace={navigateToTrace} />
          )}
          {currentTab === 'evaluations' && (
            <EvaluationsPage onNavigateToRegression={() => setCurrentTab('regression')} />
          )}
          {currentTab === 'regression' && (
            <RegressionPage />
          )}
          {currentTab === 'analytics' && (
            <AnalyticsPage />
          )}
          {currentTab === 'projects' && (
            <ProjectsPage />
          )}
          {currentTab === 'docs' && (
            <IntegrationDocsPage />
          )}
        </main>
      </div>
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <DashboardLayout />
    </AuthProvider>
  );
}
