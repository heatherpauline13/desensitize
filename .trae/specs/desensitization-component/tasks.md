# 数据脱敏组件 - 任务列表

## 阶段一：数据模型重构（策略模式支持）

### Task 1: 扩展数据模型，支持两级配置结构
- [x] 新建 `SubSensitiveTypeConfig` 类，包含 `typeId`、`name`、`regexPattern`、`maskFormat`、`maskChar`、`maskFlag` 字段
  - 基于现有 `SensitiveTypeConfig` 的通用字段设计
  - 用于大类下的子类型定义
- [x] 扩展 `SensitiveTypeConfig` 类，新增字段：
  - `subTypes: List<SubSensitiveTypeConfig>` — 子类型列表（非数字类型使用）
  - `defaultMaskFormat: String` — 默认脱敏格式（所有子类型正则都不匹配时的 fallback）
  - `mixedRegexPattern: String` — 混合上下文正则（长文本脱敏使用）
  - `isStrategyType: boolean` — 标识是否为策略模式类型
- [x] 更新 `MaskFormatType` 枚举或工具类，确保 `defaultMaskFormat` 被正确解析

### Task 2: 更新 YAML 配置文件结构
- [x] 重写 `desensitize-config.yml`：
  - **数字类型**（phone、id_card、bank_card 等）：保留 `regexPattern`、`maskFormat`，新增 `mixedRegexPattern`
  - **非数字类型（策略模式）**：
    - `name` 大类：包含 `subTypes`（chinese_name、english_name、korean_name、japanese_name、xinjiang_name、default_name），每个子类型有独立 `regexPattern` 和 `maskFormat`
    - `address` 大类：包含 `subTypes`（chinese_address、english_address、japanese_address、default_address）
    - `name` 和 `address` 大类的 `mixedRegexPattern` 使用上下文约束正则
  - `nationality` 设置 `maskFlag: false`（过于宽泛）
- [x] 为所有类型编写 `mixedRegexPattern`（注意：name 的 mixedRegexPattern 前缀必选，不设 `?` 以避免误匹配）

### Task 3: 更新配置加载逻辑
- [x] `DesensitizeConfigProperties` 类通过 `@ConfigurationProperties` 自动绑定新字段（无代码改动）
- [x] 更新 `DesensitizeRuleRegistry`：
  - `register` 方法支持注册大类配置（含子类型），策略类型走 `registerStrategyType()`
  - 新增 `getConfig(String typeId)` 返回 `SensitiveTypeConfig`
  - 新增 `isStrategyType(String typeId)` 判断是否策略模式类型
  - 新增 `getSubTypes(String typeId)` 获取子类型列表
  - 新增 `getTypeConfigs()` / `getAllTypes()` 返回所有类型
- [x] YAML 中 `strategyType` 键名正确绑定到 `isStrategyType` 字段（Spring Boot 属性绑定规范）

## 阶段二：策略模式脱敏引擎实现

### Task 4: 实现单一类型脱敏的策略模式分支
- [x] 修改 `DesensitizeUtil.mask(content, typeId)` 方法：
  - 保留数字类型的精确正则匹配逻辑（无变化）
  - 新增策略模式分支：
    - 判断 `typeId` 是否为策略模式类型（`isStrategyType`）
    - 若是，遍历 `subTypes`，用每个子类型的 `regexPattern` 匹配 content
    - 第一个匹配到的子类型使用其 `maskFormat` 进行脱敏
    - 所有子类型都不匹配时，使用 `defaultMaskFormat` 进行脱敏
  - `maskFlag=false` 的配置依然直接返回原值

### Task 5: 实现长文本脱敏的混合正则匹配
- [x] 修改 `DesensitizeUtil.maskLongText()`：
  - 遍历所有已注册类型，收集其 `mixedRegexPattern`
  - 使用 `mixedRegexPattern` 进行正则匹配（而非 `regexPattern`）
  - 混合正则的捕获组提取：使用 group(2) 提取核心敏感数据部分
  - 策略类型核心数据委托给 `mask(coreData, typeId)` 走子类型匹配
  - 数字类型核心数据使用 `maskFormat` 直接脱敏
  - 替换回原文后按匹配长度降序排列，处理重叠
- [x] `RegexMaskEngine` 同步更新为相同逻辑

### Task 6: 策略模式单元测试
- [x] 通过 JAR 包端到端测试验证：
  - test Chinese name: `DesensitizeUtil.mask("张三", "name")` → `"张*"` ✅
  - test English name: `DesensitizeUtil.mask("John Smith", "name")` → `"J*********"` ✅
  - test fallback: 使用 defaultMaskFormat ✅
  - test Chinese address: `DesensitizeUtil.mask("北京市朝阳区建国路100号", "address")` → `"北京市**********"` ✅
  - test Phone unchanged: `DesensitizeUtil.mask("13800138000", "phone")` → `"138****8000"` ✅

## 阶段三：长文本脱敏集成

### Task 7: 长文本脱敏集成测试
- [x] 验证上下文正则能匹配：
  - "姓名：张三，手机：13800138000，身份证号：110101199001011234" → "姓名：张*，手机：138****8000，身份证号：110101********1234" ✅
- [x] 验证无上下文时不过度匹配：
  - "今天天气很好，适合出去散步" → 文本完全不变 ✅
- [x] 验证重叠匹配优先级：
  - 多个正则匹配到重叠区域时，长匹配优先 ✅

## 阶段四：注解与Runner适配

### Task 8: 更新注解脱敏序列化器
- [x] `DesensitizeJsonSerializer` 无需修改 — 已调用 `DesensitizeUtil.mask(value, typeId)`，typeId 来自注解，自动走策略模式

### Task 9: 更新 FileProcessor 表头映射
- [x] 更新 `HEADER_TYPE_MAP`：
  - `"姓名"` → `"name"`（策略模式大类，原 `"chinese_name"`）
  - `"name"` → `"name"`（策略模式大类，原 `"chinese_name"`）
  - `"地址"` → `"address"`（策略模式大类，原 `"chinese_address"`）
  - `"address"` → `"address"`（策略模式大类，原 `"chinese_address"`）
  - `"国家"` / `"nationality"` → `"nationality"`（maskFlag=false，不脱敏）

### Task 10: 更新 DesensitizeCommandRunner
- [x] `DesensitizeCommandRunner` 无需修改 — `mask.mode=long_text` 调用 `DesensitizeUtil.maskLongText()`，`mask.mode=single` 调用 `DesensitizeUtil.mask()`，内部已使用混合正则和策略模式

## 阶段五：编译打包与验证

### Task 11: 端到端编译打包验证
- [x] `mvn clean package -pl desensitize-runner -am -DskipTests` 编译通过 ✅
- [x] JAR 包端到端测试：
  - 字符串输入长文本模式 ✅
  - 字符串输入单一类型模式（策略模式 name/address） ✅
  - 数字类型不受影响 ✅
  - 无上下文纯文本不被误脱敏 ✅
- [x] 脱敏结果符合 spec 中定义的验收标准 ✅

# Task Dependencies
- Task 1 → Task 2（Task 2 的 YAML 结构依赖 Task 1 的数据模型）
- Task 2 → Task 3（Task 3 的配置加载依赖 Task 2 的 YAML 结构）
- Task 3 → Task 4, Task 5（策略模式和混合正则依赖配置加载完成）
- Task 4 → Task 6（单元测试依赖策略模式实现）
- Task 5 → Task 7（集成测试依赖混合正则实现）
- Task 4, Task 5 → Task 8, Task 9, Task 10（Runner 适配依赖核心引擎完成）
- Task 8, Task 9, Task 10 → Task 11（编译打包依赖所有代码完成）

Task 1 和 Task 8 可并行开发（无依赖关系）