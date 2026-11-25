package fr.carsupervision.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileNamesTest {

    @Test
    void keepsReadableNames() {
        assertThat(FileNames.clean("Facture garage Atlas – mai.pdf")).isEqualTo("Facture garage Atlas – mai.pdf");
        assertThat(FileNames.clean("فاتورة.pdf")).isEqualTo("فاتورة.pdf");
    }

    @Test
    void removesPathsControlCharactersAndLeadingDots() {
        assertThat(FileNames.clean("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileNames.clean("C:\\Users\\said\\facture.pdf")).isEqualTo("facture.pdf");
        assertThat(FileNames.clean("fac\u0000tu\nre\".pdf")).isEqualTo("facture.pdf");
        assertThat(FileNames.clean("..htaccess")).isEqualTo("htaccess");
    }

    @Test
    void fallsBackAndTruncates() {
        assertThat(FileNames.clean(null)).isEqualTo("justificatif");
        assertThat(FileNames.clean("  ../  ")).isEqualTo("justificatif");
        assertThat(FileNames.clean("a".repeat(200) + ".pdf")).hasSize(120);
    }
}
