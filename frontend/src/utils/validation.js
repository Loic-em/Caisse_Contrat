export function setFrenchValidationMessages(root = document) {
  if (!root) return
  const elements = root.querySelectorAll('input, select, textarea')
  elements.forEach((el) => {
    const set = () => {
      if (el.validity.valueMissing) {
        el.setCustomValidity('Veuillez renseigner ce champ.')
      } else if (el.validity.typeMismatch) {
        el.setCustomValidity(el.type === 'email' ? 'Veuillez saisir une adresse e-mail valide.' : 'Veuillez saisir une valeur valide.')
      } else if (el.validity.tooShort) {
        el.setCustomValidity(`Veuillez saisir au moins ${el.minLength} caractères.`)
      } else if (el.validity.rangeUnderflow || el.validity.rangeOverflow || el.validity.stepMismatch) {
        el.setCustomValidity('Veuillez saisir une valeur valide.')
      } else if (el.validity.patternMismatch) {
        el.setCustomValidity('Veuillez respecter le format demandé.')
      } else {
        el.setCustomValidity('')
      }
    }
    el.addEventListener('invalid', () => { set() })
    el.addEventListener('input', () => { if (el.validationMessage) set() })
    el.addEventListener('change', () => { if (el.validationMessage) set() })
  })
}
