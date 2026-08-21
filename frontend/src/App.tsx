import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import { AuthProvider, useAuth } from './lib/auth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import CodeExplorerPage from './pages/CodeExplorerPage'
import ArchitecturePage from './pages/ArchitecturePage'
import ApiEndpointsPage from './pages/ApiEndpointsPage'
import GitPage from './pages/GitPage'
import SearchPage from './pages/SearchPage'
import ChatPage from './pages/ChatPage'
import AgentPage from './pages/AgentPage'
import BugPage from './pages/BugPage'
import ReviewPage from './pages/ReviewPage'
import TestGenPage from './pages/TestGenPage'
import DocsPage from './pages/DocsPage'
import PatchesPage from './pages/PatchesPage'
import SettingsPage from './pages/SettingsPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-slate-500">Loading…</div>
    )
  }
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            element={
              <RequireAuth>
                <Layout />
              </RequireAuth>
            }
          >
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
            <Route path="/projects/:projectId/code" element={<CodeExplorerPage />} />
            <Route path="/projects/:projectId/architecture" element={<ArchitecturePage />} />
            <Route path="/projects/:projectId/apis" element={<ApiEndpointsPage />} />
            <Route path="/projects/:projectId/git" element={<GitPage />} />
            <Route path="/projects/:projectId/search" element={<SearchPage />} />
            <Route path="/projects/:projectId/chat" element={<ChatPage />} />
            <Route path="/projects/:projectId/agent" element={<AgentPage />} />
            <Route path="/projects/:projectId/bug" element={<BugPage />} />
            <Route path="/projects/:projectId/review" element={<ReviewPage />} />
            <Route path="/projects/:projectId/tests" element={<TestGenPage />} />
            <Route path="/projects/:projectId/docs" element={<DocsPage />} />
            <Route path="/projects/:projectId/patches" element={<PatchesPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}