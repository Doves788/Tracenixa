import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, ArrowRight, Sparkles } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (isRegister) {
        await register(email, password, fullName);
      } else {
        await login(email, password);
      }
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoFill = () => {
    setIsRegister(false);
    setEmail('developer@tracenixa.io');
    setPassword('Password123!');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center px-4 py-12 relative overflow-hidden">
      {/* Background radial glow */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-teal-500/10 blur-[140px] pointer-events-none rounded-full" />

      <div className="w-full max-w-md z-10">
        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex w-12 h-12 rounded-xl bg-gradient-to-tr from-teal-500 to-cyan-400 items-center justify-center shadow-xl shadow-teal-500/20 font-black text-slate-950 text-2xl mb-4">
            T
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white font-mono">TRACENIXA</h1>
          <p className="text-sm text-slate-400 mt-1 font-mono">
            Trace every step. <span className="text-teal-400">Reproduce every failure.</span>
          </p>
        </div>

        {/* Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-2xl">
          <div className="flex items-center justify-between mb-6 pb-4 border-b border-slate-800">
            <h2 className="text-lg font-semibold text-white">
              {isRegister ? 'Create Developer Workspace' : 'Sign In to Dashboard'}
            </h2>
            <button
              onClick={handleDemoFill}
              type="button"
              className="text-xs flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-teal-500/10 hover:bg-teal-500/20 text-teal-300 font-mono border border-teal-500/30 transition"
            >
              <Sparkles className="w-3.5 h-3.5" />
              Fill Demo Login
            </button>
          </div>

          {error && (
            <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-mono">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {isRegister && (
              <div>
                <label className="block text-xs font-mono text-slate-400 mb-1.5 uppercase tracking-wider">Full Name</label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="e.g. Alex Mercer"
                  className="w-full px-3.5 py-2.5 rounded-lg bg-slate-950 border border-slate-700/80 text-slate-100 text-sm focus:outline-none focus:border-teal-500 focus:ring-1 focus:ring-teal-500 transition"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-mono text-slate-400 mb-1.5 uppercase tracking-wider">Email Address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="developer@tracenixa.io"
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-950 border border-slate-700/80 text-slate-100 text-sm focus:outline-none focus:border-teal-500 focus:ring-1 focus:ring-teal-500 transition font-mono text-xs"
              />
            </div>

            <div>
              <label className="block text-xs font-mono text-slate-400 mb-1.5 uppercase tracking-wider">Password</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-950 border border-slate-700/80 text-slate-100 text-sm focus:outline-none focus:border-teal-500 focus:ring-1 focus:ring-teal-500 transition"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-gradient-to-r from-teal-500 to-cyan-500 hover:from-teal-400 hover:to-cyan-400 text-slate-950 font-semibold text-sm transition shadow-lg shadow-teal-500/20 disabled:opacity-50"
            >
              {loading ? 'Authenticating...' : isRegister ? 'Register & Provision Workspace' : 'Sign In'}
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          <div className="mt-6 pt-4 border-t border-slate-800 text-center">
            <button
              type="button"
              onClick={() => {
                setIsRegister(!isRegister);
                setError(null);
              }}
              className="text-xs text-slate-400 hover:text-teal-400 font-mono transition"
            >
              {isRegister ? 'Already have an account? Sign In' : "Don't have an account? Register new workspace"}
            </button>
          </div>
        </div>

        {/* Security footnote */}
        <div className="mt-6 flex items-center justify-center gap-2 text-xs text-slate-500 font-mono">
          <ShieldCheck className="w-4 h-4 text-teal-500" />
          <span>Project isolation with JWT & SHA-256 API key authentication</span>
        </div>
      </div>
    </div>
  );
};
