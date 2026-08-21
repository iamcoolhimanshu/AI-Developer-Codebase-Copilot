import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { FileText } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { GeneratedDocument, RepositoryDto } from '../lib/types'
import { Button, Card, CardHeader, ErrorText, Field, inputCls, Spinner } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

const TYPES = ['README', 'ARCHITECTURE', 'API_GUIDE', 'CONTRIBUTING', 'SETUP']

export default function DocsPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [repos, setRepos] = useState<RepositoryDto[] | null>(null)
  const [repoId, setRepoId] = useState('')
  const [type, setType] = useState('README')
  const [doc, setDoc] = useState<GeneratedDocument | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!pid) return
    unwrap(api.get(`/projects/${pid}/repositories`))
      .then(setRepos)
      .catch((e) => setError(errorMessage(e)))
  }, [pid])

  async function generate() {
    if (!repoId) return
    setBusy(true)
    setError(null)
    setDoc(null)
    try {
      setDoc(await unwrap(api.post(`/projects/${pid}/documentation?repositoryId=${repoId}&type=${type}`)))
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
        <h1 className="mb-1 text-lg font-bold text-slate-100">Documentation Generator</h1>
        <p className="mb-4 text-sm text-slate-500">Generate accurate project documentation from the indexed code — classes, endpoints, and dependencies.</p>

        <div className="mb-4 grid gap-3 rounded-2xl border border-slate-800 bg-slate-900/60 p-4 sm:grid-cols-[2fr_1fr_auto]">
          <Field label="Repository">
            <select className={inputCls} value={repoId} onChange={(e) => setRepoId(e.target.value)}>
              <option value="">Select…</option>
              {(repos ?? []).map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Document type">
            <select className={inputCls} value={type} onChange={(e) => setType(e.target.value)}>
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </Field>
          <div className="flex items-end">
            <Button onClick={generate} disabled={busy || !repoId}>
              <FileText className="h-4 w-4" />
              {busy ? 'Generating…' : 'Generate'}
            </Button>
          </div>
        </div>

        <ErrorText>{error}</ErrorText>

        {busy && <Spinner label="Writing documentation…" />}
        {doc && (
          <Card>
            <CardHeader title={doc.fileName} subtitle={doc.contentType} />
            <div className="p-2">
              <Editor
                height="70vh"
                language="markdown"
                value={doc.content}
                theme="vs-dark"
                options={{ readOnly: true, minimap: { enabled: false }, fontSize: 12 }}
              />
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}