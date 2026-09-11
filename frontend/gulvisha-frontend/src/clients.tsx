import { useEffect, useState } from 'react'
import { getAuthHeaders } from './auth'

type Client = {
  id: string
  name: string
  firstName: string | null
  lastName: string | null
  email: string | null
  phone: string | null
  industry: string | null
  address: string | null
  website: string | null
  status: string
  assignedManager: string | null
}

const STATUSES = ['ACTIVE', 'INACTIVE', 'ON HOLD', 'CLOSED']

function json<T>(response: Response): Promise<T> {
  if (!response.ok) throw new Error(`Request failed (${response.status})`)
  return response.json() as Promise<T>
}
function statusBadge(status: string): string {
  return `portal-badge portal-badge-${status.toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([])
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState({ name: '', firstName: '', lastName: '', email: '', phone: '', industry: '', address: '', website: '', status: 'ACTIVE', assignedManager: '' })
  const [portalUserTarget, setPortalUserTarget] = useState<Client | null>(null)
  const [portalForm, setPortalForm] = useState({ username: '', email: '', fullName: '', password: '' })
  const [busy, setBusy] = useState(false)

  async function refresh() {
    try {
      const res = await fetch('/api/clients?size=100', { headers: getAuthHeaders() })
      setClients((await json<{ content: Client[] }>(res)).content)
      setLoaded(true); setError('')
    } catch { setError('Could not load clients. Is the backend running?') }
  }

  useEffect(() => { refresh() }, [])

  const flash = (t: string) => { setMessage(t); setTimeout(() => setMessage(''), 3000) }
  const fail = (e: unknown) => { setError(e instanceof Error ? e.message : 'Something went wrong'); setMessage('') }
  const api = (url: string, method: string, body?: unknown) => fetch(url, { method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) })
  const resetForm = () => setForm({ name: '', firstName: '', lastName: '', email: '', phone: '', industry: '', address: '', website: '', status: 'ACTIVE', assignedManager: '' })

  async function create(e: React.FormEvent) {
    e.preventDefault()
    try { await json(await api('/api/clients', 'POST', form)); flash('Client created'); resetForm(); await refresh() } catch (e) { fail(e) }
  }
  async function saveEdit(id: string) {
    try { await json(await api(`/api/clients/${id}`, 'PUT', form)); flash('Client updated'); setEditingId(null); await refresh() } catch (e) { fail(e) }
  }
  async function updateStatus(id: string, status: string) {
    try { await json(await api(`/api/clients/${id}`, 'PUT', { ...clients.find(c => c.id === id), status })); flash('Status updated'); await refresh() } catch (e) { fail(e) }
  }
  async function remove(id: string) {
    if (!confirm('Delete this client?')) return
    try { await json(await api(`/api/clients/${id}`, 'DELETE')); flash('Client deleted'); await refresh() } catch (e) { fail(e) }
  }
  function startEdit(c: Client) {
    setEditingId(c.id)
    setForm({ name: c.name, firstName: c.firstName || '', lastName: c.lastName || '', email: c.email || '', phone: c.phone || '', industry: c.industry || '', address: c.address || '', website: c.website || '', status: c.status, assignedManager: c.assignedManager || '' })
  }

  async function createPortalUser() {
    if (!portalUserTarget) return
    if (!portalForm.username.trim() || !portalForm.email.trim() || !portalForm.password.trim()) {
      setError('Username, email and password are required'); return
    }
    setBusy(true); setError('')
    try {
      await json(await api(`/api/clients/${portalUserTarget.id}/portal-user`, 'POST', {
        username: portalForm.username.trim(),
        email: portalForm.email.trim(),
        fullName: portalForm.fullName.trim() || `${portalUserTarget.name} Contact`,
        password: portalForm.password,
      }))
      flash(`Portal user "${portalForm.username}" created. They can now sign in at /portal.`)
      setPortalUserTarget(null); setPortalForm({ username: '', email: '', fullName: '', password: '' })
    } catch (e) { fail(e) } finally { setBusy(false) }
  }
  return (
    <div>
      <div className="portal-card-header"><h1>Clients</h1><p className="portal-note">Manage your clients and provision their portal login.</p></div>
      {message && <div className="alert alert-success" onClick={() => setMessage('')}>{message} <span>&times;</span></div>}
      {error && <div className="alert alert-error" onClick={() => setError('')}>{error} <span>&times;</span></div>}
      <div className="portal-card"><h2>{editingId ? 'Edit client' : 'Add client'}</h2>
        <form onSubmit={editingId ? e => { e.preventDefault(); saveEdit(editingId) } : create}>
          <div className="settings-grid">
            <label>Company name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
            <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>{STATUSES.map(s => <option key={s} value={s}>{s}</option>)}</select></label>
            <label>First name<input value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} /></label>
            <label>Last name<input value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} /></label>
            <label>Email<input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></label>
            <label>Phone<input value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></label>
            <label>Industry<input value={form.industry} onChange={e => setForm({ ...form, industry: e.target.value })} /></label>
            <label>Website<input value={form.website} onChange={e => setForm({ ...form, website: e.target.value })} /></label>
            <label className="settings-full">Address<textarea rows={2} value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} /></label>
            <label>Assigned manager<input value={form.assignedManager} onChange={e => setForm({ ...form, assignedManager: e.target.value })} /></label>
          </div>
          <div className="portal-header-actions"><button className="button button-primary" type="submit">{editingId ? 'Save' : 'Create client'}</button>{editingId && <button className="button button-ghost" type="button" onClick={() => setEditingId(null)}>Cancel</button>}</div>
        </form>
      </div>
      {!loaded ? (<p className="portal-note">Loading...</p>) : clients.length === 0 ? (<p className="portal-note">No clients yet. Create one above.</p>) : (
        <section className="portal-list">
          {clients.map(c => (
            <article key={c.id} className="portal-card">
              <div className="portal-card-head"><h2>{c.name} <span className={statusBadge(c.status)}>{c.status}</span></h2>
                <div className="portal-header-actions">
                  <button className="button button-primary" type="button" onClick={() => { setPortalUserTarget(c); setPortalForm({ username: '', email: c.email || '', fullName: `${c.firstName || ''} ${c.lastName || ''}`.trim(), password: '' }) }}>Create portal user</button>
                  <button className="button button-ghost" type="button" onClick={() => startEdit(c)}>Edit</button>
                  <button className="button button-danger" type="button" onClick={() => remove(c.id)}>Delete</button>
                </div>
              </div>
              <dl className="portal-meta">
                <div><dt>Contact</dt><dd>{[c.firstName, c.lastName].filter(Boolean).join(' ') || 'â€”'}</dd></div>
                <div><dt>Email</dt><dd>{c.email ?? 'â€”'}</dd></div>
                <div><dt>Phone</dt><dd>{c.phone ?? 'â€”'}</dd></div>
                <div><dt>Industry</dt><dd>{c.industry ?? 'â€”'}</dd></div>
                <div><dt>Manager</dt><dd>{c.assignedManager ?? 'â€”'}</dd></div>
              </dl>
              <label style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}><span className="portal-note">Set status:</span><select value={c.status} onChange={e => updateStatus(c.id, e.target.value)}>{STATUSES.map(s => <option key={s} value={s}>{s}</option>)}</select></label>
            </article>
          ))}
        </section>
      )}
      {portalUserTarget && (
        <div className="portal-card">
          <div className="portal-card-head"><h2>Create portal user for {portalUserTarget.name}</h2><button className="button button-ghost" type="button" onClick={() => setPortalUserTarget(null)}>&times;</button></div>
          <p className="portal-note">This creates a CLIENT-role account linked to this client. They sign in at <code>/portal</code> and see only their own data.</p>
          <div className="settings-grid">
            <label>Username<input required value={portalForm.username} onChange={e => setPortalForm({ ...portalForm, username: e.target.value })} placeholder="e.g. acme-contact" /></label>
            <label>Email<input required type="email" value={portalForm.email} onChange={e => setPortalForm({ ...portalForm, email: e.target.value })} /></label>
            <label>Full name<input value={portalForm.fullName} onChange={e => setPortalForm({ ...portalForm, fullName: e.target.value })} /></label>
            <label>Temp password<input required type="text" value={portalForm.password} onChange={e => setPortalForm({ ...portalForm, password: e.target.value })} placeholder="min 8 chars" /></label>
          </div>
          <div className="portal-header-actions"><button className="button button-primary" type="button" onClick={() => void createPortalUser()} disabled={busy}>{busy ? 'Creating...' : 'Create portal user'}</button></div>
        </div>
      )}
    </div>
  )
}