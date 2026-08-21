import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  Bot,
  Bug,
  FileCode2,
  FileText,
  FlaskConical,
  GitBranch,
  LayoutDashboard,
  MessageSquare,
  Search,
  Settings,
  ShieldCheck,
  Wand2,
  LogOut,
  Hammer,
} from 'lucide-react'
import { useAuth } from '../lib/auth'

const iconCls = 'h-4 w-4'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const links = [
    { to: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard className={iconCls} /> },
    { to: '/chat', label: 'AI Copilot', icon: <MessageSquare className={iconCls} /> },
    { to: '/search', label: 'Search', icon: <Search className={iconCls} /> },
    { to: '/bug', label: 'Debug', icon: <Bug className={iconCls} /> },
    { to: '/review', label: 'Review', icon: <ShieldCheck className={iconCls} /> },
    { to: '/tests', label: 'Tests', icon: <FlaskConical className={iconCls} /> },
    { to: '/docs', label: 'Docs', icon: <FileText className={iconCls} /> },
    { to: '/patches', label: 'Patches', icon: <Hammer className={iconCls} /> },
  ]

  const explorer = [
    { to: '/code', label: 'Code Explorer', icon: <FileCode2 className={iconCls} /> },
    { to: '/architecture', label: 'Architecture', icon: <GitBranch className={iconCls} /> },
    { to: '/git', label: 'Git History', icon: <GitBranch className={iconCls} /> },
  ]

  return (
    <div className="flex h-full">
      <aside className="flex w-56 shrink-0 flex-col border-r border-slate-800 bg-slate-950">
        <div className="flex items-center gap-2 border-b border-slate-800 px-4 py-4">
          <img src="/logo.svg" alt="Codebase Copilot" className="h-7 w-7" />
          <div>
            <div className="text-sm font-bold text-slate-100">Codebase Copilot</div>
            <div className="text-[10px] text-slate-500">AI developer platform</div>
          </div>
        </div>
        <nav className="flex-1 overflow-y-auto px-2 py-3">
          <div className="mb-1 px-2 text-[10px] font-semibold uppercase tracking-wider text-slate-600">Assistants</div>
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors ${
                  isActive ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`
              }
            >
              {l.icon}
              {l.label}
            </NavLink>
          ))}
          <div className="mb-1 mt-4 px-2 text-[10px] font-semibold uppercase tracking-wider text-slate-600">
            Explorer
          </div>
          {explorer.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors ${
                  isActive ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`
              }
            >
              {l.icon}
              {l.label}
            </NavLink>
          ))}
          <div className="mb-1 mt-4 px-2 text-[10px] font-semibold uppercase tracking-wider text-slate-600">System</div>
          <NavLink
            to="/agent"
            className={({ isActive }) =>
              `flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors ${
                isActive ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`
            }
          >
            <Bot className={iconCls} />
            Agent
          </NavLink>
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              `flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors ${
                isActive ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`
            }
          >
            <Settings className={iconCls} />
            Settings
          </NavLink>
        </nav>
        <div className="border-t border-slate-800 p-3">
          <div className="mb-2 flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-sky-600 text-xs font-bold text-white">
              {user?.username?.[0]?.toUpperCase() ?? '?'}
            </div>
            <div className="min-w-0">
              <div className="truncate text-xs font-medium text-slate-200">{user?.username}</div>
              <div className="flex items-center gap-1 text-[10px] text-slate-500">
                <Wand2 className="h-3 w-3" />
                {user?.roles}
              </div>
            </div>
          </div>
          <button
            onClick={() => {
              logout()
              navigate('/login')
            }}
            className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-xs text-slate-400 hover:bg-slate-800 hover:text-slate-200"
          >
            <LogOut className="h-3.5 w-3.5" />
            Sign out
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}