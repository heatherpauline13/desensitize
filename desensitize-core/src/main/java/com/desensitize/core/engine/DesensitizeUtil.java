package com.desensitize.core.engine;

import com.desensitize.core.model.MaskFormat;
import com.desensitize.core.model.MaskFormatType;
import com.desensitize.core.model.SensitiveTypeConfig;
import com.desensitize.core.model.SubSensitiveTypeConfig;
import com.desensitize.core.registry.DesensitizeRuleRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DesensitizeUtil {

    private static volatile DesensitizeRuleRegistry registry;

    private DesensitizeUtil() {
    }

    public static void setRegistry(DesensitizeRuleRegistry ruleRegistry) {
        registry = ruleRegistry;
    }

    private static DesensitizeRuleRegistry getRegistry() {
        if (registry == null) {
            throw new IllegalStateException("DesensitizeRuleRegistry 未初始化，请确保应用已正确启动");
        }
        return registry;
    }

    public static String mask(String content, String typeId) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        SensitiveTypeConfig config = getRegistry().getTypeConfig(typeId);
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
                    return applyMaskFormat(content, subType.getMaskFormat(), maskChar);
                }
            }

            if (config.getDefaultMaskFormat() != null) {
                return applyMaskFormat(content, config.getDefaultMaskFormat(), maskChar);
            }
            return content;
        }

        return applySingleTypeMask(content, config);
    }

    public static String maskLongText(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        List<SensitiveTypeConfig> allTypes = getRegistry().getAllTypes();
        List<MaskMatch> allMatches = new ArrayList<>();

        for (SensitiveTypeConfig config : allTypes) {
            if (!config.isMaskFlag()) {
                continue;
            }
            String regex = config.getMixedRegexPattern();
            if (regex == null || regex.isEmpty()) {
                continue;
            }
            try {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(content);
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
                        maskedCore = mask(coreData, config.getTypeId());
                    } else {
                        String maskChar = config.getMaskChar() != null ? config.getMaskChar() : "*";
                        maskedCore = applyMaskFormat(coreData, config.getMaskFormat(), maskChar);
                    }
                    String fullMasked = matchedText.replace(coreData, maskedCore);

                    allMatches.add(new MaskMatch(
                            matcher.start(),
                            matcher.end(),
                            fullMasked,
                            config
                    ));
                }
            } catch (Exception e) {
                log.warn("混合正则匹配异常 typeId={}: {}", config.getTypeId(), e.getMessage());
            }
        }

        allMatches.sort(Comparator.comparingInt(MaskMatch::getLength).reversed());

        boolean[] maskedPositions = new boolean[content.length()];
        StringBuilder result = new StringBuilder(content);

        for (MaskMatch match : allMatches) {
            boolean alreadyMasked = false;
            for (int i = match.getStart(); i < match.getEnd(); i++) {
                if (maskedPositions[i]) {
                    alreadyMasked = true;
                    break;
                }
            }

            if (alreadyMasked) {
                continue;
            }

            result.replace(match.getStart(), match.getEnd(), match.getMatchedText());

            for (int i = match.getStart(); i < match.getStart() + match.getMatchedText().length(); i++) {
                if (i < maskedPositions.length) {
                    maskedPositions[i] = true;
                }
            }
        }

        return result.toString();
    }

    private static String applySingleTypeMask(String content, SensitiveTypeConfig config) {
        MaskFormat format = MaskFormat.parse(config.getMaskFormat());
        String maskChar = config.getMaskChar();

        Pattern pattern;
        try {
            pattern = Pattern.compile(config.getRegexPattern());
        } catch (Exception e) {
            log.error("正则表达式编译失败 typeId={}", config.getTypeId(), e);
            return content;
        }

        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return content;
        }

        return applyMaskValue(matcher.group(), config);
    }

    static String applyMaskValue(String text, SensitiveTypeConfig config) {
        MaskFormat format = MaskFormat.parse(config.getMaskFormat());
        String maskChar = config.getMaskChar();

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

    private static String applyMaskFormat(String content, String formatExpression, String maskChar) {
        MaskFormat format = MaskFormat.parse(formatExpression);

        switch (format.getType()) {
            case MASK_ALL:
                return maskChar.repeat(content.length());
            case PRESERVE:
                return applyPreserve(content, format.getParam1(), format.getParam2(), maskChar);
            case REPLACE:
                return applyReplace(content, format.getParam1(), format.getParam2(), maskChar);
            default:
                return content;
        }
    }

    private static String applyPreserve(String text, int keepPrefix, int keepSuffix, String maskChar) {
        if (text.length() <= keepPrefix + keepSuffix) {
            return text;
        }
        String prefix = text.substring(0, keepPrefix);
        String suffix = text.substring(text.length() - keepSuffix);
        int maskLen = text.length() - keepPrefix - keepSuffix;
        return prefix + maskChar.repeat(maskLen) + suffix;
    }

    private static String applyReplace(String text, int start, int length, String maskChar) {
        int actualStart = Math.max(0, start);
        int actualEnd = Math.min(text.length(), actualStart + length);
        int maskLen = actualEnd - actualStart;
        if (maskLen <= 0) {
            return text;
        }
        return text.substring(0, actualStart) + maskChar.repeat(maskLen) + text.substring(actualEnd);
    }

    static class MaskMatch {
        private final int start;
        private final int end;
        private final String matchedText;
        private final SensitiveTypeConfig config;

        MaskMatch(int start, int end, String matchedText, SensitiveTypeConfig config) {
            this.start = start;
            this.end = end;
            this.matchedText = matchedText;
            this.config = config;
        }

        int getStart() { return start; }
        int getEnd() { return end; }
        int getLength() { return matchedText.length(); }
        String getMatchedText() { return matchedText; }
        SensitiveTypeConfig getConfig() { return config; }
    }
}
