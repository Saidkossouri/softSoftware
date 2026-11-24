package fr.carsupervision.common;

import java.util.Locale;
import java.util.regex.Pattern;

/** Règles de normalisation partagées (FR-005, FR-016). */
public final class Normalizers {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern REGISTRATION_SEPARATORS = Pattern.compile("[\\s-]+");

    private Normalizers() {
    }

    /** Email comparable : espaces de bord retirés, minuscules. {@code null} reste {@code null}. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Vérifie le format d'un email déjà normalisé. */
    public static boolean isValidEmail(String normalizedEmail) {
        return normalizedEmail != null
                && normalizedEmail.length() <= 320
                && EMAIL.matcher(normalizedEmail).matches();
    }

    /** Immatriculation telle qu'affichée : espaces de bord retirés, majuscules. */
    public static String normalizeRegistrationDisplay(String registration) {
        return registration == null ? null : registration.strip().toUpperCase(Locale.ROOT);
    }

    /** Clé d'unicité d'une immatriculation : majuscules, sans espaces ni tirets. */
    public static String registrationKey(String registration) {
        return registration == null
                ? null
                : REGISTRATION_SEPARATORS.matcher(registration.toUpperCase(Locale.ROOT)).replaceAll("");
    }

    /** Texte facultatif : espaces de bord retirés, chaîne vide traitée comme absente. */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
