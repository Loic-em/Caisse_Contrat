import { apiRequest } from '../../api/http'

export function getSupportingDocumentAvailability() {
  return apiRequest('/api/supporting-documents/availability')
}

export function getSupportingDocuments(requestId) {
  return apiRequest(`/api/cash-requests/${requestId}/supporting-documents`)
}

export function uploadSupportingDocument(requestId, values) {
  const body = new FormData()
  body.set('documentType', values.documentType)
  body.set('amount', values.amount)
  body.set('documentDate', values.documentDate)
  body.set('file', values.file)
  if (values.replacesId) body.set('replacesId', values.replacesId)

  return apiRequest(`/api/cash-requests/${requestId}/supporting-documents`, {
    method: 'POST',
    body,
  })
}

export function reviewSupportingDocument(requestId, documentId, decision, reason) {
  const query = new URLSearchParams({ decision })
  if (reason) query.set('reason', reason)
  return apiRequest(
    `/api/cash-requests/${requestId}/supporting-documents/${documentId}/review?${query}`,
    { method: 'PUT' },
  )
}
