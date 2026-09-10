import { useEffect, useState } from 'react'

export type SiteContent = {
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

export type SiteConfig = {
  organizationId: string
  name: string
  displayName: string | null
  slug: string | null
  description: string | null
  logoUrl: string | null
  faviconUrl: string | null
  primaryColor: string | null
  accentColor: string | null
  industry: string | null
  email: string | null
  phone: string | null
  website: string | null
  address: string | null
  siteContent: SiteContent | null
}

// Cache keyed by slug so different tenants don't share cached config
const cache = new Map<string, SiteConfig>()

/** Fetch the public site config. Cache is keyed by slug ('' = default). */
export async function fetchSiteConfig(slug?: string): Promise<SiteConfig> {
  const key = slug ?? ''
  const cached = cache.get(key)
  if (cached) return cached

  const url = slug
    ? `/api/public/site?slug=${encodeURIComponent(slug)}`
    : '/api/public/site'

  const res = await fetch(url)
  if (!res.ok) throw new Error('Failed to load site config')
  const config = (await res.json()) as SiteConfig

  cache.set(key, config)
  return config
}

/** Apply branding (CSS variables, favicon, document title) to the page. */
export function applyBranding(config: SiteConfig): void {
  const root = document.documentElement

  if (config.primaryColor) {
    root.style.setProperty('--brand-primary', config.primaryColor)
  } else {
    root.style.removeProperty('--brand-primary')
  }
  if (config.accentColor) {
    root.style.setProperty('--brand-accent', config.accentColor)
  } else {
    root.style.removeProperty('--brand-accent')
  }

  if (config.faviconUrl) {
    let link = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
    if (!link) {
      link = document.createElement('link')
      link.rel = 'icon'
      document.head.appendChild(link)
    }
    link.href = config.faviconUrl
  }

  const title = config.displayName || config.name
  if (title) {
    document.title = title
  }
}

/** React hook: loads config on mount and applies branding. Refetches when slug changes. */
export function useSiteConfig(slug?: string) {
  const [config, setConfig] = useState<SiteConfig | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    fetchSiteConfig(slug)
      .then((cfg) => {
        setConfig(cfg)
        applyBranding(cfg)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [slug])

  return { config, loading, error }
}

/** Display name fallback: displayName → name → 'Platform'. */
export function siteDisplayName(config: SiteConfig | null): string {
  if (!config) return 'Platform'
  return config.displayName || config.name || 'Platform'
}

/** Short brand initial for avatar/logo fallback. */
export function siteInitial(config: SiteConfig | null): string {
  const name = siteDisplayName(config)
  return name.charAt(0).toUpperCase()
}
