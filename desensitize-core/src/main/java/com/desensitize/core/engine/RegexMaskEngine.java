package com.desensitize.core.engine;

import com.desensitize.core.model.MaskFormat;
import com.desensitize.core.model.MaskFormatType;
import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.model.SubSensitiveTypeConfig;
import com.desensitize.core.registry.DesensitizeRuleRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RegexMaskEngine {

    private static final int[] DATE_SEPARATORS;

    static {
        DATE_SEPARATORS = new int[128];
        DATE_SEPARATORS['-'] = 1;
        DATE_SEPARATORS['/'] = 1;
        DATE_SEPARATORS[':'] = 1;
    }

    private final DesensitizeRuleRegistry registry;

    public RegexMaskEngine(DesensitizeRuleRegistry registry) {
        this.registry = registry;
    }

    public String mask(String content, String typeId) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        SensitiveTypeConfig config = registry.getTypeConfig(typeId);
        if (config == null) {
            log.warn("未找到敏感类型 [{}]，返回原值", typeId);
            return content;
        }

        if (!config.isMaskFlag()) {
            return content;
        }

        if (config.isStrategyType()) {
            String maskChar = config.getMaskChar() != null ? config.getMaskChar() : "*";
            for (SubSensitiveTypeConfig subType : config.getSubTypes()) {
                if (subType.getRegexPattern() == null || subType.getRegexPattern().isEmpty()) {
                    continue;
                }
                Pattern p = Pattern.compile(subType.getRegexPattern());
                Matcher m = p.matcher(content);
                if (m.find()) {
                    return applyMaskValue(content, subType.getMaskFormat(), maskChar);
                }
            }
            if (config.getDefaultMaskFormat() != null) {
                return applyMaskValue(content, config.getDefaultMaskFormat(), maskChar);
            }
            return content;
        }

        return applyMask(content, config);
    }

    public String maskLongText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        List<MaskMatch> allMatches = collectMixedMatches(text);

        boolean[] maskedPositions = new boolean[text.length()];
        StringBuilder result = new StringBuilder(text);

        for (MaskMatch match : allMatches) {
            boolean alreadyMasked = false;
            for (int i = match.getStart(); i < match.getEnd() && i < maskedPositions.length; i++) {
                if (maskedPositions[i]) {
                    alreadyMasked = true;
                    break;
                }
            }

            if (alreadyMasked) {
                continue;
            }

            result.replace(match.getStart(), match.getEnd(), match.getMaskedValue());

            int newEnd = match.getStart() + match.getMaskedValue().length();
            for (int i = match.getStart(); i < newEnd && i < maskedPositions.length; i++) {
                maskedPositions[i] = true;
            }
        }

        return result.toString();
    }

    private List<MaskMatch> collectMixedMatches(String text) {
        List<MaskMatch> allMatches = new ArrayList<>();

        for (SensitiveTypeConfig config : registry.getTypeConfigs()) {
            if (!config.isMaskFlag()) {
                continue;
            }
            if (config.getMixedRegexPattern() == null || config.getMixedRegexPattern().isEmpty()) {
                continue;
            }

            try {
                Pattern pattern = Pattern.compile(config.getMixedRegexPattern());
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    String matchedText = matcher.group();
                    String coreData = matchedText;

                    if (matcher.groupCount() >= 2) {
                        coreData = matcher.group(2);
                    } else if (matcher.groupCount() == 1) {
                        coreData = matcher.group(1);
                    }

                    String maskedCore;
                    if (config.isStrategyType()) {
                        maskedCore = maskStrategyCore(coreData, config);
                    } else {
                        String maskChar = config.getMaskChar() != null ? config.getMaskChar() : "*";
                        maskedCore = applyMaskValue(coreData, config.getMaskFormat(), maskChar);
                    }

                    String fullMasked = matchedText.replace(coreData, maskedCore);

                    allMatches.add(new MaskMatch(matcher.start(), matcher.end(), fullMasked));
                }
            } catch (Exception e) {
                log.warn("混合正则匹配异常 typeId={}: {}", config.getTypeId(), e.getMessage());
            }
        }

        allMatches.sort(Comparator.comparingInt(MaskMatch::getLength).reversed());
        return allMatches;
    }

    private String maskStrategyCore(String coreData, SensitiveTypeConfig config) {
        String maskChar = config.getMaskChar() != null ? config.getMaskChar() : "*";
        for (SubSensitiveTypeConfig subType : config.getSubTypes()) {
            if (subType.getRegexPattern() == null || subType.getRegexPattern().isEmpty()) {
                continue;
            }
            Pattern p = Pattern.compile(subType.getRegexPattern());
            Matcher m = p.matcher(coreData);
            if (m.find()) {
                return applyMaskValue(coreData, subType.getMaskFormat(), maskChar);
            }
        }
        if (config.getDefaultMaskFormat() != null) {
            return applyMaskValue(coreData, config.getDefaultMaskFormat(), maskChar);
        }
        return coreData;
    }

    String applyMask(String content, SensitiveTypeConfig config) {
        String maskChar = config.getMaskChar();

        Pattern pattern;
        try {
            pattern = Pattern.compile(config.getRegexPattern());
        } catch (Exception e) {
            log.error("正则表达式编译失败 typeId={}, pattern={}", config.getTypeId(), config.getRegexPattern(), e);
            return content;
        }

        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return content;
        }

        return applyMaskValue(matcher.group(), config.getMaskFormat(), maskChar);
    }

    String applyMaskValue(String text, String formatExpression, String maskChar) {
        if (formatExpression == null || formatExpression.isEmpty()) {
            return text;
        }
        MaskFormat format = MaskFormat.parse(formatExpression);

        switch (format.getType()) {
            case MASK_ALL:
                return maskChar.repeat(text.length());
            case PRESERVE:
                return applyPreserve(text, format.getParam1(), format.getParam2(), maskChar);
            case REPLACE:
                return applyReplace(text, format.getParam1(), format.getParam2(), maskChar);
            case NAME_MASK:
                return applyNameMask(text, maskChar);
            case EMAIL_MASK:
                return applyEmailMask(text, maskChar);
            case DATE_MASK:
                return applyDateMask(text, maskChar);
            case LANDLINE_MASK:
                return applyLandlineMask(text, maskChar);
            case ADDRESS_MASK:
                return applyAddressMask(text, maskChar);
            case PASSPORT_MASK:
                return applyPassportMask(text, maskChar);
            default:
                return text;
        }
    }

    private String applyNameMask(String name, String maskChar) {
        if (name.contains(" ")) {
            return applyEnglishNameMask(name, maskChar);
        }
        if (name.length() <= 2) {
            return name.substring(0, 1) + maskChar.repeat(name.length() - 1);
        }
        return name.substring(0, 2) + maskChar.repeat(name.length() - 2);
    }

    private String applyEnglishNameMask(String name, String maskChar) {
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        result.append(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(' ');
            if (words[i].length() > 0) {
                result.append(maskChar.repeat(words[i].length()));
            }
        }
        return result.toString();
    }

    private String applyEmailMask(String email, String maskChar) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskChar.repeat(email.length());
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        StringBuilder maskedLocal = new StringBuilder();
        if (localPart.length() < 3) {
            maskedLocal.append(localPart);
            maskedLocal.append(maskChar.repeat(3));
        } else {
            maskedLocal.append(localPart, 0, 3);
            maskedLocal.append(maskChar.repeat(3));
        }
        return maskedLocal.toString() + domainPart;
    }

    private String applyDateMask(String dateStr, String maskChar) {
        StringBuilder result = new StringBuilder(dateStr.length());
        boolean inYear = true;
        for (int i = 0; i < dateStr.length(); i++) {
            char c = dateStr.charAt(i);
            if (c >= '0' && c <= '9') {
                if (inYear) {
                    result.append(c);
                } else {
                    result.append(maskChar);
                }
            } else {
                if (c < 128 && DATE_SEPARATORS[c] == 1) {
                    inYear = false;
                }
                result.append(c);
            }
        }
        return result.toString();
    }

    private String applyLandlineMask(String phone, String maskChar) {
        int dashIndex = phone.indexOf('-');
        if (dashIndex <= 0) {
            return applyPreserve(phone, 2, 2, maskChar);
        }
        String areaCode = phone.substring(0, dashIndex + 1);
        String number = phone.substring(dashIndex + 1);
        if (number.length() <= 2) {
            return areaCode + maskChar.repeat(number.length());
        }
        return areaCode
                + maskChar.repeat(number.length() - 2)
                + number.substring(number.length() - 2);
    }

    private String applyAddressMask(String address, String maskChar) {
        int shengIdx = address.indexOf('省');
        if (shengIdx >= 0) {
            String province = address.substring(0, shengIdx + 1);
            return province + maskChar.repeat(address.length() - province.length());
        }

        int kenIdx = address.indexOf('県');
        if (kenIdx >= 0) {
            String ken = address.substring(0, kenIdx + 1);
            return ken + maskChar.repeat(address.length() - ken.length());
        }

        int siIdx = indexOfKoreanRegion(address);
        if (siIdx >= 0) {
            String region = address.substring(0, siIdx + 1);
            return region + maskChar.repeat(address.length() - region.length());
        }

        int lastComma = address.lastIndexOf(',');
        if (lastComma >= 0) {
            String region = address.substring(lastComma + 1);
            return maskChar.repeat(lastComma + 1) + region.trim();
        }

        int lastSpace = address.lastIndexOf(' ');
        if (lastSpace >= 0) {
            String region = address.substring(lastSpace + 1);
            return maskChar.repeat(lastSpace + 1) + region;
        }

        if (address.length() <= 3) {
            return maskChar.repeat(address.length());
        }
        return address.substring(0, 3) + maskChar.repeat(address.length() - 3);
    }

    private int indexOfKoreanRegion(String address) {
        int len = address.length();
        for (int i = 0; i < len; i++) {
            char c = address.charAt(i);
            if (isKoreanSyllable(c)) {
                if (i + 1 < len) {
                    char next = address.charAt(i + 1);
                    if (next == '시' || next == '도') {
                        return i + 1;
                    }
                }
            }
            if (c == '시' || c == '도') {
                if (i >= 2) {
                    char prev1 = address.charAt(i - 1);
                    char prev2 = address.charAt(i - 2);
                    if (isKoreanSyllable(prev1) || isKoreanSyllable(prev2)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean isKoreanSyllable(char c) {
        return c >= 0xAC00 && c <= 0xD7AF;
    }

    private String applyPassportMask(String passport, String maskChar) {
        if (passport.length() <= 4) {
            return passport.charAt(0) + maskChar.repeat(passport.length() - 1);
        }
        return passport.charAt(0)
                + maskChar.repeat(passport.length() - 4)
                + passport.substring(passport.length() - 3);
    }

    private String applyPreserve(String text, int keepPrefix, int keepSuffix, String maskChar) {
        if (text.length() <= keepPrefix + keepSuffix) {
            return text;
        }
        String prefix = text.substring(0, keepPrefix);
        String suffix = text.substring(text.length() - keepSuffix);
        int maskLen = text.length() - keepPrefix - keepSuffix;
        return prefix + maskChar.repeat(maskLen) + suffix;
    }

    private String applyReplace(String text, int start, int length, String maskChar) {
        int actualStart = Math.max(0, start);
        int actualEnd = Math.min(text.length(), actualStart + length);
        int maskLen = actualEnd - actualStart;
        if (maskLen <= 0) {
            return text;
        }
        return text.substring(0, actualStart) + maskChar.repeat(maskLen) + text.substring(actualEnd);
    }

    private static class MaskMatch {
        private final int start;
        private final int end;
        private final String maskedValue;

        MaskMatch(int start, int end, String maskedValue) {
            this.start = start;
            this.end = end;
            this.maskedValue = maskedValue;
        }

        int getStart() { return start; }
        int getEnd() { return end; }
        int getLength() { return end - start; }
        String getMaskedValue() { return maskedValue; }
    }
}