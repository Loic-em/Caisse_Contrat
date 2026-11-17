import { useEffect, useState } from 'react'
import PageBackButton from '../../../components/layout/PageBackButton'
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
  A_CONTROLER: 'à contrôler',
  ACCEPTE: 'Accept',
  REJETE: 'Rejet',
}

function formatDate(value) {
  return dateFormatter.format(new Date(`${value}T00:00:00`))
}

function SupportingDocumentsPage({ currentUser, focusRequestId = '', onOpenRegularization, onNavigate }) {
  const [requests, setRequests] = useState([])
  const [documentsByRequest, setDocumentsByRequest] = useState({})
  const [uploadsEnabled, setUploadsEnabled] = useState(false)
  const [availabilityCheckFailed, setAvailabilityCheckFailed] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingAction, setPendingAction] = useState('')
  const [formByRequest, setFormByRequest] = useState({})
  const [reviewByDocument, setReviewByDocument] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const canReview = currentUser.role === 'COMPTABLE' || currentUser.role === 'ADJOINT_COMPTABLE'

  async function fetchPageData() {
    const [allRequests, availabilityResult] = await Promise.all([
      getCashRequests(),
      getSupportingDocumentAvailability()
        .then((availability) => ({ availability, failed: false }))
        .catch(() => ({ availability: null, failed: true })),
    ])
    const eligibleRequests = allRequests.filter((request) =>
      ['DECAISSEE', 'EN_REGULARISATION', 'REGULARISEE'].includes(request.status))
    const entries = await Promise.all(eligibleRequests.map(async (request) => [
      request.id,
      await getSupportingDocuments(request.id),
    ]))
    return {
      requests: eligibleRequests,
      documentsByRequest: Object.fromEntries(entries),
      uploadsEnabled: availabilityResult.availability?.uploadsEnabled === true,
      availabilityCheckFailed: availabilityResult.failed,
    }
  }

  function applyPageData(pageData) {
    setRequests(pageData.requests)
    setDocumentsByRequest(pageData.documentsByRequest)
    setUploadsEnabled(pageData.uploadsEnabled)
    setAvailabilityCheckFailed(pageData.availabilityCheckFailed)
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

  useEffect(() => {
    if (isLoading || !focusRequestId) return undefined

    const frame = window.requestAnimationFrame(() => {
      document.getElementById(`supporting-request-${focusRequestId}`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
    return () => window.cancelAnimationFrame(frame)
  }, [focusRequestId, isLoading, requests])

  async function handleUpload(event, request) {
    event.preventDefault()
    const uploadFormElement = event.currentTarget
    if (!uploadsEnabled) {
      setError("Le dépôt reste désactivé tant que ClamAV n'est pas opérationnel ou que la clé de chiffrement n'est pas configurée.")
      return
    }
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
      uploadFormElement?.reset?.()
    } catch (uploadError) {
      setError(uploadError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleReview(request, document, decision) {
    const review = reviewByDocument[document.id] || {}
    if (decision === 'REJETE' && !review.reason?.trim()) {
      setError('Un motif est obligatoire pour rejetéer un justificatif.')
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
      setSuccess(`Le justificatif a t ${decision === 'ACCEPTE' ? 'accepté' : 'rejeté'}.`)
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
          <h1>Justificatifs de dépenses</h1>
          <p>Après le décaissement, déposez les preuves des dépenses de la mission, consultez-les et suivez leur contrôle comptable.</p>
        </div>
        <div className="supporting-documents-heading-actions">
          <PageBackButton onClick={() => onNavigate?.('receipts')}>Retour aux bons</PageBackButton>
          <button type="button" onClick={loadPage}>Actualiser</button>
        </div>
      </header>

      <section className="supporting-documents-purpose" aria-labelledby="supporting-documents-purpose-title">
        <h2 id="supporting-documents-purpose-title">À quoi servent les justificatifs de dépenses ?</h2>
        <p>
          Après le décaissement, ils prouvent les dépenses effectuées avec l’avance de caisse : factures,
          reçus, tickets de transport, d'hébergement ou de restauration.
        </p>
        <p>
          Pour chaque pièce, indiquez sa nature, sa date et le montant correspondant. La comptabilité les
          contrôle et les montants acceptés servent à comparer les dépenses à l’avance reçue, calculer le
          solde à régulariser et traiter un éventuel retour de fonds.
        </p>
        <p>
          Cette rubrique ne sert pas à préparer la mission ni à déposer un ordre de mission avant le départ.
          Elle concerne uniquement les dépenses réalisées après la remise des fonds.
        </p>
        <p>
          Le formulaire de dépôt apparaît uniquement pour le bénéficiaire de la demande, après le statut
           « Décaissée » et pendant « En régularisation ». Si le formulaire est visible mais désactivé,
          l’avertissement de sécurité ci-dessous indique pourquoi.
        </p>
      </section>

      {!uploadsEnabled && (
        <div className="supporting-documents-warning" role="status">
          <p>Le dépôt des fichiers reste désactivé tant que ClamAV n'est pas opérationnel ou que la clé de chiffrement n'est pas configurée.</p>
          <p>La consultation et le contrôle des pièces déjà déposéees restent disponibles.</p>
          {availabilityCheckFailed && <small>La disponibilité des services n'a pas pu être vérifiée ; le dépôt reste bloqué par sécurité.</small>}
        </div>
      )}
      {error && <p className="supporting-documents-message is-error" role="alert">{error}</p>}
      {success && <p className="supporting-documents-message is-success" role="status">{success}</p>}

      {requests.length === 0 ? (
        <div className="supporting-documents-empty">
          <h2>Aucun dossier décaissé ou régularisé</h2>
          <p>Les justificatifs de dépenses apparaîtront ici après le décaissement d’une demande visible pour votre compte.</p>
        </div>
      ) : requests.map((request) => {
        const isBeneficiary = request.requesterId === currentUser.id
        const documents = documentsByRequest[request.id] || []
        const canPrepareDeposit = isBeneficiary
          && ['DECAISSEE', 'EN_REGULARISATION'].includes(request.status)
        return (
          <article className="supporting-request-card" id={`supporting-request-${request.id}`} key={request.id}>
            <header>
              <div>
                <h2>{request.subject}</h2>
                <p>{request.requesterName}  {request.location}  · mission terminée le {formatDate(request.endDate)}</p>
              </div>
              <div className="supporting-request-header-actions">
                <strong>{amountFormatter.format(Number(request.finalAmount))} FCFA</strong>
                {request.status === 'EN_REGULARISATION' && onOpenRegularization && (
                  <button type="button" onClick={() => onOpenRegularization(request.id)}>
                    Suivre la régularisation
                  </button>
                )}
                {request.status === 'REGULARISEE' && <span className="supporting-closed-label">Régularisée</span>}
              </div>
            </header>

            {canPrepareDeposit && (
              <form className="supporting-upload-form" onSubmit={(event) => handleUpload(event, request)}>
                <label>
                  Type de pièce
                  <select
                    required
                    disabled={!uploadsEnabled || isSubmitting}
                    value={formByRequest[request.id]?.documentType || ''}
                    onChange={(event) => setFormByRequest((current) => ({
                      ...current,
                      [request.id]: { ...current[request.id], documentType: event.target.value },
                    }))}
                  >
                    <option value="">Choisissez une catgorie</option>
                    <option value="Facture">Facture</option>
                    <option value="Reu">Reu</option>
                    <option value="Ticket de transport">Ticket de transport</option>
                    <option value="Carburant">Carburant</option>
                    <option value="Hbergement">Hbergement</option>
                    <option value="Restauration">Restauration</option>
                    <option value="Page et stationnement">Page et stationnement</option>
                    <option value="Fournitures et matriel">Fournitures et matriel</option>
                    <option value="Autre">Autre</option>
                  </select>
                </label>
                <label>
                  Montant justifi (FCFA)
                  <input
                    required
                    disabled={!uploadsEnabled || isSubmitting}
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
                    disabled={!uploadsEnabled || isSubmitting}
                    type="date"
                    value={formByRequest[request.id]?.documentDate || ''}
                    onChange={(event) => setFormByRequest((current) => ({
                      ...current,
                      [request.id]: { ...current[request.id], documentDate: event.target.value },
                    }))}
                  />
                </label>
                <label>
                  Fichier PDF ou photo (10 Mo maximum)
                  <input
                    required
                    disabled={!uploadsEnabled || isSubmitting}
                    accept="application/pdf,image/*"
                    type="file"
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

            {isBeneficiary && request.status === 'REGULARISEE' && (
              <p className="supporting-closed-notice">La régularisation est clôturée. Les pièces restent consultables dans ce dossier.</p>
            )}

            {documents.some((document) => (
              document.status === 'REJETE' && !documents.some((candidate) => candidate.replacesId === document.id)
            )) && isBeneficiary && uploadsEnabled && (
              <p className="supporting-replacement-notice">
                Un justificatif a t rejeté : vous pouvez déposer une pièce de remplacement.
              </p>
            )}

            {documents.length === 0 ? (
              <p className="supporting-no-documents">Aucune pièce déposé.</p>
            ) : (
              <ul className="supporting-document-list">
                {documents.map((document) => {
                  const canAct = canReview && request.status !== 'REGULARISEE' && document.status === 'A_CONTROLER'
                    && request.requesterId !== currentUser.id
                    && (currentUser.role === 'COMPTABLE'
                      || request.requesterRole === 'COMPTABLE')
                  return (
                    <li key={document.id}>
                      <div className="supporting-document-details">
                        <strong>{document.documentType}  {document.fileName}</strong>
                        <span>{amountFormatter.format(Number(document.amount))} FCFA  pièce du {formatDate(document.documentDate)}</span>
                        <span>Déposé par {document.uploadedByName}  {dateFormatter.format(new Date(document.uploadedAt))}</span>
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
                            aria-label={`Motif de rejeté pour ${document.fileName}`}
                            placeholder="Motif obligatoire si rejeté"
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
