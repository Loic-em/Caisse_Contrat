import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  EnvironmentOutlined,
  FileTextOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import './RequestPreview.css'

function formatDate(value) {
  if (!value) {
    return '—'
  }

  // Un champ date HTML renvoie AAAA-MM-JJ ; l'aperçu utilise le format français JJ/MM/AAAA.
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

function formatAmount(value) {
  // Intl applique les séparateurs de milliers utilisés en français.
  return new Intl.NumberFormat('fr-FR').format(Number(value))
}

function RequestPreview({ form, error, isSaving, isEditing, onClose, onConfirm }) {
  // La fenêtre montre les valeurs saisies, mais ne valide ni n'enregistre la demande.
  return (
    <div className="request-preview-backdrop" role="presentation">
      <section
        aria-labelledby="request-preview-title"
        aria-modal="true"
        className="request-preview-dialog"
        role="dialog"
      >
        <div className="request-preview-heading">
          <span className="request-preview-success-icon"><CheckCircleOutlined aria-hidden="true" /></span>
          <div>
            <p className="eyebrow">VÉRIFICATION</p>
            <h2 id="request-preview-title">Vérifier la demande</h2>
          </div>
        </div>

        <p className="request-preview-notice">
          Vérifiez les informations avant {isEditing ? 'd’enregistrer les changements' : 'de créer le brouillon'}.
          Les justificatifs de dépenses se déposent après le décaissement, dans l’espace correspondant.
        </p>

        <dl className="request-preview-details">
          <div>
            <dt><FileTextOutlined aria-hidden="true" /> Objet</dt>
            <dd>{form.subject}</dd>
          </div>
          <div>
            <dt><EnvironmentOutlined aria-hidden="true" /> Lieu</dt>
            <dd>{form.location}</dd>
          </div>
          <div>
            <dt>Dates de mission</dt>
            <dd>{formatDate(form.startDate)} – {formatDate(form.endDate)}</dd>
          </div>
          <div>
            <dt><WalletOutlined aria-hidden="true" /> Catégorie</dt>
            <dd>{form.category}</dd>
          </div>
          <div>
            <dt>Montant estimé</dt>
            <dd>{formatAmount(form.estimatedAmount)} FCFA</dd>
          </div>
        </dl>

        {error && <p className="request-preview-error" role="alert">{error}</p>}

        <div className="request-preview-actions">
          <button className="request-preview-close" type="button" onClick={onClose} disabled={isSaving}>
            <ArrowLeftOutlined aria-hidden="true" />
            Modifier
          </button>
          <button className="request-preview-confirm" type="button" onClick={onConfirm} disabled={isSaving}>
            {isSaving ? 'Enregistrement…' : isEditing ? 'Enregistrer les changements' : 'Créer le brouillon'}
          </button>
        </div>
      </section>
    </div>
  )
}

export default RequestPreview
