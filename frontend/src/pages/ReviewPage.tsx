import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { ReviewFinding, ReviewResponse } from '../lib/types'
import { Badge, Button, Card, CardHeader, ErrorText, Field, inputCls, Spinner, TextArea } from '../components/ui'
import Markdown from '../components/Markdown'
import ProjectHeader from '../components/ProjectHeader'

const severityColor: Record<string, string> = { CRITICAL: 'red', HIGH: 'red', MEDIUM: 'amber', LOW: 'slate', INFO: 'sky' }

export default function ReviewPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [targetType, setTargetType] = useState('FILE')
  const [filePath, setFilePath] = useState('')
  const [commitId, setCommitId] = useState('')
  const [diff, setDiffText] = useState('')
  const [repositoryId, setRepositoryId] = useState('')
  const [result, setResult] = useState<ReviewResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function run(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      setResult(
        await unwrap(
          api.post(`/projects/${pid}/code-review`, {
            targetType,
            filePath: filePath || undefined,
            commitId: commitId || undefined,
            diff: diff || undefined,
            repositoryId: repositoryId ? Number(repositoryId) : undefined,
          }),
        ),
      )
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-4xl p-6">
        <h1 className="mb-1 text-lg font-bold text-slate-100">Code Review</h1>
        <p className="mb-4 text-sm text-slate-500">AI-powered review with severity-ranked findings, grounded in the indexed codebase.</p>

        <form onSubmit={run} className="space-y-3 rounded-2xl border border-slate-800 bg-slate-900/60 p-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Target type">
              <select className={inputCls} value={targetType} onChange={(e) => setTargetType(e.target.value)}>
                <option value="FILE">File</option>
                <option value="COMMIT">Commit</option>
                <option value="DIFF">Paste diff</option>
              </select>
            </Field>
            <Field label="Repository id" hint="Optional for FILE/DIFF">
              <input className={inputCls} value={repositoryId} onChange={(e) => setRepositoryId(e.target.value)} placeholder="1" />
            </Field>
            {targetType === 'FILE' && (
              <Field label="File path">
                <input className={inputCls} value={filePath} onChange={(e) => setFilePath(e.target.value)} placeholder="src/main/java/…/OrderService.java" required />
              </Field>
            )}
            {targetType === 'COMMIT' && (
              <Field label="Commit id">
                <input className={inputCls} value={commitId} onChange={(e) => setCommitId(e.target.value)} placeholder="a1b2c3d…" required />
              </Field>
            )}
            {targetType === 'DIFF' && (
              <Field label="Diff text">
                <TextArea rows={10} value={diff} onChange={(e) => setDiffText(e.target.value)} placeholder="diff --git a/… b/…" required />
              </Field>
            )}
          </div>
          <Button type="submit" disabled={busy}>
            <ShieldCheck className="h-4 w-4" />
            {busy ? 'Reviewing…' : 'Run review'}
          </Button>
        </form>

        <ErrorText>{error}</ErrorText>

        <div className="mt-4">
          {busy && <Spinner label="Grok is reviewing the code…" />}
          {result && (
            <div className="space-y-4">
              <Card>
                <CardHeader title="Summary" />
                <div className="p-4">
                  <Markdown content={result.summary} />
                </div>
              </Card>
              <Card>
                <CardHeader title={`Findings (${result.findings.length})`} />
                <div className="divide-y divide-slate-800">
                  {result.findings.length === 0 && <div className="px-4 py-6 text-center text-sm text-slate-500">No findings.</div>}
                  {result.findings.map((f: ReviewFinding, i) => (
                    <div key={i} className="px-4 py-3">
                      <div className="mb-1 flex flex-wrap items-center gap-2">
                        <Badge color={severityColor[f.severity] ?? 'slate'}>{f.severity}</Badge>
                        <Badge color="slate">{f.category}</Badge>
                        <span className="font-mono text-xs text-slate-400">
                          {f.filePath}
                          {f.line ? `:${f.line}` : ''}
                        </span>
                      </div>
                      <p className="text-sm text-slate-300">{f.message}</p>
                      {f.suggestion && (
                        <pre className="mt-1 overflow-x-auto rounded-lg bg-slate-950 p-2 text-xs text-slate-400">{f.suggestion}</pre>
                      )}
                    </div>
                  ))}
                </div>
              </Card>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}