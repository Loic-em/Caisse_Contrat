import { useEffect, useMemo, useState } from 'react'
import {
  authorizePartialReturn,
  closeRegularization,
  getCashVoucher,
  getRegularizationSummary,
  issueEntryVoucher,
  signEntryVoucher,
} from '../api/cashRequestsApi'
import { getSupportingDocuments } from '../api/supportingDocumentsApi'
import { getOwnSignature, reauthenticate, saveOwnSignature } from '../../auth/authApi'
import './RegularizationPanel.css'

const amountFormatter = new Intl.NumberFormat('fr-FR')
const dateFormatter = new Intl.DateTimeFormat('fr-FR')
const fundOriginLabels = {
  BNI: 'BNI',
  NSIA: 'NSIA',
  BRIDGE_BANK: 'Bridge Bank',
  BGDI: 'BGDI',
  BICICI: 'BICICI',
  DG: 'DG',
}
const visaLabels = {
  BENEFICIAIRE: 'Bénéficiaire',
  CAISSE: 'Caisse',
  DIRECTION: 'La Direction',
}
const supportingDocumentStatusLabels = {
  A_CONTROLER: 'à contrôler',
  ACCEPTE: 'Accept',
  REJETE: 'Rejet',
}

function formatDate(value) {
  return value ? dateFormatter.format(new Date(`${value}T00:00:00`)) : ''
}

function formatTimestamp(value) {
  return value
    ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
    : ''
}

function formatAmount(value) {
  return `${amountFormatter.format(Number(value || 0))} FCFA`
}

function RegularizationPanel({ request, currentUser, onClose, onNavigate, onRequestUpdated }) {
  const [summary, setSummary] = useState(null)
  const [sourceVoucher, setSourceVoucher] = useState(null)
  const [documents, setDocuments] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [returnFormOpen, setReturnFormOpen] = useState(false)
  const [returnForm, setReturnForm] = useState({
    amount: '',
    reason: '',
    returnedOn: new Date().toISOString().slice(0, 10),
    fundOrigin: '',
    authorizationId: '',
  })
  const [authorizationFormOpen, setAuthorizationFormOpen] = useState(false)
  const [authorizationForm, setAuthorizationForm] = useState({ amount: '', reason: '' })
  const [visaForm, setVisaForm] = useState(null)
  const [signatureAvailable, setSignatureAvailable] = useState(null)
  const [signatureDataUrl, setSignatureDataUrl] = useState('')
  const [printingEntryId, setPrintingEntryId] = useState('')

  const isAccountant = currentUser.role === 'COMPTABLE'
    || (currentUser.role === 'ADJOINT_COMPTABLE' && request.requesterRole === 'COMPTABLE')
  const isBeneficiary = request.requesterId === currentUser.id
  const canAccount = isAccountant && !isBeneficiary
  const balance = Number(summary?.outstandingBalance || 0)
  const returnedCount = Number(summary?.partialReturnCount || 0)
  const needsAuthorization = returnedCount > 0
  const canAuthorizePartialReturn = currentUser.role === 'DIRECTEUR_GENERAL'
    && !isBeneficiary
    && needsAuthorization
    && balance > 0
  const availableAuthorizations = useMemo(() => (summary?.authorizations || [])
    .filter((authorization) => !authorization.consumedAt
      && Number(authorization.authorizedAmount) === Number(returnForm.amount)),
  [summary, returnForm.amount])
  const hasUnreviewedDocuments = documents.some((document) => document.status === 'A_CONTROLER')
  const hasUnfinishedEntryVoucher = (summary?.entryVouchers || []).some((voucher) => (
    (voucher.visas || []).length < 3
  ))
  const canClose = canAccount
    && balance === 0
    && !hasUnreviewedDocuments
    && !hasUnfinishedEntryVoucher

  async function loadRegularization() {
    setIsLoading(true)
    setError('')
    try {
      const [nextSummary, nextSourceVoucher, nextDocuments] = await Promise.all([
        getRegularizationSummary(request.id),
        getCashVoucher(request.id),
        getSupportingDocuments(request.id),
      ])
      setSummary(nextSummary)
      setSourceVoucher(nextSourceVoucher)
      setDocuments(nextDocuments)
      setReturnForm((current) => ({
        ...current,
        fundOrigin: current.fundOrigin || nextSourceVoucher.fundOrigin || '',
      }))
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadRegularization()
  }, [request.id])

  useEffect(() => {
    if (!printingEntryId) return undefined
    const clearPrintingEntry = () => setPrintingEntryId('')
    window.addEventListener('afterprint', clearPrintingEntry)
    return () => window.removeEventListener('afterprint', clearPrintingEntry)
  }, [printingEntryId])

  function handleReturnAmountChange(value) {
    const authorization = (summary?.authorizations || []).find((item) => (
      !item.consumedAt && Number(item.authorizedAmount) === Number(value)
    ))
    setReturnForm((current) => ({
      ...current,
      amount: value,
      authorizationId: authorization?.id || '',
    }))
  }

  async function handleIssueEntryVoucher(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      const payload = {
        amount: Number(returnForm.amount),
        reason: returnForm.reason.trim(),
        returnedOn: returnForm.returnedOn,
        fundOrigin: returnForm.fundOrigin || null,
        ...(needsAuthorization ? { authorizationId: returnForm.authorizationId } : {}),
      }
      await issueEntryVoucher(request.id, payload)
      setReturnFormOpen(false)
      setReturnForm((current) => ({ ...current, reason: '', authorizationId: '' }))
      setSuccess('Le bon d’entrée a été émis. Il doit recevoir les trois visas avant la clôture.')
      await loadRegularization()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleAuthorizePartialReturn(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      await authorizePartialReturn(request.id, {
        amount: Number(authorizationForm.amount),
        reason: authorizationForm.reason.trim(),
      })
      setAuthorizationFormOpen(false)
      setAuthorizationForm({ amount: '', reason: '' })
      setSuccess('L’autorisation préalable du DG a été enregistrée pour ce montant.')
      await loadRegularization()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function openVisaForm(voucher, type) {
    setError('')
    setSignatureDataUrl('')
    setSignatureAvailable(null)
    setVisaForm({ entryVoucherId: voucher.id, type, password: '' })
    try {
      await getOwnSignature()
      setSignatureAvailable(true)
    } catch (signatureError) {
      if (signatureError.status === 404) {
        setSignatureAvailable(false)
      } else {
        setError(signatureError.message)
      }
    }
  }

  function handleSignatureFile(event) {
    const file = event.target.files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/') || file.size > 2 * 1024 * 1024) {
      setError('La signature doit être une image de 2 Mo maximum.')
      event.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setSignatureDataUrl(reader.result)
        setSignatureAvailable(true)
        setError('')
      }
    }
    reader.onerror = () => setError('Impossible de lire le fichier de signature.')
    reader.readAsDataURL(file)
  }

  async function handleSignEntryVoucher(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      if (signatureDataUrl) {
        await saveOwnSignature(signatureDataUrl)
      } else if (!signatureAvailable) {
        throw new Error('Choisissez votre signature avant de continuer.')
      }
      await reauthenticate(visaForm.password)
      await signEntryVoucher(request.id, visaForm.entryVoucherId, visaForm.type)
      setVisaForm(null)
      setSignatureDataUrl('')
      setSuccess(`Le visa « ${visaLabels[visaForm.type]} » a été enregistré.`)
      await loadRegularization()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleCloseRegularization() {
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      await closeRegularization(request.id)
      setSuccess('La régularisation a été clôturée.')
      onRequestUpdated({ ...request, status: 'REGULARISEE' })
      onClose()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setIsSaving(false)
    }
  }

  function canSign(voucher, type) {
    const visas = voucher.visas || []
    if (visas.some((visa) => visa.type === type || visa.signerId === currentUser.id)) return false
    if (type === 'BENEFICIAIRE') {
      return isBeneficiary
        && visas.some((visa) => visa.type === 'DIRECTION')
        && visas.some((visa) => visa.type === 'CAISSE')
    }
    if (type === 'DIRECTION') {
      return !isBeneficiary && (
        request.validatorN2Id === currentUser.id
        || (request.validatorN2Id == null
          && request.validatorN1Id === currentUser.id
          && currentUser.role === 'DIRECTEUR_GENERAL')
      )
    }
    return !isBeneficiary && isAccountant
  }

  if (isLoading) {
    return <section className="regularization-panel content-page"><p role="status">Chargement de la régularisation&</p></section>
  }

  if (!summary || !sourceVoucher) {
    return (
      <section className="regularization-panel content-page">
        <header className="regularization-heading">
          <div><p className="eyebrow">CAISSE</p><h1>Régularisation</h1></div>
          <button type="button" onClick={onClose}>Retour aux demandes</button>
        </header>
        {error && <p className="regularization-message is-error" role="alert">{error}</p>}
        <button type="button" onClick={loadRegularization}>Ressayer</button>
      </section>
    )
  }

  return (
    <section className="regularization-panel content-page">
      <header className="regularization-heading">
        <div>
          <p className="eyebrow">CAISSE · APRÈS MISSION</p>
          <h1>Régularisation de la mission</h1>
          <p>{request.subject} · {request.requesterName} · bon de sortie n° {summary.sourceVoucherNumber}</p>
        </div>
        <div className="regularization-heading-actions">
          <button type="button" onClick={loadRegularization}>Actualiser</button>
          <button type="button" onClick={onClose}>Retour aux demandes</button>
        </div>
      </header>

      {error && <p className="regularization-message is-error" role="alert">{error}</p>}
      {success && <p className="regularization-message is-success" role="status">{success}</p>}

      <section className="regularization-summary" aria-label="Résumé financier de la mission">
        <div><span>Montant décaissé</span><strong>{formatAmount(summary.disbursedAmount)}</strong></div>
        <div><span>Justificatifs de dépenses acceptés</span><strong>{formatAmount(summary.acceptedDocumentAmount)}</strong></div>
        <div><span>Retours enregistrés</span><strong>{formatAmount(summary.returnedAmount)}</strong></div>
        <div className={balance > 0 ? 'has-balance' : 'is-balanced'}>
          <span>Solde à régulariser</span><strong>{formatAmount(summary.outstandingBalance)}</strong>
        </div>
      </section>

      {balance < 0 && (
        <p className="regularization-message is-error" role="alert">
          Le solde est négatif. La décision de prise en charge d’un dépassement dépend de la matrice PAR-13, encore à définir.
        </p>
      )}

      <section className="regularization-section regularization-documents-section">
        <div className="regularization-section-heading">
          <div>
            <h2>Justificatifs de dépenses</h2>
            <p>{documents.length === 0
              ? 'Aucune pièce déposée pour cette mission.'
              : `${documents.length} pièce${documents.length > 1 ? 's' : ''}  ${documents.filter((document) => document.status === 'A_CONTROLER').length} à contrôler`}</p>
          </div>
          <button
            type="button"
            onClick={() => onNavigate?.('supporting-documents', { requestId: request.id })}
          >
            Ouvrir les justificatifs de dépenses
          </button>
        </div>

        {documents.length > 0 && (
          <ul className="regularization-document-list">
            {documents.map((document) => (
              <li key={document.id}>
                <div>
                  <strong>{document.documentType}  {document.fileName}</strong>
                  <span>{formatAmount(document.amount)}  pièce du {formatDate(document.documentDate)}</span>
                  {document.rejectionReason && <span className="regularization-document-rejection">{document.rejectionReason}</span>}
                </div>
                <a
                  href={`/api/cash-requests/${request.id}/supporting-documents/${document.id}/file`}
                  target="_blank"
                  rel="noreferrer"
                >
                  Consulter
                </a>
                <span className={`regularization-document-status status-${document.status.toLowerCase()}`}>
                  {supportingDocumentStatusLabels[document.status] || document.status}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="regularization-section">
        <div className="regularization-section-heading">
          <div><h2>Bons d’entrée</h2><p>Chaque retour est rattaché au bon de sortie d’origine et suit ses trois visas.</p></div>
          {canAccount && balance > 0 && !returnFormOpen && (
            <button type="button" onClick={() => {
              setReturnForm({
                amount: String(balance),
                reason: '',
                returnedOn: new Date().toISOString().slice(0, 10),
                fundOrigin: sourceVoucher.fundOrigin || '',
                authorizationId: '',
              })
              setError('')
              setReturnFormOpen(true)
            }}>Émettre un bon d’entrée</button>
          )}
        </div>

        {returnFormOpen && (
          <form className="regularization-form" onSubmit={handleIssueEntryVoucher}>
            <div className="regularization-form-grid">
              <label>
                Montant retourn (FCFA)
                <input
                  type="number"
                  required
                  min="1"
                  max={balance}
                  step="1"
                  value={returnForm.amount}
                  onChange={(event) => handleReturnAmountChange(event.target.value)}
                />
              </label>
              <label>
                Date du retour
                <input
                  type="date"
                  required
                  min={sourceVoucher.disbursedOn || undefined}
                  max={new Date().toISOString().slice(0, 10)}
                  value={returnForm.returnedOn}
                  onChange={(event) => setReturnForm((current) => ({ ...current, returnedOn: event.target.value }))}
                />
              </label>
              <label>
                Origine à recréditer
                <select
                  required
                  value={returnForm.fundOrigin}
                  onChange={(event) => setReturnForm((current) => ({ ...current, fundOrigin: event.target.value }))}
                >
                  <option value="">Choisir une origine</option>
                  {Object.entries(fundOriginLabels).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </label>
              <label className="regularization-form-wide">
                Motif du retour
                <textarea
                  required
                  maxLength="1000"
                  rows="2"
                  value={returnForm.reason}
                  onChange={(event) => setReturnForm((current) => ({ ...current, reason: event.target.value }))}
                />
              </label>
              {needsAuthorization && (
                <label className="regularization-form-wide">
                  Autorisation préalable du DG
                  <select
                    required
                    value={returnForm.authorizationId}
                    onChange={(event) => setReturnForm((current) => ({ ...current, authorizationId: event.target.value }))}
                  >
                    <option value="">Choisir une autorisation correspondant au montant</option>
                    {availableAuthorizations.map((authorization) => (
                      <option key={authorization.id} value={authorization.id}>
                        {formatAmount(authorization.authorizedAmount)}  {authorization.authorizedByName}  {formatTimestamp(authorization.authorizedAt)}
                      </option>
                    ))}
                  </select>
                  {availableAuthorizations.length === 0 && (
                    <small>Le DG doit dabord autoriser ce montant. Une autorisation ne peut servir quune fois.</small>
                  )}
                </label>
              )}
            </div>
            <div className="regularization-actions">
              <button type="submit" disabled={isSaving || (needsAuthorization && !returnForm.authorizationId)}>
                {isSaving ? 'Enregistrement…' : 'Émettre le bon d’entrée'}
              </button>
              <button type="button" disabled={isSaving} onClick={() => setReturnFormOpen(false)}>Annuler</button>
            </div>
          </form>
        )}

        {(summary.entryVouchers || []).length === 0 ? (
          <p className="regularization-empty">Aucun retour de fonds enregistré.</p>
        ) : (
          <div className="regularization-entry-list">
            {summary.entryVouchers.map((voucher) => {
              const visas = voucher.visas || []
              const canPrint = visas.length === 3
              return (
                <article className="regularization-entry-card" key={voucher.id}>
                  <div className="regularization-entry-heading">
                    <div><h3>Bon d’entrée n° {voucher.voucherNumber}</h3><p>Bon de sortie d’origine n° {voucher.sourceVoucherNumber}</p></div>
                    <span className={canPrint ? 'entry-status is-complete' : 'entry-status'}>
                      {canPrint ? 'Trois visas posés' : `${visas.length}/3 visas`}
                    </span>
                  </div>
                  <dl className="regularization-entry-details">
                    <div><dt>Bénéficiaire</dt><dd>{voucher.beneficiaryName}</dd></div>
                    <div><dt>Montant</dt><dd>{formatAmount(voucher.amount)}</dd></div>
                    <div><dt>Date du retour</dt><dd>{formatDate(voucher.returnedOn)}</dd></div>
                    <div><dt>Origine à recréditer</dt><dd>{fundOriginLabels[voucher.fundOrigin] || '—'}</dd></div>
                    <div className="entry-reason"><dt>Motif</dt><dd>{voucher.reason}</dd></div>
                    <div className="entry-reason"><dt>Montant en lettres</dt><dd>{voucher.amountInWords}</dd></div>
                  </dl>

                  <div className="regularization-visas">
                    {Object.entries(visaLabels).map(([type, label]) => {
                      const visa = visas.find((entry) => entry.type === type)
                      return (
                        <div className={visa ? 'regularization-visa is-signed' : 'regularization-visa'} key={type}>
                          <strong>{label}</strong>
                          {visa ? (
                            <>
                              <img src={visa.signatureDataUrl} alt={`Signature de ${visa.signerName}`} />
                              <span>{visa.signerName}</span>
                              <time dateTime={visa.signedAt}>{formatTimestamp(visa.signedAt)}</time>
                            </>
                          ) : (
                            <>
                              <span>En attente</span>
                              {canSign(voucher, type) && (
                                <button type="button" onClick={() => openVisaForm(voucher, type)}>Apposer mon visa</button>
                              )}
                            </>
                          )}
                        </div>
                      )
                    })}
                  </div>

                  {visaForm?.entryVoucherId === voucher.id && (
                    <form className="regularization-form regularization-sign-form" onSubmit={handleSignEntryVoucher}>
                      <p>Confirmez votre identité avant d’apposer le visa « {visaLabels[visaForm.type]} ».</p>
                      {signatureAvailable === false && <p>Chargez votre signature personnelle pour pouvoir viser ce bon.</p>}
                      {signatureAvailable === true && !signatureDataUrl && <p>Votre signature enregistrée sera copiée sur le bon.</p>}
                      <label>
                        Signature personnelle (image, 2 Mo maximum)
                        <input accept="image/*" type="file" onChange={handleSignatureFile} />
                      </label>
                      {signatureDataUrl && <img className="regularization-signature-preview" src={signatureDataUrl} alt="Aperçu de la signature" />}
                      <label>
                        Mot de passe pour confirmer votre identité
                        <input
                          type="password"
                          autoComplete="current-password"
                          required
                          value={visaForm.password}
                          onChange={(event) => setVisaForm((current) => ({ ...current, password: event.target.value }))}
                        />
                      </label>
                      <div className="regularization-actions">
                        <button type="submit" disabled={isSaving || signatureAvailable === null}>
                          {isSaving ? 'Visa&' : 'Confirmer le visa'}
                        </button>
                        <button type="button" disabled={isSaving} onClick={() => setVisaForm(null)}>Annuler</button>
                      </div>
                    </form>
                  )}

                  {canPrint && (
                    <button
                      className="regularization-print-button"
                      type="button"
                      onClick={() => {
                        setPrintingEntryId(voucher.id)
                        requestAnimationFrame(() => window.print())
                      }}
                    >Imprimer le bon</button>
                  )}

                  <div className={`regularization-entry-paper ${printingEntryId === voucher.id ? 'is-printing' : ''}`}>
                    <header><strong>DIGIN FACTORY</strong><h2>BON DE CAISSE  ENTRE</h2><b>{voucher.voucherNumber}</b></header>
                    <p><span>Date du retour</span><strong>{formatDate(voucher.returnedOn)}</strong></p>
                    <p><span>Bénéficiaire</span><strong>{voucher.beneficiaryName}</strong></p>
                    <p><span>Bon d’origine</span><strong>{voucher.sourceVoucherNumber}</strong></p>
                    <p><span>Montant</span><strong>{formatAmount(voucher.amount)}</strong></p>
                    <p><span>Montant en lettres</span><strong>{voucher.amountInWords}</strong></p>
                    <p><span>Motif</span><strong>{voucher.reason}</strong></p>
                    <p><span>Origine à recréditer</span><strong>{fundOriginLabels[voucher.fundOrigin] || '—'}</strong></p>
                    <div className="regularization-paper-visas">
                      {Object.entries(visaLabels).map(([type, label]) => {
                        const visa = visas.find((entry) => entry.type === type)
                        return <section key={type}><strong>{label}</strong>{visa && <><img src={visa.signatureDataUrl} alt="" /><span>{visa.signerName}</span><time>{formatTimestamp(visa.signedAt)}</time></>}</section>
                      })}
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>

      {canAuthorizePartialReturn && (
        <section className="regularization-section">
          <div className="regularization-section-heading">
            <div><h2>Autorisation de retour ultérieur</h2><p>Une autorisation du DG est requise avant tout retour après le premier.</p></div>
            {!authorizationFormOpen && <button type="button" onClick={() => {
              setAuthorizationForm({ amount: String(balance), reason: '' })
              setAuthorizationFormOpen(true)
            }}>Autoriser un montant</button>}
          </div>
          {authorizationFormOpen && (
            <form className="regularization-form" onSubmit={handleAuthorizePartialReturn}>
              <div className="regularization-form-grid">
                <label>
                  Montant autoris (FCFA)
                  <input
                    type="number"
                    required
                    min="1"
                    max={balance}
                    step="1"
                    value={authorizationForm.amount}
                    onChange={(event) => setAuthorizationForm((current) => ({ ...current, amount: event.target.value }))}
                  />
                </label>
                <label className="regularization-form-wide">
                  Motif de lautorisation
                  <textarea
                    required
                    maxLength="1000"
                    rows="2"
                    value={authorizationForm.reason}
                    onChange={(event) => setAuthorizationForm((current) => ({ ...current, reason: event.target.value }))}
                  />
                </label>
              </div>
              <div className="regularization-actions">
                <button type="submit" disabled={isSaving}>{isSaving ? 'Enregistrement…' : 'Enregistrer l’autorisation'}</button>
                <button type="button" disabled={isSaving} onClick={() => setAuthorizationFormOpen(false)}>Annuler</button>
              </div>
            </form>
          )}
          {(summary.authorizations || []).length > 0 && (
            <ul className="regularization-authorization-list">
              {summary.authorizations.map((authorization) => (
                <li key={authorization.id}>
                  <strong>{formatAmount(authorization.authorizedAmount)}</strong>
                  <span>{authorization.reason}</span>
                  <span>Autorisé par {authorization.authorizedByName} · {formatTimestamp(authorization.authorizedAt)}</span>
                  <em>{authorization.consumedAt ? 'Utilisée' : 'Disponible'}</em>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {canAccount && (
        <section className="regularization-close-card">
          <div>
            <h2>Clôture</h2>
            <p>
              {balance !== 0
                ? 'Le solde doit êêtre nul avant la clôture.'
                : hasUnreviewedDocuments
                  ? 'Des justificatifs de dépenses attendent encore le contrôle comptable.'
                  : hasUnfinishedEntryVoucher
                    ? 'Chaque bon d’entrée doit recevoir ses trois visas.'
                    : 'Les justificatifs de dépenses sont contrôlés et le solde est nul.'}
            </p>
          </div>
          <button type="button" disabled={!canClose || isSaving} onClick={handleCloseRegularization}>
            {isSaving ? 'Clôture&' : 'Clôturer la régularisation'}
          </button>
        </section>
      )}

      <button className="regularization-back-link" type="button" onClick={onClose}>Retour aux demandes</button>
    </section>
  )
}

export default RegularizationPanel


