import { apiRequest } from '../api/http'

export function getContracts() {
  return apiRequest('/api/contracts')
}

export function getContract(id) {
  return apiRequest(`/api/contracts/${id}`)
}

export function getContractEmployees() {
  return apiRequest('/api/contracts/employees')
}

export function getContractSupervisors() {
  return apiRequest('/api/contracts/supervisors')
}

export function getContractDocumentAvailability() {
  return apiRequest('/api/contracts/document-upload-availability')
}

export function createContract(payload) {
  return apiRequest('/api/contracts', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateContract(id, payload) {
  return apiRequest(`/api/contracts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function getContractDocuments(id) {
  return apiRequest(`/api/contracts/${id}/documents`)
}

export function getContractHistory(id) {
  return apiRequest(`/api/contracts/${id}/history`)
}

export function uploadContractDocument(id, file) {
  const body = new FormData()
  body.set('file', file)
  return apiRequest(`/api/contracts/${id}/documents`, {
    method: 'POST',
    body,
  })
}
