import { useEffect, useState } from 'react'
import { useAuth, getAuthHeaders } from './auth'

type WorkflowRow = {
  id: string
  name: string
  description: string | null
  triggerType: string
  enabled: boolean
  createdAt: string
}

type WorkflowStep = {
  id: string
  workflowId: string
  name: string | null
  actionType: string
  config: string | null
  sortOrder: number
}

type WorkflowExecution = {
  id: string
  workflowId: string
  triggerType: string | null
  entityId: string | null
  status: string
  log: string | null
  startedAt: string
  finishedAt: string | null
}

type EnquiryOption = {
  id: string
  name: string
  email: string
  company: string
  service: string
  receivedAt: string
}

const workflowTriggers = ['ENQUIRY_CREATED', 'MANUAL']
const stepActions = ['CREATE_LEAD', 'ADD_NOTE']

const emptyCreate = { name: '', description: '', triggerType: 'ENQUIRY_CREATED', enabled: true }
const emptyStep = { name: '', actionType: 'CREATE_LEAD', sortOrder: 1, config: '' }

function json<T>(response: Response | Promise<Response>): Promise<T> {
  return Promise.resolve(response).then((r) => {
    if (!r.ok) throw new Error(`Request failed (${r.status})`)
    return r.json() as Promise<T>
  })
}

function statusBadge(status: string): string {
  return `portal-badge portal-badge-${(status || 'unknown').toLowerCase().replace(/[^a-z]/g, '')}`
}

export default function WorkflowsPage() {
  const { auth } = useAuth()
  const canManage = (auth?.permissions ?? []).includes('workflow:manage')

  const [workflows, setWorkflows] = useState<WorkflowRow[]>([])
  const [loaded, setLoaded] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const [createForm, setCreateForm] = useState(emptyCreate)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editForm, setEditForm] = useState(emptyCreate)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const [steps, setSteps] = useState<WorkflowStep[]>([])
  const [executions, setExecutions] = useState<WorkflowExecution[]>([])
  const [enquiries, setEnquiries] = useState<EnquiryOption[]>([])
  const [detailLoading, setDetailLoading] = useState(false)

  const [stepForm, setStepForm] = useState(emptyStep)
  const [runEntityId, setRunEntityId] = useState('')
  const [runningId, setRunningId] = useState<string | null>(null)
  const [runResult, setRunResult] = useState('')

  async function refresh() {
    try {
      const response = await fetch('/api/workflows', { headers: getAuthHeaders() })
      setWorkflows(await json<WorkflowRow[]>(response))
      setLoaded(true)
      setError('')
    } catch (err) {
      setError(err instanceof Error && err.message.includes('403')
        ? 'This account cannot view workflows. Sign in as an admin.'
        : 'Could not load workflows. Is the backend running?')
    }
  }

  useEffect(() => { void refresh() }, [])

  function flash(text: string) { setMessage(text); setError('') }
  function fail(err: unknown) { setError(err instanceof Error ? err.message : 'Something went wrong'); setMessage('') }

  function api(url: string, method: string, body: unknown): Promise<Response> {
    return fetch(url, {
      method,
      headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  }

  async function createWorkflow(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canManage) return
    try {
      await json(await api('/api/workflows', 'POST', createForm))
      setCreateForm(emptyCreate)
      flash('Workflow created')
      await refresh()
    } catch (err) { fail(err) }
  }

  async function saveEdit(workflowId: string) {
    if (!canManage) return
    try {
      await json(await api(`/api/workflows/${workflowId}`, 'PUT', editForm))
      setEditingId(null)
      flash('Workflow updated')
      await refresh()
    } catch (err) { fail(err) }
  }

  async function deleteWorkflow(workflowId: string) {
    if (!canManage) return
    try {
      await api(`/api/workflows/${workflowId}`, 'DELETE', undefined)
      if (selectedId === workflowId) setSelectedId(null)
      flash('Workflow deleted')
      await refresh()
    } catch (err) { fail(err) }
  }

  async function toggleEnabled(workflow: WorkflowRow) {
    if (!canManage) return
    try {
      await json(await api(`/api/workflows/${workflow.id}`, 'PUT', {
        name: workflow.name,
        description: workflow.description ?? '',
        triggerType: workflow.triggerType,
        enabled: !workflow.enabled,
      }))
      flash(workflow.enabled ? 'Workflow paused.' : 'Workflow enabled.')
      await refresh()
    } catch (err) { fail(err) }
  }

  async function loadEnquiries() {
    try {
      const response = await fetch('/api/admin/quote-requests?page=0&size=50', { headers: getAuthHeaders() })
      if (response.ok) {
        const page = await json<{ content: EnquiryOption[] }>(response)
        setEnquiries(page.content ?? [])
      }
    } catch {
      // Enquiry picker is optional; a manual entity ID still works.
    }
  }

  async function loadDetail(workflowId: string) {
    setDetailLoading(true)
    setError('')
    try {
      const headers = getAuthHeaders()
      const [stepRes, execRes] = await Promise.all([
        fetch(`/api/workflows/${workflowId}/steps`, { headers }),
        fetch(`/api/workflows/${workflowId}/executions`, { headers }),
      ])
      setSteps(await json<WorkflowStep[]>(stepRes))
      setExecutions(await json<WorkflowExecution[]>(execRes))
      await loadEnquiries()
    } catch (err) {
      setError(err instanceof Error && err.message.includes('403')
        ? 'This account cannot view workflow details.'
        : 'Could not load workflow details.')
    } finally {
      setDetailLoading(false)
    }
  }

  function toggleDetail(workflow: WorkflowRow) {
    if (selectedId === workflow.id) {
      setSelectedId(null)
      return
    }
    setSelectedId(workflow.id)
    void loadDetail(workflow.id)
  }

  async function addStep(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canManage || !selectedId) return
    try {
      await json(await api(`/api/workflows/${selectedId}/steps`, 'POST', stepForm))
      setStepForm({ ...emptyStep, sortOrder: steps.length + 1 })
      flash('Step added')
      await loadDetail(selectedId)
    } catch (err) { fail(err) }
  }

  async function deleteStep(step: WorkflowStep) {
    if (!canManage || !selectedId) return
    try {
      await api(`/api/workflows/steps/${step.id}`, 'DELETE', undefined)
      flash('Step removed')
      await loadDetail(selectedId)
    } catch (err) { fail(err) }
  }

  async function runWorkflow(workflowId: string) {
    if (!canManage) return
    if (!runEntityId.trim()) {
      setError('Choose an enquiry or paste an entity ID to run against.')
      setMessage('')
      return
    }
    setRunningId(workflowId)
    setRunResult('')
    try {
      const execution = await json<WorkflowExecution>(await api(`/api/workflows/${workflowId}/run`, 'POST', { entityId: runEntityId.trim() }))
      setRunResult(`${execution.status} — started ${new Date(execution.startedAt).toLocaleString()}`)
      flash(`Workflow ${execution.status.toLowerCase()}`)
      await loadDetail(workflowId)
    } catch (err) { fail(err) } finally {
      setRunningId(null)
    }
  }

  return (
    <div className="portal-shell">
      <div className="portal-header">
        <div>
          <h1>Workflows</h1>
          <p className="portal-note">Automate actions when enquiries arrive or run them manually.</p>
        </div>
        <div className="portal-header-actions">
          <button className="button button-ghost" type="button" onClick={() => void refresh()}>Refresh</button>
          
        </div>
      </div>

      {message && <p className="form-message success" aria-live="polite">{message}</p>}
      {error && <p className="form-message error" aria-live="polite">{error}</p>}

      {!loaded ? (
        <p className="portal-note">Loading workflows…</p>
      ) : (
        <>
          {canManage && (
            <form className="portal-card" onSubmit={createWorkflow}>
              <h2>Create workflow</h2>
              <div className="settings-grid">
                <label>Name
                  <input required value={createForm.name} onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })} placeholder="e.g. New enquiry follow-up" />
                </label>
                <label>Trigger
                  <select value={createForm.triggerType} onChange={(e) => setCreateForm({ ...createForm, triggerType: e.target.value })}>
                    {workflowTriggers.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                </label>
                <label className="settings-full">Description
                  <input value={createForm.description} onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })} placeholder="Optional context for this workflow" />
                </label>
                <label className="settings-check">
                  <input type="checkbox" checked={createForm.enabled} onChange={(e) => setCreateForm({ ...createForm, enabled: e.target.checked })} /> Active
                </label>
              </div>
              <div className="portal-header-actions">
                <button className="button button-primary" type="submit">Create workflow</button>
              </div>
            </form>
          )}

          {workflows.length === 0 && <p className="portal-note">No workflows yet. Create one above to get started.</p>}

          {workflows.map((workflow) => (
            <article key={workflow.id} className="portal-card">
              {editingId === workflow.id ? (
                <form onSubmit={(e) => { e.preventDefault(); saveEdit(workflow.id) }}>
                  <div className="settings-grid">
                    <label>Name
                      <input required value={editForm.name} onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} />
                    </label>
                    <label>Trigger
                      <select value={editForm.triggerType} onChange={(e) => setEditForm({ ...editForm, triggerType: e.target.value })}>
                        {workflowTriggers.map((t) => <option key={t} value={t}>{t}</option>)}
                      </select>
                    </label>
                    <label className="settings-full">Description
                      <input value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
                    </label>
                    <label className="settings-check">
                      <input type="checkbox" checked={editForm.enabled} onChange={(e) => setEditForm({ ...editForm, enabled: e.target.checked })} /> Active
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
                    <h2>
                      {workflow.name}{' '}
                      <span className="portal-note">({workflow.triggerType})</span>{' '}
                      <span className={statusBadge(workflow.enabled ? 'active' : 'onhold')}>
                        {workflow.enabled ? 'Active' : 'Paused'}
                      </span>
                    </h2>
                    <div className="portal-header-actions">
                      {canManage && (
                        <>
                          <button className="button button-ghost" type="button" onClick={() => { setEditingId(workflow.id); setEditForm({ name: workflow.name, description: workflow.description ?? '', triggerType: workflow.triggerType, enabled: workflow.enabled }) }}>Edit</button>
                          <button className="button button-ghost" type="button" onClick={() => toggleEnabled(workflow)}>{workflow.enabled ? 'Pause' : 'Enable'}</button>
                          <button className="button button-ghost" type="button" onClick={() => deleteWorkflow(workflow.id)}>Delete</button>
                        </>
                      )}
                      <button className="button button-ghost" type="button" onClick={() => toggleDetail(workflow)}>
                        {selectedId === workflow.id ? 'Hide details' : 'View details'}
                      </button>
                    </div>
                  </div>
                  {workflow.description && <p className="portal-note">{workflow.description}</p>}
                  <dl className="portal-meta">
                    <div><dt>Created</dt><dd>{new Date(workflow.createdAt).toLocaleDateString()}</dd></div>
                    <div><dt>Trigger</dt><dd>{workflow.triggerType}</dd></div>
                  </dl>

                  {selectedId === workflow.id && (
                    <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 18 }}>
                      {detailLoading && <p className="portal-note">Loading details…</p>}

                      {!detailLoading && (
                        <>
                          <section>
                            <h3 style={{ fontSize: '1rem', margin: '0 0 8px' }}>Steps</h3>
                            {steps.length === 0 && <p className="portal-note">No steps yet.</p>}
                            {steps.map((step) => (
                              <div key={step.id} className="portal-meta" style={{ alignItems: 'center', justifyContent: 'space-between' }}>
                                <div>
                                  <strong>#{step.sortOrder}</strong> {step.name || step.actionType}
                                  <span className="portal-note"> ({step.actionType})</span>
                                </div>
                                {canManage && (
                                  <button className="button button-ghost" type="button" onClick={() => deleteStep(step)}>Remove</button>
                                )}
                              </div>
                            ))}
                            {canManage && (
                              <form onSubmit={addStep} style={{ marginTop: 10 }}>
                                <div className="settings-grid">
                                  <label>Step name
                                    <input value={stepForm.name} onChange={(e) => setStepForm({ ...stepForm, name: e.target.value })} placeholder="Optional label" />
                                  </label>
                                  <label>Action
                                    <select value={stepForm.actionType} onChange={(e) => setStepForm({ ...stepForm, actionType: e.target.value })}>
                                      {stepActions.map((a) => <option key={a} value={a}>{a}</option>)}
                                    </select>
                                  </label>
                                  <label>Sort order
                                    <input type="number" min={1} value={stepForm.sortOrder} onChange={(e) => setStepForm({ ...stepForm, sortOrder: Number(e.target.value) })} />
                                  </label>
                                  <label className="settings-full">Config (JSON)
                                    <input value={stepForm.config} onChange={(e) => setStepForm({ ...stepForm, config: e.target.value })} placeholder='{"key": "value"}' />
                                  </label>
                                </div>
                                <div className="portal-header-actions">
                                  <button className="button button-primary" type="submit">Add step</button>
                                </div>
                              </form>
                            )}
                          </section>

                          {workflow.triggerType === 'MANUAL' && canManage && (
                            <section>
                              <h3 style={{ fontSize: '1rem', margin: '0 0 8px' }}>Run manually</h3>
                              <div className="settings-grid">
                                <label className="settings-full">Entity (enquiry)
                                  <select value={runEntityId} onChange={(e) => setRunEntityId(e.target.value)}>
                                    <option value="">— Paste an ID or pick an enquiry —</option>
                                    {enquiries.map((enq) => (
                                      <option key={enq.id} value={enq.id}>{enq.name} — {enq.service} ({new Date(enq.receivedAt).toLocaleDateString()})</option>
                                    ))}
                                  </select>
                                </label>
                                <label className="settings-full">Or paste entity ID directly
                                  <input value={runEntityId} onChange={(e) => setRunEntityId(e.target.value)} placeholder="UUID of the enquiry or lead" />
                                </label>
                              </div>
                              <div className="portal-header-actions">
                                <button className="button button-primary" type="button" onClick={() => runWorkflow(workflow.id)} disabled={runningId === workflow.id}>
                                  {runningId === workflow.id ? 'Running…' : 'Run workflow'}
                                </button>
                              </div>
                              {runResult && <p className="portal-note">{runResult}</p>}
                            </section>
                          )}

                          <section>
                            <h3 style={{ fontSize: '1rem', margin: '0 0 8px' }}>Executions</h3>
                            {executions.length === 0 && <p className="portal-note">No executions yet.</p>}
                            {executions.map((exec) => (
                              <div key={exec.id} className="portal-meta" style={{ alignItems: 'center', justifyContent: 'space-between' }}>
                                <div>
                                  <span className={statusBadge(exec.status)}>{exec.status}</span>
                                  <span className="portal-note"> started {new Date(exec.startedAt).toLocaleString()}</span>
                                </div>
                                <span className="portal-note">{exec.finishedAt ? `finished ${new Date(exec.finishedAt).toLocaleString()}` : 'in progress'}</span>
                              </div>
                            ))}
                          </section>
                        </>
                      )}
                    </div>
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


