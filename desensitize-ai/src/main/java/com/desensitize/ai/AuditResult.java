package com.desensitize.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditResult {

    private String type;

    private String content;

    private String suggestion;
}
