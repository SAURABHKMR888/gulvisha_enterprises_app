import { FormEvent, useEffect, useState, ReactNode } from 'react'
import { AuthProvider, homePathFor, useAuth } from './auth'
import { useSiteConfig, siteDisplayName, siteInitial, fetchSiteConfig, SiteConfig } from './siteConfig'
import LoginPage from './LoginPage'
import AdminPage from './admin'
import PortalPage from './portal'
import SettingsPage from './settings'
import UsersPage from './users'
import SitePage from './sitePages'
import WorkflowsPage from './workflows'
import AiPage from './ai'
import PlatformAdminPage from './platform-admin'
import ClientsPage from './clients'
import ProjectsPage from './projects'
import TasksPage from './tasks'
import ResourcesPage from './resources'
import AdminLayout from './admin-layout'

type HealthResponse = {
  status: string
  application: string
}

type QuoteForm = {
  name: string
  email: string
  company: string
  phone: string
  serviceCategory: string
  service: string
  message: string
  budget: string
  timeline: string
  source: string
  preferredContactMethod: string
}

type ServiceItem = {
  id: string
  name: string
  description: string
  category: string
}



const defaultIndustries = [
  'Healthcare',
  'Professional Services',
  'Retail & E-commerce',
  'Operations-heavy businesses',
  'Startups & scaleups',
  'Education & training',
]

const defaultProcessSteps = [
  { step: '01', title: 'Understand the work', description: 'We map your business process, bottlenecks, and operational goals before suggesting a way forward.' },
  { step: '02', title: 'Design the solution', description: 'We structure the right mix of people, process, software, and automation to fit your reality.' },
  { step: '03', title: 'Deliver with clarity', description: 'Our work is built around practical milestones, measurable outcomes, and transparent communication.' },
  { step: '04', title: 'Support as you grow', description: 'We remain available for iteration, maintenance, optimization, and long-term operational support.' },
]

const defaultCapabilities = [
  'Business process outsourcing',
  'Custom web applications',
  'REST APIs & integrations',
  'Modern web interfaces',
  'Data processing & validation',
  'AI chatbot & automation workflows',
]

const initialQuoteForm: QuoteForm = {
  name: '',
  email: '',
  company: '',
  phone: '',
  serviceCategory: '',
  service: '',
  message: '',
  budget: '',
  timeline: '',
  source: 'WEBSITE',
  preferredContactMethod: '',
}

function PublicSite() {
  const [apiStatus, setApiStatus] = useState<'checking' | 'connected' | 'unavailable'>('checking')
  const [quoteForm, setQuoteForm] = useState<QuoteForm>(initialQuoteForm)
  const [submissionStatus, setSubmissionStatus] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle')
  const { auth } = useAuth()

  // Read tenant slug from URL: ?tenant=abc → ABC Consulting, no param → default (first org)
  const tenantSlug = new URLSearchParams(window.location.search).get('tenant') || undefined
  const { config, loading: configLoading } = useSiteConfig(tenantSlug)
  const [services, setServices] = useState<ServiceItem[]>([])
  const [servicesLoading, setServicesLoading] = useState(true)

  const currentPath = typeof window !== 'undefined' ? window.location.pathname : '/'

  if (currentPath && currentPath !== '/') {
    return <SitePage path={currentPath} />
  }

  useEffect(() => {
    const servicesUrl = tenantSlug ? `/api/services?slug=${encodeURIComponent(tenantSlug)}` : '/api/services'
    fetch(servicesUrl)
      .then((res) => (res.ok ? res.json() : { value: [] }))
      .then((data) => {
        const items = Array.isArray(data) ? data : (data.value || [])
        setServices(items)
      })
      .catch(() => setServices([]))
      .finally(() => setServicesLoading(false))
  }, [tenantSlug])

  useEffect(() => {
    fetch('/api/health')
      .then((response) => response.ok ? response.json() as Promise<HealthResponse> : Promise.reject())
      .then(() => setApiStatus('connected'))
      .catch(() => setApiStatus('unavailable'))
  }, [])

  const serviceOptionsByCategory: Record<string, string[]> = {}
  services.forEach((s) => {
    const cat = s.category || 'Other'
    if (!serviceOptionsByCategory[cat]) serviceOptionsByCategory[cat] = []
    serviceOptionsByCategory[cat].push(s.name)
  })

  function updateQuoteField(field: keyof QuoteForm, value: string) {
    setQuoteForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function updateServiceCategory(category: string) {
    setQuoteForm((currentForm) => ({
      ...currentForm,
      serviceCategory: category,
      service: '',
    }))
  }

  const brandName = siteDisplayName(config)
  const brandEmail = config?.email || 'hello@example.com'
  const brandDescription = config?.description || 'Business solutions, thoughtfully delivered.'
  const sc = config?.siteContent

  // Replace {brandName} placeholder in content strings
  const replaceBrand = (text: string | undefined, fallback: string) => {
    const value = text ?? fallback
    return value.replace(/\{brandName\}/g, brandName)
  }

  const heroEyebrow = sc?.heroEyebrow || 'BUSINESS · TECHNOLOGY · AUTOMATION'
  const heroTitle = replaceBrand(sc?.heroTitle, `${brandName} — practical support for your business.`)
  const aboutHeading = sc?.aboutHeading || 'Practical support for growing teams.'
  const aboutCapabilities = sc?.aboutCapabilities?.length ? sc.aboutCapabilities : defaultCapabilities
  const industries = sc?.industries?.length ? sc.industries : defaultIndustries
  const processHeading = sc?.processHeading || 'Clear steps. Useful outcomes.'
  const processSteps = sc?.processSteps?.length ? sc.processSteps : defaultProcessSteps
  const quoteHeading = sc?.quoteHeading || "Let's make the work lighter."
  const quoteDescription = sc?.quoteDescription || 'Tell us what is slowing your team down and we will come back with a practical next step.'

  async function submitQuoteRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmissionStatus('submitting')

        try {
      let url = '/api/enquiries'
      if (tenantSlug) url += '?tenant=' + encodeURIComponent(tenantSlug)

      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(quoteForm),
      })

      if (!response.ok) throw new Error('Request failed')

      setQuoteForm(initialQuoteForm)
      setSubmissionStatus('success')
    } catch {
      setSubmissionStatus('error')
    }
  }

  return (
    <>
      <main>
        <nav className="nav" aria-label="Main navigation">
          <a className="brand" href="/">GULVISHA<span>.</span></a>
          <div className="nav-links">
            <a href="/about">About</a>
            <a href="/services">Services</a>
            <a href="/process">How we work</a>
            <a href="/contact">Contact</a>
          </div>
          <div className="nav-actions">
            <a className="nav-cta" href="#quote">Start a conversation</a>
            {auth ? (
              <a className="nav-admin" href={homePathFor(auth)}>My workspace</a>
            ) : (
              <a className="nav-admin" href="/login">Sign in</a>
            )}
          </div>
        </nav>

        <section className="hero" id="top">
          <p className="eyebrow">{heroEyebrow}</p>
          <h1 dangerouslySetInnerHTML={{ __html: heroTitle }} />
          <p className="hero-copy">{sc?.heroSubheading ?? brandDescription}</p>
          <div className="hero-actions">
            <a className="button button-primary" href="#quote">Request a quote <span aria-hidden="true">&#8599;</span></a>
            <a className="button button-quiet" href="#services">Explore services</a>
          </div>
          <div className="status" aria-live="polite">
            <i className={apiStatus === 'connected' ? 'online' : ''} />
            {apiStatus === 'checking' && 'Connecting to our platform'}
            {apiStatus === 'connected' && 'Platform API connected'}
            {apiStatus === 'unavailable' && 'Platform API temporarily unavailable'}
          </div>
        </section>

        <section className="about" id="about">
          <div className="section-heading narrow-heading">
            <p className="eyebrow">ABOUT</p>
            <h2 dangerouslySetInnerHTML={{ __html: aboutHeading }} />
          </div>
          <div className="about-grid">
            <div>
              <p>{brandDescription}</p>
            </div>
            <div className="about-panel">
              <h3>What we bring</h3>
              <ul>
                {aboutCapabilities.map((capability) => (
                  <li key={capability}>{capability}</li>
                ))}
              </ul>
            </div>
          </div>
        </section>

        <section className="services" id="services" aria-labelledby="services-heading">
          <div className="section-heading">
            <p className="eyebrow">WHAT WE DO</p>
            <h2 id="services-heading">Services built around execution, efficiency, and growth.</h2>
          </div>
          <div className="service-grid">
            {services.map((service, idx) => (
              <article className="service-card" key={service.id}>
                <p className="service-number">{String(idx + 1).padStart(2, "0")}</p>
                <h3>{service.name}</h3>
                <p>{service.description}</p>
                <span aria-hidden="true">&#8599;</span>
              </article>
            ))}
          </div>
        </section>

        <section className="industries" aria-labelledby="industries-heading">
          <div className="section-heading narrow-heading">
            <p className="eyebrow">INDUSTRIES</p>
            <h2 id="industries-heading">Support across the kinds of work businesses depend on.</h2>
          </div>
          <div className="industry-grid">
            {industries.map((industry) => (
              <div className="industry-item" key={industry}>{industry}</div>
            ))}
          </div>
        </section>

        <section className="process" id="process" aria-labelledby="process-heading">
          <div className="section-heading narrow-heading">
            <p className="eyebrow">HOW WE WORK</p>
            <h2 id="process-heading">A straightforward, practical delivery model.</h2>
          </div>
          <div className="process-grid">
            {processSteps.map((step) => (
              <article className="process-card" key={step.step}>
                <p className="service-number">{step.step}</p>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="capabilities" aria-labelledby="capabilities-heading">
          <div className="section-heading narrow-heading">
            <p className="eyebrow">CAPABILITIES</p>
            <h2 id="capabilities-heading">Technology and operations that support real business outcomes.</h2>
          </div>
          <div className="capability-list">
            {aboutCapabilities.map((capability) => (
              <div className="capability-pill" key={capability}>{capability}</div>
            ))}
          </div>
        </section>

        <section className="cta-panel" aria-labelledby="cta-heading">
          <div>
            <p className="eyebrow">READY TO TALK?</p>
            <h2 id="cta-heading">Let’s build a better way to work.</h2>
          </div>
          <a className="button button-primary" href="#quote">Request a quote <span aria-hidden="true">&#8599;</span></a>
        </section>

        <section className="quote" id="quote" aria-labelledby="quote-heading">
          <div className="quote-intro">
            <p className="eyebrow">START A CONVERSATION</p>
            <h2 id="quote-heading">Tell us what you need.</h2>
            <p>Share a few details and we will come back with a practical next step.</p>
          </div>
          <form className="quote-form" onSubmit={submitQuoteRequest}>
            <label>Name<input required value={quoteForm.name} onChange={(event) => updateQuoteField('name', event.target.value)} /></label>
            <label>Email<input required type="email" value={quoteForm.email} onChange={(event) => updateQuoteField('email', event.target.value)} /></label>
            <label>Company <span>(optional)</span><input value={quoteForm.company} onChange={(event) => updateQuoteField('company', event.target.value)} /></label>
            <label>Phone <span>(optional)</span><input type="tel" value={quoteForm.phone} onChange={(event) => updateQuoteField('phone', event.target.value)} /></label>
            <label>Service category<select required value={quoteForm.serviceCategory} onChange={(event) => updateServiceCategory(event.target.value)}><option value="">Select a category</option>{Object.keys(serviceOptionsByCategory).map((category) => <option key={category} value={category}>{category}</option>)}</select></label>
            <label>Specific service<select required value={quoteForm.service} disabled={!quoteForm.serviceCategory} onChange={(event) => updateQuoteField('service', event.target.value)}><option value="">{quoteForm.serviceCategory ? 'Select a service' : 'Select a category first'}</option>{(serviceOptionsByCategory[quoteForm.serviceCategory] ?? []).map((service) => <option key={service} value={service}>{service}</option>)}</select></label>
            <label>Budget <span>(optional)</span><input value={quoteForm.budget} onChange={(event) => updateQuoteField('budget', event.target.value)} placeholder="e.g. INR 50,000" /></label>
            <label>Expected timeline <span>(optional)</span><input value={quoteForm.timeline} onChange={(event) => updateQuoteField('timeline', event.target.value)} placeholder="e.g. 4-6 weeks" /></label>
            <label>Preferred contact <span>(optional)</span><select value={quoteForm.preferredContactMethod} onChange={(event) => updateQuoteField('preferredContactMethod', event.target.value)}><option value="">No preference</option><option>Email</option><option>Phone</option><option>WhatsApp</option></select></label>
            <label>How did you find us?<select value={quoteForm.source} onChange={(event) => updateQuoteField('source', event.target.value)}><option value="WEBSITE">Website</option><option>LinkedIn</option><option>Upwork</option><option>Email</option><option>Phone</option><option>Referral</option><option>Other</option></select></label>
            <label className="quote-details">Project description<textarea required rows={5} value={quoteForm.message} onChange={(event) => updateQuoteField('message', event.target.value)} /></label>
            <button className="button button-primary" disabled={submissionStatus === 'submitting'} type="submit">{submissionStatus === 'submitting' ? 'Sending request…' : 'Send request'} <span aria-hidden="true">&#8599;</span></button>
            <p className={`form-message ${submissionStatus}`} aria-live="polite">{submissionStatus === 'success' && 'Thanks — your request is with us. We will be in touch shortly.'}{submissionStatus === 'error' && `We could not send your request. Please try again or email ${brandEmail}.`}</p>
          </form>
        </section>

        <footer>&copy; {new Date().getFullYear()} {brandName} <span>{brandDescription}</span></footer>
      </main>
    </>
  )
}

function RequireRole({ clientOnly, children }: { clientOnly?: boolean; children: ReactNode }) {
  const { isAuthenticated, auth } = useAuth()
  const isClient = auth?.roles.includes('CLIENT') ?? false
  const needsClient = clientOnly ?? false
  // One redirect decision: to /login when signed out, or to the correct home when in the wrong area
  const target = !isAuthenticated
    ? '/login'
    : needsClient === isClient ? null : isClient ? '/portal' : '/admin'

  useEffect(() => {
    if (target) window.location.replace(target)
  }, [target])

  if (target) return null
  return <>{children}</>
}

function AdminRoute() {
  return <RequireRole><AdminLayout currentPath="/admin"><AdminPage /></AdminLayout></RequireRole>
}
function PlatformAdminRoute() {
  return <RequireRole><PlatformAdminPage /></RequireRole>
}

function PortalRoute() {
  return <RequireRole clientOnly><PortalPage /></RequireRole>
}

function SettingsRoute() {
  return <RequireRole><AdminLayout currentPath="/settings"><SettingsPage /></AdminLayout></RequireRole>
}

function UsersRoute() {
  return <RequireRole><AdminLayout currentPath="/users"><UsersPage /></AdminLayout></RequireRole>
}

function WorkflowsRoute() {
  return <RequireRole><AdminLayout currentPath="/workflows"><WorkflowsPage /></AdminLayout></RequireRole>
}

function ProjectsRoute() {
  return <RequireRole><AdminLayout currentPath="/projects"><ProjectsPage /></AdminLayout></RequireRole>
}

function ClientsRoute() {
  return <RequireRole><AdminLayout currentPath="/clients"><ClientsPage /></AdminLayout></RequireRole>
}

function TasksRoute() {
  return <RequireRole><AdminLayout currentPath="/tasks"><TasksPage /></AdminLayout></RequireRole>
}

function ResourcesRoute() {
  return <RequireRole><AdminLayout currentPath="/resources"><ResourcesPage /></AdminLayout></RequireRole>
}

function AiRoute() {
  return <RequireRole><AdminLayout currentPath="/ai"><AiPage /></AdminLayout></RequireRole>
}

function AppRouter() {
  const currentPath = typeof window !== 'undefined' ? window.location.pathname : '/' 
  if (currentPath === '/platform-admin') return <PlatformAdminRoute />
  if (currentPath === '/admin') return <AdminRoute />
  if (currentPath === '/portal') return <PortalRoute />
  if (currentPath === '/settings') return <SettingsRoute />
  if (currentPath === '/users') return <UsersRoute />
    if (currentPath === '/workflows') return <WorkflowsRoute />
  if (currentPath === '/projects') return <ProjectsRoute />
  if (currentPath === '/clients') return <ClientsRoute />
  if (currentPath === '/tasks') return <TasksRoute />
  if (currentPath === '/resources') return <ResourcesRoute />
  if (currentPath === '/ai') return <AiRoute />
  if (currentPath === '/login') return <LoginPage />
  return <PublicSite />
}

export default function App() {
  return (
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  )
}

