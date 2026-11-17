import { useEffect, useMemo, useState } from 'react'
import {
  createContract,
  getContract,
  getContractDocumentAvailability,
  getContractDocuments,
  getContractEmployees,
  getContractHistory,
  getContracts,
  getContractSupervisors,
  updateContract,
  uploadContractDocument,
} from './contractsApi'
import PageBackButton from '../../components/layout/PageBackButton'
import './ContractsPage.css'

const typeLabels = { STAGE: 'Stage', CDD: 'CDD', CDI: 'CDI' }
const statusLabels = {
  BROUILLON: 'Brouillon',
  ACTIF: 'Actif',
  A_RENOUVELER: 'À renouveler',
  TERMINE: 'Termin',
  ROMPU: 'Rompu',
}
const dateFormatter = new Intl.DateTimeFormat('fr-FR')

function formatDate(value) {
  return value ? dateFormatter.format(new Date(`${value}T00:00:00`)) : ''
}

function emptyForm() {
  return {
    employeeId: '',
    type: 'CDD',
    startDate: '',
    endDate: '',
    jobTitle: '',
    department: '',
    supervisorId: '',
    probationPeriod: '',
    workplace: '',
    baseSalaryAmount: '',
    monthlyBonusAmount: '',
    bonusDetails: '',
    otherBenefits: '',
    internalNotes: '',
  }
}

function canEditContract(user, contract) {
  if (!contract) return false
  if (user.role === 'COMPTABLE') return contract.employeeId !== user.id
  return user.role === 'DIRECTEUR_GENERAL'
    && ['COMPTABLE', 'ADJOINT_COMPTABLE'].includes(contract.employeeRole)
}

function ContractsPage({ currentUser, onNavigate }) {
  const canManage = ['COMPTABLE', 'DIRECTEUR_GENERAL'].includes(currentUser.role)
  const [contracts, setContracts] = useState([])
  const [employees, setEmployees] = useState([])
  const [supervisors, setSupervisors] = useState([])
  const [selectedContract, setSelectedContract] = useState(null)
  const [documents, setDocuments] = useState([])
  const [history, setHistory] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [isEditing, setIsEditing] = useState(false)
  const [isCreating, setIsCreating] = useState(false)
  const [uploadsEnabled, setUploadsEnabled] = useState(false)
  const [filter, setFilter] = useState({ query: '', type: '', status: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const canViewAllContracts = canManage
  const employeesWithoutContract = useMemo(
    () => employees.filter((employee) => !contracts.some((contract) => contract.employeeId === employee.id)),
    [contracts, employees],
  )

  async function loadPage() {
    setError('')
    setIsLoading(true)
    try {
      const results = await Promise.all([
        getContracts(),
        canManage ? getContractEmployees() : Promise.resolve([]),
        canManage ? getContractSupervisors() : Promise.resolve([]),
        canManage ? getContractDocumentAvailability() : Promise.resolve({ uploadsEnabled: false }),
      ])
      setContracts(results[0])
      setEmployees(results[1])
      setSupervisors(results[2])
      setUploadsEnabled(results[3].uploadsEnabled)
      setSelectedContract((current) => results[0].find((item) => item.id === current?.id) || null)
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    let isMounted = true
    Promise.all([
      getContracts(),
      canManage ? getContractEmployees() : Promise.resolve([]),
      canManage ? getContractSupervisors() : Promise.resolve([]),
      canManage ? getContractDocumentAvailability() : Promise.resolve({ uploadsEnabled: false }),
    ])
      .then(([loadedContracts, loadedEmployees, loadedSupervisors, availability]) => {
        if (!isMounted) return
        setContracts(loadedContracts)
        setEmployees(loadedEmployees)
        setSupervisors(loadedSupervisors)
        setUploadsEnabled(availability.uploadsEnabled)
        if (!canManage && loadedContracts.length > 0
          && loadedContracts.every((contract) => contract.employeeId === currentUser.id)) {
          selectContract(loadedContracts[0])
        }
      })
      .catch((loadError) => {
        if (isMounted) setError(loadError.message)
      })
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })
    return () => { isMounted = false }
  }, [canManage])

  async function selectContract(contract) {
    setSelectedContract(contract)
    setIsCreating(false)
    setIsEditing(false)
    setError('')
    try {
      const [details, contractDocuments, contractHistory] = await Promise.all([
        getContract(contract.id),
        getContractDocuments(contract.id),
        getContractHistory(contract.id),
      ])
      setSelectedContract(details || contract)
      setDocuments(contractDocuments)
      setHistory(contractHistory)
    } catch (loadError) {
      setError(loadError.message)
    }
  }

  function beginCreate(employeeId = '') {
    const employee = employees.find((item) => item.id === employeeId)
    setSelectedContract(null)
    setDocuments([])
    setHistory([])
    setForm({
      ...emptyForm(),
      employeeId: employee?.id || '',
      department: employee?.department || '',
      jobTitle: employee?.jobTitle || '',
      supervisorId: employee?.managerN1Id || '',
    })
    setIsCreating(true)
    setIsEditing(false)
    setError('')
    setSuccess('')
  }

  function beginEdit() {
    if (!selectedContract) return
    setForm({
      employeeId: selectedContract.employeeId,
      type: selectedContract.type,
      startDate: selectedContract.startDate,
      endDate: selectedContract.endDate || '',
      jobTitle: selectedContract.jobTitle,
      department: selectedContract.department,
      supervisorId: selectedContract.supervisorId,
      probationPeriod: selectedContract.probationPeriod || '',
      workplace: selectedContract.workplace || '',
      baseSalaryAmount: selectedContract.baseSalaryAmount?.toString() || '',
      monthlyBonusAmount: selectedContract.monthlyBonusAmount?.toString() || '',
      bonusDetails: selectedContract.bonusDetails || '',
      otherBenefits: selectedContract.otherBenefits || '',
      internalNotes: selectedContract.internalNotes || '',
    })
    setIsEditing(true)
    setIsCreating(false)
  }

  function updateForm(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function chooseEmployee(employeeId) {
    const employee = employees.find((item) => item.id === employeeId)
    setForm((current) => ({
      ...current,
      employeeId,
      department: employee?.department || current.department,
      jobTitle: employee?.jobTitle || current.jobTitle,
      supervisorId: employee?.managerN1Id || '',
    }))
  }

  async function submitContract(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setSuccess('')
    const payload = {
      ...form,
      endDate: form.type === 'CDI' ? null : form.endDate || null,
      baseSalaryAmount: form.baseSalaryAmount === '' ? null : Number(form.baseSalaryAmount),
      monthlyBonusAmount: form.monthlyBonusAmount === '' ? null : Number(form.monthlyBonusAmount),
      supervisorId: form.supervisorId || null,
      bonusDetails: form.bonusDetails || null,
      otherBenefits: form.otherBenefits || null,
      internalNotes: form.internalNotes || null,
    }
    try {
      const saved = isCreating
        ? await createContract(payload)
        : await updateContract(selectedContract.id, payload)
      await loadPage()
      setSelectedContract(saved)
      setIsCreating(false)
      setIsEditing(false)
      setSuccess(isCreating ? 'Le contrat brouillon a été créé.' : 'Le contrat a été mis à jour.')
    } catch (saveError) {
      setError(saveError.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function submitDocument(event) {
    event.preventDefault()
    const formElement = event.currentTarget
    const file = formElement.elements.namedItem('contractDocument').files?.[0]
    if (!file || !selectedContract) return
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      await uploadContractDocument(selectedContract.id, file)
      await selectContract(selectedContract)
      await loadPage()
      setSuccess('Le PDF sign a t ajout comme nouvelle version.')
      formElement?.reset?.()
    } catch (uploadError) {
      setError(uploadError.message)
    } finally {
      setIsSaving(false)
    }
  }

  const visibleContracts = useMemo(() => contracts.filter((contract) => {
    const query = filter.query.trim().toLocaleLowerCase('fr-FR')
    return (!query || `${contract.employeeName} ${contract.jobTitle} ${contract.department}`.toLocaleLowerCase('fr-FR').includes(query))
      && (!filter.type || contract.type === filter.type)
      && (!filter.status || contract.status === filter.status)
  }), [contracts, filter])

  if (isLoading) {
    return <section className="content-page"><p role="status">Chargement des contrats&</p></section>
  }

  return (
    <section className="content-page contracts-page">
      <header className="contracts-heading">
        <div>
          <p className="eyebrow">RESSOURCES HUMAINES</p>
          <h1>Contrats de travail</h1>
          <p>{canManage
            ? 'Consultez et grez les contrats de travail de lquipe.'
            : canViewAllContracts
              ? 'Consultez les contrats des collaborateurs dont vous tes responsable.'
              : 'Votre contrat de travail et ses documents signés sont regroupés ici.'}</p>
        </div>
        <div className="contracts-heading-actions">
          <PageBackButton onClick={() => onNavigate?.(currentUser.role === 'ADMINISTRATEUR' ? 'team' : 'overview')}>
            Retour
          </PageBackButton>
          <button type="button" className="contracts-secondary-button" onClick={loadPage}>Actualiser</button>
          {canManage && <button type="button" onClick={beginCreate}>Nouveau contrat</button>}
        </div>
      </header>

      {!uploadsEnabled && canManage && (
        <p className="contracts-warning" role="status">
          Le dépôt des PDF est bloqué tant que ClamAV n'est pas opérationnel ou que la clé de chiffrement des documents n'est pas configurée. Les documents déjà déposés restent consultables.
        </p>
      )}
      {error && <p className="contracts-message is-error" role="alert">{error}</p>}
      {success && <p className="contracts-message is-success" role="status">{success}</p>}

      {canManage && (
        <section className="contracts-missing-panel" aria-labelledby="contracts-missing-title">
          <header className="contracts-missing-heading">
            <div>
              <h2 id="contracts-missing-title">Comptes sans contrat</h2>
              <p>Sélectionnez un compte actif pour créer son contrat de travail.</p>
            </div>
            <span>{employeesWithoutContract.length} à traiter</span>
          </header>
          {employeesWithoutContract.length === 0 ? (
            <p className="contracts-empty">Tous les comptes actifs accessibles depuis votre profil ont déjà un contrat.</p>
          ) : (
            <ul className="contracts-missing-list">
              {employeesWithoutContract.map((employee) => (
                <li key={employee.id}>
                  <div>
                    <strong>{employee.firstName} {employee.lastName}</strong>
                    <span>{employee.jobTitle || 'Poste non renseign'}  {employee.department || 'Service non renseign'}</span>
                    <small>Identifiant : {employee.employeeId}</small>
                  </div>
                  <button type="button" onClick={() => beginCreate(employee.id)} disabled={isSaving}>
                    Créer le contrat
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {(isCreating || isEditing) && (
        <form className="contract-form" onSubmit={submitContract}>
          <div className="contract-form-heading">
            <h2>{isCreating ? 'Créer un contrat brouillon' : 'Modifier le contrat'}</h2>
            <button type="button" className="contracts-secondary-button" onClick={() => { setIsCreating(false); setIsEditing(false) }}>
              Retour  la liste
            </button>
          </div>
          {isCreating ? (
            <label>
              Employ
              <select required value={form.employeeId} onChange={(event) => chooseEmployee(event.target.value)}>
                <option value="">Sélectionner un employé</option>
                {employees.map((employee) => (
                  <option key={employee.id} value={employee.id}>
                    {employee.firstName} {employee.lastName}  {employee.employeeId}
                  </option>
                ))}
              </select>
            </label>
          ) : <p className="contract-form-employee">{selectedContract?.employeeName}</p>}
          <label>
            Type
            <select required value={form.type} onChange={(event) => updateForm('type', event.target.value)}>
              <option value="STAGE">Stage</option><option value="CDD">CDD</option><option value="CDI">CDI</option>
            </select>
          </label>
          <label>
            Date de dbut
            <input required type="date" value={form.startDate} onChange={(event) => updateForm('startDate', event.target.value)} />
          </label>
          {form.type !== 'CDI' && (
            <label>
              Date de fin
              <input required type="date" value={form.endDate} onChange={(event) => updateForm('endDate', event.target.value)} />
            </label>
          )}
          <label>
            Poste
            <input required maxLength="160" value={form.jobTitle} onChange={(event) => updateForm('jobTitle', event.target.value)} />
          </label>
          <label>
            Service
            <input required maxLength="160" value={form.department} onChange={(event) => updateForm('department', event.target.value)} />
          </label>
          <label>
            Responsable
            <select required value={form.supervisorId} onChange={(event) => updateForm('supervisorId', event.target.value)}>
              <option value="">Sélectionner un responsable</option>
              {supervisors.filter((employee) => employee.id !== form.employeeId).map((employee) => (
                <option key={employee.id} value={employee.id}>{employee.firstName} {employee.lastName}</option>
              ))}
            </select>
          </label>
          <label>
            Période d’essai (durée et unité)
            <input maxLength="100" value={form.probationPeriod} onChange={(event) => updateForm('probationPeriod', event.target.value)} />
          </label>
          <label>
            Lieu de travail
            <input maxLength="200" value={form.workplace} onChange={(event) => updateForm('workplace', event.target.value)} />
          </label>
          <label>
            Rémunération de base mensuelle (FCFA)
            <input
              min="0"
              required
              step="1"
              type="number"
              value={form.baseSalaryAmount}
              onChange={(event) => updateForm('baseSalaryAmount', event.target.value)}
            />
          </label>
          <label>
            Total des primes fixes mensuelles (FCFA)
            <input
              min="0"
              step="1"
              type="number"
              value={form.monthlyBonusAmount}
              onChange={(event) => updateForm('monthlyBonusAmount', event.target.value)}
            />
          </label>
          <label className="contract-notes-field">
            Détail des primes fixes
            <textarea
              maxLength="2000"
              rows="2"
              value={form.bonusDetails}
              onChange={(event) => updateForm('bonusDetails', event.target.value)}
              placeholder="Indiquer les primes incluses dans le total ci-dessus"
            />
          </label>
          <label className="contract-notes-field">
            Indemnités et autres avantages
            <textarea
              maxLength="2000"
              rows="2"
              value={form.otherBenefits}
              onChange={(event) => updateForm('otherBenefits', event.target.value)}
              placeholder="Ex. transport, tlphone ou avantage en nature"
            />
          </label>
          <label className="contract-notes-field">
            Notes internes (visibles  la comptabilit et  la Direction)
            <textarea maxLength="2000" rows="3" value={form.internalNotes} onChange={(event) => updateForm('internalNotes', event.target.value)} />
          </label>
          <button type="submit" disabled={isSaving}>{isSaving ? 'Enregistrement…' : 'Enregistrer le brouillon'}</button>
        </form>
      )}

      <div className="contracts-layout">
        <div className="contracts-list-panel">
          {canViewAllContracts && <div className="contracts-filters">
            <input
              aria-label="Rechercher un contrat"
              placeholder="Rechercher un employé, un poste…"
              value={filter.query}
              onChange={(event) => setFilter((current) => ({ ...current, query: event.target.value }))}
            />
            <select aria-label="Filtrer par type" value={filter.type} onChange={(event) => setFilter((current) => ({ ...current, type: event.target.value }))}>
              <option value="">Tous les types</option>
              {Object.entries(typeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
            <select aria-label="Filtrer par statut" value={filter.status} onChange={(event) => setFilter((current) => ({ ...current, status: event.target.value }))}>
              <option value="">Tous les statuts</option>
              {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>}
          {visibleContracts.length === 0 ? (
            <p className="contracts-empty">{canViewAllContracts ? 'Aucun contrat ne correspond à ces filtres.' : 'Aucun contrat n’est rattaché à votre compte.'}</p>
          ) : (
            <ul className="contracts-list">
              {visibleContracts.map((contract) => (
                <li key={contract.id}>
                  <button
                    type="button"
                    className={selectedContract?.id === contract.id ? 'is-selected' : ''}
                    onClick={() => selectContract(contract)}
                  >
                    <strong>{contract.employeeName}</strong>
                    <span>{contract.jobTitle}  {typeLabels[contract.type]}</span>
                    <small>{formatDate(contract.startDate)}  {formatDate(contract.endDate)}</small>
                    <em>{statusLabels[contract.status]}</em>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <article className="contract-detail-panel">
          {!selectedContract ? (
            <div className="contracts-empty"><h2>Sélectionnez un contrat</h2><p>Les informations détaillées apparaîtront ici.</p></div>
          ) : (
            <>
              <header className="contract-detail-heading">
                <div>
                  <p className="eyebrow">{typeLabels[selectedContract.type]}  {statusLabels[selectedContract.status]}</p>
                  <h2>{selectedContract.employeeName}</h2>
                  <p>{selectedContract.jobTitle}  {selectedContract.department}</p>
                </div>
                <div className="contract-detail-actions">
                  <PageBackButton onClick={() => setSelectedContract(null)}>Retour  la liste</PageBackButton>
                  {canEditContract(currentUser, selectedContract) && (
                    <button type="button" className="contracts-secondary-button" onClick={beginEdit}>Modifier</button>
                  )}
                </div>
              </header>
              <dl className="contract-facts">
                <div><dt>Dbut</dt><dd>{formatDate(selectedContract.startDate)}</dd></div>
                <div><dt>Fin</dt><dd>{formatDate(selectedContract.endDate)}</dd></div>
                <div><dt>Responsable</dt><dd>{selectedContract.supervisorName}</dd></div>
                <div><dt>Période d’essai</dt><dd>{selectedContract.probationPeriod || '—'}</dd></div>
                <div><dt>Lieu de travail</dt><dd>{selectedContract.workplace || ''}</dd></div>
                <div>
                  <dt>Rémunération de base mensuelle</dt>
                  <dd>{selectedContract.baseSalaryAmount == null
                    ? 'Non renseignée'
                    : `${new Intl.NumberFormat('fr-FR').format(Number(selectedContract.baseSalaryAmount))} FCFA`}</dd>
                </div>
                <div>
                  <dt>Primes fixes mensuelles</dt>
                  <dd>{selectedContract.monthlyBonusAmount == null
                    ? 'Aucune renseignée'
                    : `${new Intl.NumberFormat('fr-FR').format(Number(selectedContract.monthlyBonusAmount))} FCFA`}</dd>
                </div>
                <div><dt>Ancienneté sur le contrat actuel</dt><dd>{formatDate(selectedContract.startDate)} → aujourd’hui</dd></div>
              </dl>
              {(selectedContract.bonusDetails || selectedContract.otherBenefits) && (
                <div className="contract-compensation-details">
                  {selectedContract.bonusDetails && <p><strong>Détail des primes :</strong> {selectedContract.bonusDetails}</p>}
                  {selectedContract.otherBenefits && <p><strong>Indemnités et autres avantages :</strong> {selectedContract.otherBenefits}</p>}
                </div>
              )}
              {selectedContract.internalNotes && (
                <div className="contract-internal-notes"><strong>Notes internes</strong><p>{selectedContract.internalNotes}</p></div>
              )}
              <section className="contract-documents-section">
                <div className="contract-section-heading"><h3>Documents signs</h3><span>{documents.length} version(s)</span></div>
                {canEditContract(currentUser, selectedContract) && (
                  <form className="contract-document-form" onSubmit={submitDocument}>
                    <label>
                      Ajouter un document sign (PDF ou photo, 10 Mo maximum)
                      <input name="contractDocument" required type="file" accepté="application/pdf,image/*,.pdf,.png,.jpg,.jpeg,.webp" disabled={!uploadsEnabled || isSaving} />
                    </label>
                    <button type="submit" disabled={!uploadsEnabled || isSaving}>Ajouter une version</button>
                  </form>
                )}
                {documents.length === 0 ? <p className="contracts-empty">Aucun document signé n'a encore été déposé.</p> : (
                  <ul className="contract-document-list">
                    {documents.map((document) => (
                      <li key={document.id}>
                        <span><strong>Version {document.version}</strong>  {document.fileName}</span>
                        <small>Ajout le {new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' }).format(new Date(document.uploadedAt))} par {document.uploadedByName}</small>
                        <a href={`/api/contracts/${selectedContract.id}/documents/${document.id}/file`} target="_blank" rel="noreferrer">Ouvrir</a>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
              <section className="contract-history-section">
                <h3>Historique</h3>
                {history.length === 0 ? <p className="contracts-empty">Aucune action enregistrée.</p> : (
                  <ul>{history.map((event, index) => (
                    <li key={`${event.action}-${event.occurredAt}-${index}`}>
                      <span>{event.action.replaceAll('_', ' ').toLocaleLowerCase('fr-FR')}</span>
                      <time>{new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(event.occurredAt))}</time>
                    </li>
                  ))}</ul>
                )}
              </section>
            </>
          )}
        </article>
      </div>
    </section>
  )
}

export default ContractsPage

