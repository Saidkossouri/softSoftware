package fr.carsupervision.common.error;

import java.util.Map;
import org.springframework.validation.FieldError;

/** Messages de validation en français, par contrainte Bean Validation. */
final class ValidationMessages {

    private static final Map<String, String> BY_CONSTRAINT = Map.of(
            "NotNull", "Ce champ est obligatoire.",
            "NotBlank", "Ce champ est obligatoire.",
            "Email", "Saisissez une adresse email valide.",
            "Size", "La longueur de ce champ n'est pas valide.",
            "Min", "La valeur est trop petite.",
            "Max", "La valeur est trop grande.",
            "Pattern", "Le format de ce champ n'est pas valide.");

    private ValidationMessages() {
    }

    static String toFrench(FieldError error) {
        String code = error.getCode();
        if ("Size".equals(code) && error.getArguments() != null && error.getArguments().length >= 3) {
            Object max = error.getArguments()[1];
            Object min = error.getArguments()[2];
            return "La longueur doit être comprise entre " + min + " et " + max + " caractères.";
        }
        return BY_CONSTRAINT.getOrDefault(code, "Valeur invalide.");
    }
}
