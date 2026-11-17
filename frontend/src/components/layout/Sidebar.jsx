import { getNavigationGroupsForRole } from '../../app/navigation'
import Icon from '../icons/Icon'
import './Sidebar.css'

function Sidebar({ activePage, currentUser, homePageId, onNavigate }) {
  const visibleGroups = getNavigationGroupsForRole(currentUser.role)
  return (
    <aside className="sidebar">
      {/* Le bouton de marque ramène toujours à la page d'accueil. */}
      <button
        className="brand"
        type="button"
        onClick={() => onNavigate(homePageId)}
        aria-label={currentUser.role === 'ADMINISTRATEUR' ? 'Retour aux comptes' : 'Retour à la vue d’ensemble'}
      >
        <img
          className="brand-logo"
          src="https://www.diginfactory.com/logo-white.png"
          alt="DIGIN FACTORY"
        />
        <span className="brand-copy">
          <span>Gestion interne</span>
        </span>
      </button>

      <nav className="main-nav" aria-label="Navigation principale">
        {visibleGroups.map((group) => (
          <div className="nav-group" key={group.label}>
            <p className="nav-group-title">{group.label}</p>
            {group.items.map((item) => (
                <button
                  className={`nav-link ${activePage === item.id ? 'active' : ''}`}
                  key={item.id}
                  type="button"
                  aria-current={activePage === item.id ? 'page' : undefined}
                  onClick={() => onNavigate(item.id)}
                >
                  <Icon name={item.icon} size={19} />
                  <span>{item.label}</span>
                </button>
              ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-bottom">
        {/* La note rappelle que le menu et les actions sont adaptés au profil connecté. */}
        <div className="sidebar-note">
          <span className="note-icon"><Icon name="shield" size={18} /></span>
          <p>
            <strong>Accès selon votre profil</strong>
            <span>
              Les pages et les actions disponibles suivent les droits de votre compte.
            </span>
          </p>
        </div>
      </div>
    </aside>
  )
}

export default Sidebar
