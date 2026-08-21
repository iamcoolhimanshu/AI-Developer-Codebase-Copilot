import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api, { errorMessage, unwrap } from '../lib/api'
import type { ArchitectureGraph } from '../lib/types'
import ArchGraph from '../components/ArchGraph'
import { Badge, ErrorText, Spinner } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

export default function ArchitecturePage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [graph, setGraph] = useState<ArchitectureGraph | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!pid) return
    unwrap(api.get(`/projects/${pid}/architecture`))
      .then(setGraph)
      .catch((e) => setError(errorMessage(e)))
  }, [pid])

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-[1400px] p-6">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <h1 className="mr-2 text-lg font-bold text-slate-100">Architecture</h1>
          {graph && (
            <>
              <Badge>{graph.nodes.length} types</Badge>
              <Badge color="sky">{graph.edges.length} dependencies</Badge>
              {Object.entries(graph.stereotypes).map(([k, v]) => (
                <Badge key={k} color="slate">
                  {k} × {v}
                </Badge>
              ))}
            </>
          )}
        </div>
        <ErrorText>{error}</ErrorText>
        {!graph ? <Spinner label="Building graph…" /> : <ArchGraph graph={graph} />}
      </div>
    </div>
  )
}