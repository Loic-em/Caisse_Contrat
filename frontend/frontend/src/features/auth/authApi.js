import { apiRequest } from '../api/http'

export function getCurrentUser() {
  return apiRequest('/api/auth/me')
}

export function getDemoAccounts() {
  return apiRequest('/api/auth/demo-accounts')
}

export function login(credentials) {
  return apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}

export function logout() {
  return apiRequest('/api/auth/logout', { method: 'POST' })
}

export function reauthenticate(password) {
  return apiRequest('/api/auth/reauthenticate', {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

export function getOwnSignature() {
  return apiRequest('/api/auth/signature')
}

export function saveOwnSignature(dataUrl) {
  return apiRequest('/api/auth/signature', {
    method: 'PUT',
    body: JSON.stringify({ dataUrl }),
  })
}
