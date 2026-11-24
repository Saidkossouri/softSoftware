package fr.carsupervision.security;

/** Chemins qui exposent des données ou la documentation : session et membre autorisé exigés. */
final class ProtectedPaths {

    static final String[] PATTERNS = {"/api/**", "/openapi.yaml", "/swagger-ui/**", "/swagger-ui.html", "/v3/**"};

    private ProtectedPaths() {
    }

    static boolean isProtected(String path) {
        return path.startsWith("/api/")
                || path.equals("/api")
                || path.equals("/openapi.yaml")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/");
    }

    static boolean isApi(String path) {
        return path.startsWith("/api/") || path.equals("/api");
    }
}
