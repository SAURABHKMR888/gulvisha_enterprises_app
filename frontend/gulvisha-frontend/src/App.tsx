import { FormEvent, useEffect, useState, ReactNode } from 'react'
import { AuthProvider, homePathFor, useAuth } from './auth'
import LoginPage from './LoginPage'
import AdminPage from './admin'
import PortalPage from './portal'
import SettingsPage from './settings'
import UsersPage from './users'
import SitePage from './sitePages'

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

const services = [
  {
    number: '01',
    title: 'BPO & Outsourcing',
    description: 'Reliable operational support for back-office work, customer service, data processing and virtual assistance.',
  },
  {
    number: '02',
    title: 'IT & Software Development',
    description: 'Practical web applications, business software, APIs and integrations built around your operational needs.',
  },
  {
    number: '03',
    title: 'AI & Automation',
    description: 'Thoughtful automation, AI assistants and workflow solutions designed with people in control.',
  },
]

const serviceOptionsByCategory: Record<string, string[]> = {
  'BPO & Outsourcing': [
    'Back-office operations',
    'Customer support',
    'Data processing and validation',
    'Virtual assistance',
  ],
  'IT & Software Development': [
    'Custom web application',
    'Business software',
    'REST API and integrations',
    'React and TypeScript interface',
  ],
  'AI & Automation': [
    'AI chatbot',
    'AI assistant',
    'Workflow automation',
    'Process automation and integrations',
  ],
}

const industries = [
  'Healthcare',
  'Professional Services',
  'Retail & E-commerce',
  'Operations-heavy businesses',
  'Startups & scaleups',
  'Education & training',
]

const processSteps = [
  { step: '01', title: 'Understand the work', description: 'We map your business process, bottlenecks, and operational goals before suggesting a way forward.' },
  { step: '02', title: 'Design the solution', description: 'We structure the right mix of people, process, software, and automation to fit your reality.' },
  { step: '03', title: 'Deliver with clarity', description: 'Our work is built around practical milestones, measurable outcomes, and transparent communication.' },
  { step: '04', title: 'Support as you grow', description: 'We remain available for iteration, maintenance, optimization, and long-term operational support.' },
]

const capabilities = [
  'Business process outsourcing',
  'Custom web applications',
  'Spring Boot & REST APIs',
  'React & TypeScript interfaces',
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

  const currentPath = typeof window !== 'undefined' ? window.location.pathname : '/'

  if (currentPath && currentPath !== '/') {
    return <SitePage path={currentPath} />
  }

  useEffect(() => {
    fetch('/api/health')
      .then((response) => response.ok ? response.json() as Promise<HealthResponse> : Promise.reject())
      .then(() => setApiStatus('connected'))
      .catch(() => setApiStatus('unavailable'))
  }, [])

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

  async function submitQuoteRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmissionStatus('submitting')

    try {
      const response = await fetch('/api/enquiries', {
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
          <p className="eyebrow">OUTSOURCING &middot; TECHNOLOGY &middot; AI</p>
          <h1>Technology, outsourcing and AI solutions for <em>growing businesses.</em></h1>
          <p className="hero-copy">Gulvisha Enterprises helps teams simplify operations, build practical digital systems, and automate the work that slows growth.</p>
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
            <p className="eyebrow">ABOUT US</p>
            <h2>Practical support for the work that keeps a business moving.</h2>
          </div>
          <div className="about-grid">
            <div>
              <p>Gulvisha Enterprises supports growing businesses with outsourced operations, technology delivery, and AI-powered improvements that are built for real-world workflows.</p>
              <p>Whether you need dependable back-office support, a custom business system, or a smarter automation layer, we focus on solutions that are useful, manageable, and aligned to your priorities.</p>
            </div>
            <div className="about-panel">
              <h3>What we bring</h3>
              <ul>
                <li>Clear communication</li>
                <li>Operational focus</li>
                <li>Flexible delivery</li>
                <li>Business-minded technology</li>
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
            {services.map((service) => (
              <article className="service-card" key={service.number}>
                <p className="service-number">{service.number}</p>
                <h3>{service.title}</h3>
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
            {capabilities.map((capability) => (
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
            <p className={`form-message ${submissionStatus}`} aria-live="polite">{submissionStatus === 'success' && 'Thanks — your request is with us. We will be in touch shortly.'}{submissionStatus === 'error' && 'We could not send your request. Please try again or email hello@gulvisha.com.'}</p>
          </form>
        </section>

        <footer>&copy; {new Date().getFullYear()} Gulvisha Enterprises <span>Business solutions, thoughtfully delivered.</span></footer>
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
  return <RequireRole><AdminPage /></RequireRole>
}

function PortalRoute() {
  return <RequireRole clientOnly><PortalPage /></RequireRole>
}

function SettingsRoute() {
  return <RequireRole><SettingsPage /></RequireRole>
}

function UsersRoute() {
  return <RequireRole><UsersPage /></RequireRole>
}

function AppRouter() {
  const currentPath = typeof window !== 'undefined' ? window.location.pathname : '/'
  if (currentPath === '/admin') return <AdminRoute />
  if (currentPath === '/portal') return <PortalRoute />
  if (currentPath === '/settings') return <SettingsRoute />
  if (currentPath === '/users') return <UsersRoute />
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