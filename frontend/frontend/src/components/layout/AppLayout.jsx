import Sidebar from './Sidebar'
import Topbar from './Topbar'
import './AppLayout.css'

function AppLayout({ activePage, pageTitle, onNavigate, user, onLogout, children }) {
  // Le menu et l'en-tête sont partagés ; "children" reçoit la page sélectionnée.
  return (
    <div className="app-shell">
      <Sidebar
        activePage={activePage}
        canManageAccounts={user.role === 'ADMINISTRATEUR'}
        homePageId={user.role === 'ADMINISTRATEUR' ? 'team' : 'overview'}
        onNavigate={onNavigate}
      />
      <main className="main-area">
        <Topbar pageTitle={pageTitle} user={user} onLogout={onLogout} />
        {children}
      </main>
    </div>
  )
}

export default AppLayout
