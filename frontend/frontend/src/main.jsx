import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/base.css'
import App from './App.jsx'

// Monte l'application React dans l'élément #root déclaré dans index.html.
// StrictMode aide à repérer les effets de bord pendant le développement.
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
