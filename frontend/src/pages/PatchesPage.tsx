import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { CheckCircle2, Hammer, Play, XCircle } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { GeneratedPatch, RepositoryDto } from '../lib/types'
import { Badge, Button, Card, CardHeader, Empty, ErrorText, Field, inputCls, Spinner, TextArea } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

const statusColor: Record<string, string> = { PENDING: 'amber', APPROVED: 'sky', APPLIED: 'green', REJECTED: 'red' }

export default function PatchesPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [repos, setRepos] = useState<RepositoryDto[] | null>(null)
  const [repoId, setRepoId] = useState('')
  const [instruction, setInstruction] = useState('')
  const [patches, setPatches] = useState<GeneratedPatch[] | null>(null)
  const [expanded, setExpanded] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function load() {
    try {
      setPatches(await unwrap(api.get(`/projects/${pid}/patches`)))
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  useEffect(() => {
    if (!pid) return
    load()
    unwrap(api.get(`/projects/${pid}/repositories`))
      .then(setRepos)
      .catch(() => undefined)
  }, [pid])

  async function generate(e: FormEvent) {
    e.preventDefault()
    if (!repoId || !instruction.trim()) return
    setBusy(true)
    setError(null)
    try {
      const p = await unwrap(
        api.post(`/projects/${pid}/patches`, { instruction: instruction.trim(), repositoryId: Number(repoId) }),
      )
      setInstruction('')
      setExpanded(p.id)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function act(patchId: number, action: 'approve' | 'apply' | 'reject') {
    setError(null)
    try {
      await api.post(`/projects/${pid}/patches/${patchId}/${action}`)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-5xl p-6">
        <h1 className="mb-1 text-lg font-bold text-slate-100">AI Patch Generation</h1>
        <p className="mb-4 text-sm text-slate-500">
          Describe a change in plain language. The AI produces a git diff against your repository — you review, approve, and apply it safely.
        </p>

        <form onSubmit={generate} className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/60 p-4">
          <div className="mb-3 grid gap-3 sm:grid-cols-[1fr_2fr]">
            <Field label="Repository">
              <select className={inputCls} value={repoId} onChange={(e) => setRepoId(e.target.value)} required>
                <option value="">Select…</option>
                {(repos ?? []).map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Instruction" hint="Be specific: file, class, method, expected behavior">
              <TextArea
                rows={2}
                value={instruction}
                onChange={(e) => setInstruction(e.target.value)}
                placeholder="Add input validation to OrderService.create() that rejects negative quantities with a BadRequestException"
                required
              />
            </Field>
          </div>
          <Button type="submit" disabled={busy}>
            <Hammer className="h-4 w-4" />
            {busy ? 'Generating…' : 'Generate patch'}
          </Button>
        </form>

        <ErrorText>{error}</ErrorText>

        {!patches ? (
          <Spinner />
        ) : patches.length === 0 ? (
          <Empty text="No patches generated yet." />
        ) : (
          <div className="space-y-4">
            {patches.map((p) => (
              <Card key={p.id}>
                <CardHeader
                  title={p.instruction}
                  right={<Badge color={statusColor[p.status] ?? 'slate'}>{p.status}</Badge>}
                />
                <div className="px-4 py-2 text-xs text-slate-500">
                  repo #{p.repositoryId} · created {new Date(p.createdAt).toLocaleString()}
                  {p.appliedAt ? ` · applied ${new Date(p.appliedAt).toLocaleString()}` : ''}
                </div>
                <div className="px-4 pb-3">
                  <button
                    onClick={() => setExpanded(expanded === p.id ? null : p.id)}
                    className="mb-2 text-xs font-medium text-sky-400 hover:text-sky-300"
                  >
                    {expanded === p.id ? 'Hide diff' : 'Show diff'}
                  </button>
                  {expanded === p.id && (
                    <pre className="max-h-96 overflow-auto rounded-lg bg-slate-950 p-3 font-mono text-xs leading-relaxed text-slate-300">
                      {p.diff}
                    </pre>
                  )}
                </div>
                <div className="flex gap-2 border-t border-slate-800 px-4 py-3">
                  {p.status === 'PENDING' && (
                    <>
                      <Button variant="success" onClick={() => act(p.id, 'approve')}>
                        <CheckCircle2 className="h-4 w-4" />
                        Approve
                      </Button>
                      <Button variant="danger" onClick={() => act(p.id, 'reject')}>
                        <XCircle className="h-4 w-4" />
                        Reject
                      </Button>
                    </>
                  )}
                  {p.status === 'APPROVED' && (
                    <Button variant="success" onClick={() => act(p.id, 'apply')}>
                      <Play className="h-4 w-4" />
                      Apply to repository
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}