import { useEffect, useState } from 'react'
import { getAuthHeaders, useAuth } from './auth'

type Organization = {
  id: string
  name: string
  displayName: string | null
  slug: string | null
  status: string
  email: string | null
  industry: string | null
  createdAt: string
}

const STATUS_BADGES: Record<string, string> = {
  ONBOARDING: 'badge-warning',
  ACTIVE: 'badge-success',
  SUSPENDED: 'badge-danger',
}

function json<T>(response: Response | Promise<Response>): Promise<T> {
  return Promise.resolve(response).then((r) => {
    if (!r.ok) throw new Error(`Request failed (${r.status})`)
    return r.json() as Promise<T>
  })
}

export default function PlatformAdminPage() {
  const { auth, logout } = useAuth()
  const [orgs, setOrgs] = useState<Organization[]>([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({
    name: '', displayName: '', slug: '', description: '', industry: '',
    email: '', phone: '', website: '', address: '',
    primaryColor: '#315941', accentColor: '#c94f2c',
    adminUsername: '', adminPassword: '', adminName: '', adminEmail: '',
  })

  const load = () => {
    fetch('/api/admin/organizations', { headers: getAuthHeaders() })
      .then((r) => json<Organization[]>(r))
      .then(setOrgs)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const flash = (msg: string) => { setMessage(msg); setTimeout(() => setMessage(''), 3000) }
  const fail = (err: unknown) => setError(err instanceof Error ? err.message : 'Request failed')

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await json(fetch('/api/admin/organizations', {
        method: 'POST',
        headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      }))
      flash('Tenant created successfully')
      setShowCreate(false)
      setForm({ name: '', displayName: '', slug: '', description: '', industry: '', email: '', phone: '', website: '', address: '', primaryColor: '#315941', accentColor: '#c94f2c', adminUsername: '', adminPassword: '', adminName: '', adminEmail: '' })
      load()
    } catch (err) { fail(err) }
  }

  const changeStatus = async (id: string, action: string) => {
    try {
      await json(fetch(`/api/admin/organizations/${id}/${action}`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
      }))
      flash(`Tenant ${action}ed`)
      load()
    } catch (err) { fail(err) }
  }

  const remove = async (id: string) => {
    if (!confirm('Delete this tenant? This cannot be undone.')) return
    try {
      await json(fetch(`/api/admin/organizations/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      }))
      flash('Tenant deleted')
      load()
    } catch (err) { fail(err) }
  }

  if (!auth) return null

  return (
    <div className="portal-container">
      <div className="portal-header">
        <div>
          <h1>Platform Administration</h1>
          <p className="portal-note">Manage tenants across the platform</p>
        </div>
        <div style={{ display: 'flex', gap: '.75rem' }}>
          <button className="button button-quiet" onClick={() => logout()}>Sign Out</button>
          <button className="button button-primary" onClick={() => setShowCreate(!showCreate)}>
            {showCreate ? 'Cancel' : 'Create Tenant'}
          </button>
        </div>
      </div>

      {message && <div className="alert-success" style={{ margin: '1rem 0' }}>{message}</div>}
      {error && <div className="alert-error" style={{ margin: '1rem 0' }}>{error}</div>}

      {showCreate && (
        <form className="settings-grid" onSubmit={create}>
          <h3>Organization</h3>
          <label>Company Name<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          <label>Display Name<input value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></label>
          <label>Slug<input required value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} /></label>
          <label>Description<textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} /></label>
          <label>Industry<input value={form.industry} onChange={(e) => setForm({ ...form, industry: e.target.value })} /></label>
          <label>Email<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
          <label>Phone<input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>
          <label>Website<input value={form.website} onChange={(e) => setForm({ ...form, website: e.target.value })} /></label>
          <label>Address<textarea value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} rows={2} /></label>

          <h3 style={{ marginTop: '1.5rem' }}>Branding</h3>
          <div style={{ display: 'flex', gap: '1rem' }}>
            <label>Primary Color<input type="color" value={form.primaryColor} onChange={(e) => setForm({ ...form, primaryColor: e.target.value })} /></label>
            <label>Accent Color<input type="color" value={form.accentColor} onChange={(e) => setForm({ ...form, accentColor: e.target.value })} /></label>
          </div>

          <h3 style={{ marginTop: '1.5rem' }}>Initial Admin User</h3>
          <div className="settings-grid">
            <label>Username<input required value={form.adminUsername} onChange={(e) => setForm({ ...form, adminUsername: e.target.value })} /></label>
            <label>Password<input required type="password" value={form.adminPassword} onChange={(e) => setForm({ ...form, adminPassword: e.target.value })} /></label>
            <label>Full Name<input value={form.adminName} onChange={(e) => setForm({ ...form, adminName: e.target.value })} /></label>
            <label>Email<input type="email" value={form.adminEmail} onChange={(e) => setForm({ ...form, adminEmail: e.target.value })} /></label>
          </div>
          <div className="portal-header-actions" style={{ marginTop: '1rem' }}>
            <button className="button button-primary" type="submit">Create Tenant</button>
          </div>
        </form>
      )}

      {loading ? (
        <p>Loading tenants...</p>
      ) : orgs.length === 0 ? (
        <p className="portal-note">No tenants yet.</p>
      ) : (
        <div className="portal-card">
          <h2>Tenants ({orgs.length})</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '1rem' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '0.75rem' }}>Name</th>
                <th style={{ padding: '0.75rem' }}>Slug</th>
                <th style={{ padding: '0.75rem' }}>Status</th>
                <th style={{ padding: '0.75rem' }}>Industry</th>
                <th style={{ padding: '0.75rem' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {orgs.map((org) => (
                <tr key={org.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '0.75rem' }}>
                    <strong>{org.displayName || org.name}</strong>
                    <br /><small style={{ color: '#64748b' }}>{org.email}</small>
                  </td>
                  <td style={{ padding: '0.75rem' }}>{org.slug}</td>
                  <td style={{ padding: '0.75rem' }}>
                    <span className={`portal-badge ${STATUS_BADGES[org.status] || 'badge'}`}>{org.status}</span>
                  </td>
                  <td style={{ padding: '0.75rem' }}>{org.industry}</td>
                  <td style={{ padding: '0.75rem' }}>
                    {org.status === 'ONBOARDING' && (
                      <button className="button button-small button-primary" onClick={() => changeStatus(org.id, 'activate')}>Activate</button>
                    )}
                    {org.status === 'ACTIVE' && (
                      <button className="button button-small button-ghost" onClick={() => changeStatus(org.id, 'suspend')}>Suspend</button>
                    )}
                    {org.status === 'SUSPENDED' && (
                      <button className="button button-small button-primary" onClick={() => changeStatus(org.id, 'activate')}>Reactivate</button>
                    )}
                    <button className="button button-small button-danger" onClick={() => remove(org.id)} style={{ marginLeft: '0.5rem' }}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}