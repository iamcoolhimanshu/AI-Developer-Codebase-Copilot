import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FolderGit2, Plus } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { ProjectDto } from '../lib/types'
import { Badge, Button, Card, CardHeader, Empty, ErrorText, Field, inputCls, Spinner } from '../components/ui'

export default function DashboardPage() {
  const [projects, setProjects] = useState<ProjectDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  async function load() {
    try {
      setProjects(await unwrap(api.get('/projects')))
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function create() {
    if (!name.trim()) return
    setCreating(true)
    setError(null)
    try {
      const p = await unwrap(api.post<ProjectDto>('/projects', { name: name.trim(), description: description.trim() }))
      setName('')
      setDescription('')
      await load()
      window.location.href = `/projects/${p.id}`
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl p-6">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-slate-100">Projects</h1>
        <p className="text-sm text-slate-500">Connect repositories and let the copilot index them</p>
      </div>

      <Card className="mb-6">
        <CardHeader title="New project" />
        <div className="grid gap-3 p-4 sm:grid-cols-[1fr_2fr_auto]">
          <Field label="Name">
            <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} placeholder="My service" />
          </Field>
          <Field label="Description">
            <input
              className={inputCls}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description"
            />
          </Field>
          <div className="flex items-end">
            <Button onClick={create} disabled={creating || !name.trim()}>
              <Plus className="h-4 w-4" />
              Create
            </Button>
          </div>
        </div>
      </Card>

      <ErrorText>{error}</ErrorText>

      {!projects ? (
        <Spinner label="Loading projects…" />
      ) : projects.length === 0 ? (
        <Empty text="No projects yet — create one above to get started." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p) => (
            <Link key={p.id} to={`/projects/${p.id}`} className="block">
              <Card className="h-full transition-colors hover:border-sky-700">
                <div className="p-4">
                  <div className="mb-2 flex items-start justify-between">
                    <FolderGit2 className="h-5 w-5 text-sky-400" />
                    <Badge color="slate">{p.memberCount ?? 1} member(s)</Badge>
                  </div>
                  <h3 className="font-semibold text-slate-100">{p.name}</h3>
                  <p className="mt-1 line-clamp-2 text-sm text-slate-500">{p.description || 'No description'}</p>
                  <button className="mt-3 text-xs font-medium text-sky-400 hover:text-sky-300">Open project →</button>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}