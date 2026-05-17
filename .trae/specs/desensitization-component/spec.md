# 数据脱敏组件 - 产品需求文档

## 概述

* **摘要**: 基于Spring Boot框架实现一个可独立运行的数据脱敏组件(JAR包)。组件通过YAML配置文件定义敏感数据类型及其脱敏规则（精确匹配正则、混合上下文正则、脱敏格式、掩码符号），通过注解方式标记需要脱敏的字段。支持单一类型脱敏、长文本全面扫描脱敏、以及基于Spring AI的内嵌式AI脱敏三种模式。JAR包支持命令行运行，可处理字符串输入（输出到控制台）、文档文件（长文本脱敏后保存到result目录）和表格文件（逐字段脱敏后保存到result目录）。

* **目的**: 为各类Java应用提供统一的、符合GB/T 45574-2025国家标准的敏感数据脱敏能力，降低敏感信息泄露风险，满足《个人信息保护法》《数据安全法》等法规的合规要求。

* **目标用户**: Java后端开发者、数据安全工程师、需要处理敏感数据的业务系统。

## 目标

* 提供配置驱动的脱敏规则管理，支持YAML文件中定义多种敏感数据类型及其精确匹配正则、混合上下文正则、脱敏格式、掩码符号

* 提供基于注解的字段级脱敏能力，开发者通过简单注解即可标记字段所属的敏感数据类型

* **长文本脱敏使用混合上下文正则**，通过上下文前缀/后缀约束避免正则过于宽泛导致的误匹配

* **非数字类型采用策略模式**，配置文件两级结构（大类 → 子类型），脱敏时自动识别子类型并使用对应规则

* 提供基于Spring AI的AI脱敏方法，通过LLM接口实现智能脱敏

* 提供可独立运行的JAR包，支持字符串、文档文件、表格文件三种输入方式

* 脱敏规则全面覆盖GB/T 45574-2025标准中定义的敏感数据类型

## 非目标（不在范围内）

* 不提供数据库层面的脱敏能力（如SQL视图脱敏、数据库脱敏函数）

* 不提供实时日志脱敏的AOP切面拦截能力（仅提供组件级API供上层集成）

* 不提供Web API接口形式的脱敏服务

* 不提供GUI图形界面

* 不提供脱敏数据的水印技术或访问权限控制

## 背景与上下文

* 国家标准GB/T 45574-2025《数据安全技术 敏感个人信息处理安全要求》已于2025年11月1日正式实施

* 相关法律法规：《个人信息保护法》《网络安全法》《数据安全法》

* 参考标准：GB/T 35273-2020、GB/T 37964-2019

* 现有脱敏规则文档已定义14大类敏感数据类型的规范脱敏格式（参见`敏感数据类型脱敏规则.md`）

* Spring AI框架提供了统一的AI模型调用抽象，支持OpenAI、Azure OpenAI等多种模型

## 功能需求

### FR-1: 配置文件驱动的脱敏规则管理

系统应支持通过YAML配置文件定义敏感数据类型。每条类型包含以下字段：

**通用字段（所有类型）**：
| 字段 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| `typeId` | String | 类型唯一标识 | **必填** |
| `name` | String | 类型中文名 | — |
| `maskChar` | String | 掩码符号 | 继承全局 `*` |
| `maskFlag` | boolean | 是否执行脱敏 | `true` |

**数字类型（手机号、身份证号、银行卡号等）**：
| 字段 | 类型 | 说明 |
|------|------|------|
| `regexPattern` | String | 精确匹配正则（单一类型脱敏使用） |
| `mixedRegexPattern` | String | 混合上下文正则（长文本脱敏使用，带上下文前后缀约束） |
| `maskFormat` | String | 脱敏格式模板 |

**非数字类型（姓名、地址等，采用策略模式）**：
| 字段 | 类型 | 说明 |
|------|------|------|
| `subTypes` | List | 子类型列表，每个子类型含 `typeId`、`name`、`regexPattern`、`maskFormat` |
| `defaultMaskFormat` | String | 默认脱敏格式（所有子类型正则都不匹配时使用） |
| `mixedRegexPattern` | String | 混合上下文正则（长文本脱敏使用） |

### FR-2: 注解驱动的字段脱敏

系统应提供`@Desensitize`注解，可标注在Java Bean的字段上。注解支持`type`属性指定该字段所属的敏感数据类型。配合序列化框架（Jackson）在序列化时自动执行脱敏。

对于非数字类型，`type` 值使用大类标识（如 `name`），脱敏时自动识别子类型。

### FR-3: 单一类型脱敏方法（精确正则匹配）

系统应提供`DesensitizeUtil.mask(String content, String type)`方法：
- **数字类型**：使用配置中的 `regexPattern` 进行精确匹配，匹配到则按 `maskFormat` 脱敏
- **非数字类型（策略模式）**：遍历 `subTypes` 中每个子类型的 `regexPattern`，匹配到则按该子类型的 `maskFormat` 脱敏；全部不匹配则使用 `defaultMaskFormat`
- 输入为 null 或空字符串时返回原值
- `maskFlag=false` 时直接返回原值

### FR-4: 长文本全面脱敏方法（混合上下文正则）

系统应提供`DesensitizeUtil.maskLongText(String content)`方法，对长篇文本内容进行全量扫描：
- 使用所有已注册类型的 **`mixedRegexPattern`（混合上下文正则）** 进行匹配
- 混合正则通过上下文前缀/后缀约束，避免过于宽泛的匹配（如姓名不会匹配所有2-4字中文，仅匹配"姓名：张三"、"尊敬的张三"等上下文中的姓名）
- 将匹配到的内容按对应脱敏格式替换，返回全面脱敏后的字符串

**重叠匹配优先级规则**: 当多个正则模式匹配到同一段文本内容时，匹配内容长度更长的规则优先执行脱敏。已被脱敏处理过的文本区域不再参与后续规则的匹配和脱敏，避免重复处理破坏脱敏结果。

**混合正则设计原则**：
- 姓名类：`(姓名[：:]\s*|尊敬的|亲爱的)?([姓名正则])(先生|女士|同志)?`
- 手机号类：`(手机|电话|联系方式|Tel)[：:]\s*([手机号正则])`
- 身份证号类：`(身份证|证件号|ID)[：:]\s*([身份证正则])`
- 银行卡号类：`(银行卡|卡号|账号)[：:]\s*([银行卡正则])`
- 地址类：`(地址|住址|Address)[：:]\s*([地址正则])`
- 邮箱类：`(邮箱|Email|邮件)[：:]\s*([邮箱正则])`

### FR-5: AI脱敏方法（Spring AI）

系统应提供`AiDesensitizeUtil.mask(String content)`方法，通过Spring AI调用配置的AI模型接口执行脱敏。

脱敏提示词模板内置于项目配置文件（`desensitize-config.yml`）中，在应用启动时加载，每次调用自动使用配置的提示词模板与输入内容组合后请求AI接口。调用方无需传入提示词，仅需提供待脱敏的内容。AI相关接口配置（模型类型、API Key、Endpoint等）通过Spring AI标准的`application.yml`配置管理。

### FR-6: 可独立运行的JAR包

组件应可编译为可执行的Spring Boot JAR包（spring-boot-maven-plugin），通过命令行接收参数运行。

### FR-7: 字符串输入处理

JAR包运行时应支持`--input.string`参数接收字符串输入，脱敏后将结果输出到标准控制台。

### FR-8: 文档文件输入处理

JAR包运行时应支持`--input.file`参数接收文档文件路径（支持.txt, .md, .json, .xml, .csv等文本格式），校验文件存在性和可读性后，使用长文本脱敏方式对文件内容进行脱敏，脱敏后文件保存到当前工作目录的`result/`子目录中，文件名格式为`原文件名_masked.扩展名`。

### FR-9: 表格文件输入处理（按列脱敏）

JAR包运行时应支持`--input.table`参数接收表格文件路径（支持.csv和.xlsx格式），校验文件存在性和可读性后，按以下规则处理：

**表头与脱敏类型映射**: 默认表头名称（支持中英文识别）与脱敏类型的对应关系如下：

| 表头名称（中文） | 表头名称（英文）    | 脱敏类型标识         |
| -------- | ----------- | -------------- |
| 姓名       | name        | `name`（大类，自动识别子类型） |
| 手机号      | phone       | `phone`        |
| 身份证号     | id\_card    | `id_card`      |
| 银行卡号     | bank\_card  | `bank_card`    |
| 地址       | address     | `address`（大类，自动识别子类型） |
| 国家       | nationality | `nationality`（maskFlag=false，不脱敏） |

**列处理逻辑**:

* 表头匹配上述已知类型 → 整列使用 `DesensitizeUtil.mask()` 单一类型脱敏（非数字类型自动走策略模式）

* 表头不匹配上述任何已知类型 → 对该列的每个单元格使用 `DesensitizeUtil.maskLongText()` 长文本脱敏

**多语言支持**: 姓名、地址等非数字类型采用策略模式，大类下有中文、英文、日文、德文、韩文等子类型，脱敏时自动匹配对应子类型规则。

脱敏后文件（保留表头不变，数据行按列规则脱敏）保存到当前工作目录的`result/`子目录中，文件名格式为`原文件名_masked.扩展名`。

### FR-10: 脱敏格式模板

系统应支持以下脱敏格式模板语法：

* `preserve(N,M)`: 保留前N个和后M个字符，中间替换为掩码符号

* `replace(2,4)`: 替换第2到第4位为掩码符号

* `maskAll()`: 全部替换为掩码符号

* 掩码符号可从配置中读取，默认使用`*`

### FR-11: GB/T 45574-2025敏感数据类型覆盖

默认配置文件应覆盖以下14大类敏感数据类型：

1. 姓名（中文、新疆少数民族、英文、韩文、日文、德文）— **策略模式，大类 typeId=`name`**
2. 手机号码（中国大陆、国际）
3. 固定电话（国内、国际）
4. 证件号码（身份证、护照、驾驶证、军官证）
5. 银行卡号（借记卡/信用卡、存折账号）
6. 地址（中文、英文、日文、德文）— **策略模式，大类 typeId=`address`**
7. 邮箱地址
8. 生日
9. IP地址
10. MAC地址
11. 车牌号码
12. 金额（人民币、外币）
13. 验证码
14. 国籍/民族/性别（maskFlag=false，不脱敏）

## 非功能性需求

### NFR-1: 性能要求

* 单一类型脱敏执行时间 < 1ms（不含首次配置加载）

* 长文本脱敏（10KB文本）执行时间 < 100ms

* AI脱敏超时时间可配置，默认30秒

### NFR-2: 安全要求

* 脱敏规则配置文件中的正则模式不得导致ReDoS（正则表达式拒绝服务攻击）

* AI脱敏的API Key等敏感配置不得在日志中明文输出

* 脱敏过程本身不应产生新的敏感信息泄露（如脱敏前在日志中打印原始数据）

### NFR-3: 可扩展性

* 新增敏感数据类型只需在配置文件中添加配置项，无需修改代码

* 非数字类型新增子类型只需在大类 `subTypes` 中添加配置项

* 掩码符号可通过配置全局修改

* AI模型切换仅需修改Spring AI YAML配置

### NFR-4: 兼容性

* 基于Spring Boot 3.x版本

* 基于Spring AI 1.0.x版本

* Java 17+

* Maven构建

### NFR-5: 可用性

* 配置文件加载失败时应有明确的错误提示

* 文件格式校验失败时应有明确的错误信息

* 脱敏结果应保持原始文档的编码格式（UTF-8）

## 约束

* **技术约束**: Spring Boot 3.x + Spring AI 1.0.x + Java 17 + Maven

* **业务约束**: 脱敏规则需符合GB/T 45574-2025标准

* **依赖约束**: 表格处理需依赖Apache POI（用于.xlsx文件），文档处理需依赖Apache Tika或直接读取

## 假设

* 假设用户运行环境已安装Java 17+的JRE

* 假设YAML配置文件位于classpath下的`desensitize-config.yml`

* 假设AI接口的API Key等凭证已通过环境变量或Spring配置正确设置

* 假设输入文件的编码格式为UTF-8

* 假设表格文件的表头位于第一行

* 假设脱敏后的result目录需要有写入权限

## 验收标准

### AC-1: 配置文件加载

* **Given**: classpath下存在有效的`desensitize-config.yml`配置文件

* **When**: 应用启动时自动加载配置文件

* **Then**: 所有敏感数据类型及其规则被正确解析，日志中输出加载的类型数量

* **Verification**: `programmatic`

### AC-2: 注解脱敏-Jackson序列化

* **Given**: 一个POJO类中某字段标注了`@Desensitize(type = "phone")`注解

* **When**: 使用Jackson ObjectMapper序列化该POJO对象

* **Then**: 该字段的值被替换为手机号脱敏格式（如`138****8000`）

* **Verification**: `programmatic`

### AC-3: 单一类型脱敏 — 数字类型精确匹配

* **Given**: 调用`DesensitizeUtil.mask("13800138000", "phone")`

* **When**: 执行脱敏方法

* **Then**: 返回值为`138****8000`（使用regexPattern精确匹配）

* **Verification**: `programmatic`

### AC-3b: 单一类型脱敏 — 非数字类型策略模式

* **Given**: 调用`DesensitizeUtil.mask("张三", "name")`

* **When**: 执行脱敏方法，`name` 大类下有 `chinese_name`、`english_name` 等子类型

* **Then**: 工具自动用子类型正则匹配，识别为 `chinese_name`，按中文姓名规则返回 `张*`

* **Verification**: `programmatic`

### AC-3c: 单一类型脱敏 — 策略模式fallback

* **Given**: 调用`DesensitizeUtil.mask("未知文本", "name")`

* **When**: 执行脱敏方法，所有子类型正则都不匹配

* **Then**: 使用大类的 `defaultMaskFormat` 进行脱敏

* **Verification**: `programmatic`

### AC-4: 长文本脱敏 — 混合上下文正则

* **Given**: 一段文本 "姓名：张三，手机：13800138000，身份证号：110101199001011234"

* **When**: 调用`DesensitizeUtil.maskLongText(text)`

* **Then**: "张三"被替换为"张*"，手机号被替换为"138****8000"，身份证号被替换为"110101********1234"，普通中文文本不被误脱敏

* **Verification**: `programmatic`

### AC-4b: 长文本脱敏 — 无上下文不误匹配

* **Given**: 一段纯普通文本 "今天天气很好，适合出去散步"

* **When**: 调用`DesensitizeUtil.maskLongText(text)`

* **Then**: 文本内容完全不变（混合正则的上下文中不包含任何敏感数据标识前缀/后缀）

* **Verification**: `programmatic`

### AC-5: AI脱敏

* **Given**: Spring AI已正确配置并可正常调用，配置文件中已设置脱敏提示词模板，调用`AiDesensitizeUtil.mask("我叫张三，电话是13800138000")`

* **When**: 执行AI脱敏方法

* **Then**: 系统自动使用配置的提示词模板组合输入内容请求AI接口，返回AI处理后的脱敏结果，内容中敏感信息已被处理

* **Verification**: `human-judgment`（取决于AI模型返回内容的可验证性）

### AC-6: JAR包字符串输入

* **Given**: 编译好的JAR包

* **When**: 执行`java -jar desensitize.jar --input.string="姓名：张三，手机：13800138000" --mask.mode=long_text`

* **Then**: 控制台输出脱敏后的文本，敏感信息已被替换

* **Verification**: `programmatic`

### AC-7: JAR包文档文件输入

* **Given**: 存在一个包含敏感数据的.txt文件

* **When**: 执行`java -jar desensitize.jar --input.file=/path/to/doc.txt`

* **Then**: `result/doc_masked.txt`文件被创建，内容已全面脱敏

* **Verification**: `programmatic`

### AC-8: JAR包表格文件输入（CSV）

* **Given**: 存在一个包含敏感数据的.csv文件，第一行为表头

* **When**: 执行`java -jar desensitize.jar --input.table=/path/to/data.csv`

* **Then**: `result/data_masked.csv`文件被创建，表头保持不变，数据行已脱敏

* **Verification**: `programmatic`

### AC-9: JAR包表格文件输入（XLSX）

* **Given**: 存在一个包含敏感数据的.xlsx文件，第一行为表头

* **When**: 执行`java -jar desensitize.jar --input.table=/path/to/data.xlsx`

* **Then**: `result/data_masked.xlsx`文件被创建，表头保持不变，数据行已脱敏

* **Verification**: `programmatic`

### AC-10: 文件校验失败

* **Given**: 指定的输入文件不存在

* **When**: 执行JAR包文件处理命令

* **Then**: 输出明确的错误信息并退出，退出码非0

* **Verification**: `programmatic`

### AC-11: 自定义掩码符号

* **Given**: 配置文件将掩码符号设置为`#`

* **When**: 执行脱敏操作

* **Then**: 脱敏结果中使用`#`号而非`*`号

* **Verification**: `programmatic`

### AC-12: 默认配置覆盖度

* **Given**: 使用内置的默认配置文件

* **When**: 检查配置文件内容

* **Then**: 至少包含14大类敏感数据类型的脱敏规则定义，覆盖GB/T 45574-2025标准要求的所有类型

* **Verification**: `human-judgment`

## 决策记录

* AI脱敏提示词采用内置模板方式，配置在项目配置文件（`desensitize-config.yml`）的`ai.prompt-template`字段中，启动时加载，调用方无需传入

* 长文本脱敏使用 `mixedRegexPattern`（混合上下文正则），通过上下文前缀/后缀约束避免误匹配；单一类型脱敏使用 `regexPattern`（精确匹配正则）

* 长文本脱敏中多个正则匹配到重叠区域时，按匹配内容长度降序排列，长度更长的规则优先脱敏，已脱敏区域不再参与后续匹配

* 非数字类型（姓名、地址）采用策略模式，配置文件两级结构（大类 → 子类型），脱敏时自动识别子类型，匹配不到时使用默认格式

* 不提供脱敏白名单机制

* `@Desensitize`注解仅支持属性（字段）级别，不支持类级别标记

* 过于宽泛的正则模式（如 `[\u4e00-\u9fff]{2,10}` 匹配所有中文）设置 `maskFlag=false` 避免误脱敏