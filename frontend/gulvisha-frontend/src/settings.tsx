import { useEffect, useState } from 'react'
import { getAuthHeaders } from './auth'

type Org = {
  id: string; name: string; description: string | null; industry: string | null
  email: string | null; phone: string | null; website: string | null; address: string | null
  timezone: string | null; currency: string | null; language: string | null
  displayName: string | null; slug: string | null
  logoUrl: string | null; faviconUrl: string | null
  primaryColor: string | null; accentColor: string | null
  siteContent: string | null
}
type ServiceItem = {
  id: string; name: string; description: string | null; category: string | null; pricingInfo: string | null
}
type Stage = { id: string; name: string; sortOrder: number; defaultStage: boolean }
type Field = {
  id: string; name: string; entityType: string; fieldType: string; options: string | null; required: boolean
}
type SiteContent = {
  heroEyebrow?: string
  heroTitle?: string
  heroSubheading?: string
  aboutHeading?: string
  aboutCapabilities?: string[]
  industries?: string[]
  processHeading?: string
  processSteps?: { step?: string; title?: string; description?: string }[]
  quoteHeading?: string
  quoteSubheading?: string
  quoteDescription?: string
}

const tabLabels = {
  organization: 'Organization',
  branding: 'Branding',
  website: 'Website content',
  services: 'Services',
  pipeline: 'Pipeline stages',
  fields: 'Custom fields',
} as const
type TabKey = keyof typeof tabLabels

const entityTypes = ['ENQUIRY', 'LEAD', 'CLIENT']
const fieldTypes = ['TEXT', 'NUMBER', 'SELECT', 'DATE', 'BOOLEAN']

function parseSiteContent(raw: string | null): SiteContent {
  if (!raw) return {}
  try { return JSON.parse(raw) as SiteContent } catch { return {} }
}
function serializeSiteContent(content: SiteContent): string {
  try { return JSON.stringify(content) } catch { return '{}' }
}

function json<T>(response: Response | Promise<Response>): Promise<T> {
  return Promise.resolve(response).then((r) => {
    if (!r.ok) throw new Error(`Request failed (${r.status})`)
    return r.json() as Promise<T>
  })
}

export default function SettingsPage() {
  const [tab, setTab] = useState<TabKey>('organization')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  // Organization
  const [org, setOrg] = useState<Org | null>(null)
  const [savingOrg, setSavingOrg] = useState(false)

  // Website content
  const [siteContent, setSiteContent] = useState<SiteContent>({})
  const [savingSite, setSavingSite] = useState(false)

  // Branding (mirrors org fields for convenience, saved via saveOrg)
  const [brandingForm, setBrandingForm] = useState({ displayName: '', slug: '', logoUrl: '', faviconUrl: '', primaryColor: '', accentColor: '' })
  const [savingBranding, setSavingBranding] = useState(false)

  // Services
  const [services, setServices] = useState<ServiceItem[]>([])
  const [serviceForm, setServiceForm] = useState({ id: '', name: '', category: '', pricingInfo: '', description: '' })

  // Pipeline
  const [stages, setStages] = useState<Stage[]>([])
  const [stageForm, setStageForm] = useState({ id: '', name: '', sortOrder: 0, defaultStage: false })

  // Custom fields
  const [fields, setFields] = useState<Field[]>([])
  const [fieldEntityType, setFieldEntityType] = useState('ENQUIRY')
  const [fieldForm, setFieldForm] = useState({ id: '', name: '', fieldType: 'TEXT', options: '', required: false })

  useEffect(() => {
    fetch('/api/organizations/current', { headers: getAuthHeaders() })
      .then((r) => {
        if (r.status === 403) throw new Error('This account does not have permission to manage settings. Sign in as an admin (admin / change-me).')
        if (r.status === 401) throw new Error('Your session has expired. Please sign in again.')
        return json<Org>(r)
      })
      .then((loaded) => {
        setOrg(loaded)
        const sc = parseSiteContent(loaded.siteContent)
        setSiteContent(sc)
        setBrandingForm({
          displayName: loaded.displayName ?? '', slug: loaded.slug ?? '',
          logoUrl: loaded.logoUrl ?? '', faviconUrl: loaded.faviconUrl ?? '',
          primaryColor: loaded.primaryColor ?? '', accentColor: loaded.accentColor ?? '',
        })
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Could not load organization settings'))
  }, [])

  useEffect(() => {
    if (tab === 'services') {
      fetch('/api/services', { headers: getAuthHeaders() }).then((r) => json<ServiceItem[]>(r)).then(setServices).catch(() => setError('Could not load services'))
    } else if (tab === 'pipeline') {
      fetch('/api/pipeline-stages', { headers: getAuthHeaders() }).then((r) => json<Stage[]>(r)).then(setStages).catch(() => setError('Could not load pipeline stages'))
    } else if (tab === 'fields') {
      fetch(`/api/custom-fields?entityType=${fieldEntityType}`, { headers: getAuthHeaders() }).then((r) => json<Field[]>(r)).then(setFields).catch(() => setError('Could not load custom fields'))
    }
  }, [tab, fieldEntityType])

  function flash(text: string) { setMessage(text); setError('') }
  function fail(err: unknown) { setError(err instanceof Error ? err.message : 'Something went wrong'); setMessage('') }

  async function saveOrg(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!org) return
    setSavingOrg(true)
    try {
      const saved = await json<Org>(fetch('/api/organizations/current', {
        method: 'PUT', headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...org, ...brandingForm, siteContent: serializeSiteContent(siteContent) }),
      }))
      setOrg(saved)
      flash('Organization settings saved')
    } catch (err) { fail(err) } finally { setSavingOrg(false) }
  }

  async function saveBranding(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!org) return
    setSavingBranding(true)
    try {
      const saved = await json<Org>(fetch('/api/organizations/current', {
        method: 'PUT', headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...org, ...brandingForm, siteContent: serializeSiteContent(siteContent) }),
      }))
      setOrg(saved)
      flash('Branding saved')
    } catch (err) { fail(err) } finally { setSavingBranding(false) }
  }

  async function saveSiteContent(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!org) return
    setSavingSite(true)
    try {
      const saved = await json<Org>(fetch('/api/organizations/current', {
        method: 'PUT', headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...org, siteContent: serializeSiteContent(siteContent) }),
      }))
      setOrg(saved)
      flash('Website content saved')
    } catch (err) { fail(err) } finally { setSavingSite(false) }
  }

  function setSiteField(field: Exclude<keyof SiteContent, 'aboutCapabilities' | 'industries' | 'processSteps'>, value: string) {
    setSiteContent((prev) => ({ ...prev, [field]: value }))
  }
  function setSiteList(field: 'aboutCapabilities' | 'industries', value: string) {
    setSiteContent((prev) => ({ ...prev, [field]: value.split(',').map((s) => s.trim()).filter(Boolean) }))
  }
  function setSiteSteps(value: string) {
    let steps = siteContent.processSteps ?? []
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) steps = parsed
    } catch { /* keep old steps when parse fails */ }
    setSiteContent((prev) => ({ ...prev, processSteps: steps }))
  }
  function setSiteStepsField(index: number, field: 'title' | 'description', value: string) {
    setSiteContent((prev) => {
      const steps = [...(prev.processSteps ?? [])]
      steps[index] = { ...steps[index], [field]: value }
      return { ...prev, processSteps: steps }
    })
  }
  async function saveService(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const method = serviceForm.id ? 'PUT' : 'POST'
    const url = serviceForm.id ? `/api/services/${serviceForm.id}` : '/api/services'
    try {
      await json(fetch(url, {
        method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: serviceForm.name, category: serviceForm.category, pricingInfo: serviceForm.pricingInfo, description: serviceForm.description }),
      }))
      setServices(await json<ServiceItem[]>(fetch('/api/services', { headers: getAuthHeaders() })))
      setServiceForm({ id: '', name: '', category: '', pricingInfo: '', description: '' })
      flash(serviceForm.id ? 'Service updated' : 'Service created')
    } catch (err) { fail(err) }
  }

  async function deleteService(id: string) {
    try { await json(fetch(`/api/services/${id}`, { method: 'DELETE', headers: getAuthHeaders() })); setServices(services.filter((s) => s.id !== id)); flash('Service deleted') }
    catch (err) { fail(err) }
  }

  async function saveStage(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const method = stageForm.id ? 'PUT' : 'POST'
    const url = stageForm.id ? `/api/pipeline-stages/${stageForm.id}` : '/api/pipeline-stages'
    try {
      await json(fetch(url, {
        method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: stageForm.name, sortOrder: stageForm.sortOrder, defaultStage: stageForm.defaultStage }),
      }))
      setStages(await json<Stage[]>(fetch('/api/pipeline-stages', { headers: getAuthHeaders() })))
      setStageForm({ id: '', name: '', sortOrder: 0, defaultStage: false })
      flash(stageForm.id ? 'Stage updated' : 'Stage created')
    } catch (err) { fail(err) }
  }

  async function deleteStage(id: string) {
    try { await json(fetch(`/api/pipeline-stages/${id}`, { method: 'DELETE', headers: getAuthHeaders() })); setStages(stages.filter((s) => s.id !== id)); flash('Stage deleted') }
    catch (err) { fail(err) }
  }

  async function saveField(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const method = fieldForm.id ? 'PUT' : 'POST'
    const url = fieldForm.id ? `/api/custom-fields/${fieldForm.id}` : '/api/custom-fields'
    try {
      await json(fetch(url, {
        method, headers: { ...Object.fromEntries(getAuthHeaders()), 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: fieldForm.name, entityType: fieldEntityType, fieldType: fieldForm.fieldType, options: fieldForm.options, required: fieldForm.required }),
      }))
      setFields(await json<Field[]>(fetch(`/api/custom-fields?entityType=${fieldEntityType}`, { headers: getAuthHeaders() })))
      setFieldForm({ id: '', name: '', fieldType: 'TEXT', options: '', required: false })
      flash(fieldForm.id ? 'Field updated' : 'Field created')
    } catch (err) { fail(err) }
  }

  async function deleteField(id: string) {
    try { await json(fetch(`/api/custom-fields/${id}`, { method: 'DELETE', headers: getAuthHeaders() })); setFields(fields.filter((f) => f.id !== id)); flash('Field deleted') }
    catch (err) { fail(err) }
  }
  return (
    <div className="portal-shell">
      <header className="portal-header">
        <div>
          <p className="eyebrow">PLATFORM SETTINGS</p>
          <h1>Configuration</h1>
        </div>
        <div className="portal-header-actions">
          <a className="button button-ghost" href="/admin">← Dashboard</a>
          <a className="button button-ghost" href="/">Website</a>
          
        </div>
      </header>

      <nav className="portal-tabs" aria-label="Settings sections">
        {(Object.keys(tabLabels) as TabKey[]).map((key) => (
          <button key={key} type="button" className={`portal-tab${tab === key ? ' active' : ''}`} onClick={() => setTab(key)}>{tabLabels[key]}</button>
        ))}
      </nav>

      {message && <p className="form-message success">{message}</p>}
      {error && <p className="form-message error" role="alert">{error}</p>}

      {tab === 'organization' && org && (
        <form className="portal-card" onSubmit={saveOrg}>
          <h2>Organization settings</h2>
          <div className="settings-grid">
            <label>Legal name<input required value={org.name} onChange={(e) => setOrg({ ...org, name: e.target.value })} /></label>
            <label>Display name<input value={brandingForm.displayName} onChange={(e) => setBrandingForm({ ...brandingForm, displayName: e.target.value })} /></label>
            <label>Slug<input value={brandingForm.slug} onChange={(e) => setBrandingForm({ ...brandingForm, slug: e.target.value })} /></label>
            <label>Industry<input value={org.industry ?? ''} onChange={(e) => setOrg({ ...org, industry: e.target.value })} /></label>
            <label>Email<input type="email" value={org.email ?? ''} onChange={(e) => setOrg({ ...org, email: e.target.value })} /></label>
            <label>Phone<input value={org.phone ?? ''} onChange={(e) => setOrg({ ...org, phone: e.target.value })} /></label>
            <label>Website<input value={org.website ?? ''} onChange={(e) => setOrg({ ...org, website: e.target.value })} /></label>
            <label>Timezone<input value={org.timezone ?? ''} onChange={(e) => setOrg({ ...org, timezone: e.target.value })} /></label>
            <label>Currency<input value={org.currency ?? ''} onChange={(e) => setOrg({ ...org, currency: e.target.value })} /></label>
            <label>Language<input value={org.language ?? ''} onChange={(e) => setOrg({ ...org, language: e.target.value })} /></label>
            <label className="settings-full">Description<textarea rows={2} value={org.description ?? ''} onChange={(e) => setOrg({ ...org, description: e.target.value })} /></label>
            <label className="settings-full">Address<input value={org.address ?? ''} onChange={(e) => setOrg({ ...org, address: e.target.value })} /></label>
          </div>
          <button className="button button-primary" type="submit" disabled={savingOrg}>{savingOrg ? 'Saving…' : 'Save settings'}</button>
        </form>
      )}

      {tab === 'branding' && org && (
        <form className="portal-card" onSubmit={saveBranding}>
          <h2>Branding</h2>
          <p className="portal-note">These values are used across the public website and the admin app header.</p>
          <div className="settings-grid">
            <label>Display name<input value={brandingForm.displayName} onChange={(e) => setBrandingForm({ ...brandingForm, displayName: e.target.value })} /></label>
            <label>Primary color<input type="color" value={brandingForm.primaryColor || '#315941'} onChange={(e) => setBrandingForm({ ...brandingForm, primaryColor: e.target.value })} /></label>
            <label>Accent color<input type="color" value={brandingForm.accentColor || '#c94f2c'} onChange={(e) => setBrandingForm({ ...brandingForm, accentColor: e.target.value })} /></label>
            <label>Logo URL<input value={brandingForm.logoUrl} onChange={(e) => setBrandingForm({ ...brandingForm, logoUrl: e.target.value })} /></label>
            <label>Favicon URL<input value={brandingForm.faviconUrl} onChange={(e) => setBrandingForm({ ...brandingForm, faviconUrl: e.target.value })} /></label>
          </div>
          <button className="button button-primary" type="submit" disabled={savingBranding}>{savingBranding ? 'Saving…' : 'Save branding'}</button>
        </form>
      )}

      {tab === 'website' && org && (
        <form className="portal-card" onSubmit={saveSiteContent}>
          <h2>Website content</h2>
          <p className="portal-note">Use {`{brandName}`} in headings — it is replaced with your display name automatically.</p>
          <div className="settings-grid">
            <label>Hero eyebrow<input value={siteContent.heroEyebrow ?? ''} onChange={(e) => setSiteField('heroEyebrow', e.target.value)} /></label>
            <label className="settings-full">Hero title<input value={siteContent.heroTitle ?? ''} onChange={(e) => setSiteField('heroTitle', e.target.value)} /></label>
            <label className="settings-full">Hero subheading<input value={siteContent.heroSubheading ?? ''} onChange={(e) => setSiteField('heroSubheading', e.target.value)} /></label>
            <label className="settings-full">About heading<input value={siteContent.aboutHeading ?? ''} onChange={(e) => setSiteField('aboutHeading', e.target.value)} /></label>
            <label className="settings-full">About capabilities (comma separated)<textarea rows={2} value={(siteContent.aboutCapabilities ?? []).join(', ')} onChange={(e) => setSiteList('aboutCapabilities', e.target.value)} /></label>
            <label className="settings-full">Industries (comma separated)<textarea rows={2} value={(siteContent.industries ?? []).join(', ')} onChange={(e) => setSiteList('industries', e.target.value)} /></label>
            <label className="settings-full">Process heading<input value={siteContent.processHeading ?? ''} onChange={(e) => setSiteField('processHeading', e.target.value)} /></label>
            <label className="settings-full">Quote heading<input value={siteContent.quoteHeading ?? ''} onChange={(e) => setSiteField('quoteHeading', e.target.value)} /></label>
            <label className="settings-full">Quote subheading<input value={siteContent.quoteSubheading ?? ''} onChange={(e) => setSiteField('quoteSubheading', e.target.value)} /></label>
            <label className="settings-full">Quote description<textarea rows={2} value={siteContent.quoteDescription ?? ''} onChange={(e) => setSiteField('quoteDescription', e.target.value)} /></label>
          </div>
          <h3 style={{ fontSize: '1rem', margin: '0 0 8px' }}>Process steps (JSON array)</h3>
          <textarea rows={4} style={{ width: '100%' }} value={JSON.stringify(siteContent.processSteps ?? [], null, 2)} onChange={(e) => setSiteSteps(e.target.value)} />
          <button className="button button-primary" type="submit" disabled={savingSite}>{savingSite ? 'Saving…' : 'Save website content'}</button>
        </form>
      )}

      {tab === 'services' && (
        <>
          <form className="portal-card" onSubmit={saveService}>
            <h2>{serviceForm.id ? 'Edit service' : 'Add service'}</h2>
            <div className="settings-grid">
              <label>Name<input required value={serviceForm.name} onChange={(e) => setServiceForm({ ...serviceForm, name: e.target.value })} /></label>
              <label>Category<input value={serviceForm.category} onChange={(e) => setServiceForm({ ...serviceForm, category: e.target.value })} /></label>
              <label className="settings-full">Pricing info<input value={serviceForm.pricingInfo} onChange={(e) => setServiceForm({ ...serviceForm, pricingInfo: e.target.value })} /></label>
              <label className="settings-full">Description<textarea rows={2} value={serviceForm.description} onChange={(e) => setServiceForm({ ...serviceForm, description: e.target.value })} /></label>
            </div>
            <div className="portal-header-actions">
              <button className="button button-primary" type="submit">{serviceForm.id ? 'Update' : 'Create'}</button>
              {serviceForm.id && <button className="button button-ghost" type="button" onClick={() => setServiceForm({ id: '', name: '', category: '', pricingInfo: '', description: '' })}>Cancel</button>}
            </div>
          </form>
          {services.map((service) => (
            <article key={service.id} className="portal-card">
              <div className="portal-card-head">
                <h2>{service.name}</h2>
                <div className="portal-header-actions">
                  <button className="button button-ghost" type="button" onClick={() => setServiceForm({ id: service.id, name: service.name, category: service.category ?? '', pricingInfo: service.pricingInfo ?? '', description: service.description ?? '' })}>Edit</button>
                  <button className="button button-ghost" type="button" onClick={() => deleteService(service.id)}>Delete</button>
                </div>
              </div>
              <p className="portal-note">{[service.category, service.pricingInfo, service.description].filter(Boolean).join(' · ') || 'No details'}</p>
            </article>
          ))}
        </>
      )}
      {tab === 'pipeline' && (
        <>
          <form className="portal-card" onSubmit={saveStage}>
            <h2>{stageForm.id ? 'Edit stage' : 'Add stage'}</h2>
            <div className="settings-grid">
              <label>Name<input required value={stageForm.name} onChange={(e) => setStageForm({ ...stageForm, name: e.target.value })} /></label>
              <label>Sort order<input type="number" value={stageForm.sortOrder} onChange={(e) => setStageForm({ ...stageForm, sortOrder: Number(e.target.value) })} /></label>
              <label className="settings-check"><input type="checkbox" checked={stageForm.defaultStage} onChange={(e) => setStageForm({ ...stageForm, defaultStage: e.target.checked })} /> Default stage for new leads</label>
            </div>
            <div className="portal-header-actions">
              <button className="button button-primary" type="submit">{stageForm.id ? 'Update' : 'Create'}</button>
              {stageForm.id && <button className="button button-ghost" type="button" onClick={() => setStageForm({ id: '', name: '', sortOrder: 0, defaultStage: false })}>Cancel</button>}
            </div>
          </form>
          {stages.map((stage) => (
            <article key={stage.id} className="portal-card">
              <div className="portal-card-head">
                <h2>{stage.name} <span className="portal-note">#{stage.sortOrder}{stage.defaultStage ? ' · default' : ''}</span></h2>
                <div className="portal-header-actions">
                  <button className="button button-ghost" type="button" onClick={() => setStageForm({ id: stage.id, name: stage.name, sortOrder: stage.sortOrder, defaultStage: stage.defaultStage })}>Edit</button>
                  <button className="button button-ghost" type="button" onClick={() => deleteStage(stage.id)}>Delete</button>
                </div>
              </div>
            </article>
          ))}
        </>
      )}

      {tab === 'fields' && (
        <>
          <label>Entity type
            <select value={fieldEntityType} onChange={(e) => setFieldEntityType(e.target.value)}>
              {entityTypes.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
          <form className="portal-card" onSubmit={saveField}>
            <h2>{fieldForm.id ? 'Edit field' : 'Add field'} — {fieldEntityType}</h2>
            <div className="settings-grid">
              <label>Name<input required value={fieldForm.name} onChange={(e) => setFieldForm({ ...fieldForm, name: e.target.value })} /></label>
              <label>Type
                <select value={fieldForm.fieldType} onChange={(e) => setFieldForm({ ...fieldForm, fieldType: e.target.value })}>
                  {fieldTypes.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </label>
              <label className="settings-full">Options (comma-separated, for SELECT)<input value={fieldForm.options} onChange={(e) => setFieldForm({ ...fieldForm, options: e.target.value })} /></label>
              <label className="settings-check"><input type="checkbox" checked={fieldForm.required} onChange={(e) => setFieldForm({ ...fieldForm, required: e.target.checked })} /> Required</label>
            </div>
            <div className="portal-header-actions">
              <button className="button button-primary" type="submit">{fieldForm.id ? 'Update' : 'Create'}</button>
              {fieldForm.id && <button className="button button-ghost" type="button" onClick={() => setFieldForm({ id: '', name: '', fieldType: 'TEXT', options: '', required: false })}>Cancel</button>}
            </div>
          </form>
          {fields.length === 0 && <p className="portal-note">No custom fields for {fieldEntityType}.</p>}
          {fields.map((field) => (
            <article key={field.id} className="portal-card">
              <div className="portal-card-head">
                <h2>{field.name} <span className="portal-note">{field.fieldType}{field.required ? ' · required' : ''}</span></h2>
                <div className="portal-header-actions">
                  <button className="button button-ghost" type="button" onClick={() => { setFieldEntityType(field.entityType); setFieldForm({ id: field.id, name: field.name, fieldType: field.fieldType, options: field.options ?? '', required: field.required }) }}>Edit</button>
                  <button className="button button-ghost" type="button" onClick={() => deleteField(field.id)}>Delete</button>
                </div>
              </div>
              {field.options && <p className="portal-note">Options: {field.options}</p>}
            </article>
          ))}
        </>
      )}
    </div>
  )
}

