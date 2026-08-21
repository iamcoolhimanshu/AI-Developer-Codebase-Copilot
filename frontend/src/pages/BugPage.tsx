import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { Bug } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { BugAnalysisResponse } from '../lib/types'
import { Badge, Button, Card, CardHeader, ErrorText, Field, inputCls, Spinner, TextArea } from '../components/ui'
import Markdown from '../components/Markdown'
import ProjectHeader from '../components/ProjectHeader'

export default function BugPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [errorMessageText, setErrorMessageText] = useState('')
  const [stackTrace, setStackTrace] = useState('')
  const [filePath, setFilePath] = useState('')
  const [result, setResult] = useState<BugAnalysisResponse | null>(null)
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
          api.post(`/projects/${pid}/bug-analysis`, {
            errorMessage: errorMessageText,
            stackTrace: stackTrace || undefined,
            filePath: filePath || undefined,
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
        <h1 className="mb-1 text-lg font-bold text-slate-100">Bug Investigator</h1>
        <p className="mb-4 text-sm text-slate-500">
          Paste an error message or stack trace — the AI locates the offending code, traces the cause with git history, and proposes a fix.
        </p>

        <form onSubmit={run} className="space-y-3 rounded-2xl border border-slate-800 bg-slate-900/60 p-4">
          <Field label="Error message">
            <input
              className={inputCls}
              value={errorMessageText}
              onChange={(e) => setErrorMessageText(e.target.value)}
              placeholder="NullPointerException: Cannot invoke … because … is null"
              required
            />
          </Field>
          <Field label="Stack trace" hint="Optional — the more lines, the better the localization">
            <TextArea
              rows={8}
              value={stackTrace}
              onChange={(e) => setStackTrace(e.target.value)}
              placeholder={'at com.example.OrderService.createOrder(OrderService.java:42)\nat com.example.OrderController.create(OrderController.java:18)\n…'}
            />
          </Field>
          <Field label="File path (optional)">
            <input
              className={inputCls}
              value={filePath}
              onChange={(e) => setFilePath(e.target.value)}
              placeholder="src/main/java/…/OrderService.java"
            />
          </Field>
          <Button type="submit" disabled={busy}>
            <Bug className="h-4 w-4" />
            {busy ? 'Investigating…' : 'Investigate'}
          </Button>
        </form>

        <ErrorText>{error}</ErrorText>

        <div className="mt-4">
          {busy && <Spinner label="Analyzing stack trace with Grok…" />}
          {result && (
            <Card>
              <CardHeader
                title="Findings"
                right={<Badge color={result.confidence >= 0.7 ? 'green' : result.confidence >= 0.4 ? 'amber' : 'slate'}>{(result.confidence * 100).toFixed(0)}% confidence</Badge>}
              />
              <div className="space-y-4 p-4">
                <div>
                  <div className="font-mono text-sm text-rose-300">{result.errorMessage}</div>
                  {result.filePath && (
                    <div className="mt-1 font-mono text-xs text-slate-400">
                      {result.filePath}
                      {result.lineNumber ? `:${result.lineNumber}` : ''}
                    </div>
                  )}
                </div>
                <div>
                  <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-slate-500">Root cause</div>
                  <Markdown content={result.rootCause} />
                </div>
                {result.explanation && (
                  <div>
                    <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-slate-500">Analysis</div>
                    <Markdown content={result.explanation} />
                  </div>
                )}
                {result.suggestedFix && (
                  <div>
                    <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-slate-500">Suggested fix</div>
                    <Markdown content={result.suggestedFix} />
                  </div>
                )}
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}