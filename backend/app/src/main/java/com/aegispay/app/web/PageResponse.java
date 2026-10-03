package com.aegispay.app.web;

import java.util.List;

public record PageResponse<T>(String requestId, List<T> items, String nextCursor, int limit) {
}
