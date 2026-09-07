import { useEffect, useState } from 'react'
import { getAuthHeaders } from './auth'

type UserRow = {
  id: string
  username: string
  email: string
  fullName: string | null
  role: string
  status: string
  createdAt: string
}

// PLATFORM_ADMIN is intentionally excluded — platform-level accounts are seeded, not created via UI
const assignableRoles = ['ORGANIZATION_ADMIN', 'MANAGER', 'EMPLOYEE', 'AGENT', 'CLIENT']

function json<T>(response: Response): Promise<T> {
  if (!response.ok) throw new Error(`Request failed (${response.status})`)
  return response.json() as Promise<T>
}

const emptyCreate = { username: '', email: '', password: '', fullName: '', role: 'EMPLOYEE' }

function statusBadge(status: string): string {
  return `portal-badge portal-badge-${status.toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserRow[]>([])
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const [createForm, setCreateForm] = useState(emptyCreate)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editForm, setEditForm] = useState({ email: '', fullName: '', role: 'EMPLOYEE' })
  const [passwordUserId, setPasswordUserId] = useState<string | null>(null)
  const [newPassword, setNewPassword] = useState('')

  async function refresh() {
    try {
      const response = await fetch('/api/users', { headers: getAuthHeaders() })
      setUsers(await json<UserRow[]>(response))
      setLoaded(true)
      setError('')
    } catch (err) {
      setError(err instanceof Error && err.message.includes('403')
        ? 'This account cannot manage users. Sign in as an admin.'
        : 'Could not load users. Is the backend running?')
    }
  }

  useEffect(() => { refresh() }, [])

  function flash(text: string) { setMessage(text); setError('') }
  function fail(err: unknown) { setError(err instanceof Error ? err.message : 'Something went wrong'); setMessage('') }

  function api(url: string, method: string, body: unknown): Promise<Response> {
    return fetch(url, {
      method,
      headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  }

  async function createUser(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      await json(await api('/api/users', 'POST', createForm))
      setCreateForm(emptyCreate)
      flash('User created')
      await refresh()
    } catch (err) { fail(err) }
  }

  async function saveEdit(userId: string) {
    try {
      await json(await api(`/api/users/${userId}`, 'PUT', editForm))
      setEditingId(null)
      flash('User updated')
      await refresh()
    } catch (err) { fail(err) }
  }

  async function resetPassword(userId: string) {
    try {
      await json(await api(`/api/users/${userId}/password`, 'PATCH', { newPassword }))
      setPasswordUserId(null)
      setNewPassword('')
      flash('Password updated')
    } catch (err) { fail(err) }
  }

  async function toggleStatus(user: UserRow) {
    const next = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await json(await api(`/api/users/${user.id}/status`, 'PATCH', { status: next }))
      flash(next === 'ACTIVE' ? `${user.username} activated` : `${user.username} deactivated`)
      await refresh()
    } catch (err) { fail(err) }
  }
  return (
    <div className="portal-shell">
      <header className="portal-header">
        <div>
          <p className="eyebrow">USER MANAGEMENT</p>
          <h1>Team members</h1>
        </div>
        <div className="portal-header-actions">
          <a className="button button-ghost" href="/admin">← Dashboard</a>
          
        </div>
      </header>

      {message && <p className="form-message success">{message}</p>}
      {error && <p className="form-message error" role="alert">{error}</p>}

      {!loaded && !error && <p className="portal-note">Loading users…</p>}

      {loaded && !error && (
        <>
          <form className="portal-card" onSubmit={createUser}>
            <h2>Add user</h2>
            <div className="settings-grid">
              <label>Username<input required minLength={3} value={createForm.username} onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })} /></label>
              <label>Email<input required type="email" value={createForm.email} onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })} /></label>
              <label>Full name<input value={createForm.fullName} onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })} /></label>
              <label>Role
                <select value={createForm.role} onChange={(e) => setCreateForm({ ...createForm, role: e.target.value })}>
                  {assignableRoles.map((role) => <option key={role} value={role}>{role}</option>)}
                </select>
              </label>
              <label className="settings-full">Password (min 8 characters)<input required minLength={8} type="password" value={createForm.password} onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })} /></label>
            </div>
            <button className="button button-primary" type="submit">Create user</button>
          </form>

          {users.map((user) => (
            <article key={user.id} className="portal-card">
              {editingId === user.id ? (
                <form onSubmit={(e) => { e.preventDefault(); saveEdit(user.id) }}>
                  <div className="settings-grid">
                    <label>Email<input required type="email" value={editForm.email} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} /></label>
                    <label>Full name<input value={editForm.fullName} onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })} /></label>
                    <label>Role
                      <select value={editForm.role} onChange={(e) => setEditForm({ ...editForm, role: e.target.value })}>
                        {assignableRoles.map((role) => <option key={role} value={role}>{role}</option>)}
                      </select>
                    </label>
                  </div>
                  <div className="portal-header-actions">
                    <button className="button button-primary" type="submit">Save</button>
                    <button className="button button-ghost" type="button" onClick={() => setEditingId(null)}>Cancel</button>
                  </div>
                </form>
              ) : (
                <>
                  <div className="portal-card-head">
                    <h2>{user.username} <span className="portal-note">({user.fullName || 'no name'})</span></h2>
                    <div className="portal-header-actions">
                      <button className="button button-ghost" type="button" onClick={() => { setEditingId(user.id); setEditForm({ email: user.email, fullName: user.fullName ?? '', role: user.role }) }}>Edit</button>
                      <button className="button button-ghost" type="button" onClick={() => { setPasswordUserId(passwordUserId === user.id ? null : user.id); setNewPassword('') }}>Reset password</button>
                      <button className="button button-ghost" type="button" onClick={() => toggleStatus(user)}>{user.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
                    </div>
                  </div>
                  <dl className="portal-meta">
                    <div><dt>Email</dt><dd>{user.email}</dd></div>
                    <div><dt>Role</dt><dd>{user.role}</dd></div>
                    <div><dt>Status</dt><dd><span className={statusBadge(user.status)}>{user.status}</span></dd></div>
                    <div><dt>Created</dt><dd>{user.createdAt.slice(0, 10)}</dd></div>
                  </dl>
                  {passwordUserId === user.id && (
                    <form onSubmit={(e) => { e.preventDefault(); resetPassword(user.id) }}>
                      <label>New password (min 8 characters)
                        <input required minLength={8} type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
                      </label>
                      <div className="portal-header-actions">
                        <button className="button button-primary" type="submit">Update password</button>
                        <button className="button button-ghost" type="button" onClick={() => setPasswordUserId(null)}>Cancel</button>
                      </div>
                    </form>
                  )}
                </>
              )}
            </article>
          ))}
        </>
      )}
    </div>
  )
}

