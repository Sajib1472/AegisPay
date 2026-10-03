package com.aegispay.app.platform.ops;

import com.aegispay.app.platform.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository records;
    private final ObjectMapper mapper;

    public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper mapper) {
        this.records = records;
        this.mapper = mapper;
    }

    @Transactional
    public <T> T run(String key, String method, String path, String requestBody, Class<T> type, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        UUID tenantId = TenantContext.requireTenantId();
        String hash = sha(requestBody == null ? "" : requestBody);
        Optional<IdempotencyRecord> existing = records.findByTenantIdAndIdempotencyKey(tenantId, key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(hash)) {
                throw new IllegalArgumentException("Idempotency-Key was reused with a different body");
            }
            try {
                return mapper.readValue(record.getResponseBody(), type);
            } catch (Exception e) {
                throw new IllegalStateException("Stored idempotent response could not be read", e);
            }
        }
        T result = action.get();
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(key);
            record.setMethod(method);
            record.setPath(path);
            record.setRequestHash(hash);
            record.setStatusCode(200);
            record.setResponseBody(mapper.writeValueAsString(result));
            records.save(record);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist idempotent response", e);
        }
        return result;
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
