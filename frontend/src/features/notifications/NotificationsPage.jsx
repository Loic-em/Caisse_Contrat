import { useEffect, useState } from 'react'
import { getNotifications, markNotificationRead } from './notificationsApi'

export default function NotificationsPage({ currentUser, onNavigate }) {
  const [notifications, setNotifications] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!currentUser?.email) return
    getNotifications(currentUser.email).then(setNotifications).catch((e) => setError(e.message))
  }, [currentUser?.email])

  async function handleRead(id) {
    await markNotificationRead(id)
    setNotifications((current) => current.map((item) => item.id === id ? { ...item, readFlag: true } : item))
  }

  return (
    <section className="content-page">
      <h1>Notifications</h1>
      {error && <p role="alert">{error}</p>}
      <ul>
        {notifications.map((notification) => (
          <li key={notification.id}>
            <strong>{notification.title}</strong>
            <p>{notification.message}</p>
            {notification.relatedRequestId && <button type="button" onClick={() => onNavigate?.('requests', { requestId: notification.relatedRequestId })}>Consulter la demande</button>}
            {!notification.readFlag && <button type="button" onClick={() => handleRead(notification.id)}>Marquer comme lu</button>}
          </li>
        ))}
      </ul>
    </section>
  )
}
