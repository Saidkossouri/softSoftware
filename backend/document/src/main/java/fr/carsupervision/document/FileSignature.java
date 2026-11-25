package fr.carsupervision.document;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Format d'un justificatif d'après ses premiers octets (research R14) : le nom et le type annoncé
 * par le navigateur sont ignorés, un fichier renommé n'est donc jamais servi sous un faux type.
 */
public final class FileSignature {

    /** Octets à lire en tête de fichier pour reconnaître les quatre formats acceptés. */
    public static final int HEADER_LENGTH = 12;

    public static final String PDF = "application/pdf";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MAGIC = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private FileSignature() {
    }

    /** Type MIME reconnu, ou vide pour tout autre contenu (fichier vide compris). */
    public static Optional<String> detect(byte[] header) {
        if (startsWith(header, 0, PDF_MAGIC)) {
            return Optional.of(PDF);
        }
        if (startsWith(header, 0, JPEG_MAGIC)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0, PNG_MAGIC)) {
            return Optional.of(PNG);
        }
        if (startsWith(header, 0, RIFF) && startsWith(header, 8, WEBP_MAGIC)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] magic) {
        return data.length >= offset + magic.length
                && Arrays.equals(data, offset, offset + magic.length, magic, 0, magic.length);
    }
}
