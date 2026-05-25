package com.desensitize.core.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class MaskFormat {

    private MaskFormatType type;

    private Integer param1;

    private Integer param2;

    public static MaskFormat parse(String formatStr) {
        if (formatStr == null || formatStr.isBlank()) {
            throw new IllegalArgumentException("脱敏格式字符串不能为空");
        }

        String trimmed = formatStr.trim();

        if ("maskAll()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.MASK_ALL, null, null);
        }

        if ("nameMask()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.NAME_MASK, null, null);
        }

        if ("emailMask()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.EMAIL_MASK, null, null);
        }

        if ("dateMask()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.DATE_MASK, null, null);
        }

        if ("landlineMask()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.LANDLINE_MASK, null, null);
        }

        if ("addressMask()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.ADDRESS_MASK, null, null);
        }

        if ("passportMask()".equalsIgnoreCase(trimmed)) {
            return new MaskFormat(MaskFormatType.PASSPORT_MASK, null, null);
        }

        if (trimmed.startsWith("preserve(") && trimmed.endsWith(")")) {
            String inner = trimmed.substring("preserve(".length(), trimmed.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("preserve格式错误，应为preserve(N,M): " + formatStr);
            }
            int keepPrefix = Integer.parseInt(parts[0].trim());
            int keepSuffix = Integer.parseInt(parts[1].trim());
            return new MaskFormat(MaskFormatType.PRESERVE, keepPrefix, keepSuffix);
        }

        if (trimmed.startsWith("replace(") && trimmed.endsWith(")")) {
            String inner = trimmed.substring("replace(".length(), trimmed.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("replace格式错误，应为replace(start,length): " + formatStr);
            }
            int start = Integer.parseInt(parts[0].trim());
            int length = Integer.parseInt(parts[1].trim());
            return new MaskFormat(MaskFormatType.REPLACE, start, length);
        }

        throw new IllegalArgumentException("不支持的脱敏格式: " + formatStr
                + "，支持: maskAll(), preserve(N,M), replace(start,length), nameMask(), emailMask(), dateMask(), landlineMask(), addressMask(), passportMask()");
    }
}
