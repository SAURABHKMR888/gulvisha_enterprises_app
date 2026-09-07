import { useEffect, useState } from 'react'
import { getAuthHeaders, useAuth } from './auth'

type DashboardSummary = {
  totalEnquiries: number
  newEnquiries: number
  qualifiedLeads: number
  inProgressEnquiries: number
  closedEnquiries: number
  rejectedEnquiries: number
  archivedEnquiries: number
}

type AdminEnquiry = {
  id: string
  name: string
  email: string
  company: string
  service: string
  phone: string
  serviceCategory: string
  budget: string
  timeline: string
  source: string
  preferredContactMethod: string
  details: string
  status: string
  internalNotes: string
  receivedAt: string
}

type EnquiryPage = {
  content: AdminEnquiry[]
  totalElements: number
}

type AuditEntry = {
  id: string
  action: string
  previousValue: string | null
  newValue: string | null
  actor: string
  createdAt: string
}

const statusOptions = ['NEW', 'QUALIFIED', 'IN_PROGRESS', 'CLOSED', 'REJECTED', 'ARCHIVED']

function csvValue(value: string | number): string {
  return `"${String(value ?? '').replace(/"/g, '""')}"`
}

function readPreference(key: string, fallback: string): string {
  if (typeof window === 'undefined') return fallback
  return window.localStorage.getItem(`gulvisha-admin-${key}`) || fallback
}

export default function AdminPage() {
  const { logout } = useAuth()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [enquiries, setEnquiries] = useState<AdminEnquiry[]>([])
  const [totalEnquiries, setTotalEnquiries] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(() => Number(readPreference('page-size', '20')))
  const [statusFilter, setStatusFilter] = useState(() => readPreference('status', 'ALL'))
  const [fromDate, setFromDate] = useState(() => readPreference('from-date', ''))
  const [toDate, setToDate] = useState(() => readPreference('to-date', ''))
  const [sortBy, setSortBy] = useState(() => readPreference('sort', 'newest'))
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState<string | null>(null)
  const [savingNotesId, setSavingNotesId] = useState<string | null>(null)
  const [isExporting, setIsExporting] = useState(false)
  const [auditById, setAuditById] = useState<Record<string, AuditEntry[]>>({})
  const [loadingAuditId, setLoadingAuditId] = useState<string | null>(null)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [bulkStatus, setBulkStatus] = useState('QUALIFIED')

  useEffect(() => {
    fetch('/api/admin/dashboard', { headers: getAuthHeaders() })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Unauthorized')
        }
        return response.json() as Promise<DashboardSummary>
      })
      .then(setSummary)
      .catch(() => setError('Admin dashboard is unavailable. Start the backend and confirm the admin credentials.'))
  }, [refreshKey])

  useEffect(() => {
    const params = new URLSearchParams({ page: String(page), size: String(pageSize) })
    if (statusFilter !== 'ALL') params.set('status', statusFilter)
    if (search.trim()) params.set('search', search.trim())
    if (fromDate) params.set('from', fromDate)
    if (toDate) params.set('to', toDate)
    const query = `/api/admin/quote-requests?${params.toString()}`

    fetch(query, { headers: getAuthHeaders() })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Unauthorized')
        }
        return response.json() as Promise<EnquiryPage>
      })
      .then((result) => {
        setEnquiries(result.content ?? [])
        setTotalEnquiries(result.totalElements ?? 0)
      })
      .catch(() => setError('Enquiry list is unavailable. Check backend authentication settings.'))
  }, [statusFilter, search, fromDate, toDate, page, pageSize, refreshKey])

  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(searchInput.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    window.localStorage.setItem('gulvisha-admin-page-size', String(pageSize))
    window.localStorage.setItem('gulvisha-admin-status', statusFilter)
    window.localStorage.setItem('gulvisha-admin-from-date', fromDate)
    window.localStorage.setItem('gulvisha-admin-to-date', toDate)
    window.localStorage.setItem('gulvisha-admin-sort', sortBy)
  }, [pageSize, statusFilter, fromDate, toDate, sortBy])

  async function updateStatus(id: string, status: string) {
    setUpdatingId(id)

    try {
      const response = await fetch(`/api/admin/quote-requests/${id}/status`, {
        method: 'PATCH',
        headers: {
          ...Object.fromEntries(getAuthHeaders().entries()),
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status }),
      })

      if (!response.ok) {
        throw new Error('Status update failed')
      }

      const updated = await response.json() as AdminEnquiry
      setEnquiries((current) => current.map((item) => item.id === id ? { ...item, status: updated.status } : item))
      setRefreshKey((current) => current + 1)
    } catch {
      setError('Unable to update enquiry status right now.')
    } finally {
      setUpdatingId(null)
    }
  }

  async function saveNotes(id: string, internalNotes: string) {
    setSavingNotesId(id)
    try {
      const response = await fetch(`/api/admin/quote-requests/${id}/notes`, {
        method: 'PATCH',
        headers: {
          ...Object.fromEntries(getAuthHeaders().entries()),
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ internalNotes }),
      })
      if (!response.ok) throw new Error('Notes update failed')
      const updated = await response.json() as AdminEnquiry
      setEnquiries((current) => current.map((item) => item.id === id ? { ...item, internalNotes: updated.internalNotes } : item))
    } catch {
      setError('Unable to save internal notes right now.')
    } finally {
      setSavingNotesId(null)
    }
  }

  async function loadAudit(id: string) {
    setLoadingAuditId(id)
    try {
      const response = await fetch(`/api/admin/quote-requests/${id}/audit`, { headers: getAuthHeaders() })
      if (!response.ok) throw new Error('Audit request failed')
      const history = await response.json() as AuditEntry[]
      setAuditById((current) => ({ ...current, [id]: history }))
    } catch {
      setError('Unable to load enquiry history right now.')
    } finally {
      setLoadingAuditId(null)
    }
  }

  async function updateBulkStatus() {
    if (selectedIds.length === 0) return
    try {
      const response = await fetch('/api/admin/quote-requests/bulk-status', {
        method: 'PATCH',
        headers: {
          ...Object.fromEntries(getAuthHeaders().entries()),
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ ids: selectedIds, status: bulkStatus }),
      })
      if (!response.ok) throw new Error('Bulk status update failed')
      setSelectedIds([])
      setRefreshKey((current) => current + 1)
    } catch {
      setError('Unable to update the selected enquiry statuses right now.')
    }
  }

  async function exportEnquiries() {
    setIsExporting(true)
    try {
      const exportPageSize = 100
      const exportPages = Math.max(1, Math.ceil(totalEnquiries / exportPageSize))
      const allEnquiries: AdminEnquiry[] = []
      for (let exportPage = 0; exportPage < exportPages; exportPage += 1) {
        const params = new URLSearchParams({ page: String(exportPage), size: String(exportPageSize) })
        if (statusFilter !== 'ALL') params.set('status', statusFilter)
        if (search.trim()) params.set('search', search.trim())
        if (fromDate) params.set('from', fromDate)
        if (toDate) params.set('to', toDate)
        const response = await fetch(`/api/admin/quote-requests?${params.toString()}`, { headers: getAuthHeaders() })
        if (!response.ok) throw new Error('Export request failed')
        const result = await response.json() as EnquiryPage
        allEnquiries.push(...(result.content ?? []))
      }

    const headers = ['Name', 'Email', 'Company', 'Service', 'Phone', 'Category', 'Budget', 'Timeline', 'Source', 'Preferred contact', 'Message', 'Internal notes', 'Status', 'Received']
    const rows = sortEnquiries(allEnquiries).map((item) => [
      item.name,
      item.email,
      item.company,
      item.service,
      item.phone,
      item.serviceCategory,
      item.budget,
      item.timeline,
      item.source,
      item.preferredContactMethod,
      item.details,
      item.internalNotes,
      item.status,
      item.receivedAt,
    ])
    const csv = [headers, ...rows].map((row) => row.map(csvValue).join(',')).join('\r\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'gulvisha-enquiries.csv'
    link.click()
    URL.revokeObjectURL(url)
    } catch {
      setError('Unable to export the filtered enquiries right now.')
    } finally {
      setIsExporting(false)
    }
  }

  function sortEnquiries(items: AdminEnquiry[]) {
    return [...items].sort((left, right) => {
    if (sortBy === 'name') return left.name.localeCompare(right.name)
    if (sortBy === 'status') return left.status.localeCompare(right.status) || right.receivedAt.localeCompare(left.receivedAt)
    if (sortBy === 'oldest') return left.receivedAt.localeCompare(right.receivedAt)
    return right.receivedAt.localeCompare(left.receivedAt)
    })
  }

  const sortedEnquiries = sortEnquiries(enquiries)

  if (!summary) {
    return <div className="admin-shell">{error ? <p className="admin-error" role="alert">{error}</p> : <p>Loading admin dashboard…</p>}</div>
  }

  const cards = [
    { label: 'Total enquiries', value: summary.totalEnquiries, filter: 'ALL' },
    { label: 'New enquiries', value: summary.newEnquiries, filter: 'NEW' },
    { label: 'Qualified leads', value: summary.qualifiedLeads, filter: 'QUALIFIED' },
    { label: 'In progress', value: summary.inProgressEnquiries, filter: 'IN_PROGRESS' },
    { label: 'Closed enquiries', value: summary.closedEnquiries, filter: 'CLOSED' },
    { label: 'Rejected enquiries', value: summary.rejectedEnquiries, filter: 'REJECTED' },
    { label: 'Archived enquiries', value: summary.archivedEnquiries, filter: 'ARCHIVED' },
  ]
  const statusMetrics = [
    { label: 'New', value: summary.newEnquiries, color: 'new' },
    { label: 'Qualified', value: summary.qualifiedLeads, color: 'qualified' },
    { label: 'In progress', value: summary.inProgressEnquiries, color: 'in-progress' },
    { label: 'Closed', value: summary.closedEnquiries, color: 'closed' },
    { label: 'Rejected', value: summary.rejectedEnquiries, color: 'rejected' },
    { label: 'Archived', value: summary.archivedEnquiries, color: 'archived' },
  ]

  return (
    <main className="admin-shell">
      <header className="admin-header">
        <div>
          <p className="eyebrow">ADMIN</p>
          <h1>Operations dashboard</h1>
        </div>
        <div className="admin-header-actions">
          <a className="admin-back-link" href="/users">Users</a>
          <a className="admin-back-link" href="/settings">Settings</a>
          <button className="admin-refresh" type="button" onClick={() => setRefreshKey((current) => current + 1)}>Refresh</button>
          <button className="admin-logout" type="button" onClick={logout}>Sign out</button>
          <a className="admin-back-link" href="/">← Back to site</a>
        </div>
      </header>
      {error && <p className="admin-error" role="alert">{error}</p>}

      <section className="admin-grid">
        {cards.map((card) => (
          <button className={`admin-card ${statusFilter === card.filter ? 'selected' : ''}`} type="button" key={card.label} onClick={() => { setStatusFilter(card.filter); setPage(0) }}>
            <p>{card.label}</p>
            <strong>{card.value}</strong>
          </button>
        ))}
      </section>

      <section className="admin-panel admin-breakdown" aria-labelledby="pipeline-heading">
        <div className="admin-panel-header">
          <h2 id="pipeline-heading">Enquiry pipeline</h2>
          <span>{summary.totalEnquiries} total</span>
        </div>
        <div className="pipeline-list">
          {statusMetrics.map((metric) => (
            <button className="pipeline-row" type="button" key={metric.label} onClick={() => { setStatusFilter(metric.label === 'In progress' ? 'IN_PROGRESS' : metric.label.toUpperCase()); setPage(0) }}>
              <span>{metric.label}</span>
              <div className="pipeline-track"><i className={metric.color} style={{ width: `${summary.totalEnquiries ? (metric.value / summary.totalEnquiries) * 100 : 0}%` }} /></div>
              <strong>{metric.value}</strong>
            </button>
          ))}
        </div>
      </section>

      <section className="admin-panel">
        <div className="admin-panel-header">
          <h2>Recent enquiries</h2>
          <div className="admin-toolbar">
            <label className="admin-search">
              Search
              <input value={searchInput} onChange={(event) => { setSearchInput(event.target.value); setPage(0) }} placeholder="Name, email, service..." />
            </label>
            <label>
              Status
              <select value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0) }}>
                <option value="ALL">All</option>
                {statusOptions.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
            <label>
              Per page
              <select value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPage(0) }}>
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </label>
            <label>
              Sort
              <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
                <option value="newest">Newest</option>
                <option value="oldest">Oldest</option>
                <option value="name">Name</option>
                <option value="status">Status</option>
              </select>
            </label>
            <span>{totalEnquiries} items</span>
            <label>
              From
              <input type="date" value={fromDate} max={toDate || undefined} onChange={(event) => { setFromDate(event.target.value); setPage(0) }} />
            </label>
            <label>
              To
              <input type="date" value={toDate} min={fromDate || undefined} onChange={(event) => { setToDate(event.target.value); setPage(0) }} />
            </label>
            {(searchInput || statusFilter !== 'ALL' || fromDate || toDate) && <button className="admin-clear" type="button" onClick={() => { setSearchInput(''); setSearch(''); setStatusFilter('ALL'); setFromDate(''); setToDate(''); setPage(0) }}>Clear filters</button>}
            <button className="admin-export" type="button" onClick={() => void exportEnquiries()} disabled={enquiries.length === 0 || isExporting}>{isExporting ? 'Exporting...' : 'Export CSV'}</button>
          </div>
        </div>

        {selectedIds.length > 0 && <div className="bulk-toolbar">
          <strong>{selectedIds.length} selected</strong>
          <select value={bulkStatus} onChange={(event) => setBulkStatus(event.target.value)}>
            {statusOptions.filter((status) => status !== 'NEW').map((status) => (
              <option key={status} value={status}>{status}</option>
            ))}
          </select>
          <button type="button" onClick={() => void updateBulkStatus()}>Update status</button>
          <button className="bulk-cancel" type="button" onClick={() => setSelectedIds([])}>Cancel</button>
        </div>}

        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th><input type="checkbox" checked={enquiries.length > 0 && selectedIds.length === enquiries.length} onChange={(event) => setSelectedIds(event.target.checked ? enquiries.map((item) => item.id) : [])} aria-label="Select all visible enquiries" /></th>
                <th>Name</th>
                <th>Company</th>
                <th>Service</th>
                <th>Submitted details</th>
                <th>Received</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {sortedEnquiries.map((item) => (
                <tr key={item.id}>
                  <td><input type="checkbox" checked={selectedIds.includes(item.id)} onChange={(event) => setSelectedIds((current) => event.target.checked ? [...current, item.id] : current.filter((id) => id !== item.id))} aria-label={`Select ${item.name}`} /></td>
                  <td>
                    <div className="user-cell">
                      <strong>{item.name}</strong>
                      <a href={`mailto:${item.email}`}>{item.email}</a>
                      {item.phone && <a href={`tel:${item.phone}`}>{item.phone}</a>}
                    </div>
                  </td>
                  <td>{item.company || '—'}</td>
                  <td>{item.service}</td>
                  <td>
                    <details className="enquiry-details">
                      <summary>View details</summary>
                      <dl>
                        <div><dt>Message</dt><dd>{item.details}</dd></div>
                        <div><dt>Phone</dt><dd>{item.phone || '—'}</dd></div>
                        <div><dt>Category</dt><dd>{item.serviceCategory || '—'}</dd></div>
                        <div><dt>Budget</dt><dd>{item.budget || '—'}</dd></div>
                        <div><dt>Timeline</dt><dd>{item.timeline || '—'}</dd></div>
                        <div><dt>Preferred contact</dt><dd>{item.preferredContactMethod || '—'}</dd></div>
                        <div><dt>Source</dt><dd>{item.source || '—'}</dd></div>
                        <div className="notes-editor"><dt>Internal notes</dt><dd><textarea defaultValue={item.internalNotes || ''} rows={3} placeholder="Add follow-up context..." id={`notes-${item.id}`} /><button type="button" onClick={() => { const notes = document.getElementById(`notes-${item.id}`) as HTMLTextAreaElement; void saveNotes(item.id, notes.value) }} disabled={savingNotesId === item.id}>{savingNotesId === item.id ? 'Saving...' : 'Save notes'}</button></dd></div>
                        <div className="audit-editor"><dt>History</dt><dd><button type="button" onClick={() => void loadAudit(item.id)} disabled={loadingAuditId === item.id}>{loadingAuditId === item.id ? 'Loading...' : auditById[item.id] ? 'Refresh history' : 'View history'}</button>{auditById[item.id]?.map((entry) => <p key={entry.id}><strong>{entry.action.replace('_', ' ')}</strong><span>{entry.previousValue || '—'} → {entry.newValue || '—'}</span><small>{new Date(entry.createdAt).toLocaleString()} by {entry.actor}</small></p>)}</dd></div>
                      </dl>
                    </details>
                  </td>
                  <td>{new Date(item.receivedAt).toLocaleDateString()}</td>
                  <td>
                    <div className="status-editor">
                      <span className={`status-badge ${item.status.toLowerCase().replace('_', '-')}`}>{item.status}</span>
                      <select value={item.status} onChange={(event) => updateStatus(item.id, event.target.value)} disabled={updatingId === item.id}>
                        {statusOptions.map((status) => (
                          <option key={status} value={status}>{status}</option>
                        ))}
                      </select>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {enquiries.length === 0 && <p className="admin-empty">No enquiries match the current filters.</p>}
        </div>
        <div className="admin-pagination">
          <button type="button" onClick={() => setPage((current) => current - 1)} disabled={page === 0}>Previous</button>
          <span>Page {page + 1} of {Math.max(1, Math.ceil(totalEnquiries / pageSize))}</span>
          <button type="button" onClick={() => setPage((current) => current + 1)} disabled={(page + 1) * pageSize >= totalEnquiries}>Next</button>
        </div>
      </section>
    </main>
  )
}
