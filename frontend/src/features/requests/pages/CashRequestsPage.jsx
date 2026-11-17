import { useEffect, useRef, useState } from 'react'
import { PlusOutlined, WalletOutlined } from '@ant-design/icons'
import {
  approveCashRequest,
  cancelCashRequest,
  getCashVoucher,
  getCashRequests,
  issueCashVoucher,
  rejectCashRequest,
  setCashRequestFinalAmount,
  submitCashRequest,
  signCashVoucher,
  disburseCashVoucher,
} from '../api/cashRequestsApi'
import { getOwnSignature, reauthenticate, saveOwnSignature } from '../../auth/authApi'
import PageBackButton from '../../../components/layout/PageBackButton'
import RequestForm from '../components/RequestForm'
import RegularizationPanel from './RegularizationPanel'
import './CashRequestsPage.css'

const statusLabels = {
  DRAFT: 'Brouillon',
  SOUMISE: 'En attente du N+1',
  VALIDEE_N1: 'En attente du N+2',
  VALIDEE: 'À traiter en comptabilité',
  A_CORRIGER: 'À corriger',
  MONTANT_ARRETE: 'Montant arrêté',
  BON_EMIS: 'Bon émis · visas en attente',
  VISA_REFUSE: 'Visa refusé',
  DECAISSEE: 'Décaissée',
  EN_REGULARISATION: 'En régularisation',
  REGULARISEE: 'Clôturée',
  ANNULEE: 'Annulée',
}

const dateFormatter = new Intl.DateTimeFormat('fr-FR')
const amountFormatter = new Intl.NumberFormat('fr-FR')
const cancellableStatuses = new Set(['DRAFT', 'SOUMISE', 'VALIDEE_N1', 'A_CORRIGER'])
const voucherStatuses = new Set(['BON_EMIS', 'VISA_REFUSE', 'DECAISSEE', 'EN_REGULARISATION', 'REGULARISEE'])
const visaLabels = {
  DIRECTION: 'La Direction',
  CAISSE: 'La Caisse',
  BENEFICIAIRE: 'Le bénéficiaire',
}
const fundOriginLabels = {
  BNI: 'BNI',
  NSIA: 'NSIA',
  BRIDGE_BANK: 'Bridge Bank',
  BGDI: 'BGDI',
  BICICI: 'BICICI',
  DG: 'DG',
}

function formatDate(value) {
  return dateFormatter.format(new Date(`${value}T00:00:00`))
}

function CashRequestsPage({
  currentUser,
  initialRegularizationRequestId = '',
  onNavigate,
  onRegularizationOpened,
  view = 'requests',
}) {
  const isVoucherView = view === 'vouchers'
  const [requests, setRequests] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [isCreatingRequest, setIsCreatingRequest] = useState(false)
  const [requestToEdit, setRequestToEdit] = useState(null)
  const [successMessage, setSuccessMessage] = useState('')
  const [actionForm, setActionForm] = useState(null)
  const [isSavingAction, setIsSavingAction] = useState(false)
  const [selectedVoucher, setSelectedVoucher] = useState(null)
  const voucherPreviewRef = useRef(null)
  const actionFormRef = useRef(null)
  const [voucherError, setVoucherError] = useState('')
  const [signatureAvailable, setSignatureAvailable] = useState(null)
  const [signatureDataUrl, setSignatureDataUrl] = useState('')
  const [regularizationRequest, setRegularizationRequest] = useState(null)

  useEffect(() => {
    if (!isVoucherView || !selectedVoucher) return undefined

    const previousTitle = document.title
    document.title = `Bon de caisse ${selectedVoucher.voucherNumber}`
    const frame = window.requestAnimationFrame(() => {
      voucherPreviewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })

    return () => {
      document.title = previousTitle
      window.cancelAnimationFrame(frame)
    }
  }, [isVoucherView, selectedVoucher])

  useEffect(() => {
    if (!actionForm || ['submit', 'approve'].includes(actionForm.action)) return undefined

    const frame = window.requestAnimationFrame(() => {
      actionFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
    return () => window.cancelAnimationFrame(frame)
  }, [actionForm?.requestId, actionForm?.action, actionForm?.visaType])

  useEffect(() => {
    const controller = new AbortController()

    getCashRequests({ signal: controller.signal })
      .then(setRequests)
      .catch((error) => {
        if (error.name !== 'AbortError') setLoadError(error.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })

    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!initialRegularizationRequestId || isLoading) return

    const targetRequest = requests.find((item) => item.id === initialRegularizationRequestId)
    if (targetRequest) {
      setLoadError('')
      setRegularizationRequest(targetRequest)
      onRegularizationOpened?.()
    } else if (!loadError) {
      setLoadError('La demande à régulariser est introuvable dans votre espace.')
      onRegularizationOpened?.()
    }
  }, [initialRegularizationRequestId, isLoading, requests, loadError, onRegularizationOpened])

  function handleCreated(request) {
    setRequests((currentRequests) => currentRequests.some((item) => item.id === request.id)
      ? currentRequests.map((item) => item.id === request.id ? request : item)
      : [request, ...currentRequests])
    setIsCreatingRequest(false)
    setRequestToEdit(null)
    setSuccessMessage(request.submittedAt
      ? 'La demande a t modifie.'
      : 'Le brouillon a été enregistré. Vous pouvez le vérifier puis le soumettre.')
  }

  function handleRetry() {
    setIsLoading(true)
    setLoadError('')
    getCashRequests()
      .then(setRequests)
      .catch((error) => setLoadError(error.message))
      .finally(() => setIsLoading(false))
  }

  function openActionForm(request, action) {
    setActionError('')
    setActionForm({
      requestId: request.id,
      action,
      reason: action === 'issue-voucher' ? request.subject : '',
      finalAmount: String(request.finalAmount ?? request.estimatedAmount),
    })
  }

  async function handleActionSubmit(event, request) {
    event.preventDefault()
    setActionError('')
    setIsSavingAction(true)

    try {
      let updated
      if (actionForm.action === 'submit') {
        updated = await submitCashRequest(request.id)
      } else if (actionForm.action === 'approve') {
        updated = await approveCashRequest(request.id)
      } else if (actionForm.action === 'reject') {
        updated = await rejectCashRequest(request.id, actionForm.reason)
      } else if (actionForm.action === 'cancel') {
        updated = await cancelCashRequest(request.id, actionForm.reason)
      } else if (actionForm.action === 'issue-voucher') {
        const voucher = await issueCashVoucher(request.id, actionForm.reason)
        setRequests((current) => current.map((item) => item.id === request.id
          ? { ...item, status: voucher.requestStatus }
          : item))
        setSelectedVoucher(voucher)
        setActionForm(null)
        setSuccessMessage(`Le bon n ${voucher.voucherNumber} a t Émis. Les visas restent  apposer.`)
        onNavigate?.('receipts')
        return
      } else if (actionForm.action === 'sign-voucher') {
        if (signatureDataUrl) {
          await saveOwnSignature(signatureDataUrl)
        } else if (!signatureAvailable) {
          throw new Error('Choisissez votre signature avant de continuer.')
        }
        await reauthenticate(actionForm.password)
        const voucher = await signCashVoucher(request.id, actionForm.visaType)
        setSelectedVoucher(voucher)
        setActionForm(null)
        setSignatureDataUrl('')
        setSuccessMessage(`Le visa  ${visaLabels[actionForm.visaType]}  a t enregistr.`)
        return
      } else if (actionForm.action === 'disburse') {
        const voucher = await disburseCashVoucher(
          request.id,
          actionForm.disbursedOn,
          actionForm.fundOrigin,
        )
        setSelectedVoucher(voucher)
        setRequests((current) => current.map((item) => item.id === request.id
          ? { ...item, status: voucher.requestStatus }
          : item))
        setActionForm(null)
        setSuccessMessage(`Le bon n ${voucher.voucherNumber} a t décaissé.`)
        return
      } else {
        updated = await setCashRequestFinalAmount(
          request.id,
          actionForm.finalAmount,
          actionForm.reason,
        )
      }

      setRequests((current) => current.map((item) => item.id === updated.id ? updated : item))
      setActionForm(null)
      setSuccessMessage('La demande a t Émise  jour.')
    } catch (error) {
      setActionError(error.message)
    } finally {
      setIsSavingAction(false)
    }
  }

  async function handleViewVoucher(request) {
    setVoucherError('')
    setSelectedVoucher(null)
    setActionForm(null)
    try {
      setSelectedVoucher(await getCashVoucher(request.id))
    } catch (error) {
      setVoucherError(error.message)
    }
  }

  const selectedRequest = requests.find((request) => request.id === selectedVoucher?.requestId)
  const visibleRequests = requests.filter((request) => voucherStatuses.has(request.status) === isVoucherView)
  const requestsToDisplay = isVoucherView && selectedVoucher
    ? visibleRequests.filter((request) => (
      request.id !== selectedVoucher.requestId || actionForm?.requestId === request.id
    ))
    : visibleRequests

  function handlePrintVoucher() {
    window.print()
  }

  async function openVisaForm(request, type) {
    setActionError('')
    setSignatureDataUrl('')
    setSignatureAvailable(null)
    setActionForm({
      requestId: request.id,
      action: 'sign-voucher',
      visaType: type,
      password: '',
    })
    try {
      await getOwnSignature()
      setSignatureAvailable(true)
    } catch (error) {
      if (error.status === 404) {
        setSignatureAvailable(false)
      } else {
        setActionError(error.message)
      }
    }
  }

  function handleSignatureFile(event) {
    const file = event.target.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/') || file.size > 2 * 1024 * 1024) {
      setActionError('La signature doit être une image de 2 Mo maximum.')
      event.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setSignatureDataUrl(reader.result)
        setSignatureAvailable(true)
        setActionError('')
      }
    }
    reader.onerror = () => setActionError('Impossible de lire le fichier de signature.')
    reader.readAsDataURL(file)
  }

  if (isCreatingRequest) {
    return (
      <RequestForm
        onCancel={() => {
          setIsCreatingRequest(false)
          setRequestToEdit(null)
        }}
        onCreated={handleCreated}
        requestToEdit={requestToEdit}
      />
    )
  }

  if (regularizationRequest) {
    return (
      <RegularizationPanel
        currentUser={currentUser}
        onNavigate={onNavigate}
        onClose={() => setRegularizationRequest(null)}
        onRequestUpdated={(updated) => setRequests((current) => current.map((item) => (
          item.id === updated.id ? updated : item
        )))}
        request={regularizationRequest}
      />
    )
  }

  return (
    <section className="content-page cash-requests-page">
      <div className="cash-requests-heading">
        <div>
          <p className="eyebrow">CAISSE</p>
          <h1>{isVoucherView ? 'Bons de caisse' : 'Demandes de caisse'}</h1>
          <p className="cash-requests-description">
            {isVoucherView
              ? 'Consultez les bons émis, suivez les visas requis et vérifiez les décaissements.'
              : 'Déclarez une mission et suivez sa validation par vos responsables.'}
          </p>
        </div>
        <div className="cash-requests-heading-actions">
          <PageBackButton onClick={() => onNavigate?.('overview')}>Retour à l’accueil</PageBackButton>
          <button className="refresh-requests-button" type="button" onClick={handleRetry} disabled={isLoading}>
            {isLoading ? 'Actualisation…' : 'Actualiser'}
          </button>
          {!isVoucherView && (
            <button
              className="create-request-button"
              type="button"
              onClick={() => {
                setSuccessMessage('')
                setRequestToEdit(null)
                setIsCreatingRequest(true)
              }}
            >
              <PlusOutlined aria-hidden="true" />
              <span>Nouvelle demande</span>
            </button>
          )}
        </div>
      </div>

      {successMessage && <div className="requests-success-notice" role="status">{successMessage}</div>}

      {!isVoucherView && (
        <div className="requests-prototype-notice" role="status">
          <span className="requests-prototype-dot" />
          Les visas et le décaissement sont disponibles. Après la mission, consultez la régularisation pour suivre les justificatifs de dépenses et les retours de fonds.
        </div>
      )}

      {voucherError && <div className="requests-feedback requests-error" role="alert">{voucherError}</div>}
      {isVoucherView && selectedVoucher && (
        <section className="cash-voucher-preview" aria-label="Bon de caisse Émis" ref={voucherPreviewRef}>
          <div className="cash-voucher-heading">
            <div>
              <p className="eyebrow">BON DE CAISSE  {selectedVoucher.type === 'ENTREE' ? 'ENTRE' : 'SORTIE'}</p>
              <h2>Bon n {selectedVoucher.voucherNumber}</h2>
            </div>
            <div className="cash-voucher-toolbar">
              <button type="button" onClick={handlePrintVoucher}>Imprimer le bon</button>
              <button type="button" onClick={() => setSelectedVoucher(null)}>Fermer</button>
            </div>
          </div>
          <div className="cash-voucher-print">
            <header className="voucher-print-header">
              <div className="voucher-print-brand">
                <img src="https://www.diginfactory.com/logo-white.png" alt="DIGIN FACTORY" />
              </div>
              <div className="voucher-print-title">
                <h3>BON DE CAISSE</h3>
                <p>N <strong>{selectedVoucher.voucherNumber}</strong></p>
              </div>
            </header>
            <div className="voucher-print-meta">
              <div className="voucher-print-type" aria-label="Type du bon">
                <span><i aria-hidden="true">{selectedVoucher.type === 'SORTIE' ? '' : ''}</i> SORTIE</span>
                <span><i aria-hidden="true">{selectedVoucher.type === 'ENTREE' ? '' : ''}</i> ENTRE</span>
              </div>
              <p className="voucher-print-date">Date : <strong>{new Intl.DateTimeFormat('fr-FR').format(new Date(selectedVoucher.issuedAt))}</strong></p>
              <p className="voucher-print-amount">
                <strong>{amountFormatter.format(Number(selectedVoucher.amount))}</strong>
                <span>DE FCFA</span>
              </p>
            </div>
            <div className="voucher-print-recipient">
              <span>M. / Mme / Mlle :</span>
              <strong>{selectedVoucher.beneficiaryName}</strong>
            </div>
            <div className="voucher-print-words">
              <span>La somme de : <small>(en lettres)</small></span>
              <strong>{selectedVoucher.amountInWords}</strong>
            </div>
            <div className="voucher-print-reason">
              <span>Motif (libell) :</span>
              <strong>{selectedVoucher.reason}</strong>
            </div>
            <div className="voucher-print-visas">
              <h4>VISAS</h4>
              <div className="voucher-print-signatures">
                {[
                  ['BENEFICIAIRE', 'Bénéficiaire'],
                  ['CAISSE', 'Caisse'],
                  ['DIRECTION', 'La Direction'],
                ].map(([type, label]) => {
                  const visa = selectedVoucher.visas.find((entry) => entry.type === type)
                  return (
                    <section className="voucher-print-signature" key={type}>
                      <strong>{label}</strong>
                      <span className="voucher-print-name">Nom : {visa?.signerName || ''}</span>
                      <span className="voucher-print-sign-label">Signature</span>
                      {visa && <img alt={`Signature de ${visa.signerName}`} src={visa.signatureDataUrl} />}
                    </section>
                  )
                })}
              </div>
            </div>
          </div>
          <dl className="cash-voucher-details cash-voucher-context">
            <div><dt>Bénéficiaire</dt><dd>{selectedVoucher.beneficiaryName}</dd></div>
            <div><dt>Mission</dt><dd>{selectedVoucher.requestSubject} · {selectedVoucher.missionLocation}</dd></div>
            <div><dt>Période</dt><dd>{formatDate(selectedVoucher.missionStartDate)} – {formatDate(selectedVoucher.missionEndDate)}</dd></div>
            <div><dt>Motif</dt><dd>{selectedVoucher.reason}</dd></div>
            <div><dt>Catgorie</dt><dd>{selectedVoucher.category}</dd></div>
            <div><dt>Montant</dt><dd>{amountFormatter.format(Number(selectedVoucher.amount))} FCFA</dd></div>
            <div className="cash-voucher-amount-words"><dt>Montant en lettres</dt><dd>{selectedVoucher.amountInWords}</dd></div>
            <div><dt>Émis par</dt><dd>{selectedVoucher.issuedByName} · {new Intl.DateTimeFormat('fr-FR', {
              dateStyle: 'medium',
              timeStyle: 'short',
            }).format(new Date(selectedVoucher.issuedAt))}</dd></div>
            {selectedVoucher.disbursedAt && (
              <>
                <div><dt>Origine des fonds</dt><dd>{fundOriginLabels[selectedVoucher.fundOrigin]}</dd></div>
                <div><dt>Décaissement</dt><dd>{formatDate(selectedVoucher.disbursedOn)} · {selectedVoucher.disbursedByName}</dd></div>
              </>
            )}
          </dl>
          <div className="cash-voucher-visas cash-voucher-context" aria-label="Détail des visas">
            {Object.entries(visaLabels).map(([type, label]) => {
              const visa = selectedVoucher.visas.find((entry) => entry.type === type)
              return (
                <div className={visa ? 'cash-voucher-visa is-signed' : 'cash-voucher-visa'} key={type}>
                  <strong>{label}</strong>
                  {visa ? (
                    <>
                      <img alt={`Signature de ${visa.signerName}`} src={visa.signatureDataUrl} />
                      <span>{visa.signerName}</span>
                      <time dateTime={visa.signedAt}>{new Intl.DateTimeFormat('fr-FR', {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      }).format(new Date(visa.signedAt))}</time>
                    </>
                  ) : <span>En attente</span>}
                </div>
              )
            })}
          </div>
          {selectedVoucher.status === 'EMIS' && selectedRequest && (
            <div className="cash-voucher-actions">
              {selectedRequest.requesterId === currentUser.id
                && !selectedVoucher.visas.some((visa) => visa.type === 'BENEFICIAIRE')
                && <button type="button" onClick={() => openVisaForm(selectedRequest, 'BENEFICIAIRE')}>Apposer mon visa bénéficiaire</button>}
              {((selectedRequest.validatorN2Id === currentUser.id)
                || (selectedRequest.validatorN2Id == null
                  && selectedRequest.validatorN1Id === currentUser.id
                  && currentUser.role === 'DIRECTEUR_GENERAL'))
                && !selectedVoucher.visas.some((visa) => visa.type === 'DIRECTION')
                && <button type="button" onClick={() => openVisaForm(selectedRequest, 'DIRECTION')}>Apposer mon visa Direction</button>}
              {(currentUser.role === 'COMPTABLE'
                || (currentUser.role === 'ADJOINT_COMPTABLE' && selectedRequest.requesterRole === 'COMPTABLE'))
                && selectedRequest.requesterId !== currentUser.id
                && !selectedVoucher.visas.some((visa) => visa.type === 'CAISSE')
                && <button type="button" onClick={() => openVisaForm(selectedRequest, 'CAISSE')}>Apposer mon visa Caisse</button>}
              {selectedVoucher.visas.length === 3
                && (currentUser.role === 'COMPTABLE'
                  || (currentUser.role === 'ADJOINT_COMPTABLE' && selectedRequest.requesterRole === 'COMPTABLE'))
                && selectedRequest.requesterId !== currentUser.id
                && <button type="button" onClick={() => {
                  setActionError('')
                  setActionForm({
                    requestId: selectedRequest.id,
                    action: 'disburse',
                    disbursedOn: new Date().toISOString().slice(0, 10),
                    fundOrigin: '',
                  })
                }}>Enregistrer le décaissement</button>}
            </div>
          )}
          {selectedVoucher.status === 'EMIS' ? (
            <p className="cash-voucher-status" role="status">
              Le décaissement est bloqué jusqu'à l'apposition des trois visas distincts.
            </p>
          ) : (
            <div className="cash-voucher-next-steps">
              <p className="cash-voucher-status is-complete" role="status">
                {selectedRequest?.status === 'REGULARISEE'
                  ? 'La régularisation est clôturé. Les justificatifs de dépenses déposés restent consultables.'
                  : 'Fonds reÉmis. Les justificatifs de dépenses et la régularisation restent  traiter.'}
              </p>
              {selectedRequest && (
                <div>
                  <button
                    type="button"
                    onClick={() => onNavigate?.('supporting-documents', { requestId: selectedRequest.id })}
                  >
                    Ouvrir les justificatifs de dépenses
                  </button>
                  {selectedRequest.status === 'EN_REGULARISATION' && (
                    <button type="button" onClick={() => setRegularizationRequest(selectedRequest)}>
                      Suivre la régularisation
                    </button>
                  )}
                </div>
              )}
            </div>
          )}
        </section>
      )}

      {loadError ? (
        <div className="requests-feedback requests-error" role="alert">
          <p>{loadError}</p>
          <button type="button" onClick={handleRetry}>Réessayer</button>
        </div>
      ) : isLoading ? (
        <div className="requests-feedback" role="status">Chargement des demandes…</div>
      ) : visibleRequests.length === 0 && !selectedVoucher ? (
        <div className="requests-empty-card">
          <span className="requests-empty-icon"><WalletOutlined aria-hidden="true" /></span>
          <h2>{isVoucherView ? 'Aucun bon de caisse à afficher' : 'Aucune demande à afficher'}</h2>
          <p>{isVoucherView
            ? 'Les bons émis pour les demandes visibles par votre compte apparaîtront ici.'
            : 'Les demandes que vous créez ou qui vous sont confiées apparaîtront ici.'}</p>
          {!isVoucherView && (
            <button
              className="requests-empty-action"
              type="button"
              onClick={() => {
                setRequestToEdit(null)
                setIsCreatingRequest(true)
              }}
            >
              <PlusOutlined aria-hidden="true" />
              Créer une demande
            </button>
          )}
        </div>
      ) : requestsToDisplay.length > 0 ? (
        <div className="cash-request-list" aria-label={isVoucherView ? 'Bons de caisse' : 'Demandes enregistrées'}>
          {requestsToDisplay.map((request) => {
            const isRequester = request.requesterId === currentUser.id
            const isN1 = request.status === 'SOUMISE' && request.validatorN1Id === currentUser.id
            const isN2 = request.status === 'VALIDEE_N1' && request.validatorN2Id === currentUser.id
            const canReviewAmount = request.status === 'VALIDEE'
              && (currentUser.role === 'COMPTABLE'
                || (currentUser.role === 'ADJOINT_COMPTABLE' && request.requesterRole === 'COMPTABLE'))
            const canIssueVoucher = request.status === 'MONTANT_ARRETE'
              && (currentUser.role === 'COMPTABLE'
                || (currentUser.role === 'ADJOINT_COMPTABLE' && request.requesterRole === 'COMPTABLE'))
            const canSubmit = isRequester && (request.status === 'DRAFT' || request.status === 'A_CORRIGER')
            const canCancel = isRequester && cancellableStatuses.has(request.status)
            const canEdit = isRequester
              && (request.status === 'DRAFT'
                || request.status === 'A_CORRIGER'
                || (request.status === 'SOUMISE' && !request.n1DecisionAt))
            const isActionFormOpen = actionForm?.requestId === request.id
            const isVoucherActionTarget = isVoucherView
              && selectedVoucher?.requestId === request.id
              && isActionFormOpen
            const amountChanged = Number(actionForm?.finalAmount) !== Number(request.estimatedAmount)

            return (
              <article
                className={`cash-request-card ${isVoucherView ? 'cash-voucher-card' : ''} ${isVoucherActionTarget ? 'cash-voucher-action-target' : ''}`}
                key={request.id}
              >
                <div className="cash-request-card-heading">
                  <div>
                    {isVoucherView && <p className="cash-voucher-card-label">BON DE CAISSE</p>}
                    <h2>{request.subject}</h2>
                    <p>{isVoucherView && 'Mission : '}{request.location} · {request.category}</p>
                    {request.requesterName && (
                      <p>Demandeur : {request.requesterName} · {request.requesterDepartment}</p>
                    )}
                  </div>
                  <span className={`cash-request-status status-${request.status.toLowerCase().replaceAll('_', '-')}`}>
                    {statusLabels[request.status] || request.status}
                  </span>
                </div>
                <div className="cash-request-card-details">
                  <span>Du {formatDate(request.startDate)} au {formatDate(request.endDate)}</span>
                  {isVoucherView ? (
                    <span>Montant du bon : <strong>{amountFormatter.format(Number(request.finalAmount ?? request.estimatedAmount))} FCFA</strong></span>
                  ) : (
                    <span>Estimation : <strong>{amountFormatter.format(Number(request.estimatedAmount))} FCFA</strong></span>
                  )}
                  {!isVoucherView && request.finalAmount != null && (
                    <span>Montant retenu : <strong>{amountFormatter.format(Number(request.finalAmount))} FCFA</strong></span>
                  )}
                </div>
                {request.rejectionReason && (
                  <p className="cash-request-reason"><strong>Motif du rejet :</strong> {request.rejectionReason}</p>
                )}
                {request.cancellationReason && (
                  <p className="cash-request-reason"><strong>Motif de lannulation :</strong> {request.cancellationReason}</p>
                )}
                <div className="cash-request-actions">
                  {canEdit && (
                    <button
                      type="button"
                      onClick={() => {
                        setRequestToEdit(request)
                        setIsCreatingRequest(true)
                      }}
                    >
                      Modifier
                    </button>
                  )}
                  {canSubmit && (
                    <button type="button" onClick={() => openActionForm(request, 'submit')}>
                      {request.status === 'A_CORRIGER' ? 'Resoumettre' : 'Soumettre'}
                    </button>
                  )}
                  {(isN1 || isN2) && (
                    <>
                      <button type="button" onClick={() => openActionForm(request, 'approve')}>
                        Valider {isN1 ? 'N+1' : 'N+2'}
                      </button>
                      <button className="request-reject-action" type="button" onClick={() => openActionForm(request, 'reject')}>
                        Rejeter
                      </button>
                    </>
                  )}
                  {canReviewAmount && (
                    <button type="button" onClick={() => openActionForm(request, 'accounting')}>
                      Fixer le montant
                    </button>
                  )}
                  {canIssueVoucher && (
                    <button type="button" onClick={() => openActionForm(request, 'issue-voucher')}>
                      Émettre le bon
                    </button>
                  )}
                  {isVoucherView && voucherStatuses.has(request.status) && (
                    <button type="button" onClick={() => handleViewVoucher(request)}>
                      {request.status === 'DECAISSEE'
                        ? 'Consulter le bon décaissé'
                        : request.status === 'REGULARISEE'
                          ? 'Consulter le bon clôturé'
                          : 'Consulter le bon'}
                    </button>
                  )}
                  {request.status === 'EN_REGULARISATION' && (
                    <button type="button" onClick={() => setRegularizationRequest(request)}>
                      Suivre la régularisation
                    </button>
                  )}
                  {canCancel && (
                    <button className="request-reject-action" type="button" onClick={() => openActionForm(request, 'cancel')}>
                      Annuler la demande
                    </button>
                  )}
                </div>
                {isActionFormOpen && actionForm.action !== 'submit' && actionForm.action !== 'approve' && (
                  <form
                    className="cash-request-action-form"
                    onSubmit={(event) => handleActionSubmit(event, request)}
                    ref={isActionFormOpen ? actionFormRef : null}
                  >
                    {actionForm.action === 'disburse' ? (
                      <>
                        <label>
                          Date de reÉmise des fonds
                          <input
                            required
                            type="date"
                            value={actionForm.disbursedOn}
                            onChange={(event) => setActionForm((current) => ({
                              ...current,
                              disbursedOn: event.target.value,
                            }))}
                          />
                        </label>
                        <label>
                          Origine des fonds
                          <select
                            required
                            value={actionForm.fundOrigin}
                            onChange={(event) => setActionForm((current) => ({
                              ...current,
                              fundOrigin: event.target.value,
                            }))}
                          >
                            <option value="">Choisir une origine</option>
                            {Object.entries(fundOriginLabels).map(([value, label]) => (
                              <option key={value} value={value}>{label}</option>
                            ))}
                          </select>
                        </label>
                      </>
                    ) : actionForm.action === 'sign-voucher' ? (
                      <>
                        <p>
                          Confirmez votre identité avec votre mot de passe juste avant le visa
                           {visaLabels[actionForm.visaType]} . Cette confirmation ne permet pas
                          de signer pour une autre personne.
                        </p>
                        {signatureAvailable === false && <p>Enregistrez une signature pour pouvoir signer.</p>}
                        {signatureAvailable === true && !signatureDataUrl && <p>Votre signature enregistrée sera copiée sur ce bon.</p>}
                        <label>
                          Signature personnelle (image, 2 Mo maximum)
                          <input accepté="image/*" type="file" onChange={handleSignatureFile} />
                        </label>
                        {signatureDataUrl && <img className="cash-signature-preview" alt="Aperçu de votre signature" src={signatureDataUrl} />}
                        <label>
                          Mot de passe pour confirmer votre identité
                          <input
                            autoComplete="current-password"
                            required
                            type="password"
                            value={actionForm.password}
                            onChange={(event) => setActionForm((current) => ({
                              ...current,
                              password: event.target.value,
                            }))}
                          />
                        </label>
                      </>
                    ) : actionForm.action === 'accounting' ? (
                      <>
                        <label>
                          Montant dfinitif (FCFA)
                          <input
                            min="1"
                            onChange={(event) => setActionForm((current) => ({ ...current, finalAmount: event.target.value }))}
                            required
                            step="1"
                            type="number"
                            value={actionForm.finalAmount}
                          />
                        </label>
                        <label>
                          Motif de lajustement {amountChanged ? '(obligatoire)' : '(facultatif)'}
                          <textarea
                            onChange={(event) => setActionForm((current) => ({ ...current, reason: event.target.value }))}
                            required={amountChanged}
                            rows="2"
                            value={actionForm.reason}
                          />
                        </label>
                      </>
                    ) : (
                      <label>
                        {actionForm.action === 'reject'
                          ? 'Motif du rejeté'
                          : actionForm.action === 'issue-voucher'
                            ? 'Motif du bon'
                            : 'Motif de lannulation'}
                        <textarea
                          minLength={1}
                          onChange={(event) => setActionForm((current) => ({ ...current, reason: event.target.value }))}
                          required
                          rows="2"
                          value={actionForm.reason}
                        />
                      </label>
                    )}
                    {actionError && <p className="requests-action-error" role="alert">{actionError}</p>}
                    <div className="cash-request-form-actions">
                      <button disabled={isSavingAction} type="submit">
                        {isSavingAction ? 'Enregistrement…' : 'Confirmer'}
                      </button>
                      <button disabled={isSavingAction} type="button" onClick={() => setActionForm(null)}>
                        Retour
                      </button>
                    </div>
                  </form>
                )}
                {isActionFormOpen && (actionForm.action === 'submit' || actionForm.action === 'approve') && (
                  <form className="cash-request-action-form" onSubmit={(event) => handleActionSubmit(event, request)}>
                    <p>
                      {actionForm.action === 'submit'
                        ? 'La demande sera transÉmise  votre responsable N+1.'
                        : 'Votre décision sera enregistrée dans le suivi de la demande.'}
                    </p>
                    {actionError && <p className="requests-action-error" role="alert">{actionError}</p>}
                    <div className="cash-request-form-actions">
                      <button disabled={isSavingAction} type="submit">
                        {isSavingAction ? 'Enregistrement…' : 'Confirmer'}
                      </button>
                      <button disabled={isSavingAction} type="button" onClick={() => setActionForm(null)}>
                        Retour
                      </button>
                    </div>
                  </form>
                )}
              </article>
            )
          })}
        </div>
      ) : null}
    </section>
  )
}

export default CashRequestsPage


