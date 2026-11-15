import Icon from '../../../components/icons/Icon'
import './WelcomePanel.css'

function WelcomePanel() {
  return (
    <section className="welcome-panel" aria-labelledby="welcome-title">
      {/* Le texte reste utile même si l'illustration décorative est masquée aux lecteurs d'écran. */}
      <div className="welcome-copy">
        <p className="eyebrow">VOTRE ESPACE DE GESTION</p>
        <h1 id="welcome-title">Bonjour <span className="wave">👋</span></h1>
        <p>
          Retrouvez au même endroit le suivi de la caisse et des contrats de travail.
        </p>
      </div>
      <div className="welcome-art" aria-hidden="true">
        <div className="art-orbit orbit-one" />
        <div className="art-orbit orbit-two" />
        <div className="art-card">
          <div className="art-card-top"><span /><span /><span /></div>
          <div className="art-card-line long" />
          <div className="art-card-line medium" />
          <div className="art-card-bottom"><span /><span /></div>
        </div>
        <div className="art-sparkle"><Icon name="spark" size={21} /></div>
      </div>
    </section>
  )
}

export default WelcomePanel
