import type { ReactNode } from 'react'

type PageProps = {
  onBackHome?: () => void
}

const services = [
  {
    index: '01',
    title: 'BPO & Outsourcing',
    summary: 'Dependable operational support for the work that keeps your business moving.',
    items: ['Back-office operations', 'Customer support', 'Data processing and validation', 'Virtual assistance'],
  },
  {
    index: '02',
    title: 'IT & Software Development',
    summary: 'Practical digital systems that fit your processes instead of forcing a new one.',
    items: ['Custom web applications', 'Business software', 'REST APIs and integrations', 'React and TypeScript interfaces'],
  },
  {
    index: '03',
    title: 'AI & Automation',
    summary: 'Thoughtful automation that gives people leverage while keeping them in control.',
    items: ['AI chatbots', 'AI assistants', 'Workflow automation', 'Process automation and integrations'],
  },
]

const process = [
  ['01', 'Understand the work', 'We map the process, bottlenecks, and outcome before suggesting a solution.'],
  ['02', 'Design the response', 'We combine people, process, software, and automation around what is actually needed.'],
  ['03', 'Deliver with clarity', 'Milestones, ownership, and communication stay visible throughout the engagement.'],
  ['04', 'Support what comes next', 'We remain available for iteration, maintenance, and continuous improvement.'],
]

function PageFrame({ eyebrow, title, intro, children }: { eyebrow: string; title: ReactNode; intro: string; children: ReactNode }) {
  return (
    <main className="page-shell">
      <section className="page-hero">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="page-intro">{intro}</p>
      </section>
      {children}
    </main>
  )
}

export default function SitePage({ path }: { path: string }) {
  if (path === '/services') {
    return (
      <PageFrame eyebrow="SERVICES" title={<>Built for <em>real work.</em></>} intro="Choose the capability your business needs today, then shape it around the way your team already operates.">
        <section className="page-section service-page-grid">
          {services.map((service) => (
            <article className="page-service" key={service.index}>
              <span className="service-number">{service.index}</span>
              <h2>{service.title}</h2>
              <p>{service.summary}</p>
              <ul>{service.items.map((item) => <li key={item}>{item}</li>)}</ul>
              <a className="text-link" href="/#quote">Discuss this service <span aria-hidden="true">&#8599;</span></a>
            </article>
          ))}
        </section>
      </PageFrame>
    )
  }

  if (path === '/process') {
    return (
      <PageFrame eyebrow="HOW WE WORK" title={<>Clear steps. <em>Useful outcomes.</em></>} intro="A practical delivery model keeps the work focused, understandable, and connected to business results.">
        <section className="page-section process-page-grid">
          {process.map(([index, title, description]) => (
            <article className="page-process" key={index}>
              <span className="service-number">{index}</span>
              <h2>{title}</h2>
              <p>{description}</p>
            </article>
          ))}
        </section>
        <section className="page-callout"><p className="eyebrow">READY TO START?</p><h2>Bring us the messy part.</h2><a className="button button-primary" href="/#quote">Start a conversation <span aria-hidden="true">&#8599;</span></a></section>
      </PageFrame>
    )
  }

  if (path === '/about') {
    return (
      <PageFrame eyebrow="ABOUT GULVISHA" title={<>Practical support for <em>growing teams.</em></>} intro="Gulvisha Enterprises brings operations, technology, and AI together around the work businesses depend on.">
        <section className="page-section about-page-grid">
          <div><h2>Useful beats impressive.</h2><p>We focus on solutions that are manageable, measurable, and built for real-world workflows. That might mean dependable back-office support, a custom business system, or an automation layer that removes repetitive work.</p></div>
          <div className="page-highlight"><span>01</span><strong>People, process, technology.</strong><p>The right answer is usually a thoughtful combination of all three.</p></div>
        </section>
      </PageFrame>
    )
  }

  return (
    <PageFrame eyebrow="START A CONVERSATION" title={<>Let’s make the work <em>lighter.</em></>} intro="Tell us what is slowing your team down and we will come back with a practical next step.">
      <section className="page-section contact-page-grid">
        <div><h2>Request a quote</h2><p>Our enquiry form lives on the homepage so it is easy to return to the main context whenever you need it.</p><a className="button button-primary" href="/#quote">Open enquiry form <span aria-hidden="true">&#8599;</span></a></div>
        <div className="contact-details"><span>EMAIL</span><a href="mailto:hello@gulvisha.com">hello@gulvisha.com</a><span>AVAILABILITY</span><p>Available for conversations about operations, software, and automation.</p></div>
      </section>
    </PageFrame>
  )
}
