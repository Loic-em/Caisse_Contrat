import Icon from '../../../components/icons/Icon'
import './MetricCard.css'

function MetricCard({ icon, color, title, description, value, isLoading }) {
  return (
    <article className="metric-card">
      <div className="metric-top">
        <span className={`metric-icon ${color}`}><Icon name={icon} size={19} /></span>
        <span className="metric-placeholder" aria-live={isLoading ? 'polite' : undefined}>
          {isLoading ? '…' : value ?? '—'}
        </span>
      </div>
      <h3>{title}</h3>
      <p>{description}</p>
    </article>
  )
}

export default MetricCard
