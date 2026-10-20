# 部署到公网

GitHub 只是代码托管，别人打不开你的系统。要让人访问，得把 jar 跑在一台有公网 IP 的机器上。

## 先选一条路

| 方案 | 成本 | 首次耗时 | 适合 |
|---|---|---|---|
| **A. Railway**（PaaS） | $5/月 | 约 10 分钟 | 想最快看到效果，不在意国内访问速度 |
| **B. 云服务器 + Docker** | 60-100 元/月 | 约 30 分钟 | 长期用、国内访问快、完全可控 |
| **C. 云服务器裸跑 jar** | 同上 | 约 20 分钟 | 不想碰 Docker，能 SSH 就行 |

> 国内机器用 IP 直接访问（`http://1.2.3.4:8080`）**不需要备案**；要绑域名才需要。

---

## 通用必做（三条，哪条路都逃不掉）

### 1. 关掉演示账号

```bash
DEMO_ACCOUNTS=false
```

不关的话，登录页会把 `zhangxiaoyu / 123456`、`admin / 123456` 明晃晃列出来，等于把后台钥匙挂在门上。

### 2. 改掉默认密码

种子数据里三个账号密码都是 `123456`。至少把 `admin` 改掉：

```sql
-- 用 BCrypt 加密后的新密码替换
UPDATE sys_user SET password = '<bcrypt-hash>' WHERE username = 'admin';
```

或者第一次登录后用「个人中心 → 修改密码」改。

### 3. 数据要持久化

H2 默认写在 `/app/data`，容器一重建就没了。必须挂卷：

```yaml
volumes:
  - ./data:/app/data
```

生产环境更推荐切 MySQL（`docker compose` 里已配好）。

---

## A. Railway（最快）

1. 打开 <https://railway.app>，用 GitHub 账号登录
2. **New Project → Deploy from GitHub repo** → 选 `mindharbor`
3. 它自动识别 Dockerfile，开始构建（首次 3-5 分钟）
4. 进 **Variables**，逐条添加：

   | 变量 | 值 |
   |---|---|
   | `DEMO_ACCOUNTS` | `false` |
   | `AI_BASE_URL` | `https://api.agnes-ai.cn/v1` |
   | `AI_MODEL` | `agnes-3.0-pro` |
   | `DASHSCOPE_API_KEY` | 你自己的 key |

5. **Settings → Networking → Generate Domain**，拿到一个 `https://xxx.up.railway.app`
6. **Settings → Volumes → Add Volume**，Mount path 填 `/app/data`（不挂的话重启即丢数据）

**注意**：Railway 新账号有一次性 $5 试用额度，用完后 Hobby 计划 $5/月。额度耗尽服务会停。

国内访问 `*.up.railway.app` 可能慢或需要代理，自己判断能否接受。

---

## B. 云服务器 + Docker（推荐长期用）

以阿里云/腾讯云 **轻量应用服务器 2核2G** 为例（约 60-100 元/月）。

### 1. 装 Docker

```bash
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker
```

国内机器拉镜像慢的话，配一下加速器：

```bash
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"]
}
EOF
systemctl restart docker
```

### 2. 拉代码

```bash
git clone https://github.com/ovo-pwp/mindharbor.git
cd mindharbor
```

### 3. 配环境变量

```bash
cp .env.example .env
vi .env
```

填上：

```ini
APP_PORT=8080
MYSQL_PASSWORD=换一个强密码
DEMO_ACCOUNTS=false
DASHSCOPE_API_KEY=sk-你的key
```

### 4. 启动

```bash
docker compose up -d --build
```

三个容器：`mysql` + `redis` + `app`。首次构建 3-5 分钟，之后 `docker compose ps` 看到 `healthy` 就成了。

### 5. 放行端口

云控制台 → 安全组 → 添加规则：TCP `8080`，来源 `0.0.0.0/0`。

然后浏览器访问 `http://你的公网IP:8080`。

### 常用命令

```bash
docker compose logs -f app     # 看日志
docker compose restart app     # 重启
docker compose down            # 停止
docker compose down -v         # 停止并清空数据（慎用）
```

---

## C. 云服务器裸跑 jar（不用 Docker）

适合不想碰容器、机器上有 Java 21 的情况。

### 1. 本地打包并上传

```bash
# 本地（Windows）
mvn clean package -DskipTests
scp target/psych-counseling-1.0.0.jar root@你的IP:/opt/mindharbor/
```

> 前端产物已内嵌在 jar 里，不需要单独传。

### 2. 服务器上启动

```bash
cd /opt/mindharbor
DEMO_ACCOUNTS=false \
DASHSCOPE_API_KEY=sk-你的key \
AI_BASE_URL=https://api.agnes-ai.cn/v1 \
AI_MODEL=agnes-3.0-pro \
nohup java -Xmx512m -jar psych-counseling-1.0.0.jar --server.port=8080 > app.log 2>&1 &
```

### 3. 配成系统服务（可选，但建议）

`/etc/systemd/system/mindharbor.service`：

```ini
[Unit]
Description=MindHarbor
After=network.target

[Service]
User=root
WorkingDirectory=/opt/mindharbor
Environment="DEMO_ACCOUNTS=false"
Environment="DASHSCOPE_API_KEY=sk-你的key"
Environment="AI_BASE_URL=https://api.agnes-ai.cn/v1"
Environment="AI_MODEL=agnes-3.0-pro"
ExecStart=/usr/bin/java -Xmx512m -jar /opt/mindharbor/psych-counseling-1.0.0.jar --server.port=8080
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable --now mindharbor
systemctl status mindharbor
```

---

## 常见问题

**启动很慢 / 一直 unhealthy？**
2G 内存是底线。`-XX:MaxRAMPercentage=75` 在 512M 容器里只给 JVM 384M，容易 OOM。加到 1G 以上。

**日志里出现 `Database may be already in use`？**
H2 不允许两个进程同时打开同一个文件库。确认没有重复启动，或用 MySQL。

**AI 一直不回复？**
服务器必须能出外网访问 `api.agnes-ai.cn`。国内机器一般没问题；有安全组限制的话要放行出站。

**页面能打开但接口 404？**
前端产物没打进 jar。本地先 `cd frontend && npm run build`，把 `dist` 内容复制到 `src/main/resources/static/` 再打包（CI 已自动做这一步）。

**重启后要重新登录？**
正常。会话存在内存里，没接 Redis。用 `docker compose` 起了 Redis 之后就持久了。
