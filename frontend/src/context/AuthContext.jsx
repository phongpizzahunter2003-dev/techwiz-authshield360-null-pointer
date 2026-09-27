import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/endpoints.js'
import { getStoredSession, getToken, setSession as persistSession, setToken } from '../api/client.js'
import { ROLE_HOME } from '../i18n/messages.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(getStoredSession())
  const [ready, setReady] = useState(false)

  // Hydrate the session from the server on first load (also validates the token).
  useEffect(() => {
    if (!getToken()) {
      setReady(true)
      return
    }
    authApi
      .session()
      .then((res) => {
        persistSession(res.data)
        setSessionState(res.data)
      })
      .catch(() => {
        setToken(null)
        persistSession(null)
        setSessionState(null)
      })
      .finally(() => setReady(true))
  }, [])

  // Force logout when the API reports 401 anywhere (UC-06).
  useEffect(() => {
    const handler = () => {
      persistSession(null)
      setSessionState(null)
    }
    window.addEventListener('auth:unauthorized', handler)
    return () => window.removeEventListener('auth:unauthorized', handler)
  }, [])

  const adopt = useCallback(async (loginResponse) => {
    setToken(loginResponse.token)
    const res = await authApi.session()
    persistSession(res.data)
    setSessionState(res.data)
    return res.data
  }, [])

  const login = useCallback(
    async (username, password, captcha = {}) => {
      const res = await authApi.login({ username, password, ...captcha })
      if (res.data.status === 'AUTHENTICATED') await adopt(res.data)
      return res.data
    },
    [adopt],
  )

  const verifyOtp = useCallback(
    async (challengeToken, code) => {
      const res = await authApi.verifyOtp({ challengeToken, code })
      if (res.data.status === 'AUTHENTICATED') await adopt(res.data)
      return res.data
    },
    [adopt],
  )

  const resendOtp = useCallback(async (challengeToken, captcha = {}) => {
    const res = await authApi.resendOtp({ challengeToken, ...captcha })
    return res.data
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      /* ignore — we clear locally regardless */
    }
    setToken(null)
    persistSession(null)
    setSessionState(null)
    // History invalidation: replace so the protected page is removed from history (UC-06).
    window.location.replace('/login')
  }, [])

  const value = useMemo(
    () => ({
      session,
      ready,
      isAuthenticated: Boolean(session),
      role: session?.role || null,
      username: session?.username || null,
      home: session ? ROLE_HOME[session.role] || '/login' : '/login',
      login,
      verifyOtp,
      resendOtp,
      logout,
    }),
    [session, ready, login, verifyOtp, resendOtp, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
