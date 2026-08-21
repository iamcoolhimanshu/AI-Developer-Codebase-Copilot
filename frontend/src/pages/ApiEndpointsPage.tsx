import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api, { errorMessage, unwrap } from '../lib/api'
import type { CodeMethodDto } from '../lib/types'
import { Badge, Card, CardHeader, Empty, ErrorText, Spinner } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

const methodColor = (m: string) =>
  m === 'GET' ? 'green' : m === 'POST' ? 'sky' : m === 'PUT' || m === 'PATCH' ? 'amber' : m === 'DELETE' ? 'red' : 'slate'

export default function ApiEndpointsPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [endpoints, setEndpoints] = useState<CodeMethodDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!pid) return
    unwrap(api.get(`/projects/${pid}/api-endpoints`))
      .then(setEndpoints)
      .catch((e) => setError(errorMessage(e)))
  }, [pid])

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-5xl p-6">
        <h1 className="mb-4 text-lg font-bold text-slate-100">API Endpoints</h1>
        <ErrorText>{error}</ErrorText>
        {!endpoints ? (
          <Spinner />
        ) : endpoints.length === 0 ? (
          <Empty text="No REST endpoints detected. Index a Java/Spring repository to discover its API." />
        ) : (
          <Card>
            <CardHeader title={`${endpoints.length} endpoints discovered`} />
            <div className="divide-y divide-slate-800">
              {endpoints.map((m) => (
                <div key={m.id} className="flex items-center gap-3 px-4 py-2.5">
                  <Badge color={methodColor(m.httpMethod ?? '')}>{m.httpMethod}</Badge>
                  <code className="flex-1 font-mono text-sm text-slate-200">{m.httpPath}</code>
                  <button
                    className="text-xs text-slate-500 hover:text-sky-400"
                    title="Open in code explorer"
                  >
                    {m.name}()
                  </button>
                </div>
              ))}
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}