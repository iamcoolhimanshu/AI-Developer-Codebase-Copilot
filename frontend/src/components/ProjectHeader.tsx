import { useEffect, useState } from 'react'
import { NavLink, useParams } from 'react-router-dom'
import api, { unwrap } from '../lib/api'
import type { ProjectDto } from '../lib/types'

const tabs = [
  { to: '', label: 'Overview', end: true },
  { to: 'code', label: 'Code' },
  { to: 'search', label: 'Search' },
  { to: 'chat', label: 'AI Copilot' },
  { to: 'architecture', label: 'Architecture' },
  { to: 'apis', label: 'API Endpoints' },
  { to: 'git', label: 'Git' },
  { to: 'bug', label: 'Debug' },
  { to: 'review', label: 'Review' },
  { to: 'tests', label: 'Tests' },
  { to: 'docs', label: 'Docs' },
  { to: 'patches', label: 'Patches' },
  { to: 'agent', label: 'Agent' },
]

export default function ProjectHeader() {
  const { projectId } = useParams()
  const [project, setProject] = useState<ProjectDto | null>(null)

  useEffect(() => {
    if (!projectId) return
    unwrap(api.get(`/projects/${projectId}`))
      .then(setProject)
      .catch(() => undefined)
  }, [projectId])

  return (
    <div className="border-b border-slate-800 bg-slate-950/60 px-6 pb-0 pt-4">
      <div className="mb-3 flex items-center gap-3">
        <h1 className="text-lg font-bold text-slate-100">{project?.name ?? 'Project'}</h1>
        {project?.description && <span className="truncate text-sm text-slate-500">{project.description}</span>}
      </div>
      <div className="flex gap-1 overflow-x-auto pb-2">
        {tabs.map((t) => (
          <NavLink
            key={t.to}
            to={t.to === '' ? `/projects/${projectId}` : `/projects/${projectId}/${t.to}`}
            end={t.end}
            className={({ isActive }) =>
              `whitespace-nowrap rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                isActive ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`
            }
          >
            {t.label}
          </NavLink>
        ))}
      </div>
    </div>
  )
}