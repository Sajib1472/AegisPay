package com.aegispay.app.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Local stand-in for S3/R2. Pay-run rows store checksums; bytes live outside the web root.
 */
@Component
public class LocalObjectStore {

    private final Path root;

    public LocalObjectStore(@Value("${aegispay.storage.local-dir:${user.home}/.aegispay/objects}") String dir) {
        this.root = Path.of(dir);
    }

    public String put(UUID tenantId, String sha256, byte[] bytes) {
        try {
            Path folder = root.resolve(tenantId.toString());
            Files.createDirectories(folder);
            Path file = folder.resolve(sha256 + ".pdf");
            Files.write(file, bytes);
            return tenantId + "/" + sha256 + ".pdf";
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store audit pack", e);
        }
    }

    public byte[] get(String storageKey) {
        try {
            return Files.readAllBytes(root.resolve(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("Audit pack missing from object storage", e);
        }
    }
}
