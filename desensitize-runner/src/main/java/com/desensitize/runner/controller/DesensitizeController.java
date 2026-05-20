package com.desensitize.runner.controller;

import com.desensitize.core.engine.DesensitizeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/desensitize")
@CrossOrigin(origins = "*")
public class DesensitizeController {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StringRequest {
        private String content;
        private String mode = "long_text";
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseResult {
        private boolean success;
        private String message;
        private Object data;

        public static ResponseResult success(Object data) {
            return new ResponseResult(true, "操作成功", data);
        }

        public static ResponseResult error(String message) {
            return new ResponseResult(false, message, null);
        }
    }

    @PostMapping("/string")
    public ResponseEntity<ResponseResult> desensitizeString(@RequestBody StringRequest request) {
        try {
            String content = request.getContent();
            String mode = request.getMode() != null ? request.getMode() : "long_text";
            
            String result;
            switch (mode) {
                case "single":
                    if (request.getType() == null || request.getType().isBlank()) {
                        return ResponseEntity.badRequest().body(ResponseResult.error("单一类型脱敏必须指定type参数"));
                    }
                    result = DesensitizeUtil.mask(content, request.getType());
                    break;
                case "ai":
                    try {
                        result = com.desensitize.ai.AiDesensitizeUtil.mask(content);
                    } catch (Exception e) {
                        log.error("AI脱敏失败", e);
                        return ResponseEntity.ok(ResponseResult.error("AI脱敏失败: " + e.getMessage()));
                    }
                    break;
                case "long_text":
                default:
                    result = DesensitizeUtil.maskLongText(content);
                    break;
            }

            Map<String, String> data = new HashMap<>();
            data.put("original", content);
            data.put("masked", result);
            return ResponseEntity.ok(ResponseResult.success(data));
        } catch (Exception e) {
            log.error("字符串脱敏失败", e);
            return ResponseEntity.ok(ResponseResult.error("脱敏失败: " + e.getMessage()));
        }
    }

    @PostMapping("/file")
    public ResponseEntity<ResponseResult> desensitizeFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ResponseResult.error("请选择要上传的文件"));
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".txt";

            Path resultDir = Paths.get("result");
            if (!Files.exists(resultDir)) {
                Files.createDirectories(resultDir);
            }

            String content = new String(file.getBytes(), "UTF-8");
            String maskedContent = DesensitizeUtil.maskLongText(content);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFilename = "masked_" + timestamp + extension;
            Path outputPath = resultDir.resolve(outputFilename);
            Files.writeString(outputPath, maskedContent, java.nio.charset.StandardCharsets.UTF_8);

            Map<String, String> data = new HashMap<>();
            data.put("originalFile", originalFilename);
            data.put("outputFile", outputFilename);
            data.put("outputPath", outputPath.toString());
            data.put("preview", maskedContent.length() > 500 ? maskedContent.substring(0, 500) + "..." : maskedContent);

            return ResponseEntity.ok(ResponseResult.success(data));
        } catch (IOException e) {
            log.error("文件脱敏失败", e);
            return ResponseEntity.ok(ResponseResult.error("文件处理失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("文件脱敏失败", e);
            return ResponseEntity.ok(ResponseResult.error("脱敏失败: " + e.getMessage()));
        }
    }

    @PostMapping("/table")
    public ResponseEntity<ResponseResult> desensitizeTable(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ResponseResult.error("请选择要脱敏的表格文件"));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || 
                (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls") && !originalFilename.endsWith(".csv"))) {
                return ResponseEntity.badRequest().body(ResponseResult.error("仅支持xlsx、xls和csv格式的表格文件"));
            }

            Path basePath = Paths.get(".").toAbsolutePath().normalize();
            Path tempDir = basePath.resolve("temp");
            Path resultDir = basePath.resolve("result");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            if (!Files.exists(resultDir)) {
                Files.createDirectories(resultDir);
            }

            Path tempInputPath = tempDir.resolve("input_" + System.currentTimeMillis() + "_" + originalFilename);
            file.transferTo(tempInputPath.toFile());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFilename = "masked_" + timestamp + originalFilename.substring(originalFilename.lastIndexOf("."));
            Path outputPath = resultDir.resolve(outputFilename);

            com.desensitize.runner.FileProcessor.processTableFile(tempInputPath.toAbsolutePath().toString(), outputPath.toAbsolutePath().toString());

            Map<String, String> data = new HashMap<>();
            data.put("originalFile", originalFilename);
            data.put("outputFile", outputFilename);
            data.put("outputPath", outputPath.toString());

            Files.deleteIfExists(tempInputPath);

            return ResponseEntity.ok(ResponseResult.success(data));
        } catch (IOException e) {
            log.error("表格脱敏失败", e);
            return ResponseEntity.ok(ResponseResult.error("文件处理失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("表格脱敏失败", e);
            return ResponseEntity.ok(ResponseResult.error("脱敏失败: " + e.getMessage()));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<ResponseResult> getSensitiveTypes() {
        try {
            com.desensitize.core.registry.DesensitizeRuleRegistry registry = 
                new com.desensitize.core.registry.DesensitizeRuleRegistry();
            
            Map<String, Object> data = new HashMap<>();
            for (com.desensitize.core.model.SensitiveTypeConfig config : registry.getAllTypes()) {
                Map<String, Object> typeInfo = new HashMap<>();
                typeInfo.put("typeId", config.getTypeId());
                typeInfo.put("name", config.getName());
                typeInfo.put("maskFlag", config.isMaskFlag());
                typeInfo.put("strategyType", config.isStrategyType());
                typeInfo.put("maskFormat", config.getMaskFormat());
                data.put(config.getTypeId(), typeInfo);
            }

            return ResponseEntity.ok(ResponseResult.success(data));
        } catch (Exception e) {
            log.error("获取敏感类型失败", e);
            return ResponseEntity.ok(ResponseResult.error("获取敏感类型失败: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("result").resolve(filename).normalize();
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            
            String encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/octet-stream"));
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setContentLength(fileContent.length);
            headers.set("Content-Encoding", "UTF-8");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("下载文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
