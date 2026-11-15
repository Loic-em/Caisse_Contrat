# Frontend

React 19 application built with Vite. It provides the user interface for the
Caisse Contrat application: authentication, dashboard, cash requests and
vouchers, supporting documents, employment contracts and team management.

## Source structure

- `src/app/`: application-level navigation and page metadata.
- `src/components/`: reusable components shared across features (layout and icons).
- `src/features/`: feature-specific pages, components and API clients.
- Each component or page keeps its stylesheet next to its JSX file.
- `src/styles/base.css`: small set of styles shared by the whole application.
- `src/assets/`: static assets.

Feature components should stay with their feature; shared UI belongs in
`src/components/`. Import a component stylesheet from its JSX file so its
presentation and behavior stay together. The frontend uses the existing
React/Vite JavaScript setup and does not add a router or other dependencies.

Main feature folders:

- `features/auth/`: login, current user, demo accounts, signature.
- `features/accounts/`: team management (administrators only).
- `features/dashboard/`: overview page and metric cards.
- `features/requests/`: cash requests, vouchers view, supporting documents,
  regularization panel.
- `features/contracts/`: employment contracts.
- `features/workspace/`: placeholder page.

## Run and validate

The Vite application is in this `frontend` directory (nested inside the
repository's top-level `frontend` folder). From `C:\Caisse_Contrat`, first enter
the application directory:

```powershell
cd frontend\frontend
```

Then run:

- `npm run dev` starts the development server (default `http://localhost:5173`).
- `npm run build` creates the production bundle.
- `npm run lint` checks the source with Oxlint.
- `npm run preview` previews the production build.

## Backend connection

The development server forwards `/api` requests to `http://localhost:8081`.
Start the backend before opening the application. If the backend uses another
port, create a `.env.local` file in this directory with the matching URL:

```dotenv
VITE_BACKEND_URL=http://localhost:8082
```

For a deployed frontend, set `VITE_API_BASE_URL` to the backend's public base
URL when building.

## Authentication

Authentication is handled by the backend through a session cookie and a CSRF
token. The frontend fetches the CSRF token before every non-GET request and
sends credentials. Demo accounts (when enabled on the backend) are listed on the
login screen in development.

## File uploads

Supporting documents (PDF, JPG, PNG, up to 10 MB) and signed contract PDFs
(up to 10 MB) are uploaded through multipart form data. Upload availability
depends on the backend ClamAV service and the matching encryption key.