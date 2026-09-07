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
  const [tab, setTab] = useState<'chat' | 'config' | 'prompts'>('chat')
  const [config, setConfig] = useState<AiConfig>(emptyConfig)
  const [apiKeyInput, setApiKeyInput] = useState('')
  const [providersList, setProvidersList] = useState<string[]>([])
  const [prompts, setPrompts] = useState<AiPrompt[]>([])
  const [conversations, setConversations] = useState<AiConversation[]>([])
  const [activeConversation, setActiveConversation] = useState<AiConversation | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [selectedPrompt, setSelectedPrompt] = useState('')
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
        }),
      })
      if (res) {
        if (!res.ok) {
          let msg = `AI request failed (${res.status})`
          try { const err = await res.json(); if (err?.message) msg = err.message } catch { /* ignore */ }
          throw new Error(msg)
        }
        const data = await res.json()
        const assistantMsg: ChatMessage = { role: 'assistant', content: data.content }
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
                <select value={selectedPrompt} onChange={e => setSelectedPrompt(e.target.value)} className="ai-prompt-select">
                  <option value="">No system prompt</option>
                  {prompts.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
              </div>

              <div className="ai-messages">
                {messages.length === 0 && <p className="portal-note">Start a conversation by typing below</p>}
                {messages.map((m, i) => (
                  <div key={i} className={`ai-message ai-message-${m.role}`}>
                    <div className="ai-message-role">{m.role}</div>
                    <div className="ai-message-content">{m.content}</div>
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
