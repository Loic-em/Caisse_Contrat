export async function getNotifications(recipientEmail) {
  const response = await fetch(`/api/notifications?recipientEmail=${encodeURIComponent(recipientEmail)}`)
  if (!response.ok) throw new Error('Impossible de charger les notifications.')
  return response.json()
}

export async function getUnreadNotificationCount(recipientEmail) {
  const response = await fetch(`/api/notifications/unread-count?recipientEmail=${encodeURIComponent(recipientEmail)}`)
  if (!response.ok) throw new Error('Impossible de charger le compteur.')
  return response.json()
}

export async function markNotificationRead(id) {
  const response = await fetch(`/api/notifications/${id}/read`, { method: 'POST' })
  if (!response.ok) throw new Error('Impossible de marquer la notification.')
}
