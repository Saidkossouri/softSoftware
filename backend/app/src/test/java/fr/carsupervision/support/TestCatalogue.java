package fr.carsupervision.support;

import java.util.UUID;

/** Identifiants fixes des types pré-remplis (changeset 007, data-model.md). */
public final class TestCatalogue {

    public static final UUID VIDANGE = maintenance(1);
    public static final UUID FILTRE_A_HUILE = maintenance(2);
    public static final UUID REVISION = maintenance(6);
    public static final UUID CLIMATISATION = maintenance(11);
    public static final UUID AUTRE = maintenance(13);
    public static final UUID ASSURANCE = UUID.fromString("20000000-0000-4000-8000-000000000001");

    private TestCatalogue() {
    }

    private static UUID maintenance(int order) {
        return UUID.fromString("10000000-0000-4000-8000-%012d".formatted(order));
    }

    /** Corps JSON d'un entretien ({@code MaintenanceRecordInput}). */
    public static String record(String date, int km, String extra, UUID... typeIds) {
        StringBuilder ids = new StringBuilder();
        for (UUID id : typeIds) {
            ids.append(ids.isEmpty() ? "" : ",").append('"').append(id).append('"');
        }
        return """
                {"recordDate":"%s","mileageKm":%d,"typeIds":[%s]%s}""".formatted(date, km, ids,
                extra == null || extra.isBlank() ? "" : "," + extra);
    }
}
