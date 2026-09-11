import { useEffect, useState } from 'react'
import { getAuthHeaders } from './auth'

type Project = { id: string; name: string; description: string | null; service: string | null; status: string; startDate: string | null; endDate: string | null; budget: number | null; manager: string | null; clientId: string | null }
type ClientOption = { id: string; name: string }
const STATUSES = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED']

function json<T>(response: Response): Promise<T> {
  if (!response.ok) throw new Error(`Request failed (${response.status})`)
  return response.json() as Promise<T>
}
function statusBadge(status: string): string {
  return `portal-badge portal-badge-${status.toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [clients, setClients] = useState<ClientOption[]>([])
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState({ name: '', description: '', service: '', clientId: '', status: 'PLANNING', startDate: '', endDate: '', budget: '', manager: '' })

  async function refresh() {
    try {
      const res = await fetch('/api/projects?size=50', { headers: getAuthHeaders() })
      setProjects((await json<{ content: Project[] }>(res)).content)
      setLoaded(true); setError('')
    } catch { setError('Could not load projects. Is the backend running?') }
  }

  useEffect(() => {
    refresh()
    fetch('/api/clients?size=100', { headers: getAuthHeaders() }).then(r => json<{ content: ClientOption[] }>(r)).then(d => setClients(d.content)).catch(() => {})
  }, [])

  const flash = (t: string) => { setMessage(t); setTimeout(() => setMessage(''), 3000) }
  const fail = (e: unknown) => { setError(e instanceof Error ? e.message : 'Something went wrong'); setMessage('') }
  const api = (url: string, method: string, body?: unknown) => fetch(url, { method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) })
  const resetForm = () => setForm({ name: '', description: '', service: '', clientId: '', status: 'PLANNING', startDate: '', endDate: '', budget: '', manager: '' })

  async function create(e: React.FormEvent) {
    e.preventDefault()
    try { await json(await api('/api/projects', 'POST', form)); flash('Project created'); resetForm(); await refresh() } catch (e) { fail(e) }
  }
  async function saveEdit(id: string) {
    try { await json(await api(`/api/projects/${id}`, 'PUT', form)); flash('Project updated'); setEditingId(null); await refresh() } catch (e) { fail(e) }
  }
  async function updateStatus(id: string, status: string) {
    try { await json(await api(`/api/projects/${id}/status`, 'PATCH', { status })); flash('Status updated'); await refresh() } catch (e) { fail(e) }
  }
  async function remove(id: string) {
    if (!confirm('Delete this project?')) return
    try { await json(await api(`/api/projects/${id}`, 'DELETE')); flash('Project deleted'); await refresh() } catch (e) { fail(e) }
  }
  function startEdit(p: Project) {
    setEditingId(p.id)
    setForm({ name: p.name, description: p.description || '', service: p.service || '', clientId: p.clientId || '', status: p.status, startDate: p.startDate || '', endDate: p.endDate || '', budget: p.budget != null ? String(p.budget) : '', manager: p.manager || '' })
  }

  return (
    <div className="portal-container">
      <div className="portal-header">
        <div>
          <h1>Projects</h1>
          <p className="portal-note">Manage client projects</p>
        </div>
      </div>
      {message && <div className="alert alert-success" onClick={() => setMessage('')}>{message}</div>}
      {error && <div className="alert alert-error" onClick={() => setError('')}>{error}</div>}

      <section className="portal-card">
        <h2>Create project</h2>
        <form onSubmit={create}>
          <div className="settings-grid">
            <label>Name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
            <label>Service<input value={form.service} onChange={e => setForm({ ...form, service: e.target.value })} /></label>
            <label>Client<select value={form.clientId} onChange={e => setForm({ ...form, clientId: e.target.value })}>
              <option value="">— No client —</option>
              {clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select></label>
            <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select></label>
            <label>Start date<input type="date" value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })} /></label>
            <label>End date<input type="date" value={form.endDate} onChange={e => setForm({ ...form, endDate: e.target.value })} /></label>
            <label>Budget<input type="number" value={form.budget} onChange={e => setForm({ ...form, budget: e.target.value })} /></label>
            <label>Manager<input value={form.manager} onChange={e => setForm({ ...form, manager: e.target.value })} /></label>
            <label className="settings-full">Description<textarea rows={2} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
          </div>
          <div className="portal-header-actions"><button className="button button-primary" type="submit">Create project</button></div>
        </form>
      </section>

      {!loaded ? <p>Loading projects...</p> : projects.length === 0 ? (
        <p className="portal-note">No projects yet.</p>
      ) : (
        <section className="portal-list">
          {projects.map(p => (
            <article key={p.id} className="portal-card">
              {editingId === p.id ? (
                <form onSubmit={e => { e.preventDefault(); saveEdit(p.id) }}>
                  <div className="settings-grid">
                    <label>Name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
                    <label>Service<input value={form.service} onChange={e => setForm({ ...form, service: e.target.value })} /></label>
                    <label>Client<select value={form.clientId} onChange={e => setForm({ ...form, clientId: e.target.value })}>
                      <option value="">— No client —</option>
                      {clients.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select></label>
                    <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                      {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select></label>
                    <label>Start date<input type="date" value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })} /></label>
                    <label>End date<input type="date" value={form.endDate} onChange={e => setForm({ ...form, endDate: e.target.value })} /></label>
                    <label>Budget<input type="number" value={form.budget} onChange={e => setForm({ ...form, budget: e.target.value })} /></label>
                    <label>Manager<input value={form.manager} onChange={e => setForm({ ...form, manager: e.target.value })} /></label>
                    <label className="settings-full">Description<textarea rows={2} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
                  </div>
                  <div className="portal-header-actions">
                    <button className="button button-primary" type="submit">Save</button>
                    <button className="button button-ghost" type="button" onClick={() => setEditingId(null)}>Cancel</button>
                  </div>
                </form>
              ) : (
                <>
                  <div className="portal-card-head">
                    <h2>{p.name} <span className={statusBadge(p.status)}>{p.status}</span></h2>
                    <div className="portal-header-actions">
                      <button className="button button-ghost" type="button" onClick={() => startEdit(p)}>Edit</button>
                      <button className="button button-danger" type="button" onClick={() => remove(p.id)}>Delete</button>
                    </div>
                  </div>
                  {p.description && <p>{p.description}</p>}
                  <dl className="portal-meta">
                    <div><dt>Service</dt><dd>{p.service ?? '—'}</dd></div>
                    <div><dt>Manager</dt><dd>{p.manager ?? '—'}</dd></div>
                    <div><dt>Start</dt><dd>{p.startDate ?? '—'}</dd></div>
                    <div><dt>End</dt><dd>{p.endDate ?? '—'}</dd></div>
                    <div><dt>Budget</dt><dd>{p.budget != null ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(p.budget) : '—'}</dd></div>
                  </dl>
                  <label style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span className="portal-note">Set status:</span>
                    <select value={p.status} onChange={e => updateStatus(p.id, e.target.value)}>
                      {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </label>
                </>
              )}
            </article>
          ))}
        </section>
      )}
    </div>
  )
}
