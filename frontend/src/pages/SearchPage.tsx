import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Search } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { SearchResultDto } from '../lib/types'
import { Badge, Card, CardHeader, Empty, ErrorText, inputCls, Spinner } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

function highlight(snippet: string, query: string) {
  if (!query || !snippet) return snippet
  const parts = snippet.split(new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'))
  return parts.map((p, i) =>
    p.toLowerCase() === query.toLowerCase() ? (
      <mark key={i} className="rounded bg-amber-900/60 px-0.5 text-amber-200">
        {p}
      </mark>
    ) : (
      <span key={i}>{p}</span>
    ),
  )
}

export default function SearchPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResultDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [searching, setSearching] = useState(false)

  const search = useCallback(async () => {
    if (!query.trim()) return
    setSearching(true)
    setError(null)
    try {
      setResults(await unwrap(api.get(`/projects/${pid}/search?query=${encodeURIComponent(query.trim())}`)))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setSearching(false)
    }
  }, [pid, query])

  useEffect(() => {
    const t = setTimeout(search, 300)
    return () => clearTimeout(t)
  }, [query, search])

  const matchColor: Record<string, string> = {
    SYMBOL: 'green',
    KEYWORD: 'slate',
    FILE: 'sky',
    API: 'amber',
    FILTER: 'violet',
    VECTOR: 'sky',
  }

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-5xl p-6">
        <h1 className="mb-4 text-lg font-bold text-slate-100">Search Codebase</h1>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            className={`${inputCls} pl-9 py-2.5`}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search classes, methods, symbols, or describe what you need…"
            autoFocus
          />
        </div>
        <ErrorText>{error}</ErrorText>
        <div className="mt-4">
          {searching ? (
            <Spinner label="Searching…" />
          ) : results === null ? (
            <Empty text="Type to search across all indexed repositories." />
          ) : results.length === 0 ? (
            <Empty text="No results." />
          ) : (
            <Card>
              <CardHeader title={`${results.length} results`} />
              <div className="divide-y divide-slate-800">
                {results.map((r, i) => (
                  <div key={i} className="px-4 py-3">
                    <div className="mb-1 flex flex-wrap items-center gap-2">
                      <Badge color={matchColor[r.matchType] ?? 'slate'}>{r.matchType}</Badge>
                      {r.className && <span className="text-sm font-medium text-slate-200">{r.className}</span>}
                      {r.methodName && <span className="text-sm text-slate-400">.{r.methodName}()</span>}
                      <span className="text-xs text-slate-500">
                        L{r.startLine}–{r.endLine}
                      </span>
                      <span className="ml-auto text-xs text-slate-500">score {r.score.toFixed(2)}</span>
                    </div>
                    <div className="font-mono text-xs text-slate-500">{r.filePath}</div>
                    <pre className="mt-1 overflow-x-auto rounded-lg bg-slate-950 p-2 text-xs leading-relaxed text-slate-300">
                      {highlight(r.snippet, query)}
                    </pre>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}