package com.edison.project.common.response;

public record CursorPageInfo(
    Long nextCursorId,
    boolean hasNext,
    int size
) implements Pagination {}
