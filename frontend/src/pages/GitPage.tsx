import { useState } from 'react'
import { useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { GitCommitDto, GitDiffDto } from '../lib/types'
import { Badge, Button, Card, Empty, ErrorText, inputCls } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

export default function GitPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [repoId, setRepoId] = useState<number | ''>('')
  const [commits, setCommits] = useState<GitCommitDto[] | null>(null)
  const [diff, setDiff] = useState<GitDiffDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [path, setPath] = useState('')

  async function loadCommits(rid: number) {
    setError(null)
    try {
      const q = path ? `?path=${encodeURIComponent(path)}` : ''
      setCommits(await unwrap(api.get(`/projects/${pid}/git/repositories/${rid}/commits${q}`)))
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  async function loadDiff(rid: number, commitId: string) {
    setError(null)
    try {
      setDiff(null)
      setDiff(await unwrap(api.get(`/projects/${pid}/git/repositories/${rid}/commits/${commitId}/diff`)))
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto max-w-6xl p-6">
        <div className="mb-4 flex items-end gap-3">
          <h1 className="mr-2 text-lg font-bold text-slate-100">Git History</h1>
          <input
            className={`${inputCls} max-w-[220px]`}
            value={repoId === '' ? '' : String(repoId)}
            onChange={(e) => setRepoId(e.target.value === '' ? '' : Number(e.target.value))}
            placeholder="Repository id"
          />
          <input
            className={`${inputCls} max-w-[260px]`}
            value={path}
            onChange={(e) => setPath(e.target.value)}
            placeholder="Filter by path (optional)"
          />
          <Button onClick={() => repoId && loadCommits(repoId)} disabled={!repoId}>
            Load commits
          </Button>
        </div>
        <ErrorText>{error}</ErrorText>
        <div className="grid gap-4 lg:grid-cols-[minmax(280px,1fr)_2fr]">
          <Card className="max-h-[70vh] overflow-y-auto">
            {!commits ? (
              <Empty text="Load commits to see history." />
            ) : (
              <div className="divide-y divide-slate-800">
                {commits.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => repoId && loadDiff(Number(repoId), c.id)}
                    className="block w-full px-4 py-2.5 text-left hover:bg-slate-800/60"
                  >
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs text-slate-500">{c.id.slice(0, 7)}</span>
                      <Badge color="slate">{c.author}</Badge>
                    </div>
                    <div className="mt-1 text-sm text-slate-200">{c.shortMessage}</div>
                    <div className="mt-0.5 text-[11px] text-slate-500">{new Date(c.date).toLocaleString()}</div>
                  </button>
                ))}
              </div>
            )}
          </Card>
          <Card className="max-h-[70vh] overflow-hidden">
            {!diff ? (
              <Empty text="Select a commit to view its diff." />
            ) : (
              <>
                <div className="border-b border-slate-800 px-4 py-2">
                  <span className="font-mono text-xs text-slate-400">{diff.commitId.slice(0, 7)}</span>
                  {diff.changes.length > 0 && (
                    <span className="ml-3 text-xs text-slate-500">
                      {diff.changes.length} files
                    </span>
                  )}
                </div>
                <Editor
                  height="60vh"
                  language="diff"
                  value={diff.diff}
                  theme="vs-dark"
                  options={{ readOnly: true, minimap: { enabled: false }, fontSize: 12 }}
                />
              </>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}