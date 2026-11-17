import {
  AppstoreOutlined,
  ArrowRightOutlined,
  ClockCircleOutlined,
  ContainerOutlined,
  FileProtectOutlined,
  FileTextOutlined,
  SettingOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  WalletOutlined,
} from '@ant-design/icons'

const iconComponents = {
  overview: AppstoreOutlined,
  wallet: WalletOutlined,
  receipt: ContainerOutlined,
  document: FileTextOutlined,
  briefcase: FileProtectOutlined,
  users: TeamOutlined,
  arrow: ArrowRightOutlined,
  clock: ClockCircleOutlined,
  shield: SafetyCertificateOutlined,
  spark: ThunderboltOutlined,
  settings: SettingOutlined,
}

// Centralise les icônes afin que les composants les choisissent avec un nom lisible.
function Icon({ name, size = 20 }) {
  const AntIcon = iconComponents[name]

  // Une faute de nom est signalée tout de suite au lieu d'afficher une icône vide.
  if (!AntIcon) {
    throw new Error(`Icône Ant Design inconnue : "${name}".`)
  }

  return (
    <AntIcon
      aria-hidden="true"
      className="icon"
      style={{ fontSize: size }}
    />
  )
}

export default Icon
