import { useEffect, useState } from 'react'
import { getAuthHeaders } from './auth'

type Resource = { id: string; name: string; role: string; skills: string | null; experienceYears: number | null; availability: string | null; status: string; assignedProjectId: string | null }
type ProjectOption = { id: string; name: string }
const STATUSES = ['AVAILABLE', 'ALLOCATED', 'ON_LEAVE', 'INACTIVE']

function json<T>(response: Response): Promise<T> {
  if (!response.ok) throw new Error(`Request failed (${response.status})`)
  return response.json() as Promise<T>
}
function statusBadge(status: string): string {
  return `portal-badge portal-badge-${status.toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function ResourcesPage() {
  const [resources, setResources] = useState<Resource[]>([])
  const [projects, setProjects] = useState<ProjectOption[]>([])
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState({ name: '', role: '', skills: '', experienceYears: '', availability: '', status: 'AVAILABLE', assignedProjectId: '' })

  async function refresh() {
    try {
      const res = await fetch('/api/resources?size=50', { headers: getAuthHeaders() })
      setResources((await json<{ content: Resource[] }>(res)).content)
      setLoaded(true); setError('')
    } catch { setError('Could not load resources. Is the backend running?') }
  }

  useEffect(() => {
    refresh()
    fetch('/api/projects?size=100', { headers: getAuthHeaders() }).then(r => json<{ content: ProjectOption[] }>(r)).then(d => setProjects(d.content)).catch(() => {})
  }, [])

  const flash = (t: string) => { setMessage(t); setTimeout(() => setMessage(''), 3000) }
  const fail = (e: unknown) => { setError(e instanceof Error ? e.message : 'Something went wrong'); setMessage('') }
  const api = (url: string, method: string, body?: unknown) => fetch(url, { method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) })
  const resetForm = () => setForm({ name: '', role: '', skills: '', experienceYears: '', availability: '', status: 'AVAILABLE', assignedProjectId: '' })
  const projectName = (id: string | null) => !id ? '\u2014' : projects.find(p => p.id === id)?.name || '\u2014'

  async function create(e: React.FormEvent) {
    e.preventDefault()
    try { await json(await api('/api/resources', 'POST', { ...form, experienceYears: form.experienceYears ? Number(form.experienceYears) : null })); flash('Resource created'); resetForm(); await refresh() } catch (e) { fail(e) }
  }
  async function saveEdit(id: string) {
    try { await json(await api(`/api/resources/${id}`, 'PUT', { ...form, experienceYears: form.experienceYears ? Number(form.experienceYears) : null })); flash('Resource updated'); setEditingId(null); await refresh() } catch (e) { fail(e) }
  }
  async function updateStatus(id: string, status: string) {
    try { await json(await api(`/api/resources/${id}/status`, 'PATCH', { status })); flash('Status updated'); await refresh() } catch (e) { fail(e) }
  }
  async function remove(id: string) {
    if (!confirm('Delete this resource?')) return
    try { await json(await api(`/api/resources/${id}`, 'DELETE')); flash('Resource deleted'); await refresh() } catch (e) { fail(e) }
  }
  function startEdit(r: Resource) {
    setEditingId(r.id)
    setForm({ name: r.name, role: r.role, skills: r.skills || '', experienceYears: r.experienceYears != null ? String(r.experienceYears) : '', availability: r.availability || '', status: r.status, assignedProjectId: r.assignedProjectId || '' })
  }

  return (
    <div className="portal-container">
      <div className="portal-header">
        <div>
          <h1>Resources</h1>
          <p className="portal-note">Manage team resources and allocation</p>
        </div>
      </div>
      {message && <div className="alert alert-success" onClick={() => setMessage('')}>{message}</div>}
      {error && <div className="alert alert-error" onClick={() => setError('')}>{error}</div>}

      <section className="portal-card">
        <h2>Add resource</h2>
        <form onSubmit={create}>
          <div className="settings-grid">
            <label>Name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
            <label>Role<input required value={form.role} onChange={e => setForm({ ...form, role: e.target.value })} /></label>
            <label>Skills<input value={form.skills} onChange={e => setForm({ ...form, skills: e.target.value })} placeholder="e.g. React, Python, Design" /></label>
            <label>Experience (years)<input type="number" value={form.experienceYears} onChange={e => setForm({ ...form, experienceYears: e.target.value })} /></label>
            <label>Availability<input value={form.availability} onChange={e => setForm({ ...form, availability: e.target.value })} placeholder="e.g. Full-time, 20h/week" /></label>
            <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
            </select></label>
            <label className="settings-full">Assigned project<select value={form.assignedProjectId} onChange={e => setForm({ ...form, assignedProjectId: e.target.value })}>
              <option value="">\u2014 Unassigned \u2014</option>
              {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select></label>
          </div>
          <div className="portal-header-actions"><button className="button button-primary" type="submit">Add resource</button></div>
        </form>
      </section>

      {!loaded ? <p>Loading resources...</p> : resources.length === 0 ? (
        <p className="portal-note">No resources yet.</p>
      ) : (
        <section className="portal-list">
          {resources.map(r => (
            <article key={r.id} className="portal-card">
              {editingId === r.id ? (
                <form onSubmit={e => { e.preventDefault(); saveEdit(r.id) }}>
                  <div className="settings-grid">
                    <label>Name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
                    <label>Role<input required value={form.role} onChange={e => setForm({ ...form, role: e.target.value })} /></label>
                    <label>Skills<input value={form.skills} onChange={e => setForm({ ...form, skills: e.target.value })} /></label>
                    <label>Experience (years)<input type="number" value={form.experienceYears} onChange={e => setForm({ ...form, experienceYears: e.target.value })} /></label>
                    <label>Availability<input value={form.availability} onChange={e => setForm({ ...form, availability: e.target.value })} /></label>
                    <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                      {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select></label>
                    <label className="settings-full">Assigned project<select value={form.assignedProjectId} onChange={e => setForm({ ...form, assignedProjectId: e.target.value })}>
                      <option value="">\u2014 Unassigned \u2014</option>
                      {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select></label>
                  </div>
                  <div className="portal-header-actions">
                    <button className="button button-primary" type="submit">Save</button>
                    <button className="button button-ghost" type="button" onClick={() => setEditingId(null)}>Cancel</button>
                  </div>
                </form>
              ) : (
                <>
                  <div className="portal-card-head">
                    <h2>{r.name} <span className={statusBadge(r.status)}>{r.status.replace('_', ' ')}</span></h2>
                    <div className="portal-header-actions">
                      <button className="button button-ghost" type="button" onClick={() => startEdit(r)}>Edit</button>
                      <button className="button button-danger" type="button" onClick={() => remove(r.id)}>Delete</button>
                    </div>
                  </div>
                  <dl className="portal-meta">
                    <div><dt>Role</dt><dd>{r.role}</dd></div>
                    <div><dt>Skills</dt><dd>{r.skills ?? '\u2014'}</dd></div>
                    <div><dt>Experience</dt><dd>{r.experienceYears != null ? `${r.experienceYears} years` : '\u2014'}</dd></div>
                    <div><dt>Availability</dt><dd>{r.availability ?? '\u2014'}</dd></div>
                    <div><dt>Project</dt><dd>{projectName(r.assignedProjectId)}</dd></div>
                  </dl>
                  <label style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span className="portal-note">Set status:</span>
                    <select value={r.status} onChange={e => updateStatus(r.id, e.target.value)}>
                      {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
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