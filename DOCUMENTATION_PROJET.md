# Documentation du projet « Caisse Contrat »

## 1. Qu'est-ce que ce projet ?

**Caisse Contrat** est une application web interne pour **Digin Factory**. Elle sert à gérer :

- les **comptes utilisateurs** (équipe, rôles, hiérarchie N+1/N+2),
- les **demandes de caisse** (missions avec estimation de frais, validation hiérarchique, arrêt du montant par la comptabilité),
- les **bons de caisse** (numérotés, avec montant en lettres, visas personnels, décaissement),
- les **justificatifs** de mission (dépôt, contrôle comptable, chiffrement),
- la **régularisation** des fonds décaissés (bons d'entrée, retours, clôture),
- les **contrats de travail** (brouillon, dépôt du PDF signé, versions, historique).

L'application est composée de deux parties :

| Partie | Technologie | Rôle |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 4.1 | API REST, règles métier, base de données, sécurité |
| **Frontend** | React 19 + Vite 8 | Interface utilisateur (pages, formulaires, affichage) |

Le backend répond sur `http://localhost:8081`, le frontend sur `http://localhost:5173`.

---

## 2. Architecture générale (image mentale)

```
[Navigateur]  <->  [Frontend React]  <->  [Backend Spring Boot]  <->  [Base de données H2/PostgreSQL]
```

- Le frontend n'a **pas** de logique métier critique : il appelle l'API.
- Le backend est la **source de vérité** : il valide tout, calcule les statuts, enregistre l'historique.
- La base de données est créée/mise à jour automatiquement par **Flyway** (scripts SQL versionnés).

---

## 3. BACKEND

Dossier : `backend/src/main/java/com/example/backend/`

### 3.1 Point d'entrée et configuration globale

| Fichier | Rôle | Spécialité |
|---|---|---|
| `BackendApplication.java` | Démarre l'application Spring Boot | Point d'entrée unique (`main`). Lance tout le reste. |
| `CorsConfig.java` | Autorise le frontend à appeler l'API | Autorise uniquement `localhost:5173`, avec cookies (session) et jeton CSRF. |

Configuration (`resources/`) :

| Fichier | Rôle |
|---|---|
| `application.properties` | Nom de l'app, import optionnel de `.env`, profil par défaut `local`, connexion PostgreSQL par variables d'environnement, Flyway activé, Hibernate en `validate`. Paramètres : rejet min., ClamAV, clés de chiffrement contrats/justificatifs. |
| `application-local.properties` | Base H2 locale (fichier dans le dossier utilisateur), port `8081`, mode PostgreSQL pour compatibilité SQL. |

### 3.2 Comptes utilisateurs (`accounts/`)

Gère la création, la modification, l'activation/désactivation et la hiérarchie des comptes.

| Fichier | Rôle | Spécialité |
|---|---|---|
| `AccountRole.java` | Énumération des rôles | `COMPTABLE`, `ADJOINT_COMPTABLE`, `DIRECTEUR_GENERAL`, `ADMINISTRATEUR`. Un compte sans rôle est traité comme utilisateur standard. |
| `AccountStatus.java` | Énumération des statuts | `ACTIVE`, `DISABLED`. |
| `UserAccount.java` | Entité JPA d'un compte | Contient identité, e-mail, service, poste, rôle, mot de passe haché, supérieur N+1/N+2, statut, dates, version. Valide les champs obligatoires et gère activation/désactivation. |
| `UserAccountRepository.java` | Accès base pour les comptes | Requêtes Spring Data : existence par identifiant/e-mail/rôle, recherche par e-mail/identifiant. |
| `AccountRequest.java` | Objet de transfert pour créer/modifier un compte | Record simple contenant tous les champs d'un compte. |
| `AccountPayload.java` | Corps JSON reçu par l'API | Ajoute les validations (`@NotBlank`, `@Email`, etc.) et convertit en `AccountRequest`. |
| `AccountResponse.java` | Réponse JSON d'un compte | Masque le mot de passe ; inclut les noms des supérieurs. |
| `AccountNotFoundException.java` | Exception « compte introuvable » | Transformée en HTTP 404 par le gestionnaire d'erreurs. |
| `AccountService.java` | Logique métier des comptes | Création, mise à jour, désactivation/réactivation, changement de mot de passe. Vérifie les doublons et la cohérence de la hiérarchie (N+2 doit être le N+1 du N+1 ; DG n'a pas de supérieur). |
| `AccountController.java` | Endpoints REST `/api/accounts` | Liste, création, modification, désactivation/réactivation. Interdit de modifier son propre rôle/hiérarchie ou de se désactiver soi-même. Réservé aux administrateurs (sécurité). |

### 3.3 Sécurité (`security/`)

| Fichier | Rôle | Spécialité |
|---|---|---|
| `SecurityConfig.java` | Configuration Spring Security | BCrypt pour les mots de passe, session HTTP, CSRF par cookie, règles d'accès par rôle, réponses 401/403 en JSON, déconnexion. |
| `AccountUserDetailsService.java` | Chargement de l'utilisateur pour l'authentification | Recherche par e-mail ; désactive le compte si inactif ou sans mot de passe. |
| `AuthController.java` | Endpoints d'authentification | Connexion, utilisateur courant, déconnexion, réauthentification (pour visas), gestion de la signature PNG (validation, nettoyage, limite 256 Ko / 2400×600). |
| `InitialAdministratorBootstrap.java` | Crée le premier administrateur au démarrage | Lit `APP_BOOTSTRAP_ADMIN_EMAIL/PASSWORD` ; ne crée le compte que s'il n'existe pas déjà d'administrateur. |
| `DemoAccountBootstrap.java` | Crée/met à jour les comptes de démo (profil local) | Profils Direction, admin, comptabilité, adjoint, Carlos (chef développeur), Christophe (designer) et Sandrine (cheffe de projet); Christophe et Sandrine dépendent de Carlos (N+1), rattaché à M. César (N+2). Désactive les comptes démo si la démo est désactivée. |
| `DemoAccountsController.java` | Liste les comptes démo pour l'écran de connexion | Uniquement en profil local et si la démo est activée. |
| `UserSignature.java` | Entité de la signature PNG d'un utilisateur | Stocke les octets PNG et la date de mise à jour. |
| `UserSignatureRepository.java` | Accès base pour les signatures | CRUD simple par identifiant utilisateur. |

### 3.4 Caisse (`cash/`)

Cœur métier : demandes, bons, visas, décaissement, justificatifs, régularisation.

#### Énumérations

| Fichier | Valeurs | Sens |
|---|---|---|
| `CashRequestStatus.java` | `DRAFT`, `SOUMISE`, `VALIDEE_N1`, `VALIDEE`, `A_CORRIGER`, `MONTANT_ARRETE`, `BON_EMIS`, `VISA_REFUSE`, `DECAISSEE`, `EN_REGULARISATION`, `REGULARISEE`, `ANNULEE` | Étapes du circuit d'une demande. |
| `CashFundOrigin.java` | `BNI`, `NSIA`, `BRIDGE_BANK`, `BGDI`, `BICICI`, `DG` | Origine des fonds décaissés. |
| `CashVoucherType.java` | `SORTIE`, `ENTREE` | Type de bon (seul `SORTIE` est utilisé pour l'émission initiale). |
| `CashVoucherStatus.java` | `EMIS`, `DECAISSE` | État d'un bon. |
| `CashVoucherVisaType.java` | `DIRECTION`, `CAISSE`, `BENEFICIAIRE` | Les trois visas obligatoires. |
| `SupportingDocumentStatus.java` | `A_CONTROLER`, `ACCEPTE`, `REJETE` | État d'un justificatif. |

#### Entités

| Fichier | Rôle | Spécialité |
|---|---|---|
| `CashRequest.java` | Demande de caisse | Contient mission, montants, statut, validateurs, dates de décision. Implémente les transitions d'état (soumission, validation N1/N2, rejet, annulation, arrêt du montant, émission de bon, décaissement, régularisation) avec vérification du statut courant. |
| `CashRequestEvent.java` | Événement d'historique | Trace chaque action (acteur, statut avant/après, motif, date). L'acteur peut être nul pour les actions automatiques (régularisation). |
| `CashRequestEventRepository.java` | Accès base aux événements | Recherche par demande, triée par date décroissante. |
| `CashRequestRepository.java` | Accès base aux demandes | Toutes triées par date ; visibles pour un utilisateur (demandeur ou validateur). |
| `CashVoucher.java` | Bon de caisse | Numéro, type, demande liée, bénéficiaire, montant, montant en lettres, motif, catégorie, statut, émetteur, visas, décaissement (date + origine). |
| `CashVoucherVisa.java` | Visa apposé sur un bon | Type, signataire, date, réauthentification, copie de la signature PNG. Contraintes d'unicité : un visa par type et par signataire. |
| `CashVoucherVisaRepository.java` | Accès base aux visas | Recherche par bon, existence par type/signataire. |
| `CashVoucherSequence.java` | Séquence des numéros de bons | Incrémente le prochain numéro et réserve l'actuel ; initialisable. |
| `CashVoucherSequenceRepository.java` | Accès base à la séquence | Verrou pessimiste pour éviter les doublons de numéros. |
| `CashEntryVoucher.java` | Bon d'entrée (retour de fonds) | Numéro `E-xxxxxx`, demande liée, bon source, bénéficiaire, montant, montant en lettres, motif, date de retour, émetteur, autorisation éventuelle, date d'émission, solde avant/après, origine des fonds. |
| `CashEntryVoucherVisa.java` | Visa apposé sur un bon d'entrée | Type, signataire, date, réauthentification, copie de la signature PNG. |
| `CashEntryVoucherVisaRepository.java` | Accès base aux visas de bon d'entrée | Recherche/existence par bon et type/signataire. |
| `CashEntryVoucherRepository.java` | Accès base aux bons d'entrée | Recherche par demande, verrouillage, sommes. |
| `CashReturnAuthorization.java` | Autorisation de retour par le DG | Montant autorisé, motif, auteur, date, consommation. |
| `CashReturnAuthorizationRepository.java` | Accès base aux autorisations | Recherche par demande, verrouillage par id/demande. |
| `CashRegularizationScheduler.java` | Démarrage de la régularisation des missions | À la fin de la mission, passe la demande décaissée en régularisation ; rattrape aussi les échéances manquées à l’arrêt du serveur et trace l’action système. |
| `SupportingDocument.java` | Justificatif de caisse | Métadonnées (type, montant, date), contenu chiffré AES-GCM, statut, remplacement, auteur, dates. |
| `SupportingDocumentRepository.java` | Accès base aux justificatifs | Recherche par demande, sommes par statut. |
| `SupportingDocumentService.java` | Dépôt et contrôle des justificatifs | Vérifie les droits, analyse ClamAV, chiffre les pièces en AES-GCM, conserve les rejets/remplacements et trace les événements. |
| `SupportingDocumentEncryptionMigration.java` | Migration de stockage des justificatifs | Chiffre par lots les fichiers historiques non chiffrés au démarrage ; exige la clé de chiffrement si des pièces restent à migrer. |
| `SupportingDocumentCrypto.java` | Chiffrement des pièces de caisse | Utilise une clé AES de 32 octets définie par `APP_CASH_DOCUMENTS_ENCRYPTION_KEY`. |
| `AntivirusScanner.java` / `ClamAvScanner.java` | Analyse antivirus | Vérifie la disponibilité ClamAV (`PING`) et analyse les fichiers (`INSTREAM`). |

#### Payloads et réponses

| Fichier | Rôle |
|---|---|
| `CashRequestPayload.java` | Corps JSON d'une demande (mission, dates, catégorie, montant, pièce jointe). |
| `CashRequestReasonPayload.java` | Corps JSON contenant un motif (rejet/annulation). |
| `CashRequestAccountingPayload.java` | Corps JSON pour l'arrêt du montant (montant définitif + motif). |
| `CashDisbursementPayload.java` | Corps JSON pour le décaissement (date + origine). |
| `CashVoucherPayload.java` | Corps JSON pour l'émission d'un bon (motif). |
| `CashVoucherVisaPayload.java` | Corps JSON pour un visa (type). |
| `CashEntryVoucherPayload.java` | Corps JSON pour un bon d'entrée (montant, motif, date, origine, autorisation). |
| `CashReturnAuthorizationPayload.java` | Corps JSON pour une autorisation de retour (montant + motif). |
| `CashRequestResponse.java` | Réponse JSON d'une demande. |
| `CashVoucherResponse.java` | Réponse JSON d'un bon (inclut la liste des visas avec signatures en base64). |
| `CashEntryVoucherResponse.java` | Réponse JSON d'un bon d'entrée. |
| `CashReturnAuthorizationResponse.java` | Réponse JSON d'une autorisation de retour. |
| `CashRegularizationResponse.java` | Réponse JSON du résumé de régularisation (montants, solde, listes). |
| `SupportingDocumentResponse.java` | Réponse JSON d'un justificatif. |

#### Services et contrôleur

| Fichier | Rôle | Spécialité |
|---|---|---|
| `CashRequestService.java` | Logique métier des demandes | Création/modification, soumission, validation N1/N2, rejet (motif minimum), annulation, arrêt du montant (comptabilité), visibilité selon le rôle, enregistrement des événements. |
| `CashVoucherService.java` | Logique métier des bons | Émission numérotée, signature (visa) avec vérification de permissions et de réauthentification, décaissement. Vérifie que les 3 visas distincts sont présents. |
| `CashReturnService.java` | Logique métier de la régularisation | Résumé, autorisation de retour par le DG, émission de bons d'entrée, signature de leurs visas, contrôle d'acceptation de justificatifs, clôture de régularisation. |
| `CashRequestController.java` | Endpoints REST `/api/cash-requests` | Expose toutes les actions : liste, détail, création, modification, soumission, validation, rejet, annulation, arrêt du montant, bon, visa, décaissement, régularisation, bons d'entrée, autorisations, clôture. |
| `SupportingDocumentController.java` | Endpoints REST `/api/cash-requests/{id}/supporting-documents` | Liste, dépôt (multipart), contrôle (acceptation/rejet), téléchargement. |
| `SupportingDocumentAvailabilityController.java` | Endpoint `/api/supporting-documents/availability` | Indique si le dépôt est activé (ClamAV opérationnel + clé configurée). |
| `FrenchAmountInWords.java` | Convertit un montant en lettres | Génère le montant en français avec « francs CFA » (jusqu'à 999 milliards). |
| `ApiExceptionHandler.java` | Gestionnaire d'erreurs REST | Transforme les exceptions de validation/métier en réponses JSON lisibles. |
| `CashRequestNotFoundException.java` | Exception « demande introuvable » | Transformée en HTTP 404. |

### 3.5 Contrats de travail (`contracts/`)

| Fichier | Rôle | Spécialité |
|---|---|---|
| `ContractStatus.java` | Statut d'un contrat | `BROUILLON`, `ACTIF`. |
| `ContractType.java` | Type de contrat | `STAGE`, `CDD`, `CDI`. |
| `EmploymentContract.java` | Entité contrat | Employé, type, dates, poste, service, responsable, statut, version. |
| `ContractDocument.java` | Document PDF signé | Version, contenu chiffré AES-GCM, métadonnées, dates. |
| `ContractDocumentCrypto.java` | Chiffrement des PDF de contrat | Utilise `APP_CONTRACTS_ENCRYPTION_KEY`. |
| `ContractDocumentAvailability.java` | Disponibilité du dépôt | Indique si ClamAV et la clé sont opérationnels. |
| `ContractEvent.java` / `ContractEventRepository.java` | Historique du contrat | Consultations, modifications, dépôts. |
| `ContractPayload.java` | Corps JSON d'un contrat | Validation des champs. |
| `ContractResponse.java` | Réponse JSON d'un contrat | |
| `ContractDocumentResponse.java` | Réponse JSON d'un document | |
| `ContractEmployeeResponse.java` | Réponse JSON d'un employé | |
| `ContractService.java` | Logique métier | Création/modification selon les rôles, dépôt de PDF (ClamAV + chiffrement), versions, historique, téléchargement. |
| `ContractController.java` | Endpoints REST `/api/contracts` | Liste, création, modification, documents, dépôt, téléchargement, historique, disponibilité. |

### 3.6 Base de données (`resources/db/migration/`)

Scripts Flyway exécutés au démarrage, dans l'ordre :

| Script | Contenu |
|---|---|
| `V1__create_user_accounts.sql` | Table `user_accounts` (comptes, rôles, hiérarchie). |
| `V2__create_cash_requests.sql` | Table `cash_requests` (demandes de base). |
| `V3__add_account_password_hash.sql` | Ajout du mot de passe haché aux comptes. |
| `V4__add_cash_request_workflow.sql` | Ajout des colonnes de workflow, table `cash_request_events`. |
| `V5__create_cash_vouchers.sql` | Tables des séquences et des bons de caisse. |
| `V6__add_voucher_visas_and_disbursement.sql` | Tables `user_signatures` et `cash_voucher_visas`, colonnes de décaissement. |
| `V7__add_cash_supporting_documents.sql` | Métadonnées et contenu des justificatifs de caisse. |
| `V8__create_employment_contracts.sql` | Contrats de travail, documents et historique. |
| `V9__encrypt_cash_supporting_documents.sql` | Marque les justificatifs historiques à chiffrer. |
| `V10__record_automatic_cash_regularization.sql` | Rend l'acteur nullable sur les événements (actions automatiques). |
| `V11__add_cash_entry_vouchers_and_return_authorizations.sql` | Tables `cash_return_authorizations` et `cash_entry_vouchers`. |
| `V12__add_cash_entry_voucher_fund_origin_and_visas.sql` | Origine des fonds sur les bons d'entrée et table `cash_entry_voucher_visas`. |

---

## 4. FRONTEND

Dossier : `frontend/src/`

### 4.1 Entrée et navigation

| Fichier | Rôle | Spécialité |
|---|---|---|
| `main.jsx` | Point d'entrée React | Monte l'application dans `#root`. |
| `App.jsx` | Composant racine | Gère la session (vérification au démarrage), l'utilisateur connecté, la page active, la déconnexion. |
| `app/navigation.js` | Configuration de navigation | Définit les groupes et items du menu (Vue d'ensemble, Demandes, Bons, Justificatifs, Contrats, Équipe). |
| `vite.config.js` | Configuration Vite | Proxy `/api` vers le backend ; transmet le secret démo au navigateur en mode dev. |
| `index.html` | Page HTML de base | Titre, langue française, point de montage. |

### 4.2 API et HTTP

| Fichier | Rôle | Spécialité |
|---|---|---|
| `features/api/http.js` | Client HTTP générique | Récupère le jeton CSRF avant chaque requête non-GET, envoie les cookies, gère les erreurs. |
| `features/auth/authApi.js` | Appels API auth | Utilisateur courant, comptes démo, connexion, déconnexion, réauthentification, signature. |
| `features/accounts/accountsApi.js` | Appels API comptes | Liste, création, modification, activation/désactivation. |
| `features/requests/api/cashRequestsApi.js` | Appels API demandes/bons | Toutes les opérations de la caisse (demandes, validation, comptabilité, bons, visas, décaissement, régularisation, bons d'entrée, autorisations). |
| `features/requests/api/supportingDocumentsApi.js` | Appels API justificatifs | Liste, dépôt, contrôle, téléchargement, disponibilité. |
| `features/contracts/contractsApi.js` | Appels API contrats | Liste, création, modification, documents, dépôt, téléchargement, historique, disponibilité. |

### 4.3 Authentification

| Fichier | Rôle | Spécialité |
|---|---|---|
| `features/auth/LoginPage.jsx` | Écran de connexion | Formulaire e-mail/mot de passe + connexion rapide aux comptes démo (en dev uniquement). |

### 4.4 Tableau de bord (`dashboard/`)

| Fichier | Rôle | Spécialité |
|---|---|---|
| `pages/OverviewPage.jsx` | Page d'accueil | Charge les demandes, calcule « demandes en cours » et « à traiter », affiche les cartes métriques et raccourcis. |
| `components/WelcomePanel.jsx` | Panneau de bienvenue | Illustration décorative + message. |
| `components/MetricCard.jsx` | Carte indicateur | Affiche icône, valeur, titre, description, état de chargement. |
| `components/QuickAccessPanel.jsx` | Raccourcis | Boutons vers Demandes et Contrats. |
| `components/ActivityPanel.jsx` | Activité récente | État vide (à connecter plus tard). |

### 4.5 Comptes (`accounts/`)

| Fichier | Rôle | Spécialité |
|---|---|---|
| `AccountsPage.jsx` | Gestion de l'équipe | Formulaire de création/modification de compte (rôle, hiérarchie), liste avec activation/désactivation. |

### 4.6 Demandes de caisse (`requests/`)

| Fichier | Rôle | Spécialité |
|---|---|---|
| `pages/CashRequestsPage.jsx` | Page principale de la caisse | Liste et actions du circuit des demandes; aperçu imprimable du bon au format papier avec montant, bénéficiaire, motif et visas; vue « Bons de caisse ». |
| `pages/SupportingDocumentsPage.jsx` | Page des justificatifs | Dépôt et suivi du contrôle des pièces d'une mission décaissée. |
| `pages/RegularizationPanel.jsx` | Panneau de régularisation | Résumé des montants, émission de bons d'entrée, visas, autorisations du DG, clôture. |
| `components/RequestForm.jsx` | Formulaire de demande | Saisie de la mission, prévisualisation avant enregistrement. |
| `components/RequestPreview.jsx` | Aperçu de la demande | Affiche les valeurs saisies, demande confirmation avant création. |

### 4.7 Contrats (`contracts/`)

| Fichier | Rôle | Spécialité |
|---|---|---|
| `ContractsPage.jsx` | Page des contrats de travail | Création/modification, dépôt de PDF signé, consultation, historique. |

### 4.8 Composants partagés

| Fichier | Rôle | Spécialité |
|---|---|---|
| `components/layout/AppLayout.jsx` | Cadre de l'application | Sidebar + Topbar + contenu. |
| `components/layout/Sidebar.jsx` | Menu latéral | Navigation par groupes ; adapte les items selon le rôle (admin voit « Équipe »). |
| `components/layout/Topbar.jsx` | Barre supérieure | Titre de page, profil, déconnexion. |
| `components/icons/Icon.jsx` | Icônes | Mappe des noms lisibles vers des icônes Ant Design. |
| `features/workspace/pages/PlaceholderPage.jsx` | Page « en préparation » | Affichée pour les modules non encore réalisés. |

---

## 5. Tests

Dossier : `backend/src/test/`

| Fichier | Ce qu'il teste |
|---|---|
| `BackendApplicationTests.java` | Le contexte Spring démarre correctement. |
| `accounts/AccountServiceTests.java` | Création de comptes, hiérarchie, désactivation, doublons. |
| `cash/CashRequestControllerTests.java` | Circuit complet : création, validation N1/N2, montant, émission de bon numéroté, visas distincts, décaissement. |
| `cash/CashRegularizationTests.java` | Régularisation : bons d'entrée, visas, autorisations, clôture. |
| `cash/FrenchAmountInWordsTests.java` | Conversion montant → lettres, rejets des valeurs invalides. |
| `cash/SupportingDocumentEncryptionMigrationTests.java` | Migration de chiffrement des justificatifs. |
| `contracts/ContractServiceTests.java` | Règles métier des contrats. |
| `contracts/ContractDocumentCryptoTests.java` | Chiffrement/déchiffrement des PDF de contrat. |
| `security/AuthControllerTests.java` | Réauthentification, signature PNG, accès interdit à l'administrateur. |
| `security/DemoAccountBootstrapTests.java` | Mise à jour d'un compte démo existant par identifiant. |

---

## 6. Démarrage rapide

1. Copier `backend/.env.example` → `backend/.env` et définir l'admin initial (e-mail + mot de passe ≥ 12 caractères).
2. Démarrer le backend : `cd backend && .\mvnw.cmd spring-boot:run` (port 8081).
3. Démarrer le frontend : `cd frontend && npm run dev` (port 5173).
4. Ouvrir `http://localhost:5173`.

---

## 7. Statuts et circuit d'une demande (résumé)

```
BROUILLON → SOUMISE → (VALIDEE_N1 si N+1≠DG) → VALIDEE → MONTANT_ARRETE → BON_EMIS → (visas) → DECAISSEE → EN_REGULARISATION → REGULARISEE
   ↘ A_CORRIGER (rejet) → resoumission possible
   ↘ ANNULEE (annulation possible tant que non décaissée)
```

- **Brouillon** : créé par un utilisateur actif (sauf admin).
- **Soumise** : transmise au N+1.
- **Validée N+1 / Validée** : selon que le N+1 est le DG (une seule validation) ou non.
- **À corriger** : rejetée par un validateur ; le demandeur corrige et resoumet.
- **Montant arrêté** : la comptabilité fixe le montant définitif.
- **Bon émis** : un bon numéroté est créé (montant en lettres automatique).
- **Décaissée** : après les 3 visas distincts et l'enregistrement de l'origine des fonds.
- **En régularisation** : automatique le lendemain de la fin de mission ; saisie de justificatifs et émission de bons d'entrée pour solder le solde.
- **Régularisée** : clôturée lorsque le solde est à zéro et qu'aucun justificatif n'attend le contrôle.

---

## 8. Points clés à retenir (pour expliquer à quelqu'un)

- Le projet est une **application web complète** : frontend React + backend Spring Boot + base de données.
- Le **backend** contient toute la logique métier et la sécurité ; le **frontend** est une interface.
- Les **comptes** ont des rôles et une hiérarchie N+1/N+2.
- Les **demandes de caisse** suivent un circuit de validation strict, historisé.
- Les **bons de caisse** sont numérotés, signés par 3 personnes différentes, puis décaissés.
- La **régularisation** solde les fonds décaissés via des bons d'entrée et des justificatifs.
- Les **contrats de travail** sont créés en brouillon puis activés par le dépôt d'un PDF signé.
- La sécurité utilise **session + CSRF + BCrypt**, avec réauthentification pour les visas.
- La base de données est gérée par **Flyway** (migrations versionnées).
- Les fichiers téléversés sont analysés par **ClamAV** et chiffrés en **AES-GCM**.

---

*Document généré pour le projet Caisse Contrat.*
## Mise a jour 2026-11-19
- Justificatifs de depenses : defilement automatique vers le message d'erreur lorsqu'un montant accepte depasserait le solde restant a regulariser.
