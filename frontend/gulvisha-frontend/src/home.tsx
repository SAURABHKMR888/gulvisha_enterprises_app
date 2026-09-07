const highlights = [
  ['01', 'Operations', 'Reliable back-office support that keeps work moving.'],
  ['02', 'Technology', 'Business software shaped around your real workflow.'],
  ['03', 'Automation', 'AI and automation that remove repetitive effort.'],
]

export default function HomePage() {
  return (
    <main className="home-shell">
      <nav className="nav" aria-label="Main navigation">
        <a className="brand" href="/">GULVISHA<span>.</span></a>
        <div className="nav-links">
          <a href="/about">About</a>
          <a href="/services">Services</a>
          <a href="/process">How we work</a>
          <a href="/contact">Contact</a>
        </div>
        <div className="nav-actions">
          <a className="nav-cta" href="/contact">Start a conversation</a>
          <a className="nav-admin" href="/admin">Admin</a>
        </div>
      </nav>

      <section className="home-hero">
        <div>
          <p className="eyebrow">OUTSOURCING · TECHNOLOGY · AI</p>
          <h1>Better systems for <em>growing businesses.</em></h1>
          <p className="hero-copy">Gulvisha Enterprises combines operational support, practical software, and thoughtful automation to help teams work with more clarity.</p>
          <div className="hero-actions">
            <a className="button button-primary" href="/contact">Request a quote <span aria-hidden="true">&#8599;</span></a>
            <a className="button button-quiet" href="/services">Explore services</a>
          </div>
        </div>
        <div className="home-hero-note"><span>GULVISHA / 2026</span><strong>People, process, technology.</strong><p>One practical response to the work that slows growth.</p></div>
      </section>

      <section className="home-intro">
        <p className="eyebrow">A PRACTICAL PARTNER</p>
        <h2>From the work behind the scenes to the systems your customers see.</h2>
        <a className="text-link" href="/about">Get to know Gulvisha <span aria-hidden="true">&#8599;</span></a>
      </section>

      <section className="home-highlights" aria-label="Our capabilities">
        {highlights.map(([number, title, description]) => <a className="home-highlight" href="/services" key={number}><span className="service-number">{number}</span><h2>{title}</h2><p>{description}</p><span className="highlight-arrow" aria-hidden="true">&#8599;</span></a>)}
      </section>

      <section className="home-footer-cta">
        <div><p className="eyebrow">READY TO TALK?</p><h2>Bring us the messy part.</h2></div>
        <a className="button button-primary" href="/contact">Start a conversation <span aria-hidden="true">&#8599;</span></a>
      </section>
    </main>
  )
}
