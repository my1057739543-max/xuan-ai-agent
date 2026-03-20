# 12. gameKey 隔离与知识模型改造

# 你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 目标

建立强制 gameKey 隔离，确保检索与回答在同一游戏域内进行，避免跨游戏串题，为 13-16 章节能力提供统一上下文锚点。

---

## 2. 范围与边界

本章节覆盖：

1. 知识上传时的 gameKey 绑定与校验。
2. 检索前 finalGameKey 决策与 metadata 强过滤。
3. gameKey 解析相关 SSE 事件与日志字段。
4. 与后续章节（切片、重写、后处理、评测）的输入输出契约。

本章节不覆盖：

1. LLM 切片 prompt 细节（见 13）。
2. 意图分类与记忆摘要算法细节（见 14）。
3. 检索后处理与回答护栏细节（见 15）。
4. 指标平台与灰度发布细节（见 16）。

---

## 3. 业务规则

1. 上传文档必须绑定 gameKey。
2. 检索默认必须带 gameKey 过滤。
3. 用户显式指定游戏时，以用户当前输入优先。
4. 用户未指定时，可从会话记忆推断；无法推断则发起澄清。
5. 不允许在 finalGameKey 为空时直接检索全库。

规则优先级：

1. Explicit（当前输入）
2. Memory（会话摘要）
3. Default（仅在显式配置开启时可用，默认建议关闭）

---

## 4. 统一枚举与别名

建议标准 gameKey：

- valorant
- cs2
- apex
- lol

建议维护 alias map（配置化）：

- 瓦、瓦罗兰特、valorant -> valorant
- cs、cs2、反恐精英2 -> cs2
- APEX、apex legends -> apex
- 英雄联盟、LOL、lol -> lol

规范要求：

1. alias 匹配大小写不敏感。
2. alias 去除首尾空格后再匹配。
3. 一个 alias 只能映射一个标准 gameKey，禁止多义配置。

---

## 5. 数据结构改造

### 5.1 向量 metadata 必填字段

- gameKey
- fileId
- fileName
- topic
- chunkIndex
- sourceType
- uploadTime

补充建议字段：

- chunkMode（llm|rule）
- docVersion
- tags

### 5.2 知识文件记录新增字段

- gameKey（必填）
- tags（可选）
- skillLevel（可选）
- uploader（可选）
- ingestionStatus（PROCESSING|SUCCEEDED|FAILED）

### 5.3 约束建议

1. knowledge_file.game_key 建议建立普通索引。
2. 向量 metadata.gameKey 必须与 knowledge_file.game_key 一致。
3. 删除文件时，按 fileId + gameKey 双条件级联删除向量记录。

---

## 6. 接口改造

### 6.1 上传接口

POST /api/knowledge/upload

新增 multipart 参数：

- file（已有）
- gameKey（新增，必填）
- tags（可选）

校验失败返回 400：

- gameKey 缺失
- gameKey 不在白名单

示例请求：

```bash
curl -X POST "http://localhost:8123/api/knowledge/upload" \
	-F "file=@valorant-aim.md" \
	-F "gameKey=valorant" \
	-F "tags=aim,entry"
```

示例响应：

```json
{
	"code": 0,
	"message": "OK",
	"data": {
		"fileId": "a48c08487afc42f9a4189303735e7e2c",
		"gameKey": "valorant",
		"status": "PROCESSING"
	}
}
```

### 6.2 检索链路

在检索请求构建阶段，统一增加 metadata filter：

- gameKey = finalGameKey

不允许默认无过滤全库检索。

### 6.3 对话接口契约补充

对话请求可选字段建议：

- gameKey（可选，显式指定时优先级最高）

对话响应 trace 中应可观测：

- detectedGameKey
- finalGameKey
- gameSource

---

## 7. finalGameKey 判定流程

1. 从当前用户输入提取 game alias。
2. 命中 alias 则映射为标准 gameKey。
3. 若输入中显式传入 gameKey，覆盖 alias 结果并记录 source=explicit。
4. 未命中则读取会话最近 gameKey（来源于 14 的 memory summary）。
5. 仍为空且开启 default-game-key 时使用默认值并记录 source=default。
6. 仍为空则发 clarification_needed 事件并引导补充。

冲突定义：

1. 输入中出现多个可映射 gameKey。
2. explicit 与 memory 不一致。

冲突策略：

1. 若存在 explicit，以 explicit 为准并发 game_conflict_detected。
2. 若无 explicit，触发 clarification_needed，不进入检索。

---

## 8. SSE 事件要求

新增或细化：

- game_resolved
- game_conflict_detected
- clarification_needed

事件 payload 建议：

- detectedGameKey
- finalGameKey
- source（explicit|memory|default）
- conflictWith
- traceId
- sessionId

示例：game_resolved

```json
{
	"event": "game_resolved",
	"payload": {
		"detectedGameKey": "valorant",
		"finalGameKey": "valorant",
		"source": "explicit",
		"traceId": "tr_20260319_001",
		"sessionId": "s_abc"
	}
}
```

示例：clarification_needed

```json
{
	"event": "clarification_needed",
	"payload": {
		"reason": "missing_game_key",
		"candidates": ["valorant", "cs2", "apex", "lol"],
		"traceId": "tr_20260319_002",
		"sessionId": "s_abc"
	}
}
```

---

## 9. 配置项建议

- xuan.rag.game-isolation-enabled=true
- xuan.rag.supported-game-keys=valorant,cs2,apex,lol
- xuan.rag.game-alias-map.*
- xuan.rag.default-game-key=
- xuan.rag.game-resolution.emit-conflict-event=true

说明：

1. default-game-key 建议默认留空，避免悄悄误判。
2. 生产环境建议将 supported-game-keys 与 alias-map 配置入集中配置中心。

建议 application.yml 片段：

```yaml
xuan:
	rag:
		game-isolation-enabled: true
		supported-game-keys: valorant,cs2,apex,lol
		default-game-key:
		game-alias-map:
			瓦: valorant
			瓦罗兰特: valorant
			cs: cs2
			反恐精英2: cs2
			apex legends: apex
			英雄联盟: lol
```

---

## 10. 后端落地步骤（M1 执行清单）

1. 扩展 RagProperties，新增 game isolation 配置项。
2. 增加 GameKeyResolver 组件：alias 归一化、优先级决策、冲突判定。
3. 改造上传接口参数校验，gameKey 不合法直接 400。
4. 改造 ingestion 入库逻辑，写入 gameKey 与扩展 metadata。
5. 改造 retrieval 构建逻辑，强制拼接 gameKey metadata filter。
6. 在对话链路输出 game_resolved / clarification_needed / game_conflict_detected。
7. 增加审计日志字段：traceId、sessionId、detectedGameKey、finalGameKey、source。

完成定义（DoD）：

1. 任意检索请求都能在日志中看到 finalGameKey。
2. finalGameKey 为空时系统不会发起向量检索。
3. 上传与检索路径都通过自动化测试。

---

## 11. 测试矩阵

单元测试：

1. alias 大小写归一化。
2. 多 alias 冲突判定。
3. explicit 覆盖 memory。
4. default-game-key 开关行为。

接口测试：

1. 上传缺失 gameKey 返回 400。
2. 上传非法 gameKey 返回 400。
3. 上传合法 gameKey 成功并落库。

集成测试：

1. 同一 query 在不同 gameKey 下召回集合差异明显。
2. finalGameKey 为空时触发 clarification_needed 且不检索。
3. 冲突场景触发 game_conflict_detected。

回归测试：

1. 1.1 单游戏固定场景回答不回退。
2. 开启/关闭 game-isolation-enabled 行为可预期。

---

## 12. 验收标准

1. 上传无 gameKey 时被拒绝。
2. 同一 query 在不同 gameKey 下召回显著不同。
3. 发生游戏冲突时有可观测日志和事件。
4. 跨游戏误答率下降到目标阈值内。
5. finalGameKey 为空时，系统必须进入澄清分支而非盲答。

---


