import { useEffect, useState } from 'react'
import { getCashRequests } from '../api/cashRequestsApi'
import {
  getSupportingDocumentAvailability,
  getSupportingDocuments,
  reviewSupportingDocument,
  uploadSupportingDocument,
} from '../api/supportingDocumentsApi'
import './SupportingDocumentsPage.css'

const amountFormatter = new Intl.NumberFormat('fr-FR')
const dateFormatter = new Intl.DateTimeFormat('fr-FR')
const statusLabels = {
  A_CONTROLER: 'À contrôler',
  ACCEPTE: 'Accepté',
  REJETE: 'Rejeté',
}

function formatDate(value) {
  return dateFormatter.format(new Date(`${value}T00:00:00`))
}

function SupportingDocumentsPage({ currentUser }) {
  const [requests, setRequests] = useState([])
  const [documentsByRequest, setDocumentsByRequest] = useState({})
  const [uploadsEnabled, setUploadsEnabled] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingAction, setPendingAction] = useState('')
  const [formByRequest, setFormByRequest] = useState({})
  const [reviewByDocument, setReviewByDocument] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const canReview = currentUser.role === 'COMPTABLE' || currentUser.role === 'ADJOINT_COMPTABLE'

  async function fetchPageData() {
    const [allRequests, scanner] = await Promise.all([
      getCashRequests(),
      getSupportingDocumentAvailability(),
    ])
    const eligibleRequests = allRequests.filter((request) =>
      ['DECAISSEE', 'EN_REGULARISATION'].includes(request.status))
    const entries = await Promise.all(eligibleRequests.map(async (request) => [
      request.id,
      await getSupportingDocuments(request.id),
    ]))
    return {
      requests: eligibleRequests,
      documentsByRequest: Object.fromEntries(entries),
      uploadsEnabled: scanner.uploadsEnabled,
    }
  }

  function applyPageData(pageData) {
    setRequests(pageData.requests)
    setDocumentsByRequest(pageData.documentsByRequest)
    setUploadsEnabled(pageData.uploadsEnabled)
  }

  async function loadPage() {
    setError('')
    setIsLoading(true)
    try {
      applyPageData(await fetchPageData())
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    let isMounted = true
    fetchPageData()
      .then((pageData) => {
        if (isMounted) applyPageData(pageData)
      })
      .catch((loadError) => {
        if (isMounted) setError(loadError.message)
      })
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })
    return () => {
      isMounted = false
    }
  }, [])

  async function handleUpload(event, request) {
    event.preventDefault()
    const form = formByRequest[request.id]
    if (!form?.file) {
      setError('Sélectionnez un justificatif à déposer.')
      return
    }
    setIsSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const requestDocuments = documentsByRequest[request.id] || []
      const replaceableDocument = requestDocuments.find((item) => (
        item.status === 'REJETE' && !requestDocuments.some((candidate) => candidate.replacesId === item.id)
      ))
      const document = await uploadSupportingDocument(request.id, {
        ...form,
        replacesId: replaceableDocument?.id,
      })
      setDocumentsByRequest((current) => ({
        ...current,
        [request.id]: [document, ...(current[request.id] || [])],
      }))
      setFormByRequest((current) => ({ ...current, [request.id]: {} }))
      setSuccess('Le justificatif a été transmis au contrôle comptable.')
      event.currentTarget.reset()
    } catch (uploadError) {
      setError(uploadError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleReview(request, document, decision) {
    const review = reviewByDocument[document.id] || {}
    if (decision === 'REJETE' && !review.reason?.trim()) {
      setError('Un motif est obligatoire pour rejeter un justificatif.')
      return
    }
    setPendingAction(document.id)
    setError('')
    setSuccess('')
    try {
      const updated = await reviewSupportingDocument(
        request.id,
        document.id,
        decision,
        review.reason,
      )
      setDocumentsByRequest((current) => ({
        ...current,
        [request.id]: current[request.id].map((item) => item.id === updated.id ? updated : item),
      }))
      setSuccess(`Le justificatif a été ${decision === 'ACCEPTE' ? 'accepté' : 'rejeté'}.`)
    } catch (reviewError) {
      setError(reviewError.message)
    } finally {
      setPendingAction('')
    }
  }

  if (isLoading) {
    return <section className="content-page"><p role="status">Chargement des justificatifs…</p></section>
  }

  return (
    <section className="content-page supporting-documents-page">
      <header className="supporting-documents-heading">
        <div>
          <p className="eyebrow">CAISSE</p>
          <h1>Justificatifs</h1>
          <p>Déposez et contrôlez les pièces associées aux missions décaissées.</p>
        </div>
        <button type="button" onClick={loadPage}>Actualiser</button>
      </header>

      {!uploadsEnabled && (
        <div className="supporting-documents-warning" role="status">
          Le dépôt des fichiers reste désactivé tant que ClamAV ou la clé de chiffrement ne sont pas opérationnels.
          La consultation et le contrôle des pièces déjà déposées restent disponibles.
        </div>
      )}
      {error && <p className="supporting-documents-message is-error" role="alert">{error}</p>}
      {success && <p className="supporting-documents-message is-success" role="status">{success}</p>}

      {requests.length === 0 ? (
        <div className="supporting-documents-empty">
          <h2>Aucun dossier décaissé</h2>
          <p>Les justificatifs apparaîtront ici après le décaissement d’une demande visible pour votre compte.</p>
        </div>
      ) : requests.map((request) => {
        const isBeneficiary = request.requesterId === currentUser.id
        const documents = documentsByRequest[request.id] || []
        return (
          <article className="supporting-request-card" key={request.id}>
            <header>
              <div>
                <h2>{request.subject}</h2>
                <p>{request.requesterName} · {request.location} · mission terminée le {formatDate(request.endDate)}</p>
              </div>
              <strong>{amountFormatter.format(Number(request.finalAmount))} FCFA</strong>
            </header>

            {isBeneficiary && (
              <form className="supporting-upload-form" onSubmit={(event) => handleUpload(event, request)}>
                <label>
                  Type de pièce
                  <input
                    required
                    maxLength="120"
                    value={formByRequest[request.id]?.documentType || ''}
                    onChange={(event) => setFormByRequest((current) => ({
                      ...current,
                      [request.id]: { ...current[request.id], documentType: event.target.value },
                    }))}
                  />
                </label>
                <label>
                  Montant justifié (FCFA)
                  <input
                    required
                    min="1"
                    step="1"
                    type="number"
                    value={formByRequest[request.id]?.amount || ''}
                    onChange={(event) => setFormByRequest((current) => ({
                      ...current,
                      [request.id]: { ...current[request.id], amount: event.target.value },
                    }))}
                  />
                </label>
                <label>
                  Date de la pièce
                  <input
                    required
                    type="date"
                    value={formByRequest[request.id]?.documentDate || ''}
                    onChange={(event) => setFormByRequest((current) => ({
                      ...current,
                      [request.id]: { ...current[request.id], documentDate: event.target.value },
                    }))}
                  />
                </label>
                <label>
                  Fichier PDF, JPG ou PNG (10 Mo maximum)
                  <input
                    required
                    accept="application/pdf,image/jpeg,image/png"
                    type="file"
                    disabled={!uploadsEnabled || isSubmitting}
                    onChange={(event) => setFormByRequest((current) => ({
                      ...current,
                      [request.id]: { ...current[request.id], file: event.target.files?.[0] },
                    }))}
                  />
                </label>
                <button type="submit" disabled={!uploadsEnabled || isSubmitting}>
                  Déposer le justificatif
                </button>
              </form>
            )}

            {documents.some((document) => (
              document.status === 'REJETE' && !documents.some((candidate) => candidate.replacesId === document.id)
            )) && isBeneficiary && uploadsEnabled && (
              <p className="supporting-replacement-notice">
                Un justificatif a été rejeté : vous pouvez déposer une pièce de remplacement.
              </p>
            )}

            {documents.length === 0 ? (
              <p className="supporting-no-documents">Aucune pièce déposée.</p>
            ) : (
              <ul className="supporting-document-list">
                {documents.map((document) => {
                  const canAct = canReview && document.status === 'A_CONTROLER'
                    && request.requesterId !== currentUser.id
                    && (currentUser.role === 'COMPTABLE'
                      || request.requesterRole === 'COMPTABLE')
                  return (
                    <li key={document.id}>
                      <div className="supporting-document-details">
                        <strong>{document.documentType} · {document.fileName}</strong>
                        <span>{amountFormatter.format(Number(document.amount))} FCFA · pièce du {formatDate(document.documentDate)}</span>
                        <span>Déposé par {document.uploadedByName} · {dateFormatter.format(new Date(document.uploadedAt))}</span>
                        {document.rejectionReason && <span className="supporting-rejection-reason">{document.rejectionReason}</span>}
                      </div>
                      <a href={`/api/cash-requests/${request.id}/supporting-documents/${document.id}/file`} target="_blank" rel="noreferrer">
                        Consulter
                      </a>
                      <span className={`supporting-document-status status-${document.status.toLowerCase()}`}>
                        {statusLabels[document.status]}
                      </span>
                      {canAct && (
                        <div className="supporting-review-actions">
                          <input
                            aria-label={`Motif de rejet pour ${document.fileName}`}
                            placeholder="Motif obligatoire si rejet"
                            value={reviewByDocument[document.id]?.reason || ''}
                            onChange={(event) => setReviewByDocument((current) => ({
                              ...current,
                              [document.id]: { reason: event.target.value },
                            }))}
                          />
                          <button
                            type="button"
                            disabled={pendingAction === document.id}
                            onClick={() => handleReview(request, document, 'ACCEPTE')}
                          >
                            Accepter
                          </button>
                          <button
                            type="button"
                            disabled={pendingAction === document.id}
                            onClick={() => handleReview(request, document, 'REJETE')}
                          >
                            Rejeter
                          </button>
                        </div>
                      )}
                    </li>
                  )
                })}
              </ul>
            )}
          </article>
        )
      })}
    </section>
  )
}

export default SupportingDocumentsPage
