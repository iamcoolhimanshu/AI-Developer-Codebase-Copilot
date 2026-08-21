import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Bot, Plus, Send, User } from 'lucide-react'
import api, { errorMessage, unwrap } from '../lib/api'
import { useSseStream } from '../lib/useSseStream'
import type { ChatMessageDto, ChatSource, ConversationDto } from '../lib/types'
import { Button, Card, Empty, ErrorText, inputCls, Spinner } from '../components/ui'
import Markdown from '../components/Markdown'
import ProjectHeader from '../components/ProjectHeader'

export default function ChatPage() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [conversations, setConversations] = useState<ConversationDto[] | null>(null)
  const [active, setActive] = useState<number | null>(null)
  const [messages, setMessages] = useState<ChatMessageDto[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const endRef = useRef<HTMLDivElement>(null)
  const { tokens, sources, streaming, stream } = useSseStream()

  async function loadConversations() {
    try {
      const list = await unwrap(api.get(`/projects/${pid}/chat/conversations`))
      setConversations(list)
      if (list.length > 0) selectConversation(list[0].id)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  useEffect(() => {
    loadConversations()
  }, [pid])

  async function selectConversation(id: number) {
    setActive(id)
    setLoading(true)
    setError(null)
    try {
      const msgs = await unwrap(api.get(`/projects/${pid}/chat/conversations/${id}/messages`))
      setMessages(msgs)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  async function newConversation() {
    try {
      const c = await unwrap(api.post(`/projects/${pid}/chat/conversations`))
      const list = await unwrap(api.get(`/projects/${pid}/chat/conversations`))
      setConversations(list)
      setMessages([])
      selectConversation(c.id)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  async function send() {
    if (!input.trim()) return
    const question = input.trim()
    setInput('')
    setError(null)
    setMessages((m) => [...m, { id: -Date.now(), role: 'USER', content: question, createdAt: new Date().toISOString() }])
    if (active) {
      await stream(`/api/projects/${pid}/chat/messages/stream`, { message: question, conversationId: active })
    } else {
      setMessages((m) => [...m, { id: -Date.now(), role: 'ASSISTANT', content: 'No conversation selected — start a new one first.', createdAt: new Date().toISOString() }])
    }
  }

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, tokens])

  return (
    <div>
      <ProjectHeader />
      <div className="mx-auto flex h-[calc(100vh-130px)] max-w-[1400px] p-4">
        <Card className="flex w-64 shrink-0 flex-col overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Conversations</span>
            <button onClick={newConversation} className="text-slate-400 hover:text-sky-400">
              <Plus className="h-4 w-4" />
            </button>
          </div>
          <div className="flex-1 overflow-y-auto">
            {!conversations ? (
              <Spinner />
            ) : conversations.length === 0 ? (
              <Empty text="No conversations yet." />
            ) : (
              conversations.map((c) => (
                <button
                  key={c.id}
                  onClick={() => selectConversation(c.id)}
                  className={`block w-full px-3 py-2 text-left text-xs transition-colors ${
                    active === c.id ? 'bg-sky-600/15 text-sky-300' : 'text-slate-400 hover:bg-slate-800'
                  }`}
                >
                  <div className="truncate font-medium">{c.title}</div>
                  <div className="text-[10px] text-slate-600">{new Date(c.createdAt).toLocaleString()}</div>
                </button>
              ))
            )}
          </div>
        </Card>

        <div className="ml-3 flex min-w-0 flex-1 flex-col">
          <Card className="flex flex-1 flex-col overflow-hidden">
            <div className="flex-1 space-y-4 overflow-y-auto p-4">
              {loading ? (
                <Spinner />
              ) : messages.length === 0 && !tokens ? (
                <Empty text="Ask anything about this codebase — the copilot grounds answers in the indexed code." />
              ) : (
                <>
                  {messages.map((m) => (
                    <div key={m.id} className={`flex gap-3 ${m.role === 'USER' ? 'justify-end' : ''}`}>
                      {m.role === 'ASSISTANT' && (
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-sky-600/20 text-sky-400">
                          <Bot className="h-4 w-4" />
                        </div>
                      )}
                      <div
                        className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm ${
                          m.role === 'USER'
                            ? 'bg-sky-600 text-white'
                            : 'border border-slate-800 bg-slate-900 text-slate-300'
                        }`}
                      >
                        {m.role === 'ASSISTANT' ? <Markdown content={m.content} /> : m.content}
                      </div>
                      {m.role === 'USER' && (
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-700 text-slate-300">
                          <User className="h-4 w-4" />
                        </div>
                      )}
                    </div>
                  ))}
                  {tokens && (
                    <div className="flex gap-3">
                      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-sky-600/20 text-sky-400">
                        <Bot className="h-4 w-4" />
                      </div>
                      <div className="max-w-[80%] rounded-2xl border border-slate-800 bg-slate-900 px-4 py-2.5 text-sm text-slate-300">
                        <Markdown content={tokens} />
                        {streaming && <span className="ml-1 inline-block h-3.5 w-1.5 animate-pulse bg-sky-400 align-middle" />}
                      </div>
                    </div>
                  )}
                </>
              )}
              <div ref={endRef} />
            </div>

            {sources.length > 0 && (
              <div className="max-h-40 overflow-y-auto border-t border-slate-800 px-4 py-2">
                <div className="mb-1 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                  Sources ({sources.length})
                </div>
                {sources.map((s: ChatSource, i) => (
                  <div key={i} className="flex items-center gap-2 py-0.5 font-mono text-[11px] text-slate-400">
                    <span className="text-slate-600">[{i + 1}]</span>
                    <span className="truncate">{s.filePath}</span>
                    <span className="text-slate-600">
                      {s.className ?? ''}{s.methodName ? `::${s.methodName}()` : ''} L{s.startLine}–{s.endLine}
                    </span>
                    <span className="ml-auto text-emerald-500">{(s.score * 100).toFixed(0)}%</span>
                  </div>
                ))}
              </div>
            )}

            <div className="flex items-end gap-2 border-t border-slate-800 p-3">
              <textarea
                rows={2}
                className={`${inputCls} flex-1`}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    send()
                  }
                }}
                placeholder="Ask about the codebase… (Enter to send, Shift+Enter for newline)"
              />
              <Button onClick={send} disabled={!input.trim() || streaming}>
                <Send className="h-4 w-4" />
                Send
              </Button>
            </div>
          </Card>
        </div>
      </div>
      <ErrorText>{error}</ErrorText>
    </div>
  )
}