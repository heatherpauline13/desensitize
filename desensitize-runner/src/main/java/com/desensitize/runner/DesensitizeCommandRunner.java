package com.desensitize.runner;

import com.desensitize.core.engine.DesensitizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DesensitizeCommandRunner implements CommandLineRunner {

    private static final String HELP_TEXT = """
            数据脱敏组件 - 命令行使用说明
            ================================
            用法: java -jar desensitize-runner.jar [选项]
            
            选项:
              --input.string=<文本>    对输入字符串进行脱敏，结果输出到控制台
              --input.file=<文件路径>  对文档文件进行脱敏，结果保存到 result/ 目录
              --input.table=<文件路径> 对表格文件进行脱敏，结果保存到 result/ 目录
              --mask.mode=<模式>       脱敏模式: long_text(默认) | single | ai
              --mask.type=<类型ID>     单一类型脱敏时指定的类型（与 --mask.mode=single 配合使用）
            
            示例:
              java -jar desensitize-runner.jar --input.string="张三的电话是13800138000"
              java -jar desensitize-runner.jar --input.file=./test.txt
              java -jar desensitize-runner.jar --input.table=./data.csv
              java -jar desensitize-runner.jar --input.string="13800138000" --mask.mode=single --mask.type=phone
            
            注意: --input.string、--input.file、--input.table 三个参数互斥，只能指定其中一个
            """;

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println(HELP_TEXT);
            return;
        }

        String inputString = null;
        String inputFile = null;
        String inputTable = null;
        String maskMode = "long_text";
        String maskType = null;

        for (String arg : args) {
            if (arg.startsWith("--input.string=")) {
                inputString = arg.substring("--input.string=".length());
            } else if (arg.startsWith("--input.file=")) {
                inputFile = arg.substring("--input.file=".length());
            } else if (arg.startsWith("--input.table=")) {
                inputTable = arg.substring("--input.table=".length());
            } else if (arg.startsWith("--mask.mode=")) {
                maskMode = arg.substring("--mask.mode=".length());
            } else if (arg.startsWith("--mask.type=")) {
                maskType = arg.substring("--mask.type=".length());
            }
        }

        int inputModeCount = 0;
        if (inputString != null) inputModeCount++;
        if (inputFile != null) inputModeCount++;
        if (inputTable != null) inputModeCount++;

        if (inputModeCount == 0) {
            System.out.println(HELP_TEXT);
            return;
        }

        if (inputModeCount > 1) {
            System.err.println("错误: --input.string、--input.file、--input.table 不能同时指定多个");
            System.exit(1);
        }

        if (inputString != null) {
            handleStringInput(inputString, maskMode, maskType);
        } else if (inputFile != null) {
            FileProcessor.processDocumentFile(inputFile);
        } else if (inputTable != null) {
            FileProcessor.processTableFile(inputTable);
        }
    }

    private void handleStringInput(String content, String mode, String type) {
        String result;
        switch (mode) {
            case "single":
                if (type == null || type.isBlank()) {
                    System.err.println("错误: --mask.mode=single 时必须指定 --mask.type");
                    System.exit(1);
                    return;
                }
                result = DesensitizeUtil.mask(content, type);
                break;
            case "ai":
                try {
                    result = com.desensitize.ai.AiDesensitizeUtil.mask(content);
                } catch (Exception e) {
                    System.err.println("AI脱敏失败: " + e.getMessage());
                    System.exit(1);
                    return;
                }
                break;
            case "long_text":
            default:
                result = DesensitizeUtil.maskLongText(content);
                break;
        }
        System.out.println(result);
    }
}
