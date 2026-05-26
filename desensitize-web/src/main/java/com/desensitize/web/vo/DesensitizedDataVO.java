package com.desensitize.web.vo;

import com.desensitize.annotation.Desensitize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 敏感数据脱敏 VO 类
 * 通过 @Desensitize 注解实现自动脱敏，无需手动调用脱敏方法
 * 序列化时自动应用脱敏规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesensitizedDataVO {

    /**
     * 姓名（中文/英文通用）
     * 脱敏规则：保留首字符，其余用*替换
     * 示例：张三 -> 张*，John -> J***
     */
    @Desensitize(type = "name")
    private String name;

    /**
     * 手机号
     * 脱敏规则：保留前3位和后4位
     * 示例：13912345678 -> 139****5678
     */
    @Desensitize(type = "phone")
    private String phone;

    /**
     * 身份证号（脱敏生日部分）
     * 脱敏规则：保留前6位和后4位，中间生日部分脱敏
     * 示例：320102199506156789 -> 320102********6789
     */
    @Desensitize(type = "id_card")
    private String idCard;

    /**
     * 身份证号（保留生日显示）
     * 脱敏规则：保留前6位、生日部分和后4位
     * 示例：320102199506156789 -> 320102199506156789（生日可见）
     */
    @Desensitize(type = "id_card_with_birth")
    private String idCardWithBirth;

    /**
     * 邮箱地址
     * 脱敏规则：保留邮箱前缀首字符和域名
     * 示例：zhangsan@company.com -> z********@company.com
     */
    @Desensitize(type = "email")
    private String email;

    /**
     * 银行卡号
     * 脱敏规则：保留前6位和后4位
     * 示例：6228481234567890123 -> 622848*********0123
     */
    @Desensitize(type = "bank_card")
    private String bankCard;

    /**
     * 存折号
     * 脱敏规则：保留前4位和后4位
     * 示例：1001234567890123456 -> 1001***********456
     */
    @Desensitize(type = "deposit_book")
    private String depositBook;

    /**
     * 家庭住址
     * 脱敏规则：保留省市区，详细地址脱敏
     * 示例：上海市浦东新区陆家嘴环路1000号 -> 上海市浦东新区********
     */
    @Desensitize(type = "address")
    private String address;

    /**
     * 护照号
     * 脱敏规则：保留前2位和后2位
     * 示例：G12345678 -> G1****78
     */
    @Desensitize(type = "passport")
    private String passport;

    /**
     * 驾驶证号
     * 脱敏规则：保留前4位和后4位
     * 示例：110101199506156789 -> 1101**********6789
     */
    @Desensitize(type = "driver_license")
    private String driverLicense;

    /**
     * 军官证号
     * 脱敏规则：保留前2位和后2位
     * 示例：J12345678 -> J1****78
     */
    @Desensitize(type = "military_id")
    private String militaryId;

    /**
     * 车牌号
     * 脱敏规则：保留省份缩写和最后2位
     * 示例：京A12345 -> 京A***45
     */
    @Desensitize(type = "license_plate")
    private String licensePlate;

    /**
     * IP地址
     * 脱敏规则：保留前两段，后两段脱敏
     * 示例：192.168.1.100 -> 192.168.*.*
     */
    @Desensitize(type = "ip_address")
    private String ipAddress;

    /**
     * MAC地址
     * 脱敏规则：保留前3段，后3段脱敏
     * 示例：00-1A-2B-3C-4D-5E -> 00-1A-2B-**-**-**
     */
    @Desensitize(type = "mac_address")
    private String macAddress;

    /**
     * 出生日期
     * 脱敏规则：完全脱敏（根据配置）
     * 示例：1995-06-15 -> **********
     */
    @Desensitize(type = "date")
    private String birthday;

    /**
     * 固定电话
     * 脱敏规则：保留区号和后4位
     * 示例：010-87654321 -> 010-****4321
     */
    @Desensitize(type = "landline_domestic")
    private String landline;

    /**
     * 国际电话
     * 脱敏规则：保留国家码和后4位
     * 示例：+8613912345678 -> +86****5678
     */
    @Desensitize(type = "international_phone")
    private String internationalPhone;

    /**
     * 人民币金额
     * 脱敏规则：根据金额大小进行脱敏
     * 示例：¥5000 -> ¥****
     */
    @Desensitize(type = "rmb_amount")
    private String rmbAmount;

    /**
     * 外币金额
     * 脱敏规则：根据金额大小进行脱敏
     * 示例：$1000 -> $****
     */
    @Desensitize(type = "foreign_amount")
    private String foreignAmount;

    /**
     * 验证码
     * 脱敏规则：完全脱敏
     * 示例：684291 -> ******
     */
    @Desensitize(type = "verification_code")
    private String verificationCode;

    /**
     * 国籍
     * 脱敏规则：保留首字符
     * 示例：中国 -> 中*
     */
    @Desensitize(type = "nationality")
    private String nationality;

    /**
     * 民族
     * 脱敏规则：保留首字符
     * 示例：汉族 -> 汉*
     */
    @Desensitize(type = "ethnicity")
    private String ethnicity;

    /**
     * 性别
     * 脱敏规则：保留首字符
     * 示例：男 -> 男
     */
    @Desensitize(type = "gender")
    private String gender;

    /**
     * 创建时间（用于记录）
     * 注意：此字段未使用 @Desensitize 注解，不会被脱敏
     */
    private LocalDateTime createTime;
}