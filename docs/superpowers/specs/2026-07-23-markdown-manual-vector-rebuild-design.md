# Markdown 手动向量重建与失败恢复设计

## 目标

补齐阶段 2 最后一个生命周期闭环：当 Qdrant 点被误删、部分写入或同版本残留了过期 chunk 时，调用方可以重新提交已登记的 Markdown 来源与文件，让该版本向量恢复到与当前切分结果一致的状态。

本轮完成后：

- 已登记的精确“来源 + 内容版本”可以绕过普通导入的重复短路，重新解析、切分并写入向量。
- 重建成功后，该来源只保留本次重建版本，并移除同版本中超出当前 chunk 数量的残留点。
- 历史上已登记但当前不活跃的版本也可以被重建，并在成功后重新成为该来源的唯一向量版本。
- 重建失败不删除已有版本登记，调用方可以使用同一请求继续重试。
- 未登记版本不能借重建接口创建新登记，仍应使用普通导入接口。

## 数据前提与边界

当前 MySQL 只保存来源、版本、内容长度和登记时间，不保存原始 Markdown 正文。因此重建接口必须由调用方重新提交文件和逻辑来源，系统以提交内容计算稳定版本并校验该精确版本已经登记。

本轮不增加正文存储字段、文档当前版本字段、任务状态表、异步队列、定时自愈、批量重建、前端按钮、RAG 检索或新的 Maven 依赖。原始内容持久化会改变数据模型、隐私边界和存储规模，应在简历与资料正式建模时单独设计，不能为一个同步恢复入口顺手引入。

逻辑来源继续采用大小写敏感且无首尾空白的精确字符串语义。版本继续由规范化换行后的 Markdown 内容计算 SHA-256；来源和内容任何一项不匹配已登记联合键，都不能执行重建。

## 方案比较

### 方案一：重传来源和 Markdown 文件

新增 multipart 重建接口，调用方提交 `source` 与 `file`。服务端重新构造 `MarkdownDocument`，确认精确版本已登记后解析、切分并让向量存储收敛。

优点是无需扩表或保存敏感正文，能够直接复用现有版本、解析、切分和稳定向量 ID；失败后请求可原样重试。缺点是调用方必须仍持有原始文件，无法只凭来源在服务端恢复。

本轮采用此方案，它与当前元数据模型一致，也是能够独立验证的最小闭环。

### 方案二：在 MySQL 持久化原始 Markdown

重建时只提交来源或版本，由服务端读取正文。调用更方便，但会同时引入正文表结构、大小与隐私治理、版本内容一致性和迁移问题，超出阶段 2 的向量生命周期范围，因此暂缓。

### 方案三：删除登记后复用普通导入

先删除已登记版本，再调用普通导入链路。表面上可以少一个编排方法，但删除登记会破坏历史去重事实；如果随后 Embedding 或 Qdrant 失败，登记也被丢失，恢复语义比当前方案更差，因此不采用。

## API 契约

新增接口：

```text
POST /api/knowledge/documents/rebuild
Content-Type: multipart/form-data

file=<UTF-8 Markdown 文件>
source=<逻辑来源>
```

成功返回 HTTP 200：

```json
{
  "source": "java/collections.md",
  "version": "规范化内容的 SHA-256",
  "chunkCount": 3
}
```

响应只返回本次恢复结果所需的来源、版本和 chunk 数量，不重复返回全部 chunk 正文。普通导入接口和现有前端响应保持不变。

如果精确“来源 + 版本”从未登记，返回 HTTP 400：

```json
{
  "message": "文档版本尚未导入，不能重建"
}
```

空来源、首尾空白来源和其他文档模型校验继续通过现有 `IllegalArgumentException` 处理为 HTTP 400。读取文件失败仍按现有控制器方式抛出 `IOException`，向量存储或 Embedding 失败继续返回服务端错误，不伪装为业务校验失败。

## 组件与契约

### `MarkdownImportRegistry`

增加精确登记查询：

```java
boolean isRegistered(MarkdownDocument document);
```

内存实现查询现有联合键集合；MySQL 实现使用 `BINARY source = BINARY #{source}` 与版本相等条件计数，保持与登记、删除相同的来源语义。该查询不创建、删除或更新任何登记。

### `MarkdownChunkIndexer`

增加默认重建操作，保持接口仍可由 lambda 实现：

```java
default void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
    index(document, chunks);
    deleteOtherVersions(document);
}
```

默认行为足以支持 `local` profile 和简单测试替身。Qdrant 实现覆盖该方法，负责精确收敛。

### `QdrantMarkdownChunkIndexer`

非空 chunk 重建顺序固定为：

```text
使用稳定 ID 新增或覆盖全部预期 chunk
→ 删除同来源的其他文档版本
→ 删除同来源、同版本且 chunk_order >= 本次 chunk 数量的残留点
```

先写后删保证新向量写入失败时不主动清理旧版本。稳定 ID 使已有预期点被覆盖；最后一步处理“同一版本曾用更小切分上限产生更多 chunk”或人工残留点的情况，避免只覆盖预期 ID 后留下多余点。

过滤条件全部使用 Spring AI `FilterExpressionBuilder`：

```text
source == document.source
AND document_version == document.version
AND chunk_order >= chunks.size
```

当切分结果为空时，没有新向量可以先写。为使来源状态与空文档一致，直接删除该来源全部向量；已登记版本仍保留。后续重新提交同一空内容仍可重建，普通导入仍会按重复版本短路。

### `MarkdownContentImporter`

增加：

```java
MarkdownRebuildResult rebuildContent(String source, String content);
```

执行顺序为：

```text
创建 MarkdownDocument
→ 校验 registry.isRegistered(document)
→ 标题解析
→ chunk 切分
→ chunkIndexer.rebuild(document, chunks)
→ 返回重建结果
```

未登记时在解析、切分和向量调用前失败。重建过程不调用 `register`、`unregister` 或 `unregisterBySource`；索引失败原样抛出，已有登记保持不变。这与普通导入的失败补偿不同：普通导入刚创建的新登记需要撤销，而重建依赖的登记是已经存在的历史事实，不能因外部服务故障删除。

新增不可变 `MarkdownRebuildResult`，保存 `MarkdownDocument` 与不可修改 chunk 列表。Web 层使用独立 `MarkdownRebuildResponse` 映射来源、版本和数量，避免改变已有导入响应。

## 一致性与失败恢复

MySQL 与 Qdrant 仍没有分布式事务，本轮通过操作顺序和幂等 ID 获得可重试收敛：

- 新向量写入失败：不执行后续删除；原登记和旧版本向量保持，重试覆盖相同稳定 ID。
- 新向量写入成功、删除其他版本失败：登记保持；同一请求重试会再次覆盖新点并继续删除。
- 其他版本删除成功、同版本残留删除失败：预期新点已经存在；重试继续执行相同写入与清理。
- 空文档来源删除失败：登记保持；同一空文件请求可以重试删除。
- 未登记文件：不触碰 Qdrant 和 MySQL。

重建历史版本成功后，现有 Qdrant 过滤语义会删除该来源的其他版本，因此该历史版本重新成为当前活跃向量版本；MySQL 中所有历史登记保持不变。

## 验证策略

自动化测试只覆盖新增恢复语义和直接回归，不扩充无关测试：

- 导入器允许已登记精确版本重建并返回切分结果。
- 未登记版本在调用索引器前被拒绝。
- 索引失败后登记仍存在，下一次重试可以成功。
- 已登记历史版本可重建并重新激活。
- 内存和 MySQL 登记器按精确来源与版本查询。
- Qdrant 非空重建按“写预期点、删其他版本、删同版本多余点”顺序调用，并生成正确过滤条件。
- Qdrant 空 chunk 重建删除整个来源，不执行向量新增。
- 控制器 multipart 重建返回来源、版本与 chunk 数量，未登记版本返回 HTTP 400。

真实 `storage` 验证使用本轮唯一来源，且只清理本轮数据：

1. 普通导入一个 Markdown 文件，确认 MySQL 登记和 Qdrant 点存在。
2. 只删除该来源的 Qdrant 点，保留 MySQL 登记。
3. 再次普通导入，确认因登记存在而返回重复，不能修复向量。
4. 人工写入一个同版本且高 `chunk_order` 的残留点。
5. 调用重建接口，确认预期点恢复、残留点删除、登记仍存在。
6. 调用现有来源删除接口，确认本轮 MySQL 登记和 Qdrant 点均清零。

最终停止本项目占用 `target` JAR 的后端，使用仓库内 Maven 缓存与临时目录执行一次 `clean verify`；验证后再用生成的 JAR 恢复 `storage` profile 服务。

## 文档与阶段收口

实现验证后同步 README、技术方案、当前进度、开发记录和 ADR-016。阶段 2 只有在自动化测试、真实存储修复和清理均通过后才标记完成；下一阶段候选是最小 RAG 检索与真实来源返回。

