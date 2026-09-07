import { useEffect, useState } from 'react'
import { getAuthHeaders, useAuth } from './auth'

type PortalDashboard = {
  totalProjects: number
  activeProjects: number
  openTasks: number
  inProgressTasks: number
  completedTasks: number
}

type PortalProject = {
  id: string
  name: string
  description: string | null
  service: string | null
  status: string
  startDate: string | null
  endDate: string | null
  budget: number | null
  manager: string | null
}

type PortalTask = {
  id: string
  projectId: string
  title: string
  description: string | null
  assignedTo: string | null
  status: string
  priority: string
  dueDate: string | null
}

type PortalProfile = {
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
}

const tabLabels = {
  dashboard: 'Dashboard',
  projects: 'Projects',
  tasks: 'Tasks',
  profile: 'Profile',
} as const

type TabKey = keyof typeof tabLabels

function formatDate(value: string | null | undefined): string {
  return value ? value : '—'
}

function formatBudget(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return new Intl.NumberFormat('en-US', { style: 'currency', maximumFractionDigits: 0 }).format(value)
}

function statusClass(status: string): string {
  return `portal-badge portal-badge-${status.toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function PortalPage() {
  const { logout } = useAuth()
  const [tab, setTab] = useState<TabKey>('dashboard')
  const [dashboard, setDashboard] = useState<PortalDashboard | null>(null)
  const [projects, setProjects] = useState<PortalProject[]>([])
  const [tasks, setTasks] = useState<PortalTask[]>([])
  const [profile, setProfile] = useState<PortalProfile | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadPortal() {
      try {
        const headers = getAuthHeaders()
        const [dashRes, projRes, taskRes, profRes] = await Promise.all([
          fetch('/api/portal/dashboard', { headers }),
          fetch('/api/portal/projects', { headers }),
          fetch('/api/portal/tasks', { headers }),
          fetch('/api/portal/profile', { headers }),
        ])

        if (dashRes.status === 401 || projRes.status === 401) {
          logout()
          return
        }
        if (dashRes.status === 403 || projRes.status === 403) {
          setError('This account does not have client portal access.')
          setLoading(false)
          return
        }
        if (!dashRes.ok || !projRes.ok || !taskRes.ok || !profRes.ok) {
          throw new Error('Portal data unavailable')
        }

        setDashboard((await dashRes.json()) as PortalDashboard)
        setProjects((await projRes.json()) as PortalProject[])
        setTasks((await taskRes.json()) as PortalTask[])
        setProfile((await profRes.json()) as PortalProfile)
      } catch {
        setError('Portal is unavailable. Start the backend and sign in again.')
      } finally {
        setLoading(false)
      }
    }
    loadPortal()
  }, [logout])

  return (
    <div className="portal-shell">
      <header className="portal-header">
        <div>
          <p className="eyebrow">CLIENT PORTAL</p>
          <h1>{profile?.name ?? 'Your workspace'}</h1>
        </div>
        <div className="portal-header-actions">
          <a className="button button-ghost" href="/">Visit website</a>
          <button className="button button-primary" type="button" onClick={logout}>Sign out</button>
        </div>
      </header>

      <nav className="portal-tabs" aria-label="Portal sections">
        {(Object.keys(tabLabels) as TabKey[]).map((key) => (
          <button
            key={key}
            type="button"
            className={`portal-tab${tab === key ? ' active' : ''}`}
            onClick={() => setTab(key)}
          >
            {tabLabels[key]}
          </button>
        ))}
      </nav>

      {error && <p className="form-message error">{error}</p>}
      {loading && <p className="portal-note">Loading your data…</p>}

      {!loading && !error && tab === 'dashboard' && dashboard && (
        <>
          <section className="portal-grid">
            <div className="metric-card"><span className="metric-value">{dashboard.totalProjects}</span><span className="metric-label">Total projects</span></div>
            <div className="metric-card"><span className="metric-value">{dashboard.activeProjects}</span><span className="metric-label">Active projects</span></div>
            <div className="metric-card"><span className="metric-value">{dashboard.openTasks}</span><span className="metric-label">Open tasks</span></div>
            <div className="metric-card"><span className="metric-value">{dashboard.inProgressTasks}</span><span className="metric-label">In progress</span></div>
            <div className="metric-card"><span className="metric-value">{dashboard.completedTasks}</span><span className="metric-label">Completed</span></div>
          </section>

          <section className="portal-section">
            <h2>Recent projects</h2>
            {projects.length === 0 && <p className="portal-note">No projects yet.</p>}
            {projects.slice(0, 3).map((project) => (
              <article key={project.id} className="portal-card">
                <div className="portal-card-head">
                  <h3>{project.name}</h3>
                  <span className={statusClass(project.status)}>{project.status}</span>
                </div>
                {project.description && <p>{project.description}</p>}
              </article>
            ))}
          </section>

          <section className="portal-section">
            <h2>Recent tasks</h2>
            {tasks.length === 0 && <p className="portal-note">No tasks yet.</p>}
            {tasks.slice(0, 3).map((task) => (
              <article key={task.id} className="portal-card">
                <div className="portal-card-head">
                  <h3>{task.title}</h3>
                  <span className={statusClass(task.status)}>{task.status.replace('_', ' ')}</span>
                </div>
                <p className="portal-note">{[task.assignedTo && `Assigned to ${task.assignedTo}`, task.priority, task.dueDate && `Due ${task.dueDate}`].filter(Boolean).join(' · ')}</p>
              </article>
            ))}
          </section>
        </>
      )}
      {!loading && !error && tab === 'projects' && (
        <section className="portal-list">
          {projects.length === 0 && <p className="portal-note">No projects yet.</p>}
          {projects.map((project) => (
            <article key={project.id} className="portal-card">
              <div className="portal-card-head">
                <h2>{project.name}</h2>
                <span className={statusClass(project.status)}>{project.status}</span>
              </div>
              {project.description && <p>{project.description}</p>}
              <dl className="portal-meta">
                <div><dt>Service</dt><dd>{project.service ?? '—'}</dd></div>
                <div><dt>Start</dt><dd>{formatDate(project.startDate)}</dd></div>
                <div><dt>End</dt><dd>{formatDate(project.endDate)}</dd></div>
                <div><dt>Budget</dt><dd>{formatBudget(project.budget)}</dd></div>
                <div><dt>Manager</dt><dd>{project.manager ?? '—'}</dd></div>
              </dl>
            </article>
          ))}
        </section>
      )}

      {!loading && !error && tab === 'tasks' && (
        <section className="portal-list">
          {tasks.length === 0 && <p className="portal-note">No tasks yet.</p>}
          {tasks.map((task) => (
            <article key={task.id} className="portal-card">
              <div className="portal-card-head">
                <h2>{task.title}</h2>
                <span className={statusClass(task.status)}>{task.status.replace('_', ' ')}</span>
              </div>
              {task.description && <p>{task.description}</p>}
              <dl className="portal-meta">
                <div><dt>Assigned to</dt><dd>{task.assignedTo ?? '—'}</dd></div>
                <div><dt>Priority</dt><dd>{task.priority}</dd></div>
                <div><dt>Due</dt><dd>{formatDate(task.dueDate)}</dd></div>
              </dl>
            </article>
          ))}
        </section>
      )}

      {!loading && !error && tab === 'profile' && profile && (
        <section className="portal-card portal-profile">
          <h2>{profile.name}</h2>
          <dl className="portal-meta">
            <div><dt>Contact</dt><dd>{[profile.firstName, profile.lastName].filter(Boolean).join(' ') || '—'}</dd></div>
            <div><dt>Email</dt><dd>{profile.email ?? '—'}</dd></div>
            <div><dt>Phone</dt><dd>{profile.phone ?? '—'}</dd></div>
            <div><dt>Industry</dt><dd>{profile.industry ?? '—'}</dd></div>
            <div><dt>Website</dt><dd>{profile.website ?? '—'}</dd></div>
            <div><dt>Address</dt><dd>{profile.address ?? '—'}</dd></div>
          </dl>
        </section>
      )}
    </div>
  )
}
