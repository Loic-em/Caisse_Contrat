# Référence fonctionnelle persistante

État du code examiné le 17 novembre 2026. Cette note résume les documents fournis et les demandes explicites de Loice afin de servir de contexte aux prochains travaux. Les documents originaux restent les références détaillées.

## Documents de référence

- `C:\Users\Loice\Documents\Cahier des charges – Logiciel de gestion interne (Caisse et Contrats de travail).pdf` — version 1.2, datée du 07/11/2026.
- `C:\Users\Loice\Documents\Specifications_fonctionnelles_gestion_interne.docx` — version 0.1, datée du 07/11/2026, annoncée comme fondée sur le cahier des charges v1.2.

### Comment les interpréter

L’utilisateur a explicitement demandé d’intégrer au projet les règles des documents même lorsqu’elles portent la mention `[À valider]` ou « à vérifier ». Elles sont donc à traiter comme des exigences approuvées. Les paramètres chiffrés qui restent « à définir » doivent être configurables ; aucune valeur métier ne doit être inventée. En cas de contradiction entre deux règles, retenir l’interprétation cohérente avec le parcours et les rôles déjà confirmés ci-dessous, puis la documenter.

## Demandes explicites de l’utilisateur à conserver

- Utiliser l’identité visuelle officielle de DIGIN FACTORY, notamment son bleu et son cyan, et ne pas réintroduire la palette vert/doré.
- Chaque compte employé non administrateur doit pouvoir être rattaché à son contrat. Il faut repérer et traiter les comptes actifs sans contrat.
- Accès aux contrats confirmé : chaque employé consulte uniquement son propre contrat ; le comptable consulte tous les contrats et peut gérer ceux de l’équipe sauf le sien ; le DG consulte tous les contrats et gère ceux du comptable et de l’adjoint comptable ; l’administrateur n’a aucun accès aux contrats. Les autres rôles voient uniquement leur contrat et ne gèrent pas les contrats.
- La fiche contrat doit gérer le salaire de base, les primes fixes mensuelles, le détail des primes, les indemnités et les autres avantages.
- Retirer temporairement les informations complémentaires du formulaire de demande de caisse et des aperçus ; garder les anciennes données en base pour éviter leur perte.
- Nommer l’espace après décaissement « Justificatifs de dépenses » et le distinguer des ordres de mission et autres documents préparatoires, qui ne sont pas gérés par cet espace.
- Désactiver les dépôts de fichiers dès que ClamAV **ou** la clé de chiffrement n’est pas opérationnel(le). La consultation et le contrôle des pièces déjà déposées doivent rester accessibles.
- Achever le parcours de caisse jusqu’aux justificatifs et à la régularisation, avec les droits adaptés à chaque profil.

## Socle métier des documents

### Caisse

- Une demande par bénéficiaire et par mission, en FCFA ; création, brouillon, soumission, correction après rejet, annulation autorisée et historique.
- Validation par N+1 puis N+2, avec motif de rejet obligatoire et séparation des tâches, y compris pour les demandes de la Direction.
- Contrôle comptable, montant définitif, motif d’écart, bon de caisse numéroté et imprimable, trois visas personnels, décaissement conditionné aux visas et à l’origine des fonds.
- Après mission : pièces multiples, contrôle comptable, remplacement historisé d’une pièce rejetée, retours de fonds par bon d’entrée, traitement des dépassements, clôture et réouverture motivée.
- Les rappels J-2, J-1, jour J, délais de justification, notifications, reporting et journal d’activité font partie du périmètre attendu.

### Contrats de travail

- Contrats Stage, CDD et CDI rattachés à un compte employé non administrateur ; chaque employé consulte son propre contrat. Le comptable consulte tous les contrats et gère ceux de l’équipe sauf le sien ; le DG consulte tous les contrats et gère ceux du comptable et de l’adjoint ; les autres profils n’accèdent qu’à leur propre contrat ; l’administrateur n’accède à aucun contrat.
- Fiche avec dates, poste, service, responsable, période d’essai et lieu ; le PDF signé et ses versions sont conservés.
- Le périmètre prévu comprend aussi ancienneté, congés, alertes d’échéance, avenants/renouvellements, recherche, reporting et exports. Les règles RH et légales figurant dans les documents sont à mettre en œuvre ; les paramètres qui ne sont pas chiffrés restent configurables.

### Transverse

- Comptes actifs, rôles, hiérarchie N+1/N+2 ; droits contrôlés côté serveur ; horodatage en fuseau Africa/Abidjan ; fichiers PDF/JPG/PNG avec antivirus ; journal append-only ; aucune suppression définitive.
- La connexion Microsoft 365/Entra ID est proposée mais marquée `[À valider]`.

## État actualisé du projet au 17/11/2026

Le code a été compilé et le front-end construit. Le parcours complet n’a pas été exécuté contre une base de données.

### Déjà présent ou récemment pris en compte

- Le cœur du parcours de caisse existe : demande, validation hiérarchique, montant définitif, bon/visas/décaissement, pièces, retours par bon d’entrée et clôture sous conditions.
- Le DG peut soumettre une demande avec des responsables N+1/N+2 configurés ; le demandeur ne peut pas valider sa propre demande.
- Une demande de caisse dont la date de début est passée ne peut pas être soumise.
- Démarrage backend vérifié : l’échec provenait de l’entrée Flyway V13 marquée `success=false` dans la base H2 locale ; aucune colonne V13 n’avait été créée. V13 a été réécrite en ajouts unitaires, l’entrée en échec a été réparée, puis Flyway a appliqué V13 à V15 et Spring Boot a démarré sur le port 8081.
- Les visas vérifient la signature et la réauthentification. Le scheduler actuel fait passer les missions échues en régularisation.
- Le dépôt des justificatifs exige côté serveur ClamAV **et** la clé ; la clé locale est valide et ClamAV répond `PONG` sur `127.0.0.1:3310`. La consultation et le contrôle des pièces déjà déposées restent disponibles.
- Les contrats sont rattachés aux comptes employés non administrateurs. L’employé voit le sien, le comptable voit tous les contrats et gère ceux de l’équipe sauf le sien, le DG voit tous les contrats et gère ceux du comptable et de l’adjoint. Les comptes actifs sans contrat sont proposés à l’affectation.
- Les fiches contrat enregistrent maintenant le salaire de base, les primes fixes, le détail des primes et les autres avantages. Les anciennes fiches gardent ces montants vides jusqu’à leur mise à jour.
- Le bloc d’informations complémentaires de la demande de caisse a été retiré temporairement de l’interface ; les colonnes et anciennes valeurs sont conservées en base.
- La page Justificatifs de dépenses explique que les pièces prouvent les dépenses faites avec l’avance et servent au contrôle comptable et au calcul du solde à régulariser ; elle précise qu’elle ne gère pas les ordres de mission préparatoires.

### Écarts à traiter avant de considérer le projet conforme

1. **Confidentialité des contrats — corrigée.** L’interface et l’API suivent maintenant la matrice d’accès confirmée par l’utilisateur ; les responsables ordinaires ne voient plus les contrats de leur équipe.
2. **Affectation des contrats — corrigée.** L’administrateur est exclu de la file et ne peut pas recevoir de contrat via l’API.
3. **Notifications et rappels — manquants.** Aucun module de notifications/e-mail n’a été repéré. Le scheduler recensé ne fait que changer le statut à la fin de mission ; les rappels de caisse et d’échéance de contrat ne sont donc pas couverts.
4. **Contrats — périmètre incomplet.** Pas de gestion des congés, soldes, avenants/conversions/renouvellements, ni d’alertes automatiques d’échéance ou d’avertissements de durée légale. Le statut `A_RENOUVELER` existe mais son cycle automatisé n’a pas été repéré.
5. **Caisse après mission — plusieurs règles manquent.** Les extensions de délai, demandes de dépassement avec décision, réouverture motivée, blocage d’une nouvelle demande en retard, autorisation de délégation/suppléance et visa manuscrit n’ont pas été repérés. La clôture et les retours sont présents.
6. **Reporting et journal transversal — incomplets.** Le tableau de bord actuel montre quelques compteurs ; le panneau Activité est explicitement vide. Les filtres/export PDF-Excel-CSV et le journal unifié prévus par les spécifications ne sont pas recensés.
7. **Authentification Microsoft 365.** L’application utilise actuellement une authentification locale par e-mail/mot de passe. Entra ID est approuvé dans le périmètre et reste à implémenter.

## Règles adoptées et paramètres à configurer

Les éléments ci-dessous sont des exigences approuvées par l’utilisateur, y compris ceux marqués `[À valider]` ou `[à vérifier]` dans les documents. Ils ne doivent pas être retirés du périmètre ni redemander une validation. Les valeurs absentes des documents restent des paramètres à configurer, sans valeur métier inventée.

- Appliquer le parcours de validation du DG avec séparation des tâches, y compris quand il est demandeur ou N+1 ; clarifier par configuration des responsables désignés, sans permettre l’auto-validation.
- Appliquer la gestion des contrats par le DG pour les contrats du comptable et de l’adjoint, ainsi que le suivi de salaire, les congés, l’ancienneté, les échéances, les renouvellements et les exports prévus.
- Paramétrer les délais, seuils, catégories, origines de fonds, dépassements, échéances, rappels, durées de conservation et reprises de numérotation papier à partir des valeurs métier de l’organisation.
- Prévoir la connexion Entra ID, les sauvegardes, les objectifs de disponibilité, la volumétrie, les RPO/RTO et la migration des données dans la mise en service.

## Repères de code vérifiés

- Contrats et droits : `src/features/contracts/ContractsPage.jsx`, `src/app/navigation.js`, `../backend/src/main/java/com/example/backend/contracts/ContractService.java`.
- Justificatifs : `src/features/requests/pages/SupportingDocumentsPage.jsx`, `../backend/src/main/java/com/example/backend/cash/SupportingDocumentAvailabilityController.java`, `SupportingDocumentService.java`.
- Fin de mission : `../backend/src/main/java/com/example/backend/cash/CashRegularizationScheduler.java`.
