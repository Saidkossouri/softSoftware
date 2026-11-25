package fr.carsupervision.document;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * Stockage des fichiers des justificatifs (research R13). La clé est toujours
 * {@code <vehicleId>/<documentId>}, jamais un nom fourni par l'utilisateur. Une implémentation
 * GCS ou S3 s'ajoutera au déploiement sans toucher au code métier.
 */
public interface DocumentStorage {

    void store(String key, InputStream content) throws IOException;

    Resource open(String key) throws IOException;

    /** Sans erreur si le fichier n'existe plus. */
    void delete(String key);
}
