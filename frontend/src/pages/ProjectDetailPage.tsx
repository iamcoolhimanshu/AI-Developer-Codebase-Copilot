import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Globe, Package, RefreshCw, Trash2, Upload } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type {
  IndexStatusDto,
  ProjectDashboardDto,
  RepositoryDto,
} from '../lib/types'
import {
  Badge,
  Button,
  Card,
  CardHeader,
  Empty,
  ErrorText,
  Field,
  inputCls,
  Spinner,
} from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

const statusColor: Record<string, string> = {
  CLONED: 'sky',
  INDEXING: 'amber',
  PENDING: 'amber',
  RUNNING: 'amber',
  COMPLETED: 'green',
  FAILED: 'red',
  ERROR: 'red',
}

export default function ProjectDetailPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [dash, setDash] = useState<ProjectDashboardDto | null>(null)
  const [repos, setRepos] = useState<RepositoryDto[] | null>(null)
  const [status, setStatus] = useState<Record<number, IndexStatusDto>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [url, setUrl] = useState('')
  const [githubToken, setGithubToken] = useState('')
  const timer = useRef<ReturnType<typeof setInterval> | null>(null)

  async function load() {
    if (!pid) return
    try {
      const [d, r] = await Promise.all([
        unwrap(api.get(`/projects/${pid}/dashboard`)),
        unwrap(api.get(`/projects/${pid}/repositories`)),
      ])
      setDash(d)
      setRepos(r)
      for (const repo of r) {
        refreshStatus(repo.id)
      }
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function refreshStatus(repoId: number) {
    try {
      const s = await unwrap(api.get(`/repositories/${repoId}/index/status?projectId=${pid}`))
      if (s) setStatus((m) => ({ ...m, [repoId]: s }))
    } catch {
      /* no job yet */
    }
  }

  useEffect(() => {
    load()
    return () => {
      if (timer.current) clearInterval(timer.current)
    }
  }, [pid])

  function poll(repoId: number) {
    if (timer.current) clearInterval(timer.current)
    timer.current = setInterval(() => {
      refreshStatus(repoId)
    }, 4000)
  }

  async function connect() {
    if (!url.trim()) return
    setBusy(true)
    setError(null)
    try {
      const headers = githubToken.trim() ? { 'X-GitHub-Token': githubToken.trim() } : undefined
      const repo = await unwrap(
        api.post(`/projects/${pid}/repositories`, { provider: 'GITHUB', url: url.trim() }, { headers }),
      )
      setUrl('')
      setGithubToken('')
      await load()
      poll(repo.id)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function upload(file: File | null) {
    if (!file) return
    setBusy(true)
    setError(null)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const repo = await unwrap(api.post(`/projects/${pid}/repositories/upload`, fd))
      await load()
      poll(repo.id)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function startIndex(repoId: number) {
    setError(null)
    try {
      await api.post(`/repositories/${repoId}/index?projectId=${pid}`)
      await refreshStatus(repoId)
      poll(repoId)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(repoId: number) {
    if (!confirm('Delete this repository and all indexed data?')) return
    try {
      await api.delete(`/repositories/${repoId}?projectId=${pid}`)
      setRepos((r) => r?.filter((x) => x.id !== repoId) ?? null)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const stats = dash
    ? [
        { label: 'Files', value: dash.fileCount },
        { label: 'Classes', value: dash.classCount },
        { label: 'Methods', value: dash.methodCount },
        { label: 'Dependencies', value: dash.dependencyCount },
        { label: 'API endpoints', value: dash.apiEndpointCount },
        { label: 'Indexed repos', value: dash.indexedRepositoryCount },
      ]
    : []

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-6xl p-6">
        <ErrorText>{error}</ErrorText>

        {!dash ? (
          <Spinner label="Loading…" />
        ) : (
          <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {stats.map((s) => (
              <Card key={s.label} className="p-4">
                <div className="text-2xl font-bold text-slate-100">{s.value}</div>
                <div className="text-xs text-slate-500">{s.label}</div>
              </Card>
            ))}
          </div>
        )}

        <Card className="mb-6">
          <CardHeader title="Connect a repository" subtitle="Clone from GitHub or upload a ZIP of your source" />
          <div className="space-y-4 p-4">
            <div className="grid gap-3 lg:grid-cols-[2fr_2fr_auto]">
              <Field label="Repository URL">
                <input
                  className={inputCls}
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  placeholder="https://github.com/org/repo"
                />
              </Field>
              <Field label="GitHub token (optional, for private repos)">
                <input
                  className={inputCls}
                  type="password"
                  value={githubToken}
                  onChange={(e) => setGithubToken(e.target.value)}
                  placeholder="ghp_…"
                />
              </Field>
              <div className="flex items-end">
                <Button onClick={connect} disabled={busy}>
                  <Globe className="h-4 w-4" />
                  Connect & clone
                </Button>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <label className="cursor-pointer">
                <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-700">
                  <Upload className="h-4 w-4" />
                  Upload ZIP
                </span>
                <input
                  type="file"
                  accept=".zip"
                  className="hidden"
                  onChange={(e) => upload(e.target.files?.[0] ?? null)}
                />
              </label>
              <span className="text-xs text-slate-500">A .zip of the codebase up to 200 MB</span>
            </div>
          </div>
        </Card>

        <Card>
          <CardHeader title="Repositories" subtitle="Indexed repositories in this project" />
          {!repos ? (
            <Spinner />
          ) : repos.length === 0 ? (
            <Empty text="No repositories connected yet." />
          ) : (
            <div className="divide-y divide-slate-800">
              {repos.map((repo) => {
                const s = status[repo.id]
                return (
                  <div key={repo.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        {repo.provider === 'GITHUB' ? (
                          <Globe className="h-4 w-4 shrink-0 text-slate-500" />
                        ) : (
                          <Package className="h-4 w-4 shrink-0 text-slate-500" />
                        )}
                        <span className="truncate font-medium text-slate-200">{repo.name}</span>
                        <Badge color={statusColor[repo.status] ?? 'slate'}>{repo.status}</Badge>
                        {repo.branch && <span className="text-xs text-slate-500">@{repo.branch}</span>}
                      </div>
                      <div className="mt-1 text-xs text-slate-500">
                        {repo.indexedFileCount} files indexed
                        {s?.status === 'COMPLETED' && s.phase ? ` · ${s.phase} (${s.progress}%)` : ''}
                        {s?.status === 'FAILED' ? ` · error: ${s.error ?? 'unknown'}` : ''}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {s?.status === 'COMPLETED' && s.phase ? (
                        <Badge color="green">{s.phase} {s.progress}%</Badge>
                      ) : s?.status === 'FAILED' ? (
                        <Badge color="red">failed</Badge>
                      ) : null}
                      <Button variant="secondary" onClick={() => startIndex(repo.id)}>
                        <RefreshCw className="h-3.5 w-3.5" />
                        Re-index
                      </Button>
                      <Button variant="danger" onClick={() => remove(repo.id)}>
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}