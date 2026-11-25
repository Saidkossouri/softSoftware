package fr.carsupervision.document;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.ContractValidation.conformsToContractForUpload;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
import static fr.carsupervision.support.TestCatalogue.record;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** US5 : joindre, consulter, télécharger et supprimer des justificatifs (US5-1 à US5-6, FR-029 à FR-034). */
class DocumentsIT extends IntegrationTestBase {

    static final String USER = "fils@example.com";
    static final byte[] PDF = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};

    @Autowired
    private DocumentStorage storage;

    private Vehicle vehicle;
    private String recordId;

    @BeforeEach
    void recordOfUser() throws Exception {
        Member user = members.create(USER, Role.USER);
        vehicle = vehicles.active("A", 50_000);
        vehicles.assign(user, vehicle);
        recordId = JsonPath.read(mockMvc.perform(as(USER, json(
                post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"),
                record("2026-06-15", 51_000, "\"costCents\":45000", VIDANGE))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/octet-stream", bytes);
    }

    private ResultActions upload(String email, MockMultipartFile file) throws Exception {
        return mockMvc.perform(as(email, multipart("/api/maintenance-records/" + recordId + "/documents").file(file)));
    }

    private String uploaded(String email, MockMultipartFile file) throws Exception {
        return JsonPath.read(upload(email, file).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String storageKey(String documentId) {
        return jdbc.queryForObject("SELECT storage_key FROM document WHERE id = ?", String.class,
                UUID.fromString(documentId));
    }

    private boolean stored(String key) {
        return Files.exists(((LocalDocumentStorage) storage).resolve(key));
    }

    @Test
    void uploadsAndServesPdfAndPhoto() throws Exception {
        upload(USER, file("Facture garage.pdf", PDF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("Facture garage.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(PDF.length))
                .andExpect(jsonPath("$.authorName").value(USER))
                .andExpect(jsonPath("$.canDelete").value(true))
                .andExpect(conformsToContractForUpload());
        String photo = uploaded(USER, file("photo.png", JPEG));

        mockMvc.perform(as(USER, get("/api/maintenance-records/" + recordId + "/documents")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].contentType").value("image/jpeg"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, get("/api/maintenance-records/" + recordId)))
                .andExpect(jsonPath("$.documentCount").value(2));
        mockMvc.perform(as(USER, get("/api/vehicles/" + vehicle.getId() + "/history")))
                .andExpect(jsonPath("$.entries[0].documentCount").value(2));

        mockMvc.perform(as(INITIAL_ADMIN, get("/api/documents/" + photo + "/content")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("Content-Disposition", containsString("photo.png")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Cache-Control", containsString("private")));
        mockMvc.perform(as(USER, get("/api/documents/" + photo + "/content").param("download", "true")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    void keepsUtf8FileNameInContentDisposition() throws Exception {
        String id = uploaded(USER, file("Facture été.pdf", PDF));

        mockMvc.perform(as(USER, get("/api/documents/" + id + "/content")))
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''Facture%20%C3%A9t%C3%A9.pdf")));
    }

    @Test
    void refusesRenamedTextEmptyAndOversizedFiles() throws Exception {
        upload(USER, file("facture.pdf", "pas un PDF".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("DOCUMENT_TYPE_NOT_ALLOWED"))
                .andExpect(conformsToContractForUpload());
        upload(USER, file("vide.pdf", new byte[0]))
                .andExpect(status().isUnsupportedMediaType());

        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        upload(USER, file("gros.pdf", big))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("DOCUMENT_TOO_LARGE"))
                .andExpect(conformsToContractForUpload());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM document", Long.class)).isZero();
    }

    @Test
    void limitsToTenDocumentsPerRecord() throws Exception {
        for (int i = 1; i <= 10; i++) {
            uploaded(USER, file("facture-" + i + ".pdf", PDF));
        }
        upload(USER, file("onzieme.pdf", PDF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_LIMIT_REACHED"))
                .andExpect(conformsToContractForUpload());
    }

    @Test
    void maliciousNameNeverReachesStoragePath() throws Exception {
        String id = uploaded(USER, file("../../etc/passwd", PDF));

        mockMvc.perform(as(USER, get("/api/maintenance-records/" + recordId + "/documents")))
                .andExpect(jsonPath("$[0].fileName").value("passwd"));
        assertThat(storageKey(id)).isEqualTo(vehicle.getId() + "/" + id);
        assertThat(stored(storageKey(id))).isTrue();
    }

    @Test
    void authorOrAdminDeletes() throws Exception {
        String byUser = uploaded(USER, file("a.pdf", PDF));
        String byAdmin = uploaded(INITIAL_ADMIN, file("b.pdf", PDF));
        String key = storageKey(byUser);

        mockMvc.perform(as(USER, get("/api/maintenance-records/" + recordId + "/documents")))
                .andExpect(jsonPath("$[1].canDelete").value(false));
        mockMvc.perform(as(USER, delete("/api/documents/" + byAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHOR"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, delete("/api/documents/" + byUser))).andExpect(status().isNoContent());
        mockMvc.perform(as(INITIAL_ADMIN, delete("/api/documents/" + byAdmin))).andExpect(status().isNoContent());

        assertThat(stored(key)).isFalse();
        mockMvc.perform(as(USER, get("/api/maintenance-records/" + recordId + "/documents")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /** US5-6 : supprimer l'entretien supprime ses justificatifs, lignes et fichiers. */
    @Test
    void deletingRecordDeletesItsDocuments() throws Exception {
        String first = uploaded(USER, file("a.pdf", PDF));
        String second = uploaded(USER, file("b.jpg", JPEG));
        String firstKey = storageKey(first);
        String secondKey = storageKey(second);

        mockMvc.perform(as(USER, delete("/api/maintenance-records/" + recordId))).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM document", Long.class)).isZero();
        assertThat(stored(firstKey)).isFalse();
        assertThat(stored(secondKey)).isFalse();
    }
}
