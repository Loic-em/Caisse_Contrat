// Une seule configuration alimente le menu, les libellés de page et les raccourcis.
export const navigationGroups = [
  {
    label: 'Espace de travail',
    items: [
      { id: 'overview', label: "Vue d'ensemble", icon: 'overview' },
      {
        id: 'requests',
        label: 'Demandes de caisse',
        icon: 'wallet',
        eyebrow: 'CAISSE',
        description: 'Les demandes de mission et leur circuit de validation seront disponibles ici.',
      },
      {
        id: 'receipts',
        label: 'Bons de caisse',
        icon: 'receipt',
        eyebrow: 'CAISSE',
        description: 'Consultez les bons émis, leurs visas et les décaissements.',
      },
      {
        id: 'supporting-documents',
        label: 'Justificatifs',
        icon: 'document',
        eyebrow: 'CAISSE',
        description: 'Déposez les pièces d’une mission décaissée et suivez leur contrôle.',
      },
    ],
  },
  {
    label: 'Ressources humaines',
    items: [
      {
        id: 'contracts',
        label: 'Contrats de travail',
        icon: 'briefcase',
        eyebrow: 'RESSOURCES HUMAINES',
        description: 'Consultez les contrats de travail, leurs documents signés et leur historique.',
      },
      {
        id: 'team',
        label: 'Équipe',
        icon: 'users',
        eyebrow: 'RESSOURCES HUMAINES',
        description: 'Comptes utilisateurs, rôles et hiérarchie de l’équipe.',
      },
    ],
  },
]

export const navigationItems = navigationGroups.flatMap((group) =>
  // Chaque entrée garde son groupe pour que l'interface puisse l'afficher correctement.
  group.items.map((item) => ({ ...item, group: group.label })),
)

// La vue d'ensemble sert de destination initiale et de page de repli.
export const overviewPage = {
  id: 'overview',
  label: "Vue d'ensemble",
  title: "Vue d'ensemble",
}
