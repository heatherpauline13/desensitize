package com.desensitize.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubSensitiveTypeConfig {
    private String typeId;
    private String name;
    private String regexPattern;
    private String maskFormat;
    private String maskChar;
    @Builder.Default
    private boolean maskFlag = true;
}