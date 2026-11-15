import { navigationGroups } from '../../app/navigation'
import Icon from '../icons/Icon'
import './Sidebar.css'

function Sidebar({ activePage, canManageAccounts, homePageId, onNavigate }) {
  return (
    <aside className="sidebar">
      {/* Le bouton de marque ramène toujours à la page d'accueil. */}
      <button
        className="brand"
        type="button"
        onClick={() => onNavigate(homePageId)}
        aria-label="Retour à la vue d’ensemble"
      >
        <span className="brand-mark">D</span>
        <span className="brand-copy">
          <strong>Digin Factory</strong>
          <span>Gestion interne</span>
        </span>
      </button>

      <div className="workspace-label">ESPACE DE TRAVAIL</div>
      <nav className="main-nav" aria-label="Navigation principale">
        {/* Les rubriques et leurs liens viennent de la configuration partagée. */}
        {navigationGroups.map((group) => {
          const visibleItems = group.items.filter((item) => (
            canManageAccounts ? item.id === 'team' : item.id !== 'team'
          ))
          if (visibleItems.length === 0) return null

          return (
            <div className="nav-group" key={group.label}>
              <p className="nav-group-title">{group.label}</p>
              {visibleItems.map((item) => (
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
          )
        })}
      </nav>

      <div className="sidebar-bottom">
        {/* Ces messages décrivent l'état prototype ; ils ne représentent pas une session réelle. */}
        <div className="sidebar-note">
          <span className="note-icon"><Icon name="shield" size={18} /></span>
          <p>
            <strong>Accès sécurisé</strong>
            <span>
              Connectez-vous avec un compte actif pour accéder aux espaces de travail.
            </span>
          </p>
        </div>
        <div className="sidebar-footer">
          <span className="footer-dot" />
          <span>Environnement de préparation</span>
        </div>
      </div>
    </aside>
  )
}

export default Sidebar
