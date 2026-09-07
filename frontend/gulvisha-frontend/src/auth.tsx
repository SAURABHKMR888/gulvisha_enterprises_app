import { createContext, useContext, useState, ReactNode } from 'react'

type AuthState = {
  token: string
  username: string
  roles: string[]
  permissions: string[]
  clientId?: string | null
}

type AuthContextType = {
  auth: AuthState | null
  login: (username: string, password: string) => Promise<AuthState>
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => {
    const stored = localStorage.getItem('gulvisha-auth')
    return stored ? JSON.parse(stored) : null
  })

  async function login(username: string, password: string): Promise<AuthState> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    if (!response.ok) throw new Error('Invalid credentials')
    const data = await response.json()
    const state: AuthState = { token: data.token, username: data.username, roles: data.roles, permissions: data.permissions || [], clientId: data.clientId ?? null }
    localStorage.setItem('gulvisha-auth', JSON.stringify(state))
    setAuth(state)
    return state
  }

  function logout() {
    localStorage.removeItem('gulvisha-auth')
    setAuth(null)
  }

  return (
    <AuthContext.Provider value={{ auth, login, logout, isAuthenticated: !!auth }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

export function homePathFor(auth: AuthState | null): string {
  return auth?.roles.includes('CLIENT') ? '/portal' : '/admin'
}

export function getAuthHeaders(): Headers {
  const stored = localStorage.getItem('gulvisha-auth')
  const headers = new Headers()
  if (stored) {
    const auth: AuthState = JSON.parse(stored)
    headers.set('Authorization', `Bearer ${auth.token}`)
  }
  return headers
}
