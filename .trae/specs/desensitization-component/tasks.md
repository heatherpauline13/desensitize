# 数据脱敏组件 - 任务列表

## 阶段一：脱敏格式类型扩展

### Task 1: 扩展 MaskFormatType 枚举
- [x] 新增 `NAME_MASK` — 姓名专用脱敏（2字保留1，3+字保留前2）
- [x] 新增 `EMAIL_MASK` — 邮箱专用脱敏（@前3位+3星，@后完整）
- [x] 新增 `DATE_MASK` — 日期专用脱敏（保留年份，月日屏蔽）
- [x] 新增 `LANDLINE_MASK` — 固话专用脱敏（区号保留，保留后2位）
- [x] 新增 `ADDRESS_MASK` — 地址专用脱敏（保留省市区路号，屏蔽具体位置）
- [x] 新增 `PASSPORT_MASK` — 护照专用脱敏（保留1字母+后3数字）

### Task 2: 扩展 MaskFormat 解析器
- [x] `MaskFormat.parse()` 支持解析 `nameMask()`、`emailMask()`、`dateMask()`、`landlineMask()`、`addressMask()`、`passportMask()`

## 阶段二：脱敏引擎核心重构

### Task 3: 重构 DesensitizeUtil.java
- [x] 新增 `applyNameMask()` — 2字保留第1字屏蔽第2字，3+字保留前2字屏蔽其余
- [x] 新增 `applyEmailMask()` — @前展示前3位+3个*，@后完整展示；@前<3位则全部展示+3*
- [x] 新增 `applyDateMask()` — 保留年份数字，月日信息用*屏蔽
- [x] 新增 `applyLandlineMask()` — 区号不隐藏，电话号码保留最后2位
- [x] 新增 `applyAddressMask()` — 保留省市区县+路街巷号，具体门牌号等信息屏蔽
- [x] 新增 `applyPassportMask()` — 保留1位字母和最后3位数字
- [x] 预编译静态 Pattern 常量（ADMIN_REGION_PATTERN, ADDRESS_SEGMENT_PATTERN）优化性能
- [x] 优化 `applyDateMask()` 使用字符查找表代替 String.valueOf
- [x] 优化 `maskBetween()` 使用 `isAddressKeepChar()` 静态方法代替正则匹配

### Task 4: 同步更新 RegexMaskEngine.java
- [x] 新增同名脱敏方法（applyNameMask, applyEmailMask, applyDateMask 等）
- [x] 新增 `maskStrategyCore()` 方法支持策略模式核心数据匹配
- [x] applyMaskValue() switch 分支覆盖所有新格式类型
- [x] 预编译静态 Pattern 常量和日期分隔符查找表

## 阶段三：配置文件重构

### Task 5: 重写 desensitize-config.yml
- [x] **姓名（name）** — 策略模式，6个子类型（chinese_name, english_name, xinjiang_name, korean_name, japanese_name, default_name），使用 nameMask()
- [x] **手机号码（phone）** — 改为策略模式，3个子类型：
  - mainland_phone：1[3-9]\d{9} → preserve(3,4)
  - hk_macau_phone：9\d{7} → preserve(2,2)
  - taiwan_phone：096\d{7} → preserve(3,3)
- [x] **身份证号码** — 拆分为两个独立类型（非策略模式）：
  - id_card：preserve(3,4) 保留前3位和后4位
  - id_card_with_birth：preserve(3,0) 仅保留前3位
- [x] **护照（passport）** — passportMask()
- [x] **固定电话（landline_domestic）** — landlineMask()
- [x] **地址（address）** — 策略模式，使用 addressMask()
- [x] **邮箱（email）** — emailMask()
- [x] **日期时间（date）** — 替代原 birthday 类型，dateMask()
- [x] **车牌号码（license_plate）** — preserve(2,2)
- [x] **银行卡号（bank_card）** — preserve(6,4)
- [x] 更新所有 mixedRegexPattern 适配新配置

## 阶段四：Runner 适配

### Task 6: 更新 FileProcessor.java 表头映射
- [x] 新增 id_card_with_birth、passport、email、license_plate、landline_domestic、date 等类型的中英文表头映射
- [x] 姓名列使用 name 大类 → 策略模式自动识别子类型
- [x] 手机号列使用 phone 大类 → 策略模式自动识别子类型

### Task 7: 验证其他模块兼容性
- [x] DesensitizeJsonSerializer 无需修改（调用 DesensitizeUtil.mask）
- [x] DesensitizeCommandRunner 无需修改（使用相同的 API）
- [x] DesensitizeRuleRegistry 注册逻辑兼容新配置

## 阶段五: 编译打包与验证

### Task 8: 端到端编译打包验证
- [x] `mvn clean package -pl desensitize-runner -am -DskipTests` 编译通过
- [x] 生成可执行 JAR：desensitize-runner/target/desensitize-runner-1.0.0-SNAPSHOT.jar
- [x] 所有脱敏规则按用户需求正确生效

# Task Dependencies
- Task 1 → Task 2（Task 2 的解析器依赖 Task 1 的枚举定义）
- Task 1, Task 2 → Task 3（脱敏引擎依赖格式类型和解析器）
- Task 3 → Task 4（RegexMaskEngine 同步更新依赖核心引擎）
- Task 1, Task 2 → Task 5（配置文件依赖格式类型定义）
- Task 3, Task 5 → Task 6（Runner 适配依赖引擎和配置完成）
- Task 6 → Task 8（编译打包依赖所有代码完成）
