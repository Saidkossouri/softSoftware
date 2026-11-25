package fr.carsupervision.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Formats reconnus sur les premiers octets, quel que soit le nom du fichier (research R14). */
class FileSignatureTest {

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    @Test
    void recognisesAcceptedFormats() {
        assertThat(FileSignature.detect("%PDF-1.7\n%".getBytes(StandardCharsets.US_ASCII))).contains("application/pdf");
        assertThat(FileSignature.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0x10))).contains("image/jpeg");
        assertThat(FileSignature.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)))
                .contains("image/png");
        assertThat(FileSignature.detect("RIFF\u0024\u0000\u0000\u0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1)))
                .contains("image/webp");
    }

    @Test
    void refusesEverythingElse() {
        assertThat(FileSignature.detect("Bonjour, ceci est un texte renommé .pdf".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(FileSignature.detect("GIF89a......".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(FileSignature.detect("RIFF\u0024\u0000\u0000\u0000WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1)))
                .isEmpty();
        assertThat(FileSignature.detect(new byte[0])).isEmpty();
        assertThat(FileSignature.detect(bytes(0xFF, 0xD8))).isEmpty();
    }
}
