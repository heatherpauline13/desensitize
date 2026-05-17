# 数据脱敏组件 - 验证清单

## 数据模型重构验证
- [x] `SubSensitiveTypeConfig` 类已创建，包含 typeId、name、regexPattern、maskFormat、maskChar、maskFlag 字段
- [x] `SensitiveTypeConfig` 已扩展：新增 subTypes、defaultMaskFormat、mixedRegexPattern、isStrategyType 字段
- [x] `DesensitizeConfigProperties` 支持绑定 subTypes、defaultMaskFormat、mixedRegexPattern 字段

## 配置文件结构验证
- [x] `desensitize-config.yml` 中所有数字类型均包含 `regexPattern`、`maskFormat`、`mixedRegexPattern` 三个字段
- [x] `desensitize-config.yml` 中 `name` 大类使用策略模式，包含 chinese_name、english_name、korean_name、japanese_name、xinjiang_name、default_name 子类型
- [x] `desensitize-config.yml` 中 `address` 大类使用策略模式，包含 chinese_address、english_address、japanese_address、default_address 子类型
- [x] `nationality` 的 `maskFlag` 设置为 `false`（正则过于宽泛）
- [x] 所有 `mixedRegexPattern` 包含上下文前缀/后缀约束（如"姓名："、"手机："等）
- [x] `name` 大类的 `mixedRegexPattern` 前缀必选（无 `?`），避免无上下文时误匹配

## 配置加载验证
- [x] 应用启动时 `desensitize-config.yml` 中的策略模式类型正确解析（33种敏感数据类型）
- [x] `DesensitizeRuleRegistry.isStrategyType("name")` 返回 `true`
- [x] `DesensitizeRuleRegistry.isStrategyType("phone")` 返回 `false`
- [x] `DesensitizeRuleRegistry.getSubTypes("name")` 返回6个子类型配置
- [x] 子类型的 YAML 配置正确绑定到 `SubSensitiveTypeConfig` 对象

## 单一类型脱敏 — 策略模式验证
- [x] `DesensitizeUtil.mask("张三", "name")` 正确识别为 chinese_name → 返回 `"张*"`
- [x] `DesensitizeUtil.mask("John Smith", "name")` 正确识别为 english_name → 返回 `"J*********"`
- [x] `DesensitizeUtil.mask("北京市朝阳区建国路100号", "address")` 正确识别为 chinese_address → 返回 `"北京市**********"`
- [x] 数字类型脱敏不受影响：`DesensitizeUtil.mask("13800138000", "phone")` → `"138****8000"`
- [x] null 和空字符串输入安全处理，不抛异常
- [x] maskFlag=false 的类型直接返回原值

## 长文本脱敏 — 混合上下文正则验证
- [x] "姓名：张三，手机：13800138000，身份证号：110101199001011234" → "姓名：张*，手机：138****8000，身份证号：110101********1234"
- [x] "今天天气很好，适合出去散步" → 文本完全不变（无上下文前缀，不误匹配）
- [x] "张三和李四去吃饭" → 不误脱敏"张三"和"李四"（缺少"姓名："等上下文前缀）
- [x] 手机号、身份证号等数字类型在长文本中正确脱敏
- [x] 多个正则匹配重叠区域时，长匹配优先，不重复处理

## 注解脱敏验证
- [x] `@Desensitize(type = "name")` 标注的字段，Jackson 序列化时走策略模式脱敏（DesensitizeJsonSerializer 调用 DesensitizeUtil.mask）
- [x] `@Desensitize(type = "address")` 标注的字段，Jackson 序列化时走策略模式脱敏
- [x] `@Desensitize(type = "phone")` 等数字类型注解工作正常，无退化

## 表格文件处理验证
- [x] CSV 表头"姓名"列使用 `name` 大类脱敏（策略模式自动识别中/英/韩文等）
- [x] CSV 表头"地址"列使用 `address` 大类脱敏（策略模式自动识别中/英/日文等）
- [x] CSV 表头"国家"列不脱敏（maskFlag=false）
- [x] XLSX 表头处理与 CSV 一致

## 命令行参数验证
- [x] `--input.string` 长文本模式使用 mixedRegexPattern
- [x] `--input.string` 单一类型模式（`--mask.mode=single --mask.type=name`）使用策略模式
- [x] `--input.table` 表格模式按列正确处理策略模式类型
- [x] 无参数运行显示帮助信息

## 编译打包验证
- [x] `mvn clean package -pl desensitize-runner -am -DskipTests` 编译成功
- [x] JAR 包端到端测试：长文本字符串输入结果正确
- [x] JAR 包端到端测试：单一类型输入结果正确
- [x] 脱敏结果符合 spec 中所有验收标准 (AC-1 ~ AC-12)