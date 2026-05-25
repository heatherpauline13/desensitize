# 数据脱敏组件 - 验证清单

## 数据模型重构验证
- [x] `SubSensitiveTypeConfig` 类已创建，包含 typeId、name、regexPattern、maskFormat、maskChar、maskFlag 字段
- [x] `SensitiveTypeConfig` 已扩展：新增 subTypes、defaultMaskFormat、mixedRegexPattern、isStrategyType 字段
- [x] `DesensitizeConfigProperties` 支持绑定 subTypes、defaultMaskFormat、mixedRegexPattern 字段
- [x] `MaskFormatType` 新增 NAME_MASK、EMAIL_MASK、DATE_MASK、LANDLINE_MASK、ADDRESS_MASK、PASSPORT_MASK 枚举值
- [x] `MaskFormat.parse()` 支持解析 nameMask()、emailMask()、dateMask()、landlineMask()、addressMask()、passportMask() 格式

## 配置文件结构验证
- [x] 姓名（name）：策略模式，包含 chinese_name、english_name、xinjiang_name、korean_name、japanese_name、default_name 子类型
- [x] 手机号码（phone）：策略模式，包含 mainland_phone、hk_macau_phone、taiwan_phone 子类型
- [x] 身份证号拆分为两个独立类型：id_card（保留前3后4）和 id_card_with_birth（保留前3）
- [x] 护照号码使用 passportMask() 脱敏格式
- [x] 固定电话使用 landlineMask() 脱敏格式
- [x] 地址（address）：策略模式，4个子类型（chinese_address, english_address, japanese_address, default_address），使用 addressMask()，中文仅保留省、日文仅保留県、英文/韩文/德文/法文保留大区信息
- [x] 邮箱使用 emailMask() 脱敏格式
- [x] 日期时间使用 dateMask() 脱敏格式
- [x] 车牌号码使用 preserve(2,2)
- [x] 银行卡号使用 preserve(6,4)
- [x] nationality、ethnicity、gender 的 maskFlag 设置为 false（不脱敏）
- [x] 所有 mixedRegexPattern 包含上下文前缀/后缀约束

## 配置加载验证
- [x] 应用启动时 desensitize-config.yml 正确解析所有敏感数据类型
- [x] DesensitizeRuleRegistry.isStrategyType("name") 返回 true
- [x] DesensitizeRuleRegistry.isStrategyType("phone") 返回 true
- [x] DesensitizeRuleRegistry.isStrategyType("address") 返回 true
- [x] DesensitizeRuleRegistry.isStrategyType("id_card") 返回 false（独立数字类型）
- [x] DesensitizeRuleRegistry.getSubTypes("phone") 返回3个子类型配置

## 单一类型脱敏验证
- [x] 姓名：mask("张三", "name") → "张*"（2字，屏蔽第2字）
- [x] 姓名：mask("欧阳锋", "name") → "欧阳*"（3字，保留前2，屏蔽其余）
- [x] 姓名英文：mask("John Smith", "name") → "John *****"（空格分隔保留第一个单词，其余屏蔽）
- [x] 姓名英文单单词：mask("Tom", "name") → "T**"（保留首字母，其余屏蔽）
- [x] 手机：mask("13800138000", "phone") → "138****8000"（大陆11位）
- [x] 手机：mask("91234567", "phone") → "91****67"（港澳8位）
- [x] 手机：mask("0961234567", "phone") → "096***4567"（台湾10位）
- [x] 身份证：mask("110101199001011234", "id_card") → "110***********1234"（保留前3后4）
- [x] 身份证含出生：mask("110101199001011234", "id_card_with_birth") → "110***************"（仅保留前3）
- [x] 护照：mask("E12345678", "passport") → "E*****678"（保留1字母+后3数字）
- [x] 固话：mask("010-12345678", "landline_domestic") → "010-******78"（区号保留，保留后2位）
- [x] 地址中文：mask("江苏省南京市鼓楼区中山路100号", "address") → "江苏省*************"（仅保留省信息）
- [x] 地址日文：mask("東京都新宿区西新宿", "address") → "東京都*****"（仅保留県信息）
- [x] 地址英文：mask("123 Main Street, New York", "address") → "*****New York"（保留大区信息）
- [x] 车牌：mask("京A12345", "license_plate") → "京A**45"（保留地区编码和后2位）
- [x] 邮箱：mask("test@example.com", "email") → "tes***@example.com"（@前展示3位+3星）
- [x] 邮箱短用户名：mask("ab@test.com", "email") → "ab***@test.com"（少于3位全部展示+3星）
- [x] 日期：mask("2024-05-17", "date") → "2024-**-**"（保留年份，月日屏蔽）
- [x] 银行卡：mask("6222021234567890123", "bank_card") → "622202*********0123"（保留前6后4）
- [x] null 和空字符串输入安全处理，不抛异常
- [x] maskFlag=false 的类型直接返回原值

## 长文本脱敏验证
- [x] 包含上下文前缀的敏感数据能被正确识别和脱敏
- [x] 无上下文前缀的纯文本不被误匹配
- [x] 重叠匹配按长匹配优先原则处理
- [x] 策略模式类型在长文本中正确识别子类型

## 注解脱敏验证
- [x] @Desensitize 注解正确调用 DesensitizeUtil.mask()
- [x] 策略模式类型注解脱敏正常
- [x] 数字类型注解脱敏正常

## 表格文件处理验证
- [x] CSV/XLSX 表头映射正确识别中英文列名
- [x] 新增的 id_card_with_birth、passport、email、license_plate、landline_domestic 类型列映射正常
- [x] 策略模式类型列自动识别子类型

## 命令行参数验证
- [x] --input.string 长文本模式使用 mixedRegexPattern
- [x] --input.string 单一类型模式使用策略模式
- [x] --input.table 表格模式按列正确处理所有类型
- [x] 无参数运行显示帮助信息

## 编译打包验证
- [x] mvn clean package -pl desensitize-runner -am -DskipTests 编译成功
- [x] 生成可执行 JAR：desensitize-runner/target/desensitize-runner-1.0.0-SNAPSHOT.jar
- [x] 所有新脱敏规则正确生效

## AI脱敏验证
- [x] AiDesensitizeUtil.initialize() 完成初始化后可正常调用 mask()
- [x] AiDesensitizeUtil.isAvailable() 在未初始化时返回 false
- [x] 未初始化时调用 mask() 抛出 IllegalStateException 明确提示
