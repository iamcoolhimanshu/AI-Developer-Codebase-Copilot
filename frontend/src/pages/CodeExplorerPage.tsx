import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { FileCode2, FileText, FolderTree } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { CodeClassDto, CodeMethodDto, RepositoryFileDto } from '../lib/types'
import { Badge, Card, Empty, ErrorText, Spinner } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

interface FileContent {
  path: string
  content: string
  language: string
}

export default function CodeExplorerPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [files, setFiles] = useState<RepositoryFileDto[] | null>(null)
  const [classes, setClasses] = useState<CodeClassDto[] | null>(null)
  const [selected, setSelected] = useState<RepositoryFileDto | null>(null)
  const [content, setContent] = useState<FileContent | null>(null)
  const [methods, setMethods] = useState<CodeMethodDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!pid) return
    Promise.all([
      unwrap(api.get(`/projects/${pid}/files`)),
      unwrap(api.get(`/projects/${pid}/classes`)),
    ])
      .then(([f, c]) => {
        setFiles(f)
        setClasses(c)
      })
      .catch((e) => setError(errorMessage(e)))
  }, [pid])

  const openFile = useCallback(
    async (fileId: number) => {
      setLoading(true)
      setError(null)
      try {
        const c = await unwrap(api.get(`/projects/${pid}/files/${fileId}`))
        setContent(c)
      } catch (e) {
        setError(errorMessage(e))
      } finally {
        setLoading(false)
      }
    },
    [pid],
  )

  const openClass = useCallback(async (cls: CodeClassDto) => {
    const f = { id: cls.fileId, path: cls.filePath ?? '', language: 'java' } as RepositoryFileDto
    setSelected(f)
    await openFile(cls.fileId)
    try {
      const m = await unwrap(api.get(`/projects/${pid}/classes/${cls.id}/methods`))
      setMethods(m)
    } catch {
      setMethods([])
    }
  }, [pid, openFile])

  const langOf = (p: string) =>
    p.endsWith('.java') ? 'java' : p.endsWith('.xml') ? 'xml' : p.endsWith('.yml') || p.endsWith('.yaml') ? 'yaml' : p.endsWith('.json') ? 'json' : p.endsWith('.html') ? 'html' : p.endsWith('.css') ? 'css' : p.endsWith('.ts') || p.endsWith('.tsx') ? 'typescript' : p.endsWith('.py') ? 'python' : p.endsWith('.js') ? 'javascript' : 'plaintext'

  function jumpToLine(m: CodeMethodDto) {
    setLoading(true)
    setError(null)
    unwrap(api.get(`/projects/${pid}/files/${selected?.id}`))
      .then((c) => {
        setContent(c)
        setTimeout(() => {
          const el = document.querySelector('.monaco-editor')
          if (el) el.scrollTop = m.startLine * 16
        }, 300)
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto flex h-[calc(100vh-130px)] max-w-[1400px] p-4">
        <Card className="flex w-64 shrink-0 flex-col overflow-hidden">
          <div className="border-b border-slate-800 px-3 py-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
            Files
          </div>
          <div className="flex-1 overflow-y-auto">
            {!files ? (
              <Spinner />
            ) : files.length === 0 ? (
              <Empty text="No files indexed." />
            ) : (
              files.map((f) => (
                <button
                  key={f.id}
                  onClick={() => {
                    setSelected(f)
                    setMethods(null)
                    openFile(f.id)
                  }}
                  className={`flex w-full items-start gap-2 px-3 py-1.5 text-left text-xs transition-colors ${
                    selected?.id === f.id ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800'
                  }`}
                >
                  <FileText className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                  <span className="break-all leading-tight">{f.path}</span>
                </button>
              ))
            )}
          </div>
        </Card>

        <Card className="ml-3 w-56 shrink-0 overflow-hidden">
          <div className="border-b border-slate-800 px-3 py-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
            Classes
          </div>
          <div className="flex-1 overflow-y-auto">
            {!classes ? (
              <Spinner />
            ) : classes.length === 0 ? (
              <Empty text="No classes parsed." />
            ) : (
              classes.map((c) => (
                <button
                  key={c.id}
                  onClick={() => openClass(c)}
                  className={`flex w-full items-start gap-2 px-3 py-1.5 text-left text-xs transition-colors ${
                    selected?.id === c.fileId ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800'
                  }`}
                >
                  <FolderTree className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                  <span className="break-all leading-tight">{c.name}</span>
                </button>
              ))
            )}
          </div>
        </Card>

        <div className="ml-3 flex min-w-0 flex-1 flex-col">
          <Card className="flex-1 overflow-hidden">
            {!content ? (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">
                {loading ? 'Loading…' : 'Select a file or class to view it'}
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
                  <div className="flex items-center gap-2 text-xs text-slate-400">
                    <FileCode2 className="h-3.5 w-3.5" />
                    <span className="font-mono">{content.path}</span>
                  </div>
                  {methods && methods.length > 0 && (
                    <select
                      className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-300"
                      onChange={(e) => {
                        const m = methods.find((x) => x.id === Number(e.target.value))
                        if (m) jumpToLine(m)
                      }}
                      defaultValue=""
                    >
                      <option value="" disabled>
                        Jump to method… ({methods.length})
                      </option>
                      {methods.map((m) => (
                        <option key={m.id} value={m.id}>
                          {m.httpMethod ? `[${m.httpMethod}] ` : ''}
                          {m.name}() : {m.returnType}
                        </option>
                      ))}
                    </select>
                  )}
                </div>
                <div className="h-full">
                  <Editor
                    height="100%"
                    language={langOf(content.path)}
                    value={content.content}
                    theme="vs-dark"
                    options={{ readOnly: true, minimap: { enabled: false }, fontSize: 13, scrollBeyondLastLine: false }}
                  />
                </div>
              </>
            )}
          </Card>

          {selected && (
            <div className="mt-2 flex flex-wrap gap-2">
              <Badge color="sky">{langOf(selected.path)}</Badge>
              {methods?.length ? <Badge>{methods.length} methods</Badge> : null}
              {methods?.some((m) => m.httpPath) ? <Badge color="green">{methods.filter((m) => m.httpPath).length} endpoints</Badge> : null}
            </div>
          )}
        </div>
      </div>
      <ErrorText>{error}</ErrorText>
    </div>
  )
}