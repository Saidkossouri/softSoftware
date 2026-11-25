package fr.carsupervision.document;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Choix du stockage par {@code app.documents.storage} ; seul {@code local} existe pour l'instant. */
@Configuration
public class DocumentStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.documents.storage", havingValue = "local", matchIfMissing = true)
    DocumentStorage localDocumentStorage(@Value("${app.documents.storage-dir:./data/documents}") Path dir) {
        return new LocalDocumentStorage(dir);
    }
}
