import { FormEvent, useState } from 'react'
import { homePathFor, useAuth } from './auth'

export default function LoginPage({ eyebrow = 'SIGN IN', heading = 'Sign in to your workspace' }: { eyebrow?: string; heading?: string }) {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const state = await login(username, password)
      // Route by role from the JWT — one navigation, no redirect chains
      window.location.replace(homePathFor(state))
    } catch {
      setError('Invalid username or password')
      setLoading(false)
    }
  }

  return (
    <main className="login-shell">
      <form className="login-form" onSubmit={handleSubmit}>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{heading}</h1>
        <label>Username
          <input required value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>
        <label>Password
          <input required type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        {error && <p className="form-message error">{error}</p>}
        <button className="button button-primary" type="submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </main>
  )
}
