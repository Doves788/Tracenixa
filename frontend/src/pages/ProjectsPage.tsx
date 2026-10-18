import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { ApiKey, ApiKeyCreated } from '../types';
import {
  FolderGit2,
  Key,
  Plus,
  Trash2,
  Copy,
  Check,
  AlertTriangle,
  ShieldCheck,
  RefreshCw
} from 'lucide-react';

export const ProjectsPage: React.FC = () => {
  const { projects, currentProject, setCurrentProject, refreshProjects } = useAuth();
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [loadingKeys, setLoadingKeys] = useState(false);

  // Modals
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [projectName, setProjectName] = useState('');
  const [projectDesc, setProjectDesc] = useState('');
  const [creatingProject, setCreatingProject] = useState(false);

  const [showKeyModal, setShowKeyModal] = useState(false);
  const [keyName, setKeyName] = useState('Production Ingestion Key');
  const [createdKey, setCreatedKey] = useState<ApiKeyCreated | null>(null);
  const [creatingKey, setCreatingKey] = useState(false);
  const [copiedKey, setCopiedKey] = useState(false);

  const fetchKeys = async () => {
    if (!currentProject) return;
    setLoadingKeys(true);
    try {
      const keys = await api.getApiKeys(currentProject.id);
      setApiKeys(keys);
    } catch (err) {
      console.error('Failed to load API keys:', err);
    } finally {
      setLoadingKeys(false);
    }
  };

  useEffect(() => {
    fetchKeys();
  }, [currentProject?.id]);

  const handleCreateProject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim()) return;
    setCreatingProject(true);
    try {
      const newProj = await api.createProject({
        name: projectName.trim(),
        description: projectDesc.trim(),
      });
      await refreshProjects();
      setCurrentProject(newProj);
      setShowProjectModal(false);
      setProjectName('');
      setProjectDesc('');
    } catch (err: any) {
      alert('Failed to create project: ' + err.message);
    } finally {
      setCreatingProject(false);
    }
  };

  const handleCreateKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProject || !keyName.trim()) return;
    setCreatingKey(true);
    try {
      const keyRes = await api.createApiKey(currentProject.id, keyName.trim());
      setCreatedKey(keyRes);
      await fetchKeys();
    } catch (err: any) {
      alert('Failed to generate key: ' + err.message);
    } finally {
      setCreatingKey(false);
    }
  };

  const handleRevokeKey = async (keyId: string) => {
    if (!currentProject) return;
    if (!confirm('Are you sure you want to revoke this API key? Applications using it will lose access immediately.')) return;
    try {
      await api.revokeApiKey(currentProject.id, keyId);
      await fetchKeys();
    } catch (err: any) {
      alert('Failed to revoke key: ' + err.message);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2500);
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono flex items-center gap-3">
            Projects & API Keys
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-teal-400 border border-teal-500/30">
              Isolation & Security
            </span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage isolated project workspaces and project-scoped API ingestion keys. Plaintext keys are hashed with SHA-256.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowProjectModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-mono border border-slate-700 transition"
          >
            <Plus className="w-3.5 h-3.5 text-teal-400" />
            New Workspace
          </button>
          <button
            onClick={() => {
              setCreatedKey(null);
              setShowKeyModal(true);
            }}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold text-xs font-mono transition shadow-lg shadow-teal-500/20"
          >
            <Key className="w-3.5 h-3.5" />
            Generate API Key
          </button>
        </div>
      </div>

      {/* Workspaces Grid */}
      <div className="space-y-4">
        <h2 className="text-sm font-bold text-slate-300 font-mono uppercase tracking-wider">
          Available Workspaces ({projects.length})
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {projects.map((p) => {
            const isActive = p.id === currentProject?.id;
            return (
              <div
                key={p.id}
                onClick={() => setCurrentProject(p)}
                className={`p-5 rounded-xl border cursor-pointer transition ${
                  isActive
                    ? 'bg-teal-500/10 border-teal-500 shadow-md shadow-teal-500/10'
                    : 'bg-slate-900 border-slate-800 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-white text-sm font-mono truncate">{p.name}</span>
                  {isActive && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-teal-500 text-slate-950">
                      ACTIVE
                    </span>
                  )}
                </div>
                <div className="text-xs font-mono text-slate-400 truncate mb-1">Slug: {p.slug}</div>
                <div className="text-[11px] text-slate-500 font-sans line-clamp-2">{p.description || 'No description'}</div>
                <div className="text-[10px] font-mono text-slate-500 mt-3 pt-3 border-t border-slate-800/80">
                  ID: {p.id}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Active Project API Keys Section */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between font-mono text-xs text-slate-400 font-semibold bg-slate-900/50">
          <span className="text-white flex items-center gap-2">
            <Key className="w-4 h-4 text-teal-400" />
            API KEYS FOR "{currentProject?.name}" ({apiKeys.length})
          </span>
          <button onClick={fetchKeys} className="hover:text-teal-400">
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 bg-slate-950/40">
                <th className="py-3 px-4">KEY NAME</th>
                <th className="py-3 px-4">PREFIX / MASK</th>
                <th className="py-3 px-4">STATUS</th>
                <th className="py-3 px-4">LAST USED</th>
                <th className="py-3 px-4 text-right">ACTION</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {apiKeys.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-500 font-sans">
                    No API keys created for this workspace. Click Generate API Key to create one.
                  </td>
                </tr>
              ) : (
                apiKeys.map((k) => (
                  <tr key={k.id} className="hover:bg-slate-800/30">
                    <td className="py-3 px-4 font-bold text-slate-200">{k.name}</td>
                    <td className="py-3 px-4 text-teal-300 font-mono">{k.keyPrefix}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          k.status === 'ACTIVE'
                            ? 'bg-emerald-500/20 text-emerald-400'
                            : 'bg-rose-500/20 text-rose-400'
                        }`}
                      >
                        {k.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400">
                      {k.lastUsedAt ? new Date(k.lastUsedAt).toLocaleString() : 'Never'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {k.status === 'ACTIVE' && (
                        <button
                          onClick={() => handleRevokeKey(k.id)}
                          className="text-rose-400 hover:text-rose-300 p-1 rounded hover:bg-rose-500/10 transition"
                          title="Revoke key"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Generate Key Modal */}
      {showKeyModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-white font-mono flex items-center gap-2">
              <Key className="w-5 h-5 text-teal-400" />
              Generate Project API Key
            </h2>

            {!createdKey ? (
              <form onSubmit={handleCreateKey} className="space-y-4 font-mono text-xs">
                <div>
                  <label className="block text-slate-400 mb-1">Key Name / Description</label>
                  <input
                    type="text"
                    required
                    value={keyName}
                    onChange={(e) => setKeyName(e.target.value)}
                    placeholder="e.g. CI/CD Ingestion Key"
                    className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-teal-500"
                  />
                </div>

                <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-300 text-[11px] leading-relaxed">
                  <AlertTriangle className="w-4 h-4 inline-block mr-1 text-amber-400" />
                  The plaintext API key will only be shown ONCE. Store it securely in your secrets manager.
                </div>

                <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setShowKeyModal(false)}
                    className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={creatingKey}
                    className="px-4 py-2 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold transition disabled:opacity-50"
                  >
                    {creatingKey ? 'Generating...' : 'Generate Key'}
                  </button>
                </div>
              </form>
            ) : (
              <div className="space-y-4 font-mono text-xs">
                <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs">
                  <Check className="w-4 h-4 inline-block mr-1" />
                  API Key Generated Successfully!
                </div>

                <div>
                  <span className="block text-slate-400 mb-1">YOUR PLAINTEXT API KEY:</span>
                  <div className="flex items-center gap-2 p-3 rounded-lg bg-slate-950 border border-teal-500/50 text-teal-300 font-bold break-all">
                    <span>{createdKey.plaintextKey}</span>
                  </div>
                </div>

                <button
                  onClick={() => copyToClipboard(createdKey.plaintextKey)}
                  className="w-full py-2.5 px-4 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold transition flex items-center justify-center gap-2 shadow-lg shadow-teal-500/20"
                >
                  {copiedKey ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                  {copiedKey ? 'Copied to Clipboard!' : 'Copy API Key'}
                </button>

                <div className="text-right pt-2 border-t border-slate-800">
                  <button
                    onClick={() => {
                      setShowKeyModal(false);
                      setCreatedKey(null);
                    }}
                    className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white"
                  >
                    Done
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Create Project Modal */}
      {showProjectModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <h2 className="text-lg font-bold text-white font-mono flex items-center gap-2">
              <FolderGit2 className="w-5 h-5 text-teal-400" />
              Create Project Workspace
            </h2>

            <form onSubmit={handleCreateProject} className="space-y-4 font-mono text-xs">
              <div>
                <label className="block text-slate-400 mb-1">Workspace Name</label>
                <input
                  type="text"
                  required
                  value={projectName}
                  onChange={(e) => setProjectName(e.target.value)}
                  placeholder="e.g. Sales Recommendation Agent"
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Description (Optional)</label>
                <textarea
                  rows={2}
                  value={projectDesc}
                  onChange={(e) => setProjectDesc(e.target.value)}
                  placeholder="Describe your agent architecture and tools..."
                  className="w-full p-2.5 rounded-lg bg-slate-950 border border-slate-700 text-slate-200 focus:outline-none focus:border-teal-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowProjectModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingProject}
                  className="px-4 py-2 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold transition disabled:opacity-50"
                >
                  {creatingProject ? 'Creating...' : 'Create Workspace'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
