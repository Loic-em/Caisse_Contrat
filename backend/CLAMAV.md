# ClamAV local

Depuis ce dossier, démarrer le moteur avec Docker Desktop ouvert :

```powershell
docker compose -f docker-compose.clamav.yml up -d
```

Le port clamd 3310 est lié à `127.0.0.1` uniquement. Le backend doit utiliser
`APP_ANTIVIRUS_CLAMD_HOST=127.0.0.1` et `APP_ANTIVIRUS_CLAMD_PORT=3310`.
Le dépôt des justificatifs ne devient disponible qu'après le démarrage de
ClamAV et si `APP_CASH_DOCUMENTS_ENCRYPTION_KEY` est correctement configurée.

Pour vérifier le conteneur :

```powershell
docker compose -f docker-compose.clamav.yml ps
```
