# 第三方素材

## Emotion Ball 表情引擎

- **来源**：<https://github.com/sam70361/aora-bot>
- **许可**：Emotion Ball Community License（**非商业免费**；商用需另行授权；球形角色视觉形象永不商用）
- **完整许可文本**：`frontend/public/emotion-ball/LICENSE`
- **在本项目中的用途**：AI 对话页右下方的伙伴小球，随对话状态切换表情

### 收录的文件

| 文件 | 说明 |
|---|---|
| `rings.js` | 25 组眼环轮廓数据 |
| `emotions.js` | 32 套表情配置 |
| `ball.js` | 球体与眼睛渲染 |
| `engine.js` | 状态机与弹簧插值动画 |
| `style.css` | 原样式（iframe 内隔离，不影响主站） |
| `embed.html` | **本项目新增**，最小嵌入页 |
| `LICENSE` | 原许可文本 |

### 本项目做的改动

原始 demo（`index.html`）是一个完整演示站，包含表情选择器、双语界面、背景粒子、页脚等。我们只需要球本体，因此：

1. 新增 `embed.html` 作为最小嵌入页，只加载 `rings/emotions/ball/engine` 四个脚本
2. 用 `postMessage` 接收 `{type:'emotion', id}` 指令切换表情
3. 未引入 `app.js`、`hero-particles.js`、`i18n.js`、`demo.html`

### 未使用的部分

- `mood-mates/` 子目录（独立原创角色子项目，受其自带的许可约束，与本项目无关）
- `site/` 演示站点

### 合规说明

本项目为个人学习用途，非商业。若将来商用，需按原仓库的 `LICENSE-COMMERCIAL.md` 单独取得授权。
