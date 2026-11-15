import Icon from '../../../components/icons/Icon'
import './QuickAccessPanel.css'

function QuickAccessPanel({ onNavigate }) {
  return (
    <section className="panel shortcuts-panel" aria-labelledby="shortcuts-title">
      <div className="panel-heading">
        <div>
          <h2 id="shortcuts-title">Accès rapides</h2>
          <p>Explorez les espaces de l’application</p>
        </div>
      </div>
      <div className="shortcut-list">
        {/* Ces boutons changent la page dans le conteneur parent, sans rechargement. */}
        <button className="shortcut-item" type="button" onClick={() => onNavigate('requests')}>
          <span className="shortcut-icon purple"><Icon name="wallet" size={18} /></span>
          <span className="shortcut-copy">
            <strong>Demandes de caisse</strong>
            <small>Mission, validation et suivi</small>
          </span>
          <Icon name="arrow" size={17} />
        </button>
        <button className="shortcut-item" type="button" onClick={() => onNavigate('receipts')}>
          <span className="shortcut-icon purple"><Icon name="receipt" size={18} /></span>
          <span className="shortcut-copy">
            <strong>Bons de caisse</strong>
            <small>Visas et décaissements</small>
          </span>
          <Icon name="arrow" size={17} />
        </button>
        <button className="shortcut-item" type="button" onClick={() => onNavigate('contracts')}>
          <span className="shortcut-icon blue"><Icon name="briefcase" size={18} /></span>
          <span className="shortcut-copy">
            <strong>Contrats de travail</strong>
            <small>Contrats, avenants et échéances</small>
          </span>
          <Icon name="arrow" size={17} />
        </button>
      </div>
    </section>
  )
}

export default QuickAccessPanel
