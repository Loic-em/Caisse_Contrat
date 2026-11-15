import { useEffect, useState } from 'react'
import ActivityPanel from '../components/ActivityPanel'
import MetricCard from '../components/MetricCard'
import QuickAccessPanel from '../components/QuickAccessPanel'
import WelcomePanel from '../components/WelcomePanel'
import { getCashRequests } from '../../requests/api/cashRequestsApi'
import './OverviewPage.css'

function OverviewPage({ onNavigate, currentUser }) {
  const [requests, setRequests] = useState(null)
  const [requestsError, setRequestsError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    getCashRequests({ signal: controller.signal })
      .then(setRequests)
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setRequestsError(error.message)
        }
      })

    return () => controller.abort()
  }, [])

  // Les dossiers restent ouverts jusqu’à la fin de leur régularisation.
  const openRequests = requests?.filter((request) =>
    ['DRAFT', 'SOUMISE', 'VALIDEE_N1', 'VALIDEE', 'A_CORRIGER', 'MONTANT_ARRETE', 'BON_EMIS', 'DECAISSEE', 'EN_REGULARISATION'].includes(request.status),
  ).length ?? null
  const requestsToRegularize = requests?.filter((request) => request.status === 'EN_REGULARISATION').length ?? null
  const requestsToReview = requests?.filter((request) => (
    (request.status === 'SOUMISE' && request.validatorN1Id === currentUser.id)
    || (request.status === 'VALIDEE_N1' && request.validatorN2Id === currentUser.id)
    || (request.status === 'VALIDEE'
      && (currentUser.role === 'COMPTABLE'
        || (currentUser.role === 'ADJOINT_COMPTABLE' && request.requesterRole === 'COMPTABLE')))
  )).length ?? null

  const metrics = [
    {
      icon: 'wallet',
      color: 'purple',
      title: 'Demandes en cours',
      description: 'Demandes en cours jusqu’au bon émis',
      value: openRequests,
      requestMetric: true,
    },
    {
      icon: 'clock',
      color: 'blue',
      title: 'À traiter',
      description: 'Demandes en attente de validation',
      value: requestsToReview,
      requestMetric: true,
    },
    {
      icon: 'receipt',
      color: 'green',
      title: 'Dossiers à régulariser',
      description: 'Missions terminées en attente de justification',
      value: requestsToRegularize,
      requestMetric: true,
    },
    {
      icon: 'briefcase',
      color: 'orange',
      title: 'Contrats à échéance',
      description: 'Indicateur non connecté',
      value: null,
    },
  ]

  return (
    <section className="content-page">
      {/* Chaque bloc garde sa responsabilité dans un composant séparé. */}
      <WelcomePanel />

      <div className="section-title-row">
        <div>
          <p className="eyebrow">EN UN COUP D’ŒIL</p>
          <h2>Vue d’ensemble</h2>
        </div>
        {requestsError ? (
          <div className="data-note data-note-error" role="alert">
            <span />
            Demandes indisponibles
            <button type="button" onClick={() => {
              setRequests(null)
              setRequestsError('')
              getCashRequests()
                .then(setRequests)
                .catch((error) => setRequestsError(error.message))
            }}>
              Réessayer
            </button>
          </div>
        ) : (
          <span className="data-note">
            <span />
            {requests === null ? 'Chargement des demandes' : 'Demandes connectées · autres données à venir'}
          </span>
        )}
      </div>

      <div className="metric-grid">
        {/* Les mêmes données pilotent l'icône, la couleur et le texte de chaque carte. */}
        {metrics.map(({ requestMetric, ...metric }) => (
          <MetricCard
            key={metric.title}
            {...metric}
            isLoading={requestMetric && requests === null && !requestsError}
          />
        ))}
      </div>

      <div className="dashboard-grid">
        {/* Les raccourcis réutilisent la navigation de l'application. */}
        <ActivityPanel />
        <QuickAccessPanel onNavigate={onNavigate} />
      </div>

      <footer className="page-footer">
        <span>Gestion interne <span className="footer-separator">·</span> Caisse &amp; contrats</span>
        <span>Version de préparation</span>
      </footer>
    </section>
  )
}

export default OverviewPage
