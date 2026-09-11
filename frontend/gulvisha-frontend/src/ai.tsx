import { useEffect, useState, useRef } from 'react'
import { getAuthHeaders } from './auth'

type AiConfig = {
  id: string
  organizationId: string
  provider: string
  model: string
  baseUrl: string
  enabled: boolean
  temperature: number
  maxTokens: number
  hasApiKey: boolean
}

type AiPrompt = {
  id: string
  name: string
  description: string | null
  content: string
  type: string
  createdAt: string
}

type AiMessage = {
  id: string
  role: string
  content: string
  tokens: number | null
  createdAt: string
}

type AiConversation = {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: AiMessage[]
}

type ChatMessage = {
  role: string
  content: string
  sources?: string[]
}

type KnowledgeDoc = {
  id: string
  title: string
  status: string
  chunkCount: number
  error: string | null
  preview: string | null
  createdAt: string | null
  updatedAt: string | null
}

type AiAgent = {
  id: string
  name: string
  description: string | null
  systemPrompt: string
  allowedTools: string | null
  provider: string | null
  model: string | null
  temperature: number | null
  enabled: boolean
  createdAt: string
}

type AiTool = { name: string; description: string; args: string }

type AiAgentRun = {
  id: string
  agentId: string
  agentName: string
  input: string
  output: string | null
  stepsJson: string | null
  status: string
  error: string | null
  createdAt: string
  finishedAt: string | null
}

const emptyConfig: AiConfig = {
  id: '',
  organizationId: '',
  provider: 'ollama',
  model: 'llama3',
  baseUrl: 'http://localhost:11434',
  enabled: false,
  temperature: 0.7,
  maxTokens: 2048,
  hasApiKey: false,
}

const providers = ['ollama', 'gemini', 'openai']

export default function AiPage() {
  const [tab, setTab] = useState<'chat' | 'config' | 'prompts' | 'knowledge' | 'agents'>('chat')
  const [config, setConfig] = useState<AiConfig>(emptyConfig)
  const [apiKeyInput, setApiKeyInput] = useState('')
  const [providersList, setProvidersList] = useState<string[]>([])
  const [prompts, setPrompts] = useState<AiPrompt[]>([])
  const [conversations, setConversations] = useState<AiConversation[]>([])
  const [activeConversation, setActiveConversation] = useState<AiConversation | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [selectedPrompt, setSelectedPrompt] = useState('')
  const [kbMode, setKbMode] = useState(() => localStorage.getItem('gulvisha-ai-kb-mode') || 'auto')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    loadConfig()
    loadProviders()
    loadPrompts()
    loadConversations()
  }, [])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  async function apiCall(url: string, options?: RequestInit) {
    const res = await fetch(url, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...Object.fromEntries(getAuthHeaders()), ...options?.headers },
    })
    if (res.status === 401) { window.location.href = '/login'; return null }
    if (res.status === 403) {
      throw new Error('Access denied (403). Your account may lack the required permission — if you are an admin, try signing out and signing in again.')
    }
    if (!res.ok) {
      const body = await res.text()
      throw new Error(body || res.statusText)
    }
    return res
  }

  async function loadConfig() {
    try {
      const res = await apiCall('/api/ai/config')
      if (res) setConfig(await res.json())
    } catch (e: any) {
      setError(e.message)
    }
  }

  async function loadProviders() {
    try {
      const res = await apiCall('/api/ai/providers')
      if (res) setProvidersList(await res.json())
    } catch {}
  }

  async function loadPrompts() {
    try {
      const res = await apiCall('/api/ai/prompts')
      if (res) setPrompts(await res.json())
    } catch {}
  }

  async function loadConversations() {
    try {
      const res = await apiCall('/api/ai/conversations')
      if (res) setConversations(await res.json())
    } catch {}
  }

  async function saveConfig() {
    setError(''); setSuccess('')
    try {
      const res = await apiCall('/api/ai/config', { method: 'PUT', body: JSON.stringify(config) })
      let saved: AiConfig | null = null
      if (res) saved = await res.json()
      if (apiKeyInput.trim()) {
        const keyRes = await apiCall('/api/ai/config/api-key', { method: 'PUT', body: JSON.stringify({ apiKey: apiKeyInput.trim() }) })
        if (keyRes) saved = await keyRes.json()
        setApiKeyInput('')
      }
      if (saved) {
        setConfig(saved)
        setSuccess(saved.hasApiKey ? 'Configuration saved (API key stored)' : 'Configuration saved')
      }
    } catch (e: any) {
      setError(e.message)
    }
  }

  async function sendMessage(e: React.FormEvent) {
    e.preventDefault()
    if (!input.trim() || loading) return
    setError('')
    const userMsg: ChatMessage = { role: 'user', content: input }
    const newMessages = [...messages, userMsg]
    setMessages(newMessages)
    setInput('')
    setLoading(true)

    try {
      const res = await apiCall('/api/ai/chat', {
        method: 'POST',
        body: JSON.stringify({
          conversationId: activeConversation?.id || '',
          message: input,
          promptId: selectedPrompt || '',
          knowledgeBaseMode: kbMode,
        }),
      })
      if (res) {
        if (!res.ok) {
          let msg = `AI request failed (${res.status})`
          try { const err = await res.json(); if (err?.message) msg = err.message } catch { /* ignore */ }
          throw new Error(msg)
        }
        const data = await res.json()
        const assistantMsg: ChatMessage = { role: 'assistant', content: data.content, sources: data.sources || undefined }
        setMessages([...newMessages, assistantMsg])

        // If new conversation, reload list
        if (!activeConversation) {
          loadConversations()
        }
      }
    } catch (e: any) {
      setError(e.message)
      setMessages(newMessages)
    } finally {
      setLoading(false)
    }
  }

  async function selectConversation(id: string) {
    try {
      const res = await apiCall(`/api/ai/conversations/${id}`)
      if (res) {
        const conv: AiConversation = await res.json()
        setActiveConversation(conv)
        setMessages(conv.messages.map(m => ({ role: m.role, content: m.content })))
      }
    } catch (e: any) {
      setError(e.message)
    }
  }

  async function newConversation() {
    setActiveConversation(null)
    setMessages([])
    setSelectedPrompt('')
  }

  async function deleteConversation(id: string) {
    try {
      await apiCall(`/api/ai/conversations/${id}`, { method: 'DELETE' })
      if (activeConversation?.id === id) {
        setActiveConversation(null)
        setMessages([])
      }
      loadConversations()
    } catch (e: any) {
      setError(e.message)
    }
  }

  async function savePrompt(prompt: Partial<AiPrompt> & { id?: string }) {
    setError(''); setSuccess('')
    try {
      if (prompt.id) {
        await apiCall(`/api/ai/prompts/${prompt.id}`, { method: 'PUT', body: JSON.stringify(prompt) })
      } else {
        await apiCall('/api/ai/prompts', { method: 'POST', body: JSON.stringify(prompt) })
      }
      loadPrompts()
      setSuccess('Prompt saved')
    } catch (e: any) {
      setError(e.message)
    }
  }

  async function deletePrompt(id: string) {
    try {
      await apiCall(`/api/ai/prompts/${id}`, { method: 'DELETE' })
      loadPrompts()
    } catch (e: any) {
      setError(e.message)
    }
  }

  return (
    <div className="portal-shell">
      <header className="portal-header">
        <div className="portal-brand">
          <span className="portal-logo">AI</span>
          <div>
            <h1>AI Assistant</h1>
            <p>Configure and interact with AI</p>
          </div>
        </div>
        <nav className="portal-nav">
          <button className={tab === 'chat' ? 'active' : ''} onClick={() => setTab('chat')}>Chat</button>
          <button className={tab === 'config' ? 'active' : ''} onClick={() => setTab('config')}>Configuration</button>
          <button className={tab === 'prompts' ? 'active' : ''} onClick={() => setTab('prompts')}>Prompts</button>
          <button className={tab === 'knowledge' ? 'active' : ''} onClick={() => setTab('knowledge')}>Knowledge</button>
          <button className={tab === 'agents' ? 'active' : ''} onClick={() => setTab('agents')}>Agents</button>
        </nav>
      </header>

      {error && <div className="alert alert-error" onClick={() => setError('')}>{error} <span>&times;</span></div>}
      {success && <div className="alert alert-success" onClick={() => setSuccess('')}>{success} <span>&times;</span></div>}

      <>
        {tab === 'chat' && (
          <div className="ai-chat-layout">
            <aside className="ai-sidebar">
              <button className="button button-primary" onClick={newConversation} style={{ width: '100%', marginBottom: '1rem' }}>
                + New Chat
              </button>
              {conversations.length === 0 && <p className="portal-note">No conversations yet</p>}
              {conversations.map(c => (
                <div key={c.id} className={`ai-conv-item ${activeConversation?.id === c.id ? 'active' : ''}`}>
                  <button onClick={() => selectConversation(c.id)} className="ai-conv-title">{c.title}</button>
                  <button onClick={() => deleteConversation(c.id)} className="ai-conv-delete">&times;</button>
                </div>
              ))}
            </aside>

            <div className="ai-chat-main">
              <div className="ai-chat-header">
                <div className="ai-chat-controls">
                  <select value={selectedPrompt} onChange={e => setSelectedPrompt(e.target.value)} className="ai-prompt-select">
                    <option value="">No system prompt</option>
                    {prompts.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                  <select
                    value={kbMode}
                    onChange={e => { setKbMode(e.target.value); localStorage.setItem('gulvisha-ai-kb-mode', e.target.value) }}
                    className="ai-prompt-select"
                    title="How the knowledge base is used when answering"
                  >
                    <option value="auto">Knowledge base: Auto</option>
                    <option value="strict">Knowledge base: Strict (only from documents)</option>
                    <option value="general">Knowledge base: Off (general answers)</option>
                  </select>
                </div>
              </div>

              <div className="ai-messages">
                {messages.length === 0 && <p className="portal-note">Start a conversation by typing below</p>}
                {messages.map((m, i) => (
                  <div key={i} className={`ai-message ai-message-${m.role}`}>
                    <div className="ai-message-role">{m.role}</div>
                    <div className="ai-message-content">{m.content}</div>
                    {m.role === 'assistant' && m.sources && m.sources.length > 0 && (
                      <div className="ai-message-sources">
                        Sources: {m.sources.map((s, j) => <span key={j} className="ai-source-tag">{s}</span>)}
                      </div>
                    )}
                  </div>
                ))}
                {loading && <div className="ai-message ai-message-assistant"><div className="ai-message-content">Thinking...</div></div>}
                <div ref={messagesEndRef} />
              </div>

              <form className="ai-input-bar" onSubmit={sendMessage}>
                <input
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  placeholder="Type your message..."
                  disabled={loading}
                />
                <button type="submit" disabled={loading || !input.trim()} className="button button-primary">
                  Send
                </button>
              </form>
            </div>
          </div>
        )}

        {tab === 'config' && (
          <div className="portal-card">
            <h2>AI Configuration</h2>
            <div className="settings-grid">
              <label>Provider
                <select value={config.provider} onChange={e => setConfig({ ...config, provider: e.target.value })}>
                  {providers.map(p => <option key={p} value={p}>{p}</option>)}
                </select>
              </label>
              <label>Model
                <input value={config.model} onChange={e => setConfig({ ...config, model: e.target.value })} />
              </label>
              <label>Base URL
                <input value={config.baseUrl} onChange={e => setConfig({ ...config, baseUrl: e.target.value })} />
              </label>
              <label>API Key
                <input
                  type="password"
                  value={apiKeyInput}
                  onChange={e => setApiKeyInput(e.target.value)}
                  placeholder={config.hasApiKey ? '•••• stored — type to replace' : 'Paste your API key (not required for Ollama)'}
                  autoComplete="off"
                />
              </label>
              <label>Temperature
                <input type="number" step="0.1" min="0" max="2" value={config.temperature} onChange={e => setConfig({ ...config, temperature: parseFloat(e.target.value) })} />
              </label>
              <label>Max Tokens
                <input type="number" min="1" value={config.maxTokens} onChange={e => setConfig({ ...config, maxTokens: parseInt(e.target.value) })} />
              </label>
              <label className="checkbox-label">
                <input type="checkbox" checked={config.enabled} onChange={e => setConfig({ ...config, enabled: e.target.checked })} />
                Enabled
              </label>
            </div>
            <div className="portal-card-actions">
              <button className="button button-primary" onClick={saveConfig}>Save Configuration</button>
            </div>
          </div>
        )}

        {tab === 'prompts' && (
          <PromptManager prompts={prompts} onSave={savePrompt} onDelete={deletePrompt} />
        )}

        {tab === 'knowledge' && (
          <KnowledgeManager apiCall={apiCall} />
        )}

        {tab === 'agents' && (
          <AgentManager apiCall={apiCall} />
        )}
      </>
    </div>
  )
}

function PromptManager({ prompts, onSave, onDelete }: { prompts: AiPrompt[]; onSave: (p: any) => void; onDelete: (id: string) => void }) {
  const [editing, setEditing] = useState<AiPrompt | null>(null)
  const [showForm, setShowForm] = useState(false)

  function startNew() {
    setEditing({ id: '', name: '', description: '', content: '', type: 'system', createdAt: '' })
    setShowForm(true)
  }

  function startEdit(p: AiPrompt) {
    setEditing(p)
    setShowForm(true)
  }

  function handleSave() {
    if (!editing) return
    onSave(editing)
    setShowForm(false)
    setEditing(null)
  }

  return (
    <div>
      <div className="portal-card-header">
        <h2>System Prompts</h2>
        <button className="button button-primary" onClick={startNew}>+ New Prompt</button>
      </div>

      {showForm && editing && (
        <div className="portal-card">
          <div className="settings-grid">
            <label>Name
              <input value={editing.name} onChange={e => setEditing({ ...editing, name: e.target.value })} />
            </label>
            <label>Type
              <select value={editing.type} onChange={e => setEditing({ ...editing, type: e.target.value })}>
                <option value="system">System</option>
                <option value="template">Template</option>
              </select>
            </label>
            <label className="full-width">Description
              <input value={editing.description || ''} onChange={e => setEditing({ ...editing, description: e.target.value })} />
            </label>
            <label className="full-width">Content
              <textarea rows={6} value={editing.content} onChange={e => setEditing({ ...editing, content: e.target.value })} />
            </label>
          </div>
          <div className="portal-card-actions">
            <button className="button button-primary" onClick={handleSave}>Save</button>
            <button className="button button-secondary" onClick={() => setShowForm(false)}>Cancel</button>
          </div>
        </div>
      )}

      {prompts.length === 0 && <p className="portal-note">No prompts yet</p>}
      {prompts.map(p => (
        <div key={p.id} className="portal-card">
          <div className="portal-card-head">
            <h3>{p.name}</h3>
            <span className="portal-badge">{p.type}</span>
          </div>
          {p.description && <p className="portal-note">{p.description}</p>}
          <pre className="ai-prompt-content">{p.content}</pre>
          <div className="portal-card-actions">
            <button className="button button-secondary" onClick={() => startEdit(p)}>Edit</button>
            <button className="button button-danger" onClick={() => onDelete(p.id)}>Delete</button>
          </div>
        </div>
      ))}
    </div>
  )
}

function KnowledgeManager({ apiCall }: { apiCall: (url: string, options?: RequestInit) => Promise<Response | null> }) {
  const [docs, setDocs] = useState<KnowledgeDoc[]>([])
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [busy, setBusy] = useState(false)
  const [localError, setLocalError] = useState('')

  useEffect(() => { void load() }, [])

  async function load() {
    try {
      const res = await apiCall('/api/ai/knowledge')
      if (res) setDocs(await res.json())
    } catch (e: any) {
      setLocalError(e.message)
    }
  }

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setContent(await file.text())
    if (!title) setTitle(file.name.replace(/\.(txt|md|markdown)$/i, ''))
    e.target.value = ''
  }

  async function ingest() {
    if (!title.trim() || !content.trim() || busy) return
    setBusy(true)
    setLocalError('')
    try {
      await apiCall('/api/ai/knowledge', { method: 'POST', body: JSON.stringify({ title: title.trim(), content }) })
      setTitle('')
      setContent('')
      await load()
    } catch (e: any) {
      setLocalError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(id: string) {
    try {
      await apiCall(`/api/ai/knowledge/${id}`, { method: 'DELETE' })
      await load()
    } catch (e: any) {
      setLocalError(e.message)
    }
  }

  async function reindex(id: string) {
    setBusy(true)
    setLocalError('')
    try {
      await apiCall(`/api/ai/knowledge/${id}/reindex`, { method: 'POST' })
      await load()
    } catch (e: any) {
      setLocalError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const statusColor: Record<string, string> = {
    READY: '#16a34a',
    FAILED: '#dc2626',
    EMBEDDING: '#d97706',
    PENDING: '#64748b',
  }

  return (
    <div>
      <div className="portal-card-header">
        <h2>Knowledge Base</h2>
      </div>
      <p className="portal-note">
        Add documents the AI can use to ground its answers (RAG). Content is chunked and embedded
        with your configured provider; relevant chunks are injected into chat automatically.
      </p>

      {localError && <div className="alert alert-error" onClick={() => setLocalError('')}>{localError} <span>&times;</span></div>}

      <div className="portal-card">
        <h3>Add document</h3>
        <div className="settings-grid">
          <label>Title
            <input value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g. Return policy" />
          </label>
          <label>Upload .txt / .md (optional)
            <input type="file" accept=".txt,.md,.markdown,text/plain" onChange={e => void handleFile(e)} />
          </label>
          <label className="full-width">Content
            <textarea rows={6} value={content} onChange={e => setContent(e.target.value)} placeholder="Paste document text here..." />
          </label>
        </div>
        <div className="portal-card-actions">
          <button className="button button-primary" onClick={() => void ingest()} disabled={busy || !title.trim() || !content.trim()}>
            {busy ? 'Embedding...' : 'Ingest document'}
          </button>
        </div>
      </div>

      {docs.length === 0 && <p className="portal-note">No documents yet</p>}
      {docs.map(d => (
        <div key={d.id} className="portal-card">
          <div className="portal-card-head">
            <h3>{d.title}</h3>
            <span className="portal-badge" style={{ background: statusColor[d.status] || '#64748b', color: 'white' }}>{d.status}</span>
          </div>
          <p className="portal-note">{d.chunkCount} chunk(s) · updated {d.updatedAt ? new Date(d.updatedAt).toLocaleString() : '—'}</p>
          {d.error && <p className="portal-note" style={{ color: '#dc2626' }}>{d.error}</p>}
          {d.preview && <pre className="ai-prompt-content">{d.preview}</pre>}
          <div className="portal-card-actions">
            <button className="button button-secondary" onClick={() => void reindex(d.id)} disabled={busy}>Reindex</button>
            <button className="button button-danger" onClick={() => void remove(d.id)}>Delete</button>
          </div>
        </div>
      ))}
    </div>
  )
}

function AgentManager({ apiCall }: { apiCall: (url: string, options?: RequestInit) => Promise<Response | null> }) {
  const [agents, setAgents] = useState<AiAgent[]>([])
  const [tools, setTools] = useState<AiTool[]>([])
  const [runs, setRuns] = useState<AiAgentRun[]>([])
  const [editing, setEditing] = useState<AiAgent | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [runTarget, setRunTarget] = useState<AiAgent | null>(null)
  const [runInput, setRunInput] = useState('')
  const [runResult, setRunResult] = useState<AiAgentRun | null>(null)
  const [busy, setBusy] = useState(false)
  const [localError, setLocalError] = useState('')

  useEffect(() => { void load() }, [])

  async function load() {
    setLocalError('')
    try {
      const [a, t, r] = await Promise.all([
        apiCall('/api/ai/agents'),
        apiCall('/api/ai/agents/tools'),
        apiCall('/api/ai/agents/runs'),
      ])
      if (a) setAgents(await a.json())
      if (t) setTools(await t.json())
      if (r) setRuns(await r.json())
    } catch (e: any) {
      setLocalError(e.message)
    }
  }

  function startNew() {
    setEditing({
      id: '', name: '', description: '', systemPrompt: '', allowedTools: '',
      provider: null, model: null, temperature: null, enabled: true, createdAt: '',
    })
    setShowForm(true)
  }

  function toggleTool(name: string) {
    if (!editing) return
    const current = (editing.allowedTools || '').split(',').map(s => s.trim()).filter(Boolean)
    const next = current.includes(name) ? current.filter(n => n !== name) : [...current, name]
    setEditing({ ...editing, allowedTools: next.join(',') })
  }

  async function save() {
    if (!editing || !editing.name.trim() || !editing.systemPrompt.trim()) return
    setBusy(true)
    setLocalError('')
    try {
      const isNew = !editing.id
      const res = await apiCall(isNew ? '/api/ai/agents' : `/api/ai/agents/${editing.id}`, {
        method: isNew ? 'POST' : 'PUT',
        body: JSON.stringify({
          name: editing.name.trim(),
          description: editing.description,
          systemPrompt: editing.systemPrompt,
          allowedTools: editing.allowedTools,
          enabled: editing.enabled,
        }),
      })
      if (res) {
        setShowForm(false)
        setEditing(null)
        await load()
      }
    } catch (e: any) {
      setLocalError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(id: string) {
    try {
      await apiCall(`/api/ai/agents/${id}`, { method: 'DELETE' })
      await load()
    } catch (e: any) {
      setLocalError(e.message)
    }
  }

  async function toggleEnabled(agent: AiAgent) {
    try {
      await apiCall(`/api/ai/agents/${agent.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: agent.name, description: agent.description, systemPrompt: agent.systemPrompt,
          allowedTools: agent.allowedTools, enabled: !agent.enabled,
        }),
      })
      await load()
    } catch (e: any) {
      setLocalError(e.message)
    }
  }

  async function executeRun() {
    if (!runTarget || !runInput.trim()) return
    setBusy(true)
    setLocalError('')
    setRunResult(null)
    try {
      const res = await apiCall(`/api/ai/agents/${runTarget.id}/run`, {
        method: 'POST',
        body: JSON.stringify({ input: runInput.trim() }),
      })
      if (res) {
        setRunResult(await res.json())
        await load()
      }
    } catch (e: any) {
      setLocalError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const runsForAgent = runTarget ? runs.filter(r => r.agentId === runTarget.id) : []

  return (
    <div>
      <div className="portal-card-header">
        <h2>AI Agents</h2>
        <button className="button button-primary" onClick={startNew}>+ New Agent</button>
      </div>
      <p className="portal-note">
        Agents answer questions and act through permission-controlled tools (tenant-scoped: they can
        only see this organization's data). Available tools: {tools.map(t => t.name).join(', ') || 'none'}.
      </p>

      {localError && <div className="alert alert-error" onClick={() => setLocalError('')}>{localError} <span>&times;</span></div>}

      {showForm && editing && (
        <div className="portal-card">
          <div className="settings-grid">
            <label>Name
              <input value={editing.name} onChange={e => setEditing({ ...editing, name: e.target.value })} placeholder="e.g. Sales Assistant" />
            </label>
            <label className="full-width">Description
              <input value={editing.description || ''} onChange={e => setEditing({ ...editing, description: e.target.value })} />
            </label>
            <label className="full-width">System prompt (tenant-specific instructions)
              <textarea rows={6} value={editing.systemPrompt} onChange={e => setEditing({ ...editing, systemPrompt: e.target.value })} placeholder="You are the assistant for [your business]..." />
            </label>
            <label className="full-width">Allowed tools
              <div>
                {tools.map(t => {
                  const on = (editing.allowedTools || '').split(',').map(s => s.trim()).includes(t.name)
                  return (
                    <label key={t.name} style={{ display: 'inline-flex', gap: '0.35rem', alignItems: 'center', marginRight: '1rem' }}>
                      <input type="checkbox" checked={on} onChange={() => toggleTool(t.name)} />
                      {t.name}
                    </label>
                  )
                })}
              </div>
            </label>
            <label style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <input type="checkbox" checked={editing.enabled} onChange={e => setEditing({ ...editing, enabled: e.target.checked })} />
              Enabled
            </label>
          </div>
          <div className="portal-card-actions">
            <button className="button button-primary" onClick={() => void save()} disabled={busy || !editing.name.trim() || !editing.systemPrompt.trim()}>
              Save
            </button>
            <button className="button button-secondary" onClick={() => setShowForm(false)}>Cancel</button>
          </div>
        </div>
      )}

      {agents.length === 0 && <p className="portal-note">No agents yet</p>}
      {agents.map(a => (
        <div key={a.id} className="portal-card">
          <div className="portal-card-head">
            <h3>{a.name}</h3>
            <span className="portal-badge" style={{ background: a.enabled ? '#16a34a' : '#64748b', color: 'white' }}>
              {a.enabled ? 'ENABLED' : 'DISABLED'}
            </span>
          </div>
          {a.description && <p className="portal-note">{a.description}</p>}
          <pre className="ai-prompt-content">{a.systemPrompt}</pre>
          <p className="portal-note">Tools: {a.allowedTools || '(none - Q&A only)'}</p>
          <div className="portal-card-actions">
            <button className="button button-primary" onClick={() => { setRunTarget(a); setRunInput(''); setRunResult(null) }}>Run</button>
            <button className="button button-secondary" onClick={() => { setEditing(a); setShowForm(true) }}>Edit</button>
            <button className="button button-secondary" onClick={() => void toggleEnabled(a)}>{a.enabled ? 'Disable' : 'Enable'}</button>
            <button className="button button-danger" onClick={() => void remove(a.id)}>Delete</button>
          </div>
        </div>
      ))}

      {runTarget && (
        <div className="portal-card">
          <div className="portal-card-head">
            <h3>Run: {runTarget.name}</h3>
            <button className="button button-secondary" onClick={() => { setRunTarget(null); setRunResult(null) }}>&times;</button>
          </div>
          <div className="settings-grid">
            <label className="full-width">Input
              <textarea rows={3} value={runInput} onChange={e => setRunInput(e.target.value)}
                placeholder="e.g. A customer asked about bulk data entry pricing - record them as a lead (jane@corp.com)" />
            </label>
          </div>
          <div className="portal-card-actions">
            <button className="button button-primary" onClick={() => void executeRun()} disabled={busy || !runInput.trim()}>
              {busy ? 'Running...' : 'Run agent'}
            </button>
          </div>
          {runResult && (
            <div style={{ marginTop: '1rem' }}>
              <p className="portal-note">Status: {runResult.status}</p>
              <pre className="ai-prompt-content">{runResult.output || '(no answer)'}</pre>
              {runResult.stepsJson && (
                <details>
                  <summary className="portal-note">Tool steps</summary>
                  <pre className="ai-prompt-content">{runResult.stepsJson}</pre>
                </details>
              )}
            </div>
          )}
          {runsForAgent.length > 0 && (
            <div style={{ marginTop: '1rem' }}>
              <h4>Recent runs</h4>
              {runsForAgent.slice(0, 5).map(r => (
                <p key={r.id} className="portal-note">
                  {new Date(r.createdAt).toLocaleString()} - [{r.status}] {r.output?.slice(0, 120) || r.input.slice(0, 120)}
                </p>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
