import { apiRequest } from '../api/http'

export function getAccounts() {
  return apiRequest('/api/accounts')
}

export function createAccount(account) {
  return apiRequest('/api/accounts', {
    method: 'POST',
    body: JSON.stringify(account),
  })
}

export function updateAccount(id, account) {
  return apiRequest(`/api/accounts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(account),
  })
}

export function setAccountActive(id, active) {
  return apiRequest(`/api/accounts/${id}/${active ? 'reactivate' : 'deactivate'}`, {
    method: 'PATCH',
  })
}
