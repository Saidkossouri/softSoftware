import { Download, FileText, Paperclip, Trash2 } from 'lucide-react'
import { useId, useRef, useState, type ChangeEvent } from 'react'
import { ApiError, type Document } from '@/api/client'
import {
  documentContentUrl,
  MAX_DOCUMENT_BYTES,
  useDeleteDocument,
  useRecordDocuments,
  useUploadDocument,
} from '@/api/documents'
import { errorMessage } from '@/components/FormErrors'
import { IconTile, ListCard } from '@/components/ListCard'
import { SectionLabel } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ConfirmDialog } from '@/components/ui/dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { formatBytes, formatInstantDate } from '@/lib/format'
import { cn } from '@/lib/utils'

/** Un 413 renvoyé par le proxy n'a pas de Problem Details : message propre dans ce cas. */
function uploadError(error: unknown): string {
  if (error instanceof ApiError && error.status === 413 && !error.problem.detail) {
    return fr.documents.tooLarge
  }
  return errorMessage(error)
}

/** Justificatifs d'un entretien (US5) : envoi avec progression, aperçu, ouverture, suppression. */
export function DocumentList({ recordId }: { recordId: string }) {
  const documents = useRecordDocuments(recordId)
  const upload = useUploadDocument(recordId)
  const remove = useDeleteDocument(recordId)
  const toast = useToast()
  const inputId = useId()
  const input = useRef<HTMLInputElement>(null)
  const [sending, setSending] = useState<{ name: string; percent: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<Document | null>(null)

  async function onFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    // Le même fichier peut être choisi de nouveau après une erreur.
    if (input.current) input.current.value = ''
    if (!file) return
    setError(null)
    if (file.size > MAX_DOCUMENT_BYTES) {
      setError(fr.documents.tooLarge)
      return
    }
    setSending({ name: file.name, percent: 0 })
    try {
      await upload.mutateAsync({
        file,
        onProgress: (percent) => setSending({ name: file.name, percent }),
      })
      toast.success(fr.documents.uploaded)
    } catch (uploadFailure) {
      setError(uploadError(uploadFailure))
    } finally {
      setSending(null)
    }
  }

  async function onDelete(document: Document) {
    try {
      await remove.mutateAsync(document.id)
      setDeleting(null)
      toast.success(fr.documents.deleted)
    } catch {
      // Message affiché dans le dialogue.
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <SectionLabel>{fr.documents.title}</SectionLabel>
          <label
            htmlFor={inputId}
            className={cn(
              buttonVariants({ variant: 'outline', className: 'w-full cursor-pointer sm:w-auto' }),
              'has-[:focus-visible]:ring-[3px] has-[:focus-visible]:ring-ring/50',
              sending && 'pointer-events-none opacity-50',
            )}
          >
            <Paperclip aria-hidden />
            {fr.documents.add}
            <input
              ref={input}
              id={inputId}
              type="file"
              accept="application/pdf,image/jpeg,image/png,image/webp"
              className="sr-only"
              disabled={sending !== null}
              onChange={(event) => void onFile(event)}
            />
          </label>
        </div>
        <p className="text-sm text-muted-foreground">{fr.documents.hint}</p>

        {sending && (
          <div className="flex flex-col gap-2">
            <p className="truncate text-sm">{fr.documents.uploading(sending.name)}</p>
            <div
              role="progressbar"
              aria-label={fr.documents.progressLabel}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={sending.percent}
              className="h-2 overflow-hidden rounded-full bg-muted"
            >
              <div
                className="h-full rounded-full bg-primary transition-[width] duration-150 motion-reduce:transition-none"
                style={{ width: `${sending.percent}%` }}
              />
            </div>
          </div>
        )}
        {error && <Alert variant="destructive">{error}</Alert>}

        {documents.isPending && (
          <div className="flex flex-col gap-3" aria-busy="true">
            <p className="sr-only">{fr.loading}</p>
            <Skeleton className="h-20" />
          </div>
        )}
        {documents.isError && (
          <Alert variant="destructive">
            {errorMessage(documents.error)}
            <Button variant="outline" size="sm" onClick={() => void documents.refetch()}>
              {fr.retry}
            </Button>
          </Alert>
        )}
        {documents.isSuccess && documents.data.length === 0 && !sending && (
          <EmptyState
            illustration="archive"
            title={fr.documents.emptyTitle}
            description={fr.documents.emptyBody}
            className="py-4"
          />
        )}
        {documents.isSuccess && documents.data.length > 0 && (
          <ul className="flex flex-col gap-3" aria-label={fr.documents.title}>
            {documents.data.map((document) => (
              <DocumentItem key={document.id} document={document} onDelete={() => setDeleting(document)} />
            ))}
          </ul>
        )}
      </CardContent>

      <ConfirmDialog
        open={deleting !== null}
        title={fr.documents.deleteTitle}
        description={deleting ? fr.documents.deleteDescription(deleting.fileName) : ''}
        confirmLabel={remove.isPending ? fr.documents.deleting : fr.documents.deleteConfirm}
        cancelLabel={fr.documents.cancel}
        pending={remove.isPending}
        error={remove.isError ? <Alert variant="destructive">{errorMessage(remove.error)}</Alert> : undefined}
        onConfirm={() => deleting && void onDelete(deleting)}
        onCancel={() => {
          setDeleting(null)
          remove.reset()
        }}
      />
    </Card>
  )
}

function DocumentItem({ document, onDelete }: { document: Document; onDelete: () => void }) {
  const isImage = document.contentType.startsWith('image/')
  return (
    <ListCard
      stack
      wrap
      className="bg-background/40 shadow-none"
      leading={
        isImage ? (
          <img
            src={documentContentUrl(document.id)}
            alt=""
            loading="lazy"
            className="size-12 shrink-0 rounded-xl bg-muted object-cover"
          />
        ) : (
          <IconTile>
            <FileText />
          </IconTile>
        )
      }
      title={
        <a
          href={documentContentUrl(document.id)}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={fr.documents.open(document.fileName)}
          className="truncate rounded-sm underline-offset-4 outline-none hover:underline focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          {document.fileName}
        </a>
      }
      subtitle={
        <>
          <span className="tabular-nums">
            {fr.documents.meta(formatBytes(document.sizeBytes), formatInstantDate(document.createdAt))}
          </span>{' '}
          {fr.documents.by(document.authorName)}
        </>
      }
      trailing={
        <>
          <a
            href={documentContentUrl(document.id, true)}
            aria-label={fr.documents.download(document.fileName)}
            className={buttonVariants({ variant: 'ghost', size: 'icon' })}
          >
            <Download aria-hidden />
          </a>
          {document.canDelete && (
            <Button
              variant="ghost"
              size="icon"
              aria-label={fr.documents.delete(document.fileName)}
              className="hover:text-destructive"
              onClick={onDelete}
            >
              <Trash2 aria-hidden />
            </Button>
          )}
        </>
      }
    />
  )
}
