import { useEffect, useState } from 'react'
import { getAuthHeaders } from './auth'

type Task = { id: string; projectId: string; title: string; description: string | null; assignedTo: string | null; status: string; priority: string; dueDate: string | null }
type ProjectOption = { id: string; name: string }
const STATUSES = ['TO_DO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'CANCELLED']
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

function json<T>(response: Response): Promise<T> {
  if (!response.ok) throw new Error(`Request failed (${response.status})`)
  return response.json() as Promise<T>
}
function statusBadge(status: string): string {
  return `portal-badge portal-badge-${status.toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function TasksPage() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [projects, setProjects] = useState<ProjectOption[]>([])
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState({ title: '', description: '', projectId: '', assignedTo: '', status: 'TO_DO', priority: 'MEDIUM', dueDate: '' })

  async function refresh() {
    try {
      const res = await fetch('/api/tasks?size=50', { headers: getAuthHeaders() })
      setTasks((await json<{ content: Task[] }>(res)).content)
      setLoaded(true); setError('')
    } catch { setError('Could not load tasks. Is the backend running?') }
  }

  useEffect(() => {
    refresh()
    fetch('/api/projects?size=100', { headers: getAuthHeaders() }).then(r => json<{ content: ProjectOption[] }>(r)).then(d => setProjects(d.content)).catch(() => {})
  }, [])

  const flash = (t: string) => { setMessage(t); setTimeout(() => setMessage(''), 3000) }
  const fail = (e: unknown) => { setError(e instanceof Error ? e.message : 'Something went wrong'); setMessage('') }
  const api = (url: string, method: string, body?: unknown) => fetch(url, { method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) })
  const resetForm = () => setForm({ title: '', description: '', projectId: '', assignedTo: '', status: 'TO_DO', priority: 'MEDIUM', dueDate: '' })
  const projectName = (id: string) => projects.find(p => p.id === id)?.name || '\u2014'

  async function create(e: React.FormEvent) {
    e.preventDefault()
    try { await json(await api('/api/tasks', 'POST', form)); flash('Task created'); resetForm(); await refresh() } catch (e) { fail(e) }
  }
  async function saveEdit(id: string) {
    try { await json(await api(`/api/tasks/${id}`, 'PUT', form)); flash('Task updated'); setEditingId(null); await refresh() } catch (e) { fail(e) }
  }
  async function updateStatus(id: string, status: string) {
    try { await json(await api(`/api/tasks/${id}/status`, 'PATCH', { status })); flash('Status updated'); await refresh() } catch (e) { fail(e) }
  }
  async function remove(id: string) {
    if (!confirm('Delete this task?')) return
    try { await json(await api(`/api/tasks/${id}`, 'DELETE')); flash('Task deleted'); await refresh() } catch (e) { fail(e) }
  }
  function startEdit(t: Task) {
    setEditingId(t.id)
    setForm({ title: t.title, description: t.description || '', projectId: t.projectId, assignedTo: t.assignedTo || '', status: t.status, priority: t.priority, dueDate: t.dueDate || '' })
  }

  return (
    <div className="portal-container">
      <div className="portal-header">
        <div>
          <h1>Tasks</h1>
          <p className="portal-note">Manage tasks across projects</p>
        </div>
      </div>
      {message && <div className="alert alert-success" onClick={() => setMessage('')}>{message}</div>}
      {error && <div className="alert alert-error" onClick={() => setError('')}>{error}</div>}

      <section className="portal-card">
        <h2>Create task</h2>
        <form onSubmit={create}>
          <div className="settings-grid">
            <label>Title<input required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></label>
            <label>Project<select required value={form.projectId} onChange={e => setForm({ ...form, projectId: e.target.value })}>
              <option value="">\u2014 Select project \u2014</option>
              {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select></label>
            <label>Assigned to<input value={form.assignedTo} onChange={e => setForm({ ...form, assignedTo: e.target.value })} /></label>
            <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
            </select></label>
            <label>Priority<select value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })}>
              {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
            </select></label>
            <label>Due date<input type="date" value={form.dueDate} onChange={e => setForm({ ...form, dueDate: e.target.value })} /></label>
            <label className="settings-full">Description<textarea rows={2} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
          </div>
          <div className="portal-header-actions"><button className="button button-primary" type="submit">Create task</button></div>
        </form>
      </section>

      {!loaded ? <p>Loading tasks...</p> : tasks.length === 0 ? (
        <p className="portal-note">No tasks yet.</p>
      ) : (
        <section className="portal-list">
          {tasks.map(t => (
            <article key={t.id} className="portal-card">
              {editingId === t.id ? (
                <form onSubmit={e => { e.preventDefault(); saveEdit(t.id) }}>
                  <div className="settings-grid">
                    <label>Title<input required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></label>
                    <label>Project<select required value={form.projectId} onChange={e => setForm({ ...form, projectId: e.target.value })}>
                      {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select></label>
                    <label>Assigned to<input value={form.assignedTo} onChange={e => setForm({ ...form, assignedTo: e.target.value })} /></label>
                    <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                      {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select></label>
                    <label>Priority<select value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })}>
                      {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
                    </select></label>
                    <label>Due date<input type="date" value={form.dueDate} onChange={e => setForm({ ...form, dueDate: e.target.value })} /></label>
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
                    <h2>{t.title} <span className={statusBadge(t.status)}>{t.status.replace('_', ' ')}</span></h2>
                    <div className="portal-header-actions">
                      <button className="button button-ghost" type="button" onClick={() => startEdit(t)}>Edit</button>
                      <button className="button button-danger" type="button" onClick={() => remove(t.id)}>Delete</button>
                    </div>
                  </div>
                  {t.description && <p>{t.description}</p>}
                  <dl className="portal-meta">
                    <div><dt>Project</dt><dd>{projectName(t.projectId)}</dd></div>
                    <div><dt>Assigned to</dt><dd>{t.assignedTo ?? '\u2014'}</dd></div>
                    <div><dt>Priority</dt><dd>{t.priority}</dd></div>
                    <div><dt>Due</dt><dd>{t.dueDate ?? '\u2014'}</dd></div>
                  </dl>
                  <label style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span className="portal-note">Set status:</span>
                    <select value={t.status} onChange={e => updateStatus(t.id, e.target.value)}>
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