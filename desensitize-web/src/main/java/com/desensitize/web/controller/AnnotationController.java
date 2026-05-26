package com.desensitize.web.controller;

import com.desensitize.web.vo.DesensitizedDataVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解脱敏演示控制器
 * 展示如何通过 @Desensitize 注解实现自动脱敏
 * 无需手动调用脱敏方法，序列化时自动脱敏
 */
@Slf4j
@RestController
@RequestMapping("/api/desensitize/annotation")
@CrossOrigin(origins = "*")
public class AnnotationController {

    /**
     * 请求 DTO - 接收原始敏感数据
     * 注意：这里不使用 @Desensitize 注解，因为这是输入数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensitiveDataRequest {
        private String name;
        private String phone;
        private String idCard;
        private String idCardWithBirth;
        private String email;
        private String bankCard;
        private String depositBook;
        private String address;
        private String passport;
        private String driverLicense;
        private String militaryId;
        private String licensePlate;
        private String ipAddress;
        private String macAddress;
        private String birthday;
        private String landline;
        private String internationalPhone;
        private String rmbAmount;
        private String foreignAmount;
        private String verificationCode;
        private String nationality;
        private String ethnicity;
        private String gender;
    }

    /**
     * 响应结果
     */
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

    /**
     * 演示注解脱敏 - 自动脱敏单个对象
     * 
     * 调用示例：
     * POST /api/desensitize/annotation/auto
     * {
     *   "name": "张三",
     *   "phone": "13912345678",
     *   "idCard": "320102199506156789",
     *   "email": "zhangsan@company.com",
     *   "bankCard": "6228481234567890123",
     *   "address": "上海市浦东新区陆家嘴环路1000号",
     *   "licensePlate": "京A12345",
     *   "ipAddress": "192.168.1.100"
     * }
     * 
     * 返回结果中所有敏感字段都会被自动脱敏
     */
    @PostMapping("/auto")
    public ResponseEntity<DesensitizedDataVO> desensitizeWithAnnotation(@RequestBody SensitiveDataRequest request) {
        log.info("接收到敏感数据脱敏请求");

        // 直接构建 VO 对象，无需手动调用脱敏方法
        // 序列化时 @Desensitize 注解会自动触发脱敏
        DesensitizedDataVO vo = DesensitizedDataVO.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .idCard(request.getIdCard())
                .idCardWithBirth(request.getIdCardWithBirth())
                .email(request.getEmail())
                .bankCard(request.getBankCard())
                .depositBook(request.getDepositBook())
                .address(request.getAddress())
                .passport(request.getPassport())
                .driverLicense(request.getDriverLicense())
                .militaryId(request.getMilitaryId())
                .licensePlate(request.getLicensePlate())
                .ipAddress(request.getIpAddress())
                .macAddress(request.getMacAddress())
                .birthday(request.getBirthday())
                .landline(request.getLandline())
                .internationalPhone(request.getInternationalPhone())
                .rmbAmount(request.getRmbAmount())
                .foreignAmount(request.getForeignAmount())
                .verificationCode(request.getVerificationCode())
                .nationality(request.getNationality())
                .ethnicity(request.getEthnicity())
                .gender(request.getGender())
                .createTime(LocalDateTime.now())
                .build();

        // 直接返回 VO 对象，Spring MVC 序列化时会自动应用 @Desensitize 注解
        return ResponseEntity.ok(vo);
    }

    /**
     * 演示注解脱敏 - 返回脱敏后的用户信息
     * 提供一个示例数据，方便测试
     */
    @GetMapping("/demo")
    public ResponseEntity<DesensitizedDataVO> getDemoData() {
        log.info("返回示例脱敏数据");

        DesensitizedDataVO vo = DesensitizedDataVO.builder()
                .name("李四")
                .phone("13888888888")
                .idCard("110101199001011234")
                .idCardWithBirth("110101199001011234")
                .email("lisi@example.com")
                .bankCard("6222021234567890123")
                .depositBook("1001234567890123456")
                .address("北京市朝阳区建国路88号")
                .passport("G87654321")
                .driverLicense("110101199001011234")
                .militaryId("J12345678")
                .licensePlate("京B88888")
                .ipAddress("10.0.0.1")
                .macAddress("AA-BB-CC-DD-EE-FF")
                .birthday("1990-01-01")
                .landline("010-12345678")
                .internationalPhone("+8613888888888")
                .rmbAmount("¥50000")
                .foreignAmount("$10000")
                .verificationCode("123456")
                .nationality("中国")
                .ethnicity("汉族")
                .gender("男")
                .createTime(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(vo);
    }

    /**
     * 演示注解脱敏 - 批量脱敏
     * 返回多个脱敏后的用户数据
     */
    @GetMapping("/batch")
    public ResponseEntity<Map<String, Object>> getBatchData() {
        log.info("返回批量脱敏数据");

        DesensitizedDataVO user1 = DesensitizedDataVO.builder()
                .name("张三")
                .phone("13912345678")
                .idCard("320102199506156789")
                .email("zhangsan@company.com")
                .address("上海市浦东新区陆家嘴环路1000号")
                .build();

        DesensitizedDataVO user2 = DesensitizedDataVO.builder()
                .name("John Smith")
                .phone("13987654321")
                .idCard("440301198512120000")
                .email("john.smith@company.com")
                .address("北京市海淀区中关村大街1号")
                .build();

        Map<String, Object> result = new HashMap<>();
        result.put("total", 2);
        result.put("data", new DesensitizedDataVO[]{user1, user2});

        return ResponseEntity.ok(result);
    }
}