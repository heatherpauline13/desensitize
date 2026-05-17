package com.desensitize.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskResult {

    private String originalValue;

    private String maskedValue;

    private String appliedType;

    private boolean masked;

    public static MaskResult unchanged(String value) {
        return MaskResult.builder()
                .originalValue(value)
                .maskedValue(value)
                .appliedType(null)
                .masked(false)
                .build();
    }

    public static MaskResult success(String originalValue, String maskedValue, String appliedType) {
        return MaskResult.builder()
                .originalValue(originalValue)
                .maskedValue(maskedValue)
                .appliedType(appliedType)
                .masked(true)
                .build();
    }
}
