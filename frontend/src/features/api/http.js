const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function getCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new ApiError(`Impossible de sécuriser la requête (HTTP ${response.status}).`, response.status)
  }

  return response.json()
}

export async function apiRequest(path, options = {}) {
  const method = options.method || 'GET'
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
  const headers = {
    Accept: 'application/json',
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json; charset=utf-8' } : {}),
    ...options.headers,
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())) {
    const csrf = await getCsrfToken()
    headers['X-XSRF-TOKEN'] = csrf.token
  }

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      method,
      headers,
      credentials: 'include',
    })
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('Impossible de joindre le serveur. Vérifiez que le backend est démarré.')
    }
    throw error
  }

  if (response.status === 204) {
    return null
  }

  const responseText = await response.text()
  let responseBody = null
  if (responseText) {
    try {
      responseBody = JSON.parse(responseText)
    } catch {
      if (response.ok) {
        throw new Error('Le serveur a renvoyé une réponse illisible.')
      }
    }
  }

  if (!response.ok) {
    throw new ApiError(
      responseBody?.message || responseBody?.detail || `La requête a échoué (HTTP ${response.status}).`,
      response.status,
    )
  }

  return responseBody
}
