package fr.carsupervision.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

/**
 * Justificatifs sur le disque local, dans {@code app.documents.storage-dir} (volume Docker
 * {@code documents-data} en conteneur). Écriture dans un fichier temporaire puis renommage
 * atomique ; toute clé qui sortirait du dossier est refusée.
 */
public class LocalDocumentStorage implements DocumentStorage {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDocumentStorage.class);

    private final Path root;

    public LocalDocumentStorage(Path root) {
        try {
            this.root = Files.createDirectories(root).toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Dossier des justificatifs inaccessible : " + root, e);
        }
    }

    @Override
    public void store(String key, InputStream content) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public Resource open(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new NoSuchFileException(key);
        }
        return new PathResource(path);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            LOG.warn("Fichier de justificatif non supprimé : {}", key, e);
        }
    }

    Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Clé de stockage invalide : " + key);
        }
        return path;
    }
}
