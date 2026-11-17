import { useEffect, useState } from 'react'
import { getDemoAccounts, login } from './authApi'
import './LoginPage.css'

const roleLabels = {
  ADMINISTRATEUR: 'Administrateur',
  DIRECTEUR_GENERAL: 'Direction générale',
  COMPTABLE: 'Comptable',
  ADJOINT_COMPTABLE: 'Adjoint comptable',
}

function LoginPage({ onLogin }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [demoAccounts, setDemoAccounts] = useState([])

  const canShowDemoAccounts = import.meta.env.DEV
    && import.meta.env.VITE_DEMO_ACCOUNTS_ENABLED
    && import.meta.env.VITE_DEMO_ACCOUNT_PASSWORD

  // Le client API ajoute le jeton CSRF et conserve la session du serveur.
  useEffect(() => {
    if (!canShowDemoAccounts) return

    let isMounted = true
    getDemoAccounts()
      .then((accounts) => {
        if (isMounted) setDemoAccounts(accounts.filter((account) => account.status === 'ACTIVE'))
      })
      .catch((requestError) => {
        if (isMounted) setError(`Comptes démo indisponibles : ${requestError.message}`)
      })

    return () => {
      isMounted = false
    }
  }, [canShowDemoAccounts])

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      onLogin(await login({ email, password }))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDemoLogin(account) {
    setError('')
    setIsSubmitting(true)

    try {
      onLogin(await login({
        email: account.email,
        password: import.meta.env.VITE_DEMO_ACCOUNT_PASSWORD,
      }))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-brand-panel" aria-label="Digin Factory">
        <div className="login-brand-lockup">
          <img
            className="login-brand-logo"
            src="https://www.diginfactory.com/logo-white.png"
            alt="DIGIN FACTORY"
          />
        </div>

        <div className="login-brand-message">
          <p className="login-brand-eyebrow">Votre espace de gestion</p>
          <h2>Vos opérations, réunies au même endroit.</h2>
          <p>Suivez vos demandes, vos contrats et votre activité en toute simplicité.</p>
        </div>

        <p className="login-brand-footer">Gestion interne <span aria-hidden="true">·</span> Digin Factory</p>
      </section>

      <section className="login-content" aria-labelledby="login-title">
        <form className="login-card" onSubmit={handleSubmit}>
          <div className="login-heading">
            <p className="eyebrow">Accès au compte</p>
            <h1 id="login-title">Connexion</h1>
            <p className="login-description">Connectez-vous avec l’adresse e-mail et le mot de passe fournis par l’administrateur.</p>
          </div>

          {error && <p className="login-error" role="alert">{error}</p>}

          <label className="login-field">
            <span>Adresse e-mail</span>
            <input
              autoComplete="username"
              autoFocus
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </label>

          <label className="login-field">
            <span>Mot de passe</span>
            <input
              autoComplete="current-password"
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </label>

          <button className="login-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Connexion…' : 'Se connecter'}
          </button>

          {demoAccounts.length > 0 && (
            <section className="demo-account-picker" aria-label="Comptes de démonstration">
              <h2>Connexion rapide pour la démo</h2>
              <p>Choisissez un profil pour vous connecter avec le mot de passe commun ci-dessous.</p>
              <div className="demo-account-list">
                {demoAccounts.map((account) => (
                  <button
                    className="demo-account-option"
                    disabled={isSubmitting}
                    key={account.id}
                    onClick={() => handleDemoLogin(account)}
                    type="button"
                  >
                    <span className="demo-account-avatar">
                      {account.firstName.slice(0, 1)}{account.lastName.slice(0, 1)}
                    </span>
                    <span className="demo-account-details">
                      <strong>{account.firstName} {account.lastName}</strong>
                      <span>{roleLabels[account.role] || account.jobTitle || 'Employé — aucun rôle'}</span>
                      <small>{account.email}</small>
                    </span>
                    <span className="demo-account-arrow" aria-hidden="true">›</span>
                  </button>
                ))}
              </div>
              <div className="demo-shared-password">
                <span>Mot de passe commun à tous les comptes démo</span>
                <code>{import.meta.env.VITE_DEMO_ACCOUNT_PASSWORD}</code>
              </div>
            </section>
          )}
        </form>
      </section>
    </main>
  )
}

export default LoginPage
