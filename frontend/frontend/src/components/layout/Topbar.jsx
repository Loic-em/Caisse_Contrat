import { useState } from 'react'
import './Topbar.css'

function Topbar({ pageTitle, user, onLogout }) {
  const [logoutError, setLogoutError] = useState('')

  async function handleLogout() {
    setLogoutError('')
    try {
      await onLogout()
    } catch (error) {
      setLogoutError(error.message)
    }
  }

  return (
    <header className="topbar">
      {/* Le titre et le profil suivent la session que l'application a vérifiée au démarrage. */}
      <div className="breadcrumbs">
        <span>Gestion interne</span>
        <span className="breadcrumb-separator">/</span>
        <strong>{pageTitle}</strong>
      </div>
      <div className="topbar-actions">
        <span className="profile-button">
          <span className="profile-avatar">{user.firstName.slice(0, 1)}</span>
          <span>{user.firstName} {user.lastName}</span>
        </span>
        <button className="logout-button" type="button" onClick={handleLogout}>Déconnexion</button>
      </div>
      {logoutError && <span className="logout-error" role="alert">{logoutError}</span>}
    </header>
  )
}

export default Topbar
