package fr.carsupervision.document;

/**
 * Nom d'origine d'un justificatif, nettoyé pour l'affichage et {@code Content-Disposition}
 * (research R14). Il ne sert jamais à construire un chemin sur le disque.
 */
public final class FileNames {

    static final int MAX_LENGTH = 120;
    static final String DEFAULT_NAME = "justificatif";

    private FileNames() {
    }

    /** Garde le dernier segment, retire caractères de contrôle et séparateurs, coupe à 120 caractères. */
    public static String clean(String original) {
        String name = original == null ? "" : original;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        name = name.substring(slash + 1);
        name = name.replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", "").strip();
        while (name.startsWith(".")) {
            name = name.substring(1).strip();
        }
        if (name.length() > MAX_LENGTH) {
            name = name.substring(0, MAX_LENGTH).strip();
        }
        return name.isEmpty() ? DEFAULT_NAME : name;
    }
}
