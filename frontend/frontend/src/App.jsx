import { useEffect, useState } from 'react'
import AppLayout from './components/layout/AppLayout'
import OverviewPage from './features/dashboard/pages/OverviewPage'
import CashRequestsPage from './features/requests/pages/CashRequestsPage'
import SupportingDocumentsPage from './features/requests/pages/SupportingDocumentsPage'
import ContractsPage from './features/contracts/ContractsPage'
import PlaceholderPage from './features/workspace/pages/PlaceholderPage'
import AccountsPage from './features/accounts/AccountsPage'
import LoginPage from './features/auth/LoginPage'
import { getCurrentUser, logout } from './features/auth/authApi'
import { navigationItems, overviewPage } from './app/navigation'

function App() {
  const [user, setUser] = useState(null)
  const [isCheckingSession, setIsCheckingSession] = useState(true)
  const [sessionError, setSessionError] = useState('')
  // L'identifiant de la page active suffit pour choisir l'écran et mettre à jour le menu.
  const [activePageId, setActivePageId] = useState(overviewPage.id)
  const activePage = navigationItems.find((item) => item.id === activePageId)
  // Si une ancienne URL ou un identifiant inconnu arrive ici, on revient à l'accueil.
  const page = user?.role === 'ADMINISTRATEUR'
    ? navigationItems.find((item) => item.id === 'team')
    : activePage ?? overviewPage

  function navigateToPage(pageId) {
    setActivePageId(user?.role === 'ADMINISTRATEUR' ? 'team' : pageId)
  }

  function setAuthenticatedUser(authenticatedUser) {
    setUser(authenticatedUser)
    setActivePageId(authenticatedUser.role === 'ADMINISTRATEUR' ? 'team' : overviewPage.id)
  }

  useEffect(() => {
    // Restaure la session côté serveur après un rafraîchissement de la page.
    let isMounted = true
    getCurrentUser()
      .then((currentUser) => {
        if (isMounted) setAuthenticatedUser(currentUser)
      })
      .catch((error) => {
        if (isMounted && error.status !== 401) {
          setSessionError(error.message)
        }
      })
      .finally(() => {
        if (isMounted) setIsCheckingSession(false)
      })

    return () => {
      isMounted = false
    }
  }, [])

  async function handleLogout() {
    await logout()
    setUser(null)
    setActivePageId(overviewPage.id)
  }

  if (isCheckingSession) {
    return <main className="session-loading">Vérification de la session…</main>
  }

  if (sessionError) {
    return <main className="session-loading" role="alert">{sessionError}</main>
  }

  if (!user) {
    return <LoginPage onLogin={setAuthenticatedUser} />
  }

  // Le contenu central dépend de la page choisie ; le cadre de l'application reste commun.
  return (
    <AppLayout
      activePage={page.id}
      pageTitle={page.label}
      onNavigate={navigateToPage}
      user={user}
      onLogout={handleLogout}
    >
      {page.id === overviewPage.id ? (
        <OverviewPage currentUser={user} onNavigate={navigateToPage} />
      ) : page.id === 'requests' ? (
        <CashRequestsPage currentUser={user} />
      ) : page.id === 'receipts' ? (
        <CashRequestsPage currentUser={user} view="vouchers" />
      ) : page.id === 'supporting-documents' ? (
        <SupportingDocumentsPage currentUser={user} />
      ) : page.id === 'contracts' ? (
        <ContractsPage currentUser={user} />
      ) : page.id === 'team' && user.role === 'ADMINISTRATEUR' ? (
        <AccountsPage currentUser={user} />
      ) : (
        <PlaceholderPage page={page} onNavigate={setActivePageId} />
      )}
    </AppLayout>
  )
}

export default App
