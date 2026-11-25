package fr.carsupervision.document;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.maintenance.MaintenanceRecord;
import fr.carsupervision.maintenance.MaintenanceRecordRepository;
import fr.carsupervision.vehicle.AuthorPolicy;
import fr.carsupervision.vehicle.AuthorPolicy.AuthorSnapshot;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Justificatifs des entretiens (US5, FR-029 à FR-034) : mêmes droits que le véhicule de
 * l'entretien, vérifiés avant toute lecture de fichier ; suppression par l'auteur ou un ADMIN.
 */
@Service
public class DocumentService {

    static final int MAX_PER_RECORD = 10;
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    static final String RECORD_NOT_FOUND = "Cet entretien n'existe pas.";
    static final String NOT_FOUND = "Ce justificatif n'existe pas.";

    private final DocumentRepository documentRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final DocumentStorage storage;
    private final VehicleAccessGuard accessGuard;
    private final AuthorPolicy authorPolicy;
    private final Clock clock;

    public DocumentService(DocumentRepository documentRepository, MaintenanceRecordRepository recordRepository,
            DocumentStorage storage, VehicleAccessGuard accessGuard, AuthorPolicy authorPolicy, Clock clock) {
        this.documentRepository = documentRepository;
        this.recordRepository = recordRepository;
        this.storage = storage;
        this.accessGuard = accessGuard;
        this.authorPolicy = authorPolicy;
        this.clock = clock;
    }

    /** Justificatif prêt à afficher, avec le droit de suppression du membre connecté. */
    public record View(Document document, boolean canDelete) {
    }

    /** Contenu à servir : la ligne (type, nom) et le fichier. */
    public record Content(Document document, Resource resource) {
    }

    @Transactional(readOnly = true)
    public List<View> list(UUID recordId) {
        MaintenanceRecord record = findRecord(recordId);
        accessGuard.assertReadable(record.getVehicleId());
        return documentRepository.findByRecordIdOrderByCreatedAtAscFileNameAsc(recordId).stream()
                .map(this::view)
                .toList();
    }

    /**
     * Écrit le fichier puis la ligne, sous le verrou du véhicule ; le fichier est effacé si la
     * transaction échoue. Le format vient de la signature du contenu.
     */
    @Transactional
    public View upload(UUID recordId, MultipartFile file) {
        MaintenanceRecord record = findRecord(recordId);
        accessGuard.lockForWrite(record.getVehicleId());
        // Normalement refusé avant par la limite multipart (413 dans ApiExceptionHandler).
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.DOCUMENT_TOO_LARGE, "Le fichier dépasse 10 Mo.");
        }
        if (documentRepository.countByRecordId(recordId) >= MAX_PER_RECORD) {
            throw ApiException.conflict(ErrorCode.DOCUMENT_LIMIT_REACHED,
                    "Cet entretien a déjà " + MAX_PER_RECORD + " justificatifs : supprimez-en un pour en joindre un autre.");
        }
        String contentType = FileSignature.detect(header(file)).orElseThrow(() -> new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.DOCUMENT_TYPE_NOT_ALLOWED,
                "Format non accepté : joignez un PDF ou une photo (JPEG, PNG ou WebP)."));
        AuthorSnapshot author = authorPolicy.authorSnapshot();
        Document document = new Document(recordId, record.getVehicleId(), FileNames.clean(file.getOriginalFilename()),
                contentType, file.getSize(), author.id(), author.name(), OffsetDateTime.now(clock));
        try (InputStream content = file.getInputStream()) {
            storage.store(document.getStorageKey(), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Enregistrement du justificatif impossible", e);
        }
        AfterCommit.onRollback(() -> storage.delete(document.getStorageKey()));
        return view(documentRepository.saveAndFlush(document));
    }

    /** Droit de lecture vérifié sur le véhicule avant d'ouvrir le fichier (SC-010). */
    @Transactional(readOnly = true)
    public Content open(UUID documentId) {
        Document document = find(documentId);
        accessGuard.assertReadable(document.getVehicleId());
        try {
            return new Content(document, storage.open(document.getStorageKey()));
        } catch (IOException e) {
            throw ApiException.notFound("Le fichier de ce justificatif est introuvable.");
        }
    }

    /** Supprime la ligne, puis le fichier une fois la suppression validée. */
    @Transactional
    public void delete(UUID documentId) {
        Document document = find(documentId);
        accessGuard.lockForWrite(document.getVehicleId());
        authorPolicy.assertAuthorOrAdmin(document.getAuthorId());
        documentRepository.delete(document);
        AfterCommit.run(() -> storage.delete(document.getStorageKey()));
    }

    private View view(Document document) {
        return new View(document, authorPolicy.canEdit(document.getAuthorId()));
    }

    private MaintenanceRecord findRecord(UUID recordId) {
        return recordRepository.findById(recordId).orElseThrow(() -> accessGuard.missing(RECORD_NOT_FOUND));
    }

    private Document find(UUID documentId) {
        return documentRepository.findById(documentId).orElseThrow(() -> accessGuard.missing(NOT_FOUND));
    }

    private static byte[] header(MultipartFile file) {
        try (InputStream content = file.getInputStream()) {
            return content.readNBytes(FileSignature.HEADER_LENGTH);
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du justificatif impossible", e);
        }
    }
}
