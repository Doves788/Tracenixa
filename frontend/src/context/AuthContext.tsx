import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Project } from '../types';
import { api } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  projects: Project[];
  currentProject: Project | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
  setCurrentProject: (project: Project) => void;
  refreshProjects: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('trx_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('trx_jwt_token'));
  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProjectState] = useState<Project | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshProjects = async () => {
    try {
      const projs = await api.getProjects();
      setProjects(projs);
      if (projs.length > 0) {
        const savedProjId = localStorage.getItem('trx_active_project_id');
        const active = projs.find(p => p.id === savedProjId) || projs[0];
        setCurrentProjectState(active);
        localStorage.setItem('trx_active_project_id', active.id);
      }
    } catch (err) {
      console.error('Failed to load projects:', err);
    }
  };

  useEffect(() => {
    const init = async () => {
      if (token) {
        try {
          const me = await api.getMe();
          setUser(me);
          localStorage.setItem('trx_user', JSON.stringify(me));
          await refreshProjects();
        } catch (e) {
          logout();
        }
      }
      setIsLoading(false);
    };
    init();
  }, [token]);

  const login = async (email: string, password: string) => {
    const res = await api.login({ email, password });
    localStorage.setItem('trx_jwt_token', res.token);
    setToken(res.token);
    const u: User = { id: res.userId, email: res.email, fullName: res.fullName, role: res.role };
    setUser(u);
    localStorage.setItem('trx_user', JSON.stringify(u));
    await refreshProjects();
  };

  const register = async (email: string, password: string, fullName: string) => {
    const res = await api.register({ email, password, fullName });
    localStorage.setItem('trx_jwt_token', res.token);
    setToken(res.token);
    const u: User = { id: res.userId, email: res.email, fullName: res.fullName, role: res.role };
    setUser(u);
    localStorage.setItem('trx_user', JSON.stringify(u));
    await refreshProjects();
  };

  const logout = () => {
    localStorage.removeItem('trx_jwt_token');
    localStorage.removeItem('trx_user');
    localStorage.removeItem('trx_active_project_id');
    setToken(null);
    setUser(null);
    setProjects([]);
    setCurrentProjectState(null);
  };

  const setCurrentProject = (p: Project) => {
    setCurrentProjectState(p);
    localStorage.setItem('trx_active_project_id', p.id);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        projects,
        currentProject,
        isLoading,
        login,
        register,
        logout,
        setCurrentProject,
        refreshProjects,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
