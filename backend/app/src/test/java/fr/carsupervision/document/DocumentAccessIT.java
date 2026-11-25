package fr.carsupervision.document;

import static fr.carsupervision.document.DocumentsIT.PDF;
import static fr.carsupervision.document.DocumentsIT.USER;
import static fr.carsupervision.document.DocumentsIT.file;
import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
import static fr.carsupervision.support.TestCatalogue.record;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US5 : un justificatif n'est servi qu'aux membres ayant accès au véhicule (FR-031, SC-010). */
class DocumentAccessIT extends IntegrationTestBase {

    private static final String OTHER = "autre@example.com";

    private Member user;
    private Vehicle vehicle;
    private String recordId;
    private String documentId;

    @BeforeEach
    void documentOfUser() throws Exception {
        user = members.create(USER, Role.USER);
        members.create(OTHER, Role.USER);
        vehicle = vehicles.active("A", 50_000);
        vehicles.assign(user, vehicle);
        recordId = JsonPath.read(mockMvc.perform(as(USER, json(
                post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"),
                record("2026-06-15", 51_000, null, VIDANGE))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        documentId = JsonPath.read(mockMvc.perform(as(USER,
                multipart("/api/maintenance-records/" + recordId + "/documents").file(file("f.pdf", PDF))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void unassignedUserGetsNothing() throws Exception {
        mockMvc.perform(as(OTHER, get("/api/documents/" + documentId + "/content")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(OTHER, get("/api/maintenance-records/" + recordId + "/documents")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(OTHER, multipart("/api/maintenance-records/" + recordId + "/documents")
                .file(file("g.pdf", PDF)))).andExpect(status().isForbidden());
        mockMvc.perform(as(OTHER, delete("/api/documents/" + documentId))).andExpect(status().isForbidden());

        mockMvc.perform(as(OTHER, get("/api/documents/" + UUID.randomUUID() + "/content")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(INITIAL_ADMIN, get("/api/documents/" + UUID.randomUUID() + "/content")))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
    }

    /** SC-010 : un accès retiré refuse le justificatif dès la requête suivante. */
    @Test
    void revokedAccessIsRefusedImmediately() throws Exception {
        mockMvc.perform(as(USER, get("/api/documents/" + documentId + "/content")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF));

        jdbc.update("DELETE FROM vehicle_access WHERE member_id = ?", user.getId());

        mockMvc.perform(as(USER, get("/api/documents/" + documentId + "/content")))
                .andExpect(status().isForbidden());
    }

    @Test
    void archivedVehicleRefusesUploadButAdminStillReads() throws Exception {
        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", vehicle.getId());

        mockMvc.perform(as(INITIAL_ADMIN, multipart("/api/maintenance-records/" + recordId + "/documents")
                .file(file("g.pdf", PDF))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"));
        mockMvc.perform(as(INITIAL_ADMIN, delete("/api/documents/" + documentId)))
                .andExpect(status().isConflict());
        mockMvc.perform(as(INITIAL_ADMIN, get("/api/documents/" + documentId + "/content")))
                .andExpect(status().isOk());
        mockMvc.perform(as(USER, get("/api/documents/" + documentId + "/content")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/documents/" + documentId + "/content"))
                .andExpect(status().isUnauthorized());
    }
}
