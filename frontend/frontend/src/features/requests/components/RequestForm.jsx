import { useState } from 'react'
import {
  ArrowLeftOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  FileTextOutlined,
  PaperClipOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { createCashRequest, updateCashRequest } from '../api/cashRequestsApi'
import RequestPreview from './RequestPreview'
import './RequestForm.css'

const initialForm = {
  subject: '',
  location: '',
  startDate: '',
  endDate: '',
  category: '',
  estimatedAmount: '',
}

function RequestForm({ onCancel, onCreated, requestToEdit }) {
  // Le formulaire est contrôlé par React : une seule source contient les valeurs des champs.
  const [form, setForm] = useState(() => ({
    ...initialForm,
    subject: requestToEdit?.subject ?? '',
    location: requestToEdit?.location ?? '',
    startDate: requestToEdit?.startDate ?? '',
    endDate: requestToEdit?.endDate ?? '',
    category: requestToEdit?.category ?? '',
    estimatedAmount: requestToEdit?.estimatedAmount?.toString() ?? '',
  }))
  // On ne conserve ici que le nom du fichier, qui n'est ni téléversé ni enregistré.
  const [selectedFileName, setSelectedFileName] = useState(requestToEdit?.attachmentName ?? '')
  const [isPreviewOpen, setIsPreviewOpen] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  function updateField(event) {
    const { name, value } = event.target
    // Met à jour le champ modifié tout en conservant les autres valeurs.
    setForm((currentForm) => ({ ...currentForm, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    setSaveError('')
    setIsPreviewOpen(true)
  }

  async function handleCreate() {
    setIsSaving(true)
    setSaveError('')

    try {
      const payload = {
        ...form,
        estimatedAmount: Number(form.estimatedAmount),
      }
      const request = requestToEdit
        ? await updateCashRequest(requestToEdit.id, payload)
        : await createCashRequest(payload)
      onCreated(request)
    } catch (error) {
      setSaveError(error.message)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="content-page request-form-page">
      <button className="request-back-button" type="button" onClick={onCancel}>
        <ArrowLeftOutlined aria-hidden="true" />
        Retour aux demandes
      </button>

      <div className="request-form-heading">
        <p className="eyebrow">CAISSE · {requestToEdit ? 'MODIFICATION' : 'NOUVELLE DEMANDE'}</p>
        <h1>{requestToEdit ? 'Modifier la demande' : 'Préparer une demande de fonds'}</h1>
        <p>Renseignez les informations de votre mission pour en prévisualiser la demande.</p>
      </div>

      <div className="request-form-notice" role="status">
        Vérifiez les informations dans l’aperçu avant de créer la demande. Les pièces jointes ne sont pas téléversées.
      </div>

      <form className="request-form-card" onSubmit={handleSubmit}>
        <div className="request-form-section-heading">
          <span className="request-form-section-icon"><FileTextOutlined aria-hidden="true" /></span>
          <div>
            <h2>Informations sur la mission</h2>
            <p>Les champs marqués d’un astérisque sont requis pour créer la demande.</p>
          </div>
        </div>

        <div className="request-form-grid">
          <label className="request-field request-field-wide">
            <span>Objet de la mission <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <FileTextOutlined aria-hidden="true" />
              <input
                autoComplete="off"
                name="subject"
                onChange={updateField}
                placeholder="Ex. Réunion avec un partenaire"
                required
                value={form.subject}
              />
            </span>
          </label>

          <label className="request-field request-field-wide">
            <span>Lieu de la mission <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <EnvironmentOutlined aria-hidden="true" />
              <input
                autoComplete="off"
                name="location"
                onChange={updateField}
                placeholder="Ville ou lieu de la mission"
                required
                value={form.location}
              />
            </span>
          </label>

          <label className="request-field">
            <span>Date de début <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <CalendarOutlined aria-hidden="true" />
              <input
                name="startDate"
                onChange={updateField}
                required
                type="date"
                value={form.startDate}
              />
            </span>
          </label>

          <label className="request-field">
            <span>Date de fin <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <CalendarOutlined aria-hidden="true" />
              {/* Le navigateur interdit une date de fin antérieure à la date de début. */}
              <input
                min={form.startDate || undefined}
                name="endDate"
                onChange={updateField}
                required
                type="date"
                value={form.endDate}
              />
            </span>
          </label>

          <label className="request-field">
            <span>Catégorie de dépense <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <WalletOutlined aria-hidden="true" />
              <input
                autoComplete="off"
                name="category"
                onChange={updateField}
                placeholder="Catégorie (liste à configurer)"
                required
                value={form.category}
              />
            </span>
            <small>La liste des catégories reste à configurer ; cette valeur est enregistrée telle quelle.</small>
          </label>

          <label className="request-field">
            <span>Estimation du montant <span className="required-mark">*</span></span>
            <span className="request-input-wrap amount-input-wrap">
              <input
                inputMode="numeric"
                min="1"
                name="estimatedAmount"
                onChange={updateField}
                placeholder="Ex. 25000"
                required
                step="1"
                type="number"
                value={form.estimatedAmount}
              />
              <span className="currency-suffix">FCFA</span>
            </span>
          </label>
        </div>

        <label className="request-field request-attachment-field">
          <span>Pièce jointe <span className="optional-label">Facultatif dans cette maquette</span></span>
          <span className="request-file-input">
            <PaperClipOutlined aria-hidden="true" />
            <span>{selectedFileName || 'Choisir un fichier PDF, JPG ou PNG'}</span>
            {/* Seul le nom est affiché dans cette maquette ; le fichier n'est pas transmis. */}
            <input
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              onChange={(event) => setSelectedFileName(event.target.files?.[0]?.name ?? '')}
              type="file"
            />
          </span>
          <small>Le fichier n’est pas envoyé ni conservé avec la demande.</small>
        </label>

        <div className="request-form-actions">
          <button className="request-secondary-button" type="button" onClick={onCancel}>
            Annuler
          </button>
          <button className="request-primary-button" type="submit">
            Prévisualiser la demande
          </button>
        </div>
      </form>

      {/* L'aperçu se superpose au formulaire et peut être fermé pour le modifier. */}
      {isPreviewOpen && (
        <RequestPreview
          form={form}
          fileName={selectedFileName}
          error={saveError}
          isEditing={Boolean(requestToEdit)}
          isSaving={isSaving}
          onClose={() => setIsPreviewOpen(false)}
          onConfirm={handleCreate}
        />
      )}
    </section>
  )
}

export default RequestForm
