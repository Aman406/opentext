package com.opentext.test;

import java.util.UUID;

public record TaskGroup(UUID groupUUID) {
    public TaskGroup {
        if (groupUUID == null) {
            throw new IllegalArgumentException("groupUUID must not be null");
        }
    }
}