import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { App } from '@/App'
import { goTo, stubApi, USER } from './apiStub'

const RECORD = '66666666-6666-6666-6666-666666666666'
const record = {
  id: RECORD,
  vehicleId: '44444444-4444-4444-4444-444444444444',
  recordDate: '2026-11-20',
  mileageKm: 52000,
  types: [{ id: '10000000-0000-4000-8000-000000000001', name: 'Vidange', active: true }],
  authorName: 'Enfant',
  canEdit: true,
  documentCount: 2,
  createdAt: '2026-11-20T08:00:00Z',
  updatedAt: '2026-11-20T08:00:00Z',
}

const photo = {
  id: '88888888-8888-8888-8888-888888888881',
  recordId: RECORD,
  fileName: 'ticket.jpg',
  contentType: 'image/jpeg',
  sizeBytes: 245760,
  authorName: 'Enfant',
  canDelete: true,
  createdAt: '2026-11-20T09:00:00Z',
}
const invoice = {
  id: '88888888-8888-8888-8888-888888888882',
  recordId: RECORD,
  fileName: 'facture-garage.pdf',
  contentType: 'application/pdf',
  sizeBytes: 2516582,
  authorName: 'Said',
  canDelete: false,
  createdAt: '2026-11-20T10:00:00Z',
}

function routes(documents: unknown[]) {
  return {
    'GET /api/me': { status: 200, body: USER },
    [`GET /api/maintenance-records/${RECORD}`]: { status: 200, body: { ...record, documentCount: documents.length } },
    [`GET /api/maintenance-records/${RECORD}/documents`]: { status: 200, body: documents },
  }
}

/** Remplace XMLHttpRequest (l'envoi passe par lui pour la progression) par une réponse donnée. */
function stubUpload(status: number, body: unknown) {
  const sent: FormData[] = []
  class FakeXhr {
    upload: { onprogress: ((event: Partial<ProgressEvent>) => void) | null } = { onprogress: null }
    status = 0
    responseText = ''
    withCredentials = false
    onload: (() => void) | null = null
    onerror: (() => void) | null = null
    open() {}
    setRequestHeader() {}
    send(form: FormData) {
      sent.push(form)
      this.upload.onprogress?.({ lengthComputable: true, loaded: 50, total: 100 })
      setTimeout(() => {
        this.status = status
        this.responseText = JSON.stringify(body)
        this.onload?.()
      }, 0)
    }
  }
  vi.stubGlobal('XMLHttpRequest', FakeXhr)
  return sent
}

describe('justificatifs d’un entretien', () => {
  afterEach(() => {
    goTo('/')
    vi.unstubAllGlobals()
  })

  it('affiche une miniature pour une photo, une icône pour un PDF, et la suppression selon les droits', async () => {
    stubApi(routes([photo, invoice]))
    goTo(`/entretiens/${RECORD}`)
    render(<App />)

    const list = await screen.findByRole('list', { name: 'Justificatifs' })
    const items = within(list).getAllByRole('listitem')
    expect(items).toHaveLength(2)

    const [photoItem, invoiceItem] = items
    expect(photoItem.querySelector('img')).toHaveAttribute('src', `/api/documents/${photo.id}/content`)
    expect(within(photoItem).getByRole('link', { name: 'Ouvrir ticket.jpg' })).toHaveAttribute('target', '_blank')
    expect(within(photoItem).getByRole('link', { name: 'Télécharger ticket.jpg' })).toHaveAttribute(
      'href',
      `/api/documents/${photo.id}/content?download=true`,
    )
    expect(within(photoItem).getByRole('button', { name: 'Supprimer ticket.jpg' })).toBeInTheDocument()

    expect(invoiceItem.querySelector('img')).toBeNull()
    expect(within(invoiceItem).getByText(/2,4 Mo/)).toBeInTheDocument()
    expect(within(invoiceItem).queryByRole('button', { name: /Supprimer/ })).toBeNull()
  })

  it('refuse un fichier de plus de 10 Mo sans l’envoyer', async () => {
    stubApi(routes([]))
    const sent = stubUpload(201, photo)
    goTo(`/entretiens/${RECORD}`)
    render(<App />)

    expect(await screen.findByText('Aucun justificatif')).toBeInTheDocument()
    const big = new File(['x'], 'scan.pdf', { type: 'application/pdf' })
    Object.defineProperty(big, 'size', { value: 10 * 1024 * 1024 + 1 })
    await userEvent.upload(screen.getByLabelText('Joindre une facture'), big)

    expect(await screen.findByText(/Ce fichier dépasse 10 Mo/)).toBeInTheDocument()
    expect(sent).toHaveLength(0)
  })

  it('envoie un justificatif et rafraîchit la liste', async () => {
    let documents: unknown[] = []
    stubApi({
      ...routes([]),
      [`GET /api/maintenance-records/${RECORD}/documents`]: () => ({ status: 200, body: documents }),
    })
    const sent = stubUpload(201, photo)
    goTo(`/entretiens/${RECORD}`)
    render(<App />)

    expect(await screen.findByText('Aucun justificatif')).toBeInTheDocument()
    documents = [photo]
    await userEvent.upload(
      screen.getByLabelText('Joindre une facture'),
      new File(['jpeg'], 'ticket.jpg', { type: 'image/jpeg' }),
    )

    expect(await screen.findByText('Justificatif ajouté.')).toBeInTheDocument()
    expect(sent).toHaveLength(1)
    expect((sent[0].get('file') as File).name).toBe('ticket.jpg')
    expect(await screen.findByRole('link', { name: 'Ouvrir ticket.jpg' })).toBeInTheDocument()
  })

  it('affiche le message du serveur quand le fichier est refusé', async () => {
    stubApi(routes([]))
    stubUpload(415, {
      status: 415,
      code: 'DOCUMENT_TYPE_NOT_ALLOWED',
      detail: 'Seuls les PDF et les photos (JPEG, PNG, WebP) sont acceptés.',
    })
    goTo(`/entretiens/${RECORD}`)
    render(<App />)

    await screen.findByText('Aucun justificatif')
    await userEvent.upload(
      screen.getByLabelText('Joindre une facture'),
      new File(['%PDF'], 'faux.pdf', { type: 'application/pdf' }),
    )

    expect(await screen.findByText('Seuls les PDF et les photos (JPEG, PNG, WebP) sont acceptés.')).toBeInTheDocument()
  })

  it('supprime un justificatif après confirmation', async () => {
    let documents: unknown[] = [photo]
    stubApi({
      ...routes([]),
      [`GET /api/maintenance-records/${RECORD}/documents`]: () => ({ status: 200, body: documents }),
      [`DELETE /api/documents/${photo.id}`]: () => {
        documents = []
        return { status: 204 }
      },
    })
    goTo(`/entretiens/${RECORD}`)
    render(<App />)

    await userEvent.click(await screen.findByRole('button', { name: 'Supprimer ticket.jpg' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('« ticket.jpg » sera supprimé définitivement.')).toBeInTheDocument()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Supprimer' }))

    expect(await screen.findByText('Justificatif supprimé.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Aucun justificatif')).toBeInTheDocument())
  })
})
