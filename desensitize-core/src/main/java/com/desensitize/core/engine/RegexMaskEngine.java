package com.desensitize.core.engine;

import com.desensitize.core.model.MaskFormat;
import com.desensitize.core.model.MaskFormatType;
import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.registry.DesensitizeRuleRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RegexMaskEngine {

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
                        maskedCore = applyMaskValue(coreData, config.getDefaultMaskFormat(),
                                config.getMaskChar() != null ? config.getMaskChar() : "*");
                    } else {
                        maskedCore = applyMaskValue(coreData, config.getMaskFormat(),
                                config.getMaskChar() != null ? config.getMaskChar() : "*");
                    }

                    String fullMasked = matchedText.replace(coreData, maskedCore);

                    allMatches.add(new MaskMatch(matcher.start(), matcher.end(), fullMasked));
                }
            } catch (Exception e) {
                log.warn("混合正则匹配异常 typeId={}: {}", config.getTypeId(), e.getMessage());
            }
        }

        allMatches.sort((a, b) -> Integer.compare(b.getLength(), a.getLength()));
        return allMatches;
    }

    String applyMask(String content, SensitiveTypeConfig config) {
        MaskFormat format = MaskFormat.parse(config.getMaskFormat());
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

        String matchedText = matcher.group();

        String masked;
        switch (format.getType()) {
            case MASK_ALL:
                masked = maskChar.repeat(matchedText.length());
                break;
            case PRESERVE:
                masked = applyPreserve(matchedText, format.getParam1(), format.getParam2(), maskChar);
                break;
            case REPLACE:
                masked = applyReplace(matchedText, format.getParam1(), format.getParam2(), maskChar);
                break;
            default:
                masked = matchedText;
        }

        return new StringBuilder(content)
                .replace(matcher.start(), matcher.end(), masked)
                .toString();
    }

    private String applyMaskValue(String text, String formatExpression, String maskChar) {
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
            default:
                return text;
        }
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