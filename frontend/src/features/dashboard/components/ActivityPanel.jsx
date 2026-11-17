import Icon from '../../../components/icons/Icon'
import './ActivityPanel.css'

function ActivityPanel() {
  return (
    <section className="panel activity-panel" aria-labelledby="activity-title">
      <div className="panel-heading">
        <div>
          <h2 id="activity-title">Activité récente</h2>
          <p>Les dernières mises à jour de vos dossiers</p>
        </div>
        <span className="panel-icon"><Icon name="clock" size={18} /></span>
      </div>
      {/* État vide volontaire : l'historique sera affiché lorsque les modules seront reliés. */}
      <div className="empty-state">
        <span className="empty-icon"><Icon name="document" size={21} /></span>
        <strong>Rien à afficher pour le moment</strong>
        <span>Les activités apparaîtront ici lorsque les modules seront connectés.</span>
      </div>
    </section>
  )
}

export default ActivityPanel
