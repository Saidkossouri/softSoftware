import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, readCsrfToken, unwrap, type Document, type Problem } from './client'

/** Taille maximale d'un justificatif (FR-030), vérifiée aussi par le serveur. */
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024

const documentsKey = (recordId: string) => ['documents', recordId] as const

/** Adresse du contenu, servi par le backend après contrôle d'accès. */
export function documentContentUrl(documentId: string, download = false) {
  return `/api/documents/${documentId}/content${download ? '?download=true' : ''}`
}

export function useRecordDocuments(recordId: string) {
  return useQuery({
    queryKey: documentsKey(recordId),
    queryFn: async () =>
      unwrap(await api.GET('/maintenance-records/{recordId}/documents', { params: { path: { recordId } } })),
  })
}

/** Un justificatif change la liste, le nombre affiché sur l'entretien et dans l'historique. */
function useInvalidateDocuments(recordId: string) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: documentsKey(recordId) }),
      queryClient.invalidateQueries({ queryKey: ['maintenance-records', recordId] }),
      queryClient.invalidateQueries({ queryKey: ['history'] }),
    ])
}

/**
 * Envoi `multipart` avec suivi de progression (XMLHttpRequest, que fetch ne permet pas) et le
 * jeton CSRF en en-tête, comme le client typé.
 */
export function uploadDocument(recordId: string, file: File, onProgress?: (percent: number) => void) {
  return new Promise<Document>((resolve, reject) => {
    const request = new XMLHttpRequest()
    request.open('POST', `/api/maintenance-records/${recordId}/documents`)
    request.withCredentials = true
    const token = readCsrfToken()
    if (token) request.setRequestHeader('X-XSRF-TOKEN', decodeURIComponent(token))
    request.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress?.(Math.round((event.loaded / event.total) * 100))
    }
    request.onload = () => {
      let body: unknown
      try {
        body = request.responseText ? JSON.parse(request.responseText) : null
      } catch {
        body = null
      }
      if (request.status >= 200 && request.status < 300) resolve(body as Document)
      else reject(new ApiError(request.status, (body ?? { status: request.status }) as Problem))
    }
    request.onerror = () => reject(new ApiError(0, { status: 0 } as Problem))
    const form = new FormData()
    form.append('file', file)
    request.send(form)
  })
}

export function useUploadDocument(recordId: string) {
  const invalidate = useInvalidateDocuments(recordId)
  return useMutation({
    mutationFn: ({ file, onProgress }: { file: File; onProgress?: (percent: number) => void }) =>
      uploadDocument(recordId, file, onProgress),
    onSuccess: invalidate,
  })
}

export function useDeleteDocument(recordId: string) {
  const invalidate = useInvalidateDocuments(recordId)
  return useMutation({
    mutationFn: async (documentId: string) =>
      unwrap(await api.DELETE('/documents/{documentId}', { params: { path: { documentId } } })),
    onSuccess: invalidate,
  })
}
