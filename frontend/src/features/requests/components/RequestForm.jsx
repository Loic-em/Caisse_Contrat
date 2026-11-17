import { useState } from 'react'
import {
  ArrowLeftOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  FileTextOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { createCashRequest, updateCashRequest } from '../api/cashRequestsApi'
import RequestPreview from './RequestPreview'
import './RequestForm.css'

const initialForm = {
  subject: '',
  startDate: '',
  endDate: '',
  category: '',
  estimatedAmount: '',
  activityArea: '',
  activityDetails: '',
  clientPartner: '',
  missionPurpose: '',
  missionReference: '',
  contactName: '',
  contactPhone: '',
}

const otherCityValue = 'AUTRE_VILLE'
const otherCategoryValue = 'AUTRE_CATEGORIE'
const missionCities = [
  'Abidjan',
  'Abengourou',
  'Aboisso',
  'Adzopé',
  'Agboville',
  'Anyama',
  'Bingerville',
  'Bondoukou',
  'Bouaflé',
  'Bouaké',
  'Bouna',
  'Dabou',
  'Dabakala',
  'Daloa',
  'Dimbokro',
  'Divo',
  'Duekoué',
  'Ferkessédougou',
  'Gagnoa',
  'Grand-Bassam',
  'Guiglo',
  'Issia',
  'Katiola',
  'Korhogo',
  'Mankono',
  'Man',
  'Odienné',
  'San-Pédro',
  'Séguéla',
  'Soubré',
  'Tanda',
  'Tiassalé',
  'Toumodi',
  'Vavoua',
  'Yamoussoukro',
  'Zuénoula',
]
const expenseCategories = [
  'Transport',
  'Carburant',
  'Hébergement',
  'Restauration',
  'Indemnités de mission',
  'Communication',
  'Fournitures et matériel',
  'Péage et stationnement',
  'Frais de représentation',
]

function getInitialMissionDetails(location = '') {
  if (!location) return { city: '', customCity: '', place: '' }

  const [savedCity, ...savedPlace] = location.split(' — ')
  if (savedPlace.length > 0) {
    const isKnownCity = missionCities.includes(savedCity)
    return {
      city: isKnownCity ? savedCity : otherCityValue,
      customCity: isKnownCity ? '' : savedCity,
      place: savedPlace.join(' — '),
    }
  }

  if (missionCities.includes(location)) {
    return { city: location, customCity: '', place: '' }
  }

  // Un ancien lieu est conservé dans le détail jusqu’à ce que sa ville soit choisie.
  return { city: '', customCity: '', place: location }
}

function formatMissionLocation({ city, customCity, place }) {
  const cityName = city === otherCityValue ? customCity.trim() : city
  return [cityName, place.trim()].filter(Boolean).join(' — ')
}

function RequestForm({ onCancel, onCreated, requestToEdit }) {
  // Le formulaire est contrôlé par React : une seule source contient les valeurs des champs.
  const [form, setForm] = useState(() => ({
    ...initialForm,
    subject: requestToEdit?.subject ?? '',
    startDate: requestToEdit?.startDate ?? '',
    endDate: requestToEdit?.endDate ?? '',
    category: requestToEdit?.category ?? '',
    estimatedAmount: requestToEdit?.estimatedAmount?.toString() ?? '',
    activityArea: requestToEdit?.activityArea ?? '',
    activityDetails: requestToEdit?.activityDetails ?? '',
    clientPartner: requestToEdit?.clientPartner ?? '',
    missionPurpose: requestToEdit?.missionPurpose ?? '',
    missionReference: requestToEdit?.missionReference ?? '',
    contactName: requestToEdit?.contactName ?? '',
    contactPhone: requestToEdit?.contactPhone ?? '',
  }))
  const [missionDetails, setMissionDetails] = useState(() => getInitialMissionDetails(requestToEdit?.location))
  const [selectedExpenseCategory, setSelectedExpenseCategory] = useState(() => (
    expenseCategories.includes(requestToEdit?.category)
      ? requestToEdit.category
      : requestToEdit?.category ? otherCategoryValue : ''
  ))
  const [customExpenseCategory, setCustomExpenseCategory] = useState(() => (
    expenseCategories.includes(requestToEdit?.category) ? '' : requestToEdit?.category ?? ''
  ))
  const [isPreviewOpen, setIsPreviewOpen] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  function updateField(event) {
    const { name, value } = event.target
    // Met à jour le champ modifié tout en conservant les autres valeurs.
    setForm((currentForm) => ({ ...currentForm, [name]: value }))
  }

  function updateMissionField(event) {
    const { name, value } = event.target
    setMissionDetails((currentDetails) => ({ ...currentDetails, [name]: value }))
  }

  function updateExpenseCategory(event) {
    const { value } = event.target
    setSelectedExpenseCategory(value)
    if (value !== otherCategoryValue) {
      setForm((currentForm) => ({ ...currentForm, category: value }))
    } else {
      setForm((currentForm) => ({ ...currentForm, category: customExpenseCategory }))
    }
  }

  function updateCustomExpenseCategory(event) {
    const { value } = event.target
    setCustomExpenseCategory(value)
    setForm((currentForm) => ({ ...currentForm, category: value }))
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
        location: formatMissionLocation(missionDetails),
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
        Vérifiez les informations dans l’aperçu avant de créer la demande. Les justificatifs de dépenses se déposent après le décaissement depuis l’espace correspondant.
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

          <label className="request-field">
            <span>Ville de mission <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <EnvironmentOutlined aria-hidden="true" />
              <select
                name="city"
                onChange={updateMissionField}
                required
                value={missionDetails.city}
              >
                <option value="">Sélectionnez une ville</option>
                {missionCities.map((city) => <option key={city} value={city}>{city}</option>)}
                <option value={otherCityValue}>Autre ville</option>
              </select>
            </span>
          </label>

          {missionDetails.city === otherCityValue && (
            <label className="request-field">
              <span>Précisez la ville <span className="required-mark">*</span></span>
              <span className="request-input-wrap">
                <EnvironmentOutlined aria-hidden="true" />
                <input
                  autoComplete="address-level2"
                  name="customCity"
                  onChange={updateMissionField}
                  placeholder="Nom de la ville"
                  required
                  value={missionDetails.customCity}
                />
              </span>
            </label>
          )}

          <label className="request-field">
            <span>Lieu exact <span className="required-mark">*</span></span>
            <span className="request-input-wrap">
              <EnvironmentOutlined aria-hidden="true" />
              <input
                autoComplete="street-address"
                name="place"
                onChange={updateMissionField}
                placeholder="Ex. quartier, établissement ou adresse"
                required
                value={missionDetails.place}
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
              <select
                name="expenseCategory"
                onChange={updateExpenseCategory}
                required
                value={selectedExpenseCategory}
              >
                <option value="">Sélectionnez une catégorie</option>
                {expenseCategories.map((category) => <option key={category} value={category}>{category}</option>)}
                <option value={otherCategoryValue}>Autre catégorie</option>
              </select>
            </span>
          </label>

          {selectedExpenseCategory === otherCategoryValue && (
            <label className="request-field">
              <span>Précisez la catégorie <span className="required-mark">*</span></span>
              <span className="request-input-wrap">
                <WalletOutlined aria-hidden="true" />
                <input
                  autoComplete="off"
                  onChange={updateCustomExpenseCategory}
                  placeholder="Nom de la catégorie"
                  required
                  value={customExpenseCategory}
                />
              </span>
            </label>
          )}

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
          form={{ ...form, location: formatMissionLocation(missionDetails) }}
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
