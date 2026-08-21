import { useEffect, useState } from 'react'
import { useAuth } from '../lib/auth'
import api, { unwrap } from '../lib/api'
import type { MetricsSnapshot } from '../lib/types'
import { Badge, Card, CardHeader, Empty, ErrorText } from '../components/ui'

export default function SettingsPage() {
  const { user } = useAuth()
  const [metrics, setMetrics] = useState<MetricsSnapshot | null>(null)
  const [error] = useState<string | null>(null)

  useEffect(() => {
    unwrap(api.get('/admin/metrics'))
      .then(setMetrics)
      .catch(() => undefined)
  }, [])

  return (
    <div className="mx-auto max-w-4xl p-6">
      <h1 className="mb-4 text-lg font-bold text-slate-100">Settings</h1>

      <Card className="mb-4">
        <CardHeader title="Account" />
        <div className="grid gap-3 p-4 sm:grid-cols-2">
          <div>
            <div className="text-xs text-slate-500">Username</div>
            <div className="text-sm text-slate-200">{user?.username}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">Email</div>
            <div className="text-sm text-slate-200">{user?.email}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">Roles</div>
            <div className="mt-1">
              <Badge color="sky">{user?.roles}</Badge>
            </div>
          </div>
        </div>
      </Card>

      <Card className="mb-4">
        <CardHeader title="Usage & observability" subtitle="In-memory counters (reset on restart)" />
        {!metrics ? (
          <Empty text="Metrics available to admin users only." />
        ) : (
          <div className="grid grid-cols-2 gap-3 p-4 sm:grid-cols-4">
            <div className="rounded-lg bg-slate-950 p-3">
              <div className="text-xl font-bold text-slate-100">{metrics.aiRequests}</div>
              <div className="text-xs text-slate-500">AI requests</div>
            </div>
            <div className="rounded-lg bg-slate-950 p-3">
              <div className="text-xl font-bold text-slate-100">{metrics.aiTokens}</div>
              <div className="text-xs text-slate-500">Tokens</div>
            </div>
            <div className="rounded-lg bg-slate-950 p-3">
              <div className="text-xl font-bold text-slate-100">{metrics.toolCalls}</div>
              <div className="text-xs text-slate-500">Tool calls</div>
            </div>
            <div className="rounded-lg bg-slate-950 p-3">
              <div className="text-xl font-bold text-slate-100">{metrics.indexingRuns}</div>
              <div className="text-xs text-slate-500">Indexing runs</div>
            </div>
          </div>
        )}
      </Card>

      <Card>
        <CardHeader title="What is stored" />
        <div className="space-y-2 p-4 text-sm text-slate-400">
          <p>
            Cloned repositories live under <code className="rounded bg-slate-950 px-1 py-0.5 text-xs">app.storage.root</code> (default{' '}
            <code className="rounded bg-slate-950 px-1 py-0.5 text-xs">./data/repos</code>). Indexed metadata (files, classes, methods,
            dependencies, chunks, embeddings) lives in the database.
          </p>
          <p>AI features call the Grok (xAI) API only when <code className="rounded bg-slate-950 px-1 py-0.5 text-xs">XAI_API_KEY</code> is set; otherwise the
            app runs offline with stub embeddings and clear “AI not configured” messages.</p>
          <p>Every AI tool call and patch action is written to the audit log.</p>
        </div>
      </Card>
      <ErrorText>{error}</ErrorText>
    </div>
  )
}