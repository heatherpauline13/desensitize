package com.desensitize.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveTypeConfig {

    private String typeId;

    private String name;

    private String regexPattern;

    private String maskFormat;

    private String maskChar;

    @Builder.Default
    private boolean maskFlag = true;

    @Builder.Default
    private boolean isStrategyType = false;

    private List<SubSensitiveTypeConfig> subTypes;

    private String defaultMaskFormat;

    private String mixedRegexPattern;
}
