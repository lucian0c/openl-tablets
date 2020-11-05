package org.openl.rules.repository;

import java.util.Arrays;

public enum RepositoryMode {
    DESIGN,
    DEPLOY_CONFIG,
    PRODUCTION;

    @Override
    public String toString() {
        return name().toLowerCase().replaceAll("_", "-");
    }

    public static RepositoryMode getTypePrefix(String configPrefix) {
        String[] prefixParts = configPrefix.split("\\.");
        if (prefixParts.length > 1) {
            String clearPrefix = prefixParts[1];
            for (RepositoryMode mode : values()) {
                if (clearPrefix.startsWith(mode.toString())) {
                    return mode;
                }
            }
        }
        throw new UnsupportedOperationException("Unsupported configuration prefix is using");
    }

    public static boolean contains(String configName) {
        return Arrays.stream(values()).anyMatch(v -> v.toString().equals(configName));
    }

}
