import Icon from '../../../components/icons/Icon'
import './PlaceholderPage.css'

function PlaceholderPage({ page, onNavigate }) {
  // Les rubriques pas encore réalisées partagent cette page informative.
  return (
    <section className="content-page placeholder-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">{page.eyebrow}</p>
          <h1>{page.label}</h1>
          <p className="page-description">{page.description}</p>
        </div>
        <span className="status-pill"><span /> En préparation</span>
      </div>
      <div className="placeholder-card">
        <div className="placeholder-illustration"><Icon name="spark" size={26} /></div>
        <h2>Cet espace prend forme</h2>
        <p>
          Les écrans et les fonctionnalités de ce module seront ajoutés progressivement.
          Aucune donnée opérationnelle n’est encore chargée.
        </p>
        {/* Permet de revenir à une page déjà disponible sans recharger le site. */}
        <button className="text-button" type="button" onClick={() => onNavigate('overview')}>
          Revenir à la vue d’ensemble <Icon name="arrow" size={17} />
        </button>
      </div>
    </section>
  )
}

export default PlaceholderPage
