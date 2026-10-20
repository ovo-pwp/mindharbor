# 心屿 · AI 心理健康咨询系统

基于 **SpringBoot 3.2 + Vue 3** 的前后端分离项目，覆盖患者、咨询师、管理员三种角色的完整业务闭环。

> 参考实现：B 站「基于 SpringBoot3 + Vue3 的 AI 心理健康咨询系统」需求
> （BV1fEzkBmEDm），技术栈与模块对齐，细节为独立实现。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2.12、Java 21 |
| 持久层 | MyBatis-Plus 3.5.7 |
| 认证 | Sa-Token 1.38（JWT 模式） |
| 密码加密 | BCrypt |
| 数据库 | H2（默认，免安装，MySQL 兼容模式）/ MySQL 8 |
| AI | 通义千问 DashScope（OpenAI 兼容模式，SSE 流式）；无 Key 时自动降级为本地共情引擎 |
| 前端 | Vue 3、Vite 5、Element Plus、Pinia、Vue Router、ECharts、Marked |

## 二、功能模块

### 患者端
- **用户认证**：注册、登录、JWT 鉴权、改密码
- **AI 智能对话**：SSE 流式响应、Markdown 渲染、多会话、对话历史持久化、危机词识别
- **心理测评**：SDS 抑郁自评（20 题）、SAS 焦虑自评（20 题），自动计分（含 8/10 道反向题）、分级、生成解读建议、历史报告
- **心理资源**：音频 / 练习 / 图文资源，浏览计数
- **心理科普**：咨询师撰写的 Markdown 文章
- **找咨询师**：按擅长领域筛选、查看详情、申请建立咨询关系
- **医患沟通**：绑定关系校验后的文字消息、会话列表、未读数
- **症状记录**：情绪 / 困扰 / 精力三维自评 + 触发事件 + 躯体症状 + 应对方式，ECharts 趋势图
- **智能推荐**：基于用户的协同过滤（余弦相似度），冷启动退化为热门推荐
- **个人中心**：资料维护、咨询师入驻申请

### 咨询师端
- **工作台**：待审患者申请、我的患者、评估报告、我的资源
- **患者管理**：审核绑定申请、查看已授权档案（情绪记录 + 测评历史）
- **评估报告**：四段式结构化报告（总体概述 / 数据洞察 / 风险预警 / 改善建议），支持 **AI 辅助生成**、保存草稿、发布给患者
- **资源发布**：提交后进入平台审核

### 管理端
- **仪表盘**：核心指标、近 7 天测评量、分级分布、最新注册
- **用户管理**：检索、启用 / 停用
- **医生审核**：入驻资质审核，通过后自动授予 DOCTOR 角色
- **量表管理**：量表基础信息、阈值分级、题目增删
- **科普管理**：文章 CRUD
- **公告管理**：公告 CRUD
- **资源审核**：咨询师发布资源的通过 / 拒绝

## 三、目录结构

```
psych-counseling/
├── src/main/java/com/mindharbor/psych/
│   ├── ai/            # QwenClient(SSE) / FallbackResponder / CrisisDetector
│   ├── common/        # R 统一响应、BizException、全局异常处理
│   ├── config/        # Sa-Token 路由鉴权、MyBatis-Plus 分页、种子数据
│   ├── controller/    # 11 个 REST 控制器
│   ├── dto/ vo/       # 请求 / 响应对象
│   ├── entity/        # 15 个实体
│   ├── enums/         # 角色、状态、资源类型
│   ├── mapper/        # 15 个 MyBatis-Plus Mapper
│   └── service/       # 11 个业务服务
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql     # 建表脚本（H2 / MySQL 通用）
└── frontend/          # Vue3 + Vite 前端
    └── src/views/     # 19 个页面
```

## 四、启动

### 最快方式：双击 `启动系统.bat`

项目已内置 JDK 21、Maven 和前端构建产物，**不依赖本机环境**：

```
双击 启动系统.bat  →  浏览器打开 http://localhost:8080
```

首次启动会自动建表并写入演示数据（约 5 秒）。

> 注：本目录下的 `.tools/` 内含便携版 JDK 21 与 Maven 3.9.9（约 350MB），
> 若只想部署到服务器，可只取 `target/psych-counseling-1.0.0.jar` 与 `启动系统.bat`。

### 1. 后端（开发）

```bash
# 需要 JDK 21
cd psych-counseling
mvn spring-boot:run
```

启动后：<http://localhost:8080>
H2 控制台：<http://localhost:8080/h2-console>（JDBC URL `jdbc:h2:file:./data/mindharbor`，用户 `sa`，密码空）

### 2. 前端

```bash
cd psych-counseling/frontend
npm install
npm run dev
```

访问 <http://localhost:5173>（已配置 `/api` 代理到 8080）

### 3. 演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 患者 | `zhangxiaoyu` | `123456` |
| 咨询师 | `liyisheng` | `123456` |
| 管理员 | `admin` | `123456` |

患者账号已预置 14 天情绪记录、2 次测评、1 份已发布报告，以及与李明远医生的咨询关系与聊天记录。

## 五、切换到 MySQL

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mindharbor?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
```

同时把 `MybatisPlusConfig` 里的 `DbType.H2` 改为 `DbType.MYSQL`。
`schema.sql` 用的是标准语法，MySQL 8 可直接执行。

## 六、AI 能力：三层降级 + 工具调用

### 怎么接模型

**方式 A：改 `ai-config.env`（推荐，双击 `启动系统.bat` 自动读取）**

```ini
AI_BASE_URL=https://api.agnes-ai.cn/v1
AI_MODEL=agnes-3.0-pro
AI_MAX_TOKENS=2000
DASHSCOPE_API_KEY=sk-xxxx
```

**方式 B：环境变量**

```bash
export AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export AI_MODEL=qwen-plus
export AI_API_KEY=sk-xxxx
```

任何 **OpenAI 兼容**的网关都能接（DashScope / 硅基流动 / agnes 等），只要支持 `/chat/completions` 与 `tools`。

> 已验证：`api.agnes-ai.cn` 的 `agnes-3.0-pro` 完整支持流式 + function calling；
> `agnes-3.0-flash` 不会主动调工具（只追问），接工具请用 `-pro`。
> 该网关会额外下发 `reasoning_content`（思维链），系统只取 `content`，不会把思考过程显示给用户。

### 三层降级

| 层 | 触发条件 | 能力 |
|---|---|---|
| 1. 大模型对话 | 配了 Key 且连通 | 自由对话 + 自主决定调哪个技能（function calling） |
| 2. 知识库 + 技能 | 没配 Key 或连不上 | 能回答 32 类心理知识问题、能执行 6 个技能 |
| 3. 共情模板 | 上面都没命中 | 情绪主题识别 → 共情 → 正常化 → 给方法 → 反问 |

（`GET /api/ai/skills` 可查当前技能清单。）

### 六个技能

| 技能 | 作用 | 本地触发示例 |
|---|---|---|
| `record_mood` | 写入一条情绪记录 | 「帮我记录一下，今天心情很差」 |
| `query_mood` | 统计近期情绪趋势 | 「我最近情绪怎么样」 |
| `my_assessments` | 查最近测评分数与分级 | 「我上次测评多少分」 |
| `recommend_counselor` | 按议题匹配咨询师 | 「有没有适合焦虑的咨询师」 |
| `search_article` | 检索科普文章 | 「帮我找几篇关于睡眠的文章」 |
| `crisis_resources` | 返回危机求助渠道 | 「不想活了」 |

- **大模型模式**：技能以 OpenAI tools 格式下发，模型自己判断调用、自己填参数，工具结果回灌后再组织语言。
  流式响应里的 `tool_calls` 是按 index 分片下发的，`QwenClient` 里做了拼装。
- **本地模式**：`SkillRegistry.matchLocal` 用「强触发词」+「对象词 × 意图词」两层判断，
  参数用规则粗抽（如「心情很差」→ mood=1，「帮我找几篇关于睡眠的文章」→ keyword=睡眠）。

### 内置知识库

`KnowledgeBase` 收录 32 条常见心理知识（CBT、抑郁症表现、惊恐发作、强迫症、PTSD、双相、
正念、CBT-I、情绪 ABC、边界感、讨好型、依恋类型、精神内耗、拖延、社交焦虑、量表解读、
选咨询师、咨询周期、抗抑郁药、情绪日记、拒绝、失恋、父母沟通、职场 PUA、自我关怀、
呼吸放松、就医时机、保密原则、自杀干预……）。

触发条件刻意收紧：**必须同时满足「命中条目关键词」和「是问句」**。
否则「我最近很抑郁」这种表达情绪的话会被知识条目抢走，反而丢了共情。

## 七、几个设计要点

**1. 危机干预**
`CrisisDetector` 对用户消息做高风险词匹配（自杀 / 自残 / 不想活等），命中后在流式回复末尾追加危机干预卡片（含 400-161-9995 等热线）。这是心理类产品的底线功能，不能省。

**2. 量表计分**
SDS / SAS 各 20 题，4 级选项（1-4）。反向题得分 = 5 - 选项值。粗分 × 1.25 取整得标准分。
SDS：<53 正常 / 53-63 轻度 / 63-73 中度 / ≥73 重度
SAS：<50 正常 / 50-60 轻度 / 60-70 中度 / ≥70 重度

**3. 协同过滤**
`RecommendService` 用 `action_log` 构建用户-资源评分矩阵，余弦相似度找最近 8 个邻居，加权预测未交互资源；矩阵为空或邻居不足时退化为热门推荐。

**4. 权限**
Sa-Token 路由级拦截：`/api/admin/**` 校验 ADMIN，`/api/doctor/workbench/**` 校验 DOCTOR，其余 `/api/**` 需登录。
医患消息在服务层再做一次绑定关系校验（`ConsultService.isBound`），避免越权。
医生查看患者档案同样校验 `doctor_patient` 的 APPROVED 状态。

**5. 数据隔离**
测评记录、情绪记录、聊天记录查询均带 `userId` 条件；报告详情同时校验「本人」或「其咨询师」。

## 八、Docker 部署（MySQL + Redis）

一条命令拉起三个容器：

```bash
cp .env.example .env      # 按需改密码 / 端口 / API Key
docker compose up -d --build
```

| 服务 | 镜像 | 端口（宿主） | 说明 |
|---|---|---|---|
| app | 本地构建 | 8080 | 前端已打进 jar，直接访问 |
| mysql | mysql:8.0 | 3307 | 数据卷 `mysql-data` |
| redis | redis:7-alpine | 6380 | 数据卷 `redis-data`，开启 AOF |

启动顺序由 healthcheck 保证：mysql / redis 健康后才起 app；app 首启会自动建表 + 写演示数据。

常用命令：

```bash
docker compose logs -f app     # 看日志
docker compose down            # 停止
docker compose down -v         # 停止并清空数据
```

### Redis 用来做什么

Sa-Token 的会话默认存在 JVM 内存里，多实例部署时登录态不共享。接 Redis 后会话集中存储，
水平扩容时任意实例都能校验同一个 token。

开关是显式的（`application-docker.yml` 里 `app.session-store: redis`），**不配就不会连 Redis**。

### 两个踩过的坑

1. **`sa-token-redis-jackson` 自带 Spring 自动配置**，只要依赖在 classpath 就会被无条件注册成
   `SaTokenDao` —— 哪怕你根本没配 Redis，表现是登录直接抛 `Unable to connect to Redis`。
   解决办法是在启动类上 `@SpringBootApplication(exclude = SaTokenDaoRedisJackson.class)`，
   再由 `SaTokenRedisConfig` 按开关显式注册。
2. **`SaTokenDaoRedisJackson` 必须调 `init(RedisConnectionFactory)`**，它靠这个方法构建内部的
   `StringRedisTemplate` 并置 `isInit=true`。直接 `new` 出来用，运行时会报「尚未初始化」。

## 九、公网部署必读

完整部署步骤（Railway / 云服务器 + Docker / 裸跑 jar 三种方案）见 **[DEPLOY.md](DEPLOY.md)**。

本地开发为了方便，登录页会显示演示账号。**一旦部署到公网，这等于把账号密码贴在门上。**

### 1. 关掉演示账号提示

```bash
# 方式一：环境变量
export DEMO_ACCOUNTS=false

# 方式二：启动参数
java -jar app.jar --app.demo-accounts=false

# 方式三：docker compose 里设
environment:
  DEMO_ACCOUNTS: "false"
```

关掉后登录页不再出现那三个账号（`application-docker.yml` 里已默认 false，容器部署无需额外配置）。

### 2. 改掉默认密码

种子数据里的 `zhangxiaoyu` / `liyisheng` / `admin` 密码都是 `123456`。
公开部署前至少要把管理员改掉，否则任何人都能进后台。

### 3. 不要提交的真实密钥

以下文件已在 `.gitignore` 里，**确认不要 `git add -f` 强行提交**：

| 文件 | 内容 |
|---|---|
| `ai-config.env` | 大模型 API Key |
| `.env` | 数据库密码等 |
| `data/` | H2 数据库文件（含用户数据） |
| `.tools/` | 便携 JDK / Maven（350MB） |

API Key 一律走环境变量或 `.env`，不要写进 `application.yml`。

### 4. 数据要持久化

H2 默认写在 `./data`，容器里必须挂卷，否则重启即丢失：

```yaml
volumes:
  - ./data:/app/data
```

或者直接用 `docker compose`（MySQL 版本，数据在 `mysql-data` 卷里）。

## 十、免责声明

本项目为教学 / 演示用途。量表结果与 AI 回复**不构成医学诊断**，不能替代专业治疗。
真实上线需接入合规的医疗机构资质、数据加密存储与等保要求。
