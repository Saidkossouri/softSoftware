package fr.carsupervision.document;

import fr.carsupervision.api.DocumentsApi;
import fr.carsupervision.api.model.Document;
import fr.carsupervision.api.model.DocumentContentType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Justificatifs : contenu servi uniquement par le backend, après contrôle d'accès (FR-031). */
@RestController
public class DocumentsController implements DocumentsApi {

    private final DocumentService documentService;

    public DocumentsController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public ResponseEntity<List<Document>> listRecordDocuments(UUID recordId) {
        return ResponseEntity.ok(documentService.list(recordId).stream().map(DocumentsController::toDto).toList());
    }

    @Override
    public ResponseEntity<Document> uploadRecordDocument(UUID recordId, MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(documentService.upload(recordId, file)));
    }

    /** Aperçu ({@code inline}) ou téléchargement ({@code attachment}), jamais mis en cache. */
    @Override
    public ResponseEntity<Resource> getDocumentContent(UUID documentId, Boolean download) {
        DocumentService.Content content = documentService.open(documentId);
        var document = content.document();
        ContentDisposition disposition = (Boolean.TRUE.equals(download)
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(document.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(content.resource());
    }

    @Override
    public ResponseEntity<Void> deleteDocument(UUID documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build();
    }

    private static Document toDto(DocumentService.View view) {
        var document = view.document();
        return new Document(document.getId(), document.getRecordId(), document.getFileName(),
                DocumentContentType.fromValue(document.getContentType()), document.getSizeBytes(),
                document.getAuthorName(), view.canDelete(), document.getCreatedAt());
    }
}
