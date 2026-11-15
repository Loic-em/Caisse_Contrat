import { apiRequest } from '../../api/http'

export async function getCashRequests(options = {}) {
  const requests = await apiRequest('/api/cash-requests', options)

  if (!Array.isArray(requests)) {
    throw new Error('Le serveur a renvoyé une liste de demandes invalide.')
  }

  return requests
}

export function createCashRequest(request) {
  return apiRequest('/api/cash-requests', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateCashRequest(id, request) {
  return apiRequest(`/api/cash-requests/${id}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function submitCashRequest(id) {
  return apiRequest(`/api/cash-requests/${id}/submit`, { method: 'POST' })
}

export function approveCashRequest(id) {
  return apiRequest(`/api/cash-requests/${id}/approve`, { method: 'POST' })
}

export function rejectCashRequest(id, reason) {
  return apiRequest(`/api/cash-requests/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  })
}

export function cancelCashRequest(id, reason) {
  return apiRequest(`/api/cash-requests/${id}/cancel`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  })
}

export function setCashRequestFinalAmount(id, finalAmount, reason) {
  return apiRequest(`/api/cash-requests/${id}/accounting-review`, {
    method: 'POST',
    body: JSON.stringify({ finalAmount: Number(finalAmount), reason: reason || null }),
  })
}

export function issueCashVoucher(id, reason) {
  return apiRequest(`/api/cash-requests/${id}/voucher`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  })
}

export function getCashVoucher(id) {
  return apiRequest(`/api/cash-requests/${id}/voucher`)
}

export function signCashVoucher(id, type) {
  return apiRequest(`/api/cash-requests/${id}/voucher/visa`, {
    method: 'POST',
    body: JSON.stringify({ type }),
  })
}

export function disburseCashVoucher(id, disbursedOn, fundOrigin) {
  return apiRequest(`/api/cash-requests/${id}/voucher/disbursement`, {
    method: 'POST',
    body: JSON.stringify({ disbursedOn, fundOrigin }),
  })
}

export function getRegularizationSummary(id) {
  return apiRequest(`/api/cash-requests/${id}/regularization`)
}

export function issueEntryVoucher(id, payload) {
  return apiRequest(`/api/cash-requests/${id}/entry-vouchers`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function signEntryVoucher(requestId, entryVoucherId, type) {
  return apiRequest(`/api/cash-requests/${requestId}/entry-vouchers/${entryVoucherId}/visa`, {
    method: 'POST',
    body: JSON.stringify({ type }),
  })
}

export function authorizePartialReturn(id, payload) {
  return apiRequest(`/api/cash-requests/${id}/entry-vouchers/authorizations`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function closeRegularization(id) {
  return apiRequest(`/api/cash-requests/${id}/regularization/close`, {
    method: 'POST',
  })
}
