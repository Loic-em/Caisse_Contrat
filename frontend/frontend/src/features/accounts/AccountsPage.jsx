import { useEffect, useState } from 'react'
import { createAccount, getAccounts, setAccountActive, updateAccount } from './accountsApi'
import './AccountsPage.css'

const emptyForm = {
  employeeId: '',
  firstName: '',
  lastName: '',
  email: '',
  department: '',
  jobTitle: '',
  role: '',
  managerN1Id: '',
  password: '',
}

const roleLabels = {
  COMPTABLE: 'Comptable',
  ADJOINT_COMPTABLE: 'Adjoint comptable',
  DIRECTEUR_GENERAL: 'Directeur général',
  ADMINISTRATEUR: 'Administrateur',
}

function AccountsPage({ currentUser }) {
  const [accounts, setAccounts] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    getAccounts()
      .then((result) => {
        if (active) setAccounts(result)
      })
      .catch((requestError) => {
        if (active) setError(requestError.message)
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const selectedManager = accounts.find((account) => account.id === form.managerN1Id)
  const isTopLevelRole = form.role === 'DIRECTEUR_GENERAL'
    || (form.role === 'ADMINISTRATEUR' && editingId && !form.managerN1Id)
  const isEditingSelf = editingId === currentUser.id
  const activeManagers = accounts.filter(
    (account) => account.status === 'ACTIVE' && account.role !== 'ADMINISTRATEUR',
  )

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setError('')
    setSuccess('')
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyForm)
    setError('')
  }

  function editAccount(account) {
    setEditingId(account.id)
    setForm({
      employeeId: account.employeeId,
      firstName: account.firstName,
      lastName: account.lastName,
      email: account.email,
      department: account.department,
      jobTitle: account.jobTitle,
      role: account.role || '',
      managerN1Id: account.managerN1Id || '',
      password: '',
    })
    setError('')
    setSuccess('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsSaving(true)

    // L'API définit le N+2 comme le supérieur du responsable N+1 choisi.
    const payload = {
      ...form,
      role: form.role || null,
      managerN1Id: isTopLevelRole ? null : form.managerN1Id || null,
      managerN2Id: isTopLevelRole ? null : selectedManager?.managerN1Id || null,
      password: form.password || null,
    }

    try {
      const saved = editingId
        ? await updateAccount(editingId, payload)
        : await createAccount(payload)
      setAccounts((current) => editingId
        ? current.map((account) => account.id === editingId ? saved : account)
        : [saved, ...current])
      setSuccess(editingId ? 'Le compte a été mis à jour.' : 'Le compte a été créé.')
      setEditingId(null)
      setForm(emptyForm)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function toggleActive(account) {
    setError('')
    setSuccess('')
    try {
      const saved = await setAccountActive(account.id, account.status !== 'ACTIVE')
      setAccounts((current) => current.map((item) => item.id === saved.id ? saved : item))
      setSuccess(account.status === 'ACTIVE' ? 'Le compte a été désactivé.' : 'Le compte a été réactivé.')
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  return (
    <section className="content-page accounts-page">
      <div className="accounts-heading">
        <div>
          <p className="eyebrow">RESSOURCES HUMAINES</p>
          <h1>Comptes utilisateurs</h1>
          <p>Gérez les comptes, leurs rôles et leur hiérarchie. Les mots de passe sont stockés sous forme hachée.</p>
        </div>
      </div>

      {error && <p className="accounts-message accounts-error" role="alert">{error}</p>}
      {success && <p className="accounts-message accounts-success" role="status">{success}</p>}

      <form className="account-form-card" onSubmit={handleSubmit}>
        <div className="account-form-heading">
          <div>
            <h2>{editingId ? 'Modifier un compte' : 'Créer un compte'}</h2>
            <p>Le mot de passe doit comporter au moins 12 caractères.</p>
          </div>
        </div>

        <div className="account-form-grid">
          <label>
            <span>Identifiant interne</span>
            <input name="employeeId" onChange={updateField} required value={form.employeeId} />
          </label>
          <label>
            <span>Prénom</span>
            <input name="firstName" onChange={updateField} required value={form.firstName} />
          </label>
          <label>
            <span>Nom</span>
            <input name="lastName" onChange={updateField} required value={form.lastName} />
          </label>
          <label>
            <span>Adresse e-mail</span>
            <input autoComplete="email" name="email" onChange={updateField} required type="email" value={form.email} />
          </label>
          <label>
            <span>Service</span>
            <input name="department" onChange={updateField} required value={form.department} />
          </label>
          <label>
            <span>Poste</span>
            <input name="jobTitle" onChange={updateField} required value={form.jobTitle} />
          </label>
          <label>
            <span>Rôle</span>
            <select disabled={isEditingSelf} name="role" onChange={updateField} value={form.role}>
              <option value="">Aucun rôle (employé)</option>
              {Object.entries(roleLabels).map(([role, label]) => <option key={role} value={role}>{label}</option>)}
            </select>
          </label>
          {!isTopLevelRole && (
            <label>
              <span>Responsable N+1</span>
              <select
                name="managerN1Id"
                onChange={updateField}
                disabled={isEditingSelf}
                required={!isTopLevelRole}
                value={form.managerN1Id}
              >
                <option value="">Choisir un responsable actif</option>
                {activeManagers.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.firstName} {account.lastName} — {roleLabels[account.role] || account.jobTitle}
                  </option>
                ))}
              </select>
              {selectedManager && selectedManager.role !== 'DIRECTEUR_GENERAL' && (
                <small>
                  N+2 : {selectedManager.managerN1Name || 'Non défini — choisissez un autre responsable N+1'}
                </small>
              )}
            </label>
          )}
          <label className="account-password-field">
            <span>{editingId ? 'Nouveau mot de passe (facultatif)' : 'Mot de passe initial'}</span>
            <input
              autoComplete="new-password"
              minLength={12}
              name="password"
              onChange={updateField}
              required={!editingId}
              type="password"
              value={form.password}
            />
          </label>
        </div>

        <div className="account-form-actions">
          {editingId && <button className="account-secondary-button" type="button" onClick={resetForm}>Annuler</button>}
          <button className="account-primary-button" disabled={isSaving} type="submit">
            {isSaving ? 'Enregistrement…' : editingId ? 'Enregistrer les modifications' : 'Créer le compte'}
          </button>
        </div>
      </form>

      <div className="account-list-heading">
        <h2>Équipe</h2>
        <span>{accounts.length} compte{accounts.length === 1 ? '' : 's'}</span>
      </div>

      {isLoading ? (
        <div className="accounts-empty-state" role="status">Chargement des comptes…</div>
      ) : accounts.length === 0 ? (
        <div className="accounts-empty-state">Aucun compte utilisateur n’a encore été créé.</div>
      ) : (
        <div className="account-list">
          {accounts.map((account) => (
            <article className="account-card" key={account.id}>
              <div className="account-card-main">
                <span className="account-avatar">{account.firstName.slice(0, 1)}{account.lastName.slice(0, 1)}</span>
                <div>
                  <h3>{account.firstName} {account.lastName}</h3>
                  <p>{account.email} · {account.employeeId}</p>
                  <small>{account.jobTitle} · {account.department}</small>
                  {(account.managerN1Name || account.managerN2Name) && (
                    <small>Hiérarchie : {account.managerN1Name || '—'}{account.managerN2Name ? ` → ${account.managerN2Name}` : ''}</small>
                  )}
                </div>
              </div>
              <div className="account-card-actions">
                <span className={`account-status ${account.status === 'ACTIVE' ? 'account-status-active' : ''}`}>
                  {account.status === 'ACTIVE' ? 'Actif' : 'Désactivé'}
                </span>
                {account.role && <small>{roleLabels[account.role] || account.role}</small>}
                <button type="button" onClick={() => editAccount(account)}>Modifier</button>
                {account.id !== currentUser.id && (
                  <button
                    className={account.status === 'ACTIVE' ? 'account-danger-button' : ''}
                    type="button"
                    onClick={() => toggleActive(account)}
                  >
                    {account.status === 'ACTIVE' ? 'Désactiver' : 'Réactiver'}
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export default AccountsPage
