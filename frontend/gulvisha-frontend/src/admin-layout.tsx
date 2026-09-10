import { ReactNode, useEffect, useState } from 'react'
import { getAuthHeaders, useAuth } from './auth'

type AdminLayoutProps = {
  children: ReactNode
  currentPath: string
}

const navItems = [
  { path: '/admin', label: 'Dashboard', icon: '◫' },
  { path: '/workflows', label: 'Workflows', icon: '⚡' },
  { path: '/ai', label: 'AI', icon: '✦' },
  { path: '/users', label: 'Users', icon: '☻' },
  { path: '/settings', label: 'Settings', icon: '⚙' },
]

type OrgInfo = {
  name: string
  displayName: string | null
  primaryColor: string | null
  accentColor: string | null
}

export default function AdminLayout({ children, currentPath }: AdminLayoutProps) {
  const { logout, auth } = useAuth()
  const [org, setOrg] = useState<OrgInfo | null>(null)

  useEffect(() => {
    fetch('/api/organizations/current', { headers: getAuthHeaders() })
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then((data) => setOrg(data as OrgInfo))
      .catch(() => setOrg(null))
  }, [])

  const brandName = org?.displayName || org?.name || 'Platform'
  const brandInitial = brandName.charAt(0).toUpperCase()

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-brand">
          <span className="admin-sidebar-logo">{brandInitial}</span>
          <div>
            <strong>{brandName}</strong>
            <small>Admin</small>
          </div>
        </div>
        <nav className="admin-sidebar-nav">
          {navItems.map((item) => (
            <a
              key={item.path}
              href={item.path}
              className={`admin-sidebar-link ${currentPath === item.path ? 'active' : ''}`}
            >
              <span className="admin-sidebar-icon">{item.icon}</span>
              {item.label}
            </a>
          ))}
        </nav>
        <div className="admin-sidebar-footer">
          <span className="admin-sidebar-user">{auth?.username || 'admin'}</span>
          <button className="button button-ghost button-small" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="admin-content">
        {children}
      </main>
    </div>
  )
}
