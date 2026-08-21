import { useCallback, useRef, useState } from 'react'
import { getAccessToken } from '../lib/api'

/**
 * POST + SSE streaming helper. The backend streams `data: {...}` events with
 * types `token` and `sources` to avoid EventSource's GET-only limitation.
 */
export function useSseStream() {
  const [tokens, setTokens] = useState('')
  const [sources, setSources] = useState<any[]>([])
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const controllerRef = useRef<AbortController | null>(null)

  const stop = useCallback(() => {
    controllerRef.current?.abort()
    setStreaming(false)
  }, [])

  const stream = useCallback(
    async (url: string, body: unknown) => {
      setTokens('')
      setSources([])
      setError(null)
      setStreaming(true)
      const controller = new AbortController()
      controllerRef.current = controller
      try {
        const res = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
            Authorization: `Bearer ${getAccessToken()}`,
          },
          body: JSON.stringify(body),
          signal: controller.signal,
        })
        if (!res.ok || !res.body) {
          const text = await res.text()
          let message = `Request failed (${res.status})`
          try {
            message = JSON.parse(text).message || message
          } catch {
            /* keep default */
          }
          setError(message)
          setStreaming(false)
          return
        }
        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buf = ''
        for (;;) {
          const { done, value } = await reader.read()
          if (done) break
          buf += decoder.decode(value, { stream: true })
          const parts = buf.split('\n\n')
          buf = parts.pop() ?? ''
          for (const part of parts) {
            for (const line of part.split('\n')) {
              if (!line.startsWith('data: ')) continue
              const raw = line.slice(6)
              try {
                const evt = JSON.parse(raw)
                if (evt.type === 'token') {
                  setTokens((t) => t + (evt.content ?? ''))
                } else if (evt.type === 'sources') {
                  setSources(evt.sources ?? [])
                } else if (evt.type === 'error') {
                  setError(evt.message ?? 'Streaming error')
                }
              } catch {
                /* malformed or keep-alive line */
              }
            }
          }
        }
      } catch (e: any) {
        if (e?.name !== 'AbortError') setError(String(e))
      } finally {
        setStreaming(false)
      }
    },
    [],
  )

  return { tokens, sources, streaming, error, stream, stop }
}