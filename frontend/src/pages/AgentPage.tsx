import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { Bot, Wrench } from 'lucide-react'
import api, { unwrap, errorMessage } from '../lib/api'
import type { AgentResponse, ToolDefinition } from '../lib/types'
import { Button, Card, CardHeader, Empty, ErrorText, Field, Spinner, TextArea } from '../components/ui'
import Markdown from '../components/Markdown'
import ProjectHeader from '../components/ProjectHeader'

export default function AgentPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [prompt, setPrompt] = useState('')
  const [tools, setTools] = useState<ToolDefinition[] | null>(null)
  const [result, setResult] = useState<AgentResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!pid) return
    unwrap(api.get(`/projects/${pid}/tools/definitions`))
      .then(setTools)
      .catch(() => undefined)
  }, [pid])

  async function run(e: FormEvent) {
    e.preventDefault()
    if (!prompt.trim()) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      setResult(await unwrap(api.post(`/projects/${pid}/agent/run`, { prompt: prompt.trim() })))
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
        <h1 className="mb-1 text-lg font-bold text-slate-100">Autonomous Agent</h1>
        <p className="mb-4 text-sm text-slate-500">
          The agent explores the codebase with read-only tools to answer complex questions.
        </p>

        <form onSubmit={run} className="mb-4">
          <Field label="Task or question">
            <TextArea
              rows={4}
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="e.g. Find why customer signup is slow, locate the root service, and explain the call chain."
            />
          </Field>
          <Button type="submit" disabled={busy || !prompt.trim()} className="mt-2">
            <Bot className="h-4 w-4" />
            {busy ? 'Running…' : 'Run agent'}
          </Button>
        </form>

        <ErrorText>{error}</ErrorText>

        {result && (
          <Card className="mb-6">
            <CardHeader title="Answer" />
            <div className="p-4">
              <Markdown content={result.answer} />
            </div>
            {result.toolCalls.length > 0 && (
              <>
                <div className="border-t border-slate-800 px-4 py-2 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                  Tool calls
                </div>
                <div className="space-y-1 px-4 pb-4 pt-1">
                  {result.toolCalls.map((t, i) => (
                    <div key={i} className="rounded-lg bg-slate-950 px-3 py-1.5 font-mono text-[11px] text-slate-400">
                      {t}
                    </div>
                  ))}
                </div>
              </>
            )}
          </Card>
        )}

        <Card>
          <CardHeader title="Available tools" subtitle="Read-only, project-scoped, fully audited" />
          {!tools ? (
            <Spinner />
          ) : tools.length === 0 ? (
            <Empty text="No tools registered." />
          ) : (
            <div className="divide-y divide-slate-800">
              {tools.map((t) => (
                <div key={t.name} className="flex items-start gap-3 px-4 py-2.5">
                  <Wrench className="mt-0.5 h-4 w-4 shrink-0 text-sky-400" />
                  <div>
                    <div className="font-mono text-sm text-slate-200">{t.name}</div>
                    <div className="text-xs text-slate-500">{t.description}</div>
                    {t.parameters && (
                      <pre className="mt-1 overflow-x-auto rounded-md bg-slate-950 p-2 text-[10px] text-slate-400">
                        {t.parameters}
                      </pre>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}