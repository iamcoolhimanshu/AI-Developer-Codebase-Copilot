import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { FlaskConical } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import type { CodeClassDto, GeneratedTests } from '../lib/types'
import { Button, Card, CardHeader, ErrorText, Field, inputCls, Spinner } from '../components/ui'
import ProjectHeader from '../components/ProjectHeader'

export default function TestGenPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [classes, setClasses] = useState<CodeClassDto[] | null>(null)
  const [classId, setClassId] = useState('')
  const [methodName, setMethodName] = useState('')
  const [tests, setTests] = useState<GeneratedTests | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!pid) return
    unwrap(api.get(`/projects/${pid}/classes`))
      .then(setClasses)
      .catch((e) => setError(errorMessage(e)))
  }, [pid])

  async function generate(e: FormEvent) {
    e.preventDefault()
    if (!classId) return
    setBusy(true)
    setError(null)
    setTests(null)
    try {
      setTests(
        await unwrap(
          api.post(`/projects/${pid}/test-generation`, {
            classId: Number(classId),
            methodName: methodName || undefined,
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
        <h1 className="mb-1 text-lg font-bold text-slate-100">Test Generator</h1>
        <p className="mb-4 text-sm text-slate-500">Generate JUnit 5 + Mockito unit tests for an indexed class or a single method.</p>

        <form onSubmit={generate} className="mb-4 grid gap-3 rounded-2xl border border-slate-800 bg-slate-900/60 p-4 sm:grid-cols-[2fr_1fr_auto]">
          <Field label="Class">
            <select className={inputCls} value={classId} onChange={(e) => setClassId(e.target.value)} required>
              <option value="">Select a class…</option>
              {(classes ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.fqName})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Method (optional)">
            <input className={inputCls} value={methodName} onChange={(e) => setMethodName(e.target.value)} placeholder="all methods" />
          </Field>
          <div className="flex items-end">
            <Button type="submit" disabled={busy || !classId}>
              <FlaskConical className="h-4 w-4" />
              {busy ? 'Generating…' : 'Generate'}
            </Button>
          </div>
        </form>

        <ErrorText>{error}</ErrorText>

        {busy && <Spinner label="Writing tests…" />}
        {tests && (
          <Card>
            <CardHeader title={tests.fileName} />
            <div className="p-2">
              <Editor
                height="60vh"
                language="java"
                value={tests.code}
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