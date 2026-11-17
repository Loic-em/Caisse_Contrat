import { ArrowLeftOutlined } from '@ant-design/icons'

function PageBackButton({ onClick, children = 'Retour' }) {
  return (
    <button className="page-back-button" type="button" onClick={onClick}>
      <ArrowLeftOutlined aria-hidden="true" />
      {children}
    </button>
  )
}

export default PageBackButton
