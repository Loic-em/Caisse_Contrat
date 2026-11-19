# Fichier mémoire — Caisse & Contrats de travail

> Ce document est le **fichier mémoire** du projet. À **chaque modification** effectuée sur le projet, il faut enregistrer ici la mémoire de ce qui a été fait (date, ce qui a changé, pourquoi, fichiers touchés). Ainsi, lors de la reprise du travail (« allumage »), on peut facilement reprendre le contexte sans tout réexpliquer.

## Projet

- **Nom** : Logiciel de gestion interne (Caisse et Contrats de travail) — Digin Factory.
- **Dossier** : `C:\Caisse_Contrat`
- **Branche** : `main` (à jour avec `origin/main`).
- **Backend** : Spring Boot (Java 21), port 8081 (profil local H2), Flyway V1..V15.
- **Frontend** : React + Vite, port dev 5173 (proxy `/api` → backend).

## Documents de référence

- `C:\Users\Loice\Documents\Cahier des charges – Logiciel de gestion interne (Caisse et Contrats de travail).pdf` (v1.2, 07/11/2026)
- `C:\Users\Loice\Documents\Specifications_fonctionnelles_gestion_interne.docx` (v0.1, 07/11/2026)
- `frontend/REFERENCE_FONCTIONNELLE.md` (référence fonctionnelle persistante, état au 17/11/2026)
- `DOCUMENTATION_PROJET.md`, `README.md`

## Règle de travail

- Toute modification doit être **consignée dans ce fichier** (section « Journal des modifications ») avec : date, résumé, fichiers touchés, impact.
- Consulter ce fichier en premier lors de la reprise pour retrouver le contexte.

## État actuel (au 17/11/2026)

### Fait
- Parcours de caisse cœur : demande, validation N+1 puis N+2, montant définitif, bon/visas/décaissement, pièces, retours par bon d'entrée, clôture sous conditions.
- Contrats rattachés aux comptes employés non admin ; matrice d'accès appliquée (comptable voit tous et gère ceux de l'équipe sauf le sien ; DG voit tous et gère ceux du comptable/adjoint ; employé voit le sien ; admin aucun accès).
- Fiche contrat avec salaire de base, primes fixes, détail des primes, indemnités et autres avantages.
- Bloc d'informations complémentaires retiré temporairement de la demande de caisse (colonnes/valeurs conservées en base).
- Espace nommé « Justificatifs de dépenses ».
- Dépôt de justificatifs bloqué tant que ClamAV ou la clé de chiffrement n'est pas opérationnel (consultation/contrôle conservés).
- Correction des accents et de l'encodage (UTF-8 backend + frontend, textes restaurés).
- Backend et frontend compilent (Maven `clean compile` OK, Vite build OK).

### Reste à faire
1. Notifications et e-mails (rappels J-2, J-1, jour J, notifications applicatives/e-mail, échecs d'envoi).
2. Module Contrats complet : congés/soldes, avenants/renouvellements/conversions, alertes d'échéance automatiques, avertissements de durée légale.
3. Caisse après mission : extensions de délai, dépassements avec décision, réouverture motivée, blocage nouvelle demande en retard, délégations/suppléances, visa manuscrit.
4. Reporting et journal transversal : filtres/exports PDF-Excel-CSV, journal unifié, panneau Activité.
5. Authentification Microsoft 365 / Entra ID (actuellement locale e-mail/mot de passe).
6. Paramètres métier à configurer (seuils, délais, catégories, etc.).

## Journal des modifications

### 2026-11-18 — Corrections FR page « Justificatifs de dépenses »
- **Fichier touché** : `frontend/src/features/requests/pages/SupportingDocumentsPage.jsx`.
- **Détail** : correction des accents/mots tronqués et de la ponctuation — statuts `Accepté`/`Rejeté`, « pour rejeter un justificatif », « a été accepté/rejeté », « a été rejeté », « Aucune pièce déposée », « Montant justifié (FCFA) », catégories `Reçu`, `Hébergement`, `Parking et stationnement`, `Fournitures et matériel`, « Choisissez une catégorie », séparateurs `·` dans l'en-tête et le détail des pièces, « Motif de rejet pour » / « Motif obligatoire si rejet ».
- **Vérification** : `npm run build` OK (dist/assets/index-B_az-StN.js).

### 2026-11-18 — Erreur 403 sur « Fixer le montant »
- **Symptôme** : clic sur « Fixer le montant » → « La requête a échoué (HTTP 403). » sans détail.
- **Cause** : `CashRequestService.requireAccountingPermission` (séparation des tâches) renvoie `ResponseStatusException(403)` quand un comptable traite **sa propre demande** ; le message n'était pas transmis au frontend (Spring Boot n'inclut pas le message par défaut → message générique). Par ailleurs le bouton restait affiché pour la demande du demandeur connecté.
- **Fichiers touchés** :
  - `backend/src/main/java/com/example/backend/cash/ApiExceptionHandler.java` : ajout d'un `@ExceptionHandler(ResponseStatusException.class)` qui renvoie `{"message": ex.getReason()}` avec le bon statut → le motif réel est désormais visible côté UI.
  - `frontend/src/features/requests/pages/CashRequestsPage.jsx` : `canReviewAmount` et `canIssueVoucher` incluent désormais `!isRequester` (garde `requesterId !== currentUser.id`), aligné sur les visas Caisse.
- **Vérification** : `mvnw compile -o` OK ; `npm run build` OK (dist/assets/index-m19v50z3.js).

### 2026-11-17 — Correction des accents et de l'encodage
- **Fichiers touchés** :
  - `backend/src/main/resources/application.properties` (ajout `server.servlet.encoding.charset=UTF-8`, `enabled=true`, `force=true`)
  - `frontend/src/features/api/http.js` (`Content-Type: application/json; charset=utf-8`)
  - `frontend/src/features/requests/pages/SupportingDocumentsPage.jsx`
  - `frontend/src/features/requests/pages/CashRequestsPage.jsx`
  - `frontend/src/features/requests/pages/RegularizationPanel.jsx`
  - `frontend/src/features/contracts/ContractsPage.jsx`
- **Détail** : restauration des accents et apostrophes dans les chaînes de texte mangled (« Justificatifs de dépenses », « décaissement », « régularisation », « bénéficiaire », « sécurité », « clé de chiffrement », etc.) ; correction des apostrophes non échappées dans les chaînes JS.
- **Vérification** : backend `mvn clean compile` BUILD SUCCESS ; frontend `npm run build` OK.

### 2026-11-17 — Création du fichier mémoire
- **Fichier** : `memoire.md` (ce document).
- **Détail** : mise en place du fichier mémoire avec règle de consignation des modifications.
- **Backend/frontend arrêtés** à la demande de l'utilisateur.

### 2026-11-17 — Stabilisation bons de caisse (consultation) et contrats (comptes sans contrat)
- **Fichiers touchés** :
  - `frontend/src/features/requests/pages/CashRequestsPage.jsx`
  - `frontend/src/features/contracts/ContractsPage.jsx`
  - `frontend/src/features/contracts/ContractsPage.css`
- **Détail** :
  - **Bons de caisse** : correction des propriétés du bon pour correspondre à l’API (`missionLocation`, `missionStartDate`, `missionEndDate` au lieu de champs corrompus), restauration des libellés/actions/attributs FR (émission, visas, décaissement, aperçu/impression, statuts, messages) ; flux de consultation conservé (`handleViewVoucher` → `getCashVoucher`, vue bons `receipts`/`isVoucherView`, `voucherStatuses`).
  - **Contrats** : correction de textes/statuts/labels de formulaire et filtres, section « Comptes sans contrat » restructurée (classes `contracts-missing-*`, texte d’aide, bouton « Créer le contrat »).
  - **CSS contrats** : ajout de styles `.contracts-missing-*` pour aligner visuellement la création depuis un compte sans contrat sur la liste « Sélectionner un contrat » (`.contracts-list-panel`/`.contract-list-item` : cartes, survol, bouton).
- **Vérification** : `npm run build` OK (frontend). Backend et frontend relancés avec succès en local (`http://127.0.0.1:8081`, `http://127.0.0.1:5173/`, `GET /api/auth/demo-accounts` 200).
- **À vérifier manuellement** : consultation d’un bon émis/existant en navigateur, chargement/rendu de la page contrats et du bloc « Comptes sans contrat ». Documents de référence (PDF/DOCX cahier des charges/spécifications) non encore exploités ; `frontend/src/assets/hero.png` à contrôler si image cassée.

### 2026-11-17 — Correction des fautes d’orthographe/accents restantes (frontend)
- **Fichiers touchés** :
  - `frontend/src/features/contracts/ContractsPage.jsx`
  - `frontend/src/features/requests/pages/CashRequestsPage.jsx`
  - `frontend/src/features/requests/pages/RegularizationPanel.jsx`
  - `frontend/src/features/requests/pages/SupportingDocumentsPage.jsx`
- **Détail** : correction systématique des libellés/messages FR corrompus ou fautifs (accents, apostrophes, accords, ponctuation) — ex. « Période d’essai », « Rémunération de base mensuelle », « Détail des primes », « Indemnités et autres avantages », « Enregistrer/Enregistrement… », « Sélectionnez/Sélectionner », « aujourd’hui », « Non renseignée/Aucune renseignée », « Montant en lettres », « Bénéficiaire », « bon d’entrée/d’origine », « clôture/clôturé », « après », « déposée(s)/déposer », « mission », « Autorisation préalable », « APRÈS MISSION », option « Autre », etc. ; corrections associées (`accept="image/*"`, `acceptedDocumentAmount` dans `RegularizationPanel.jsx`).
- **Vérification** : `npm run build` OK (`dist/assets/index-Be3SOl37.js`, `dist/assets/index-Bx3m4kbs.css`). Recherche des motifs de corruption résiduels dans `src` : aucun résultat. Contrôle visuel navigateur recommandé.

## 2026-11-19
- SupportingDocumentsPage.jsx : ajout d'un useEffect qui fait d�filer la page vers le message d'erreur apr�s un �chec d'acceptation (� Le montant accept� d�passerait le solde restant � r�gulariser. �). Corrig� une erreur de syntaxe JSX.

## Notifications (2026-11-19)
- Backend: entite Notification, repository, service, controller /api/notifications. Frontend: notificationsApi.js + NotificationsPage.jsx.
