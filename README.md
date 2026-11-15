# Caisse Contrat — démarrage local

## Avant de commencer

Installez **Java 21 ou une version ultérieure** et **Node.js avec npm**.
Le backend utilise H2 en local : il n'est pas nécessaire d'installer ou de
démarrer PostgreSQL pour lancer l'application.

## Démarrer l'application

Ouvrez deux fenêtres PowerShell. Dans chacune, commencez depuis le dossier
`C:\Caisse_Contrat`.

**Fenêtre 1 — backend**

```powershell
cd C:\Caisse_Contrat\backend
.\mvnw.cmd spring-boot:run
```

Au premier lancement, Maven peut télécharger les dépendances. Attendez le
message `Started BackendApplication` avant d'utiliser le backend. L'API répond
sur `http://localhost:8081`.

**Fenêtre 2 — frontend**

```powershell
cd C:\Caisse_Contrat\frontend\frontend
npm run dev
```

Ouvrez dans le navigateur l'adresse affichée par Vite, en général
`http://localhost:5173`.

## Premier compte administrateur

La connexion est obligatoire. Pour préparer le premier compte, copiez
`backend\.env.example` vers `backend\.env`, puis saisissez l'adresse e-mail et
un mot de passe d'au moins 12 caractères dans ce nouveau fichier :

```powershell
Copy-Item .env.example .env
notepad .env
```

Exécutez ces commandes depuis `C:\Caisse_Contrat\backend`. Le fichier `.env`
est lu automatiquement par le backend et exclu de Git. Ne le partagez pas et
ne copiez pas son contenu dans un dépôt ou une capture d'écran. Il contient le
mot de passe en texte lisible sur votre ordinateur ; choisissez un mot de passe
unique pour cette application.

Exemple de format (remplacez les valeurs, ne gardez pas de mot de passe fictif) :

```dotenv
APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
APP_BOOTSTRAP_ADMIN_PASSWORD=remplacer-par-un-secret-de-12-caracteres-minimum
```

Le compte est créé une seule fois, uniquement si aucun administrateur n'existe.
Après le démarrage, ouvrez le frontend et connectez-vous avec ces identifiants.
Ensuite, un administrateur peut créer et gérer les autres comptes depuis
**Équipe**. Ces comptes et leurs mots de passe hachés restent enregistrés dans
la base locale, pas dans `.env`.
Si les variables ne sont pas définies, le serveur démarre sans créer de compte
et indique dans son journal comment activer le premier administrateur.

## Circuit initial des demandes de caisse

Un utilisateur actif (sauf administrateur) peut créer un brouillon et le
soumettre. La demande suit ensuite les responsables N+1 et N+2 enregistrés sur
son compte ; si le N+1 est le DG, une seule validation est demandée. Après les
validations, la comptabilité fixe le montant définitif. Les actions sont
historisées et les demandes ne sont plus supprimées physiquement. La longueur
minimale du motif de rejet est réglée par `APP_CASH_REJECTION_MIN_LENGTH`
(10 caractères par défaut).

Après l’arrêt du montant, le comptable peut émettre un bon de sortie à partir
de la demande. Le numéro est séquentiel, conservé en cas d’annulation future,
et le montant en lettres est généré automatiquement. La première valeur est
`000001` en local ; avant une mise en service avec reprise des carnets papier,
définissez `APP_CASH_VOUCHER_FIRST_NUMBER` à la valeur `PAR-14 + 1` validée par
l’équipe. Le numéro n’est consommé que si l’émission est enregistrée.

L’aperçu du bon reprend le formulaire papier Digin Factory et peut être imprimé :
type de bon, date, numéro, montant, bénéficiaire, montant en lettres, motif et
trois emplacements de visas.

Le bon comporte trois visas personnels : Direction (N+2, ou DG si N+1 est le
DG), Caisse (comptable ou adjoint autorisé) et bénéficiaire. Chaque signataire
enregistre sa propre signature PNG et confirme son identité par son mot de
passe juste avant de viser. La signature est nettoyée, limitée à 256 Ko et
2 400 × 600 pixels, puis copiée dans le visa ; une modification ultérieure du
profil ne change pas les visas déjà posés. Les trois rôles doivent être tenus
par trois personnes distinctes et le bénéficiaire signe en dernier.

Après les trois visas, le comptable ou l’adjoint autorisé peut enregistrer la
date et l’origine des fonds (BNI, NSIA, Bridge Bank, BGDI, BICICI ou DG). La
demande passe alors à « Décaissée ». Le délai de réauthentification est réglé
par `APP_CASH_VISA_REAUTH_MINUTES` (5 minutes par défaut ; PAR-02 reste à
confirmer).

Au lendemain de la date de fin de mission, une demande décaissée passe
automatiquement à « En régularisation ». Le backend rattrape aussi les missions
arrivées à échéance pendant qu’il était arrêté et journalise cette transition
comme une action automatique.

La désignation du valideur des demandes du DG et le téléversement obligatoire des
pièces requises avant soumission restent à implémenter ou à paramétrer. Après un
décaissement, la demande passe en « En régularisation » : la comptabilité émet
des bons d’entrée (numérotés `E-000001`, montant en lettres, trois visas
Direction/Caisse/Bénéficiaire) pour solder les sommes non justifiées, avec un
premier retour libre puis des retours suivants soumis à l’autorisation du
Directeur général. La régularisation se clôt lorsque le solde est à zéro et
qu’aucun justificatif n’attend le contrôle. Le bénéficiaire peut déposer
plusieurs justificatifs (PDF, JPG ou PNG, 10 Mo maximum) ; la comptabilité peut
les accepter ou les rejeter avec un motif, et un justificatif rejeté reste dans
l’historique lorsqu’il est remplacé. L’analyse ClamAV est requise et bloquée par
défaut : configurez `APP_ANTIVIRUS_CLAMD_HOST` (et éventuellement
`APP_ANTIVIRUS_CLAMD_PORT`) dans `backend\.env`, avec un service ClamAV joignable
qui répond à `PING` et `INSTREAM`. Tant que ce service ne répond pas, aucun fichier
n’est enregistré. L’authentification locale par mot de passe sert actuellement
de confirmation d’identité pour les visas ; l’intégration Entra ID demeure à
valider avant la mise en service.

Les justificatifs sont chiffrés en AES-GCM avant leur enregistrement. Configurez
une clé Base64 aléatoire de 32 octets dans
`APP_CASH_DOCUMENTS_ENCRYPTION_KEY` (clé distincte de celle des contrats), à
conserver hors du dépôt et à sauvegarder séparément de la base. Au premier
démarrage après cette mise à jour, les justificatifs déjà présents sont chiffrés
automatiquement; le backend refuse de démarrer si ces documents existent et que
la clé n’est pas configurée.

## Contrats de travail

La rubrique **Contrats de travail** est accessible aux comptes autorisés par
leur rôle. La comptabilité peut créer et modifier les contrats hors son propre
contrat ; la Direction peut modifier ceux du comptable et de l’adjoint. Un
employé ne consulte que son contrat, sans les notes internes. L’administrateur
n’a pas accès à ce module. Ces contrôles sont appliqués par l’API.

Un contrat brouillon contient l’employé, le type (stage, CDD ou CDI), les dates,
le poste, le service et le responsable. La date de fin est obligatoire pour un
stage ou un CDD et interdite pour un CDI. La période d’essai (durée saisie avec
son unité) et le lieu sont facultatifs. L’ajout d’un PDF signé active le contrat
et conserve les dépôts successifs comme versions distinctes (PDF de 10 Mo
maximum).

Le dépôt d’un PDF est bloqué si ClamAV n’est pas opérationnel ou si la clé
`APP_CONTRACTS_ENCRYPTION_KEY` n’est pas définie. Générer une clé Base64
aléatoire de 32 octets avec Node.js, puis la conserver uniquement dans
`backend\.env` (ne pas la publier et en conserver une sauvegarde séparée de la
base) :

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Sans cette clé, les documents ne peuvent pas être déposés ni déchiffrés. Une
perte de clé rend les PDF déjà stockés irrécupérables. Le module enregistre les
consultations, modifications et dépôts de documents dans son historique.

Les processus de renouvellement, conversion en CDI, fin/rupture, avenants,
congés, alertes et reporting restent à finaliser. Les seuils PAR-17 à PAR-19 et
les règles de calcul RH sont à valider et ne sont pas supposés par le logiciel.

## Comptes de démonstration

Pour une présentation locale, ouvrez `backend\.env` et activez les comptes
démo avec un mot de passe réservé à la démonstration (12 caractères minimum) :

```dotenv
APP_DEMO_ACCOUNTS_ENABLED=true
APP_DEMO_ACCOUNT_PASSWORD=un-mot-de-passe-de-demo-unique
```

Redémarrez ensuite le backend et le frontend. L'écran de connexion proposera
les profils de Direction, administrateur, comptabilité, adjoint comptable,
Carlos (chef développeur), Christophe (designer) et Sandrine (cheffe de projet).
Christophe et Sandrine sont rattachés à Carlos (N+1), lui-même rattaché à
M. César (N+2). Cliquer sur un profil connecte immédiatement avec ce mot de passe,
également affiché sous la liste des profils. Tous les comptes démo partagent
ce même mot de passe. Les données de comptes sont
créées en base locale et seul leur hash y est conservé. En développement,
Vite transmet le secret démo au navigateur pour permettre la connexion rapide :
utilisez donc exclusivement un mot de passe fictif, jamais un secret réel ou
réutilisé. Pour désactiver la démo, mettez `APP_DEMO_ACCOUNTS_ENABLED=false`
puis redémarrez le backend et le frontend. Le backend local désactive alors les
comptes de démonstration déjà créés, sans toucher aux comptes réels.

## Si un port est déjà utilisé

Le backend utilise le port **8081** pour éviter le port **8080**, déjà occupé
sur le poste lors de la configuration. Si 8081 est lui aussi occupé, choisissez
un autre port temporaire dans la même fenêtre PowerShell avant de lancer Maven :

```powershell
$env:SERVER_PORT = "8082"
.\mvnw.cmd spring-boot:run
```

Remplacez `8082` par un port libre. Ce réglage ne modifie aucun fichier et
n'arrête pas le programme qui utilise déjà l'autre port.

## Base de données locale

Au démarrage, Spring exécute automatiquement les migrations SQL du backend. La
base H2 est enregistrée dans le dossier personnel de l'utilisateur sous le nom
`caisse-contrat.mv.db`. Les données sont donc conservées après l'arrêt de
l'application.

La configuration PostgreSQL reste disponible pour les environnements qui
l'utilisent : activez un profil autre que `local` et fournissez
`DATABASE_URL`, `DATABASE_USERNAME` et `DATABASE_PASSWORD`. Ne mettez pas de
mot de passe réel dans les fichiers du projet.
#   C a i s s e _ C o n t r a t  
 