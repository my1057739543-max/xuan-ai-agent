# 模块 07：文件上传与文档读取（Reader）

你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 模块目标
- 完成知识库文件上传能力。
- 基于 Spring AI Reader 体系完成 `txt`、`md`、`pdf` 的统一读取。
- 产出标准化 `Document` 列表，供后续切片与入库使用。

## 2. 本模块范围
- 上传接口：`POST /api/knowledge/upload`。
- 文件列表接口：`GET /api/knowledge/files`。
- 删除接口：`DELETE /api/knowledge/files/{fileId}`。
- 文件元数据表 `kb_file` 管理（状态：PROCESSING/READY/FAILED）。
- 扩展名、MIME、大小校验。

## 3. 读取策略（必须遵循）
- `txt`：Spring AI `TextReader`。
- `md`：Spring AI `MarkdownDocumentReader`。
- `pdf`：Spring AI `PagePdfDocumentReader`。
- 业务层只做 Reader 路由与兜底，不重复造 Parser 轮子。

## 4. 建议输出文件
- `src/main/java/com/xuan/xuanopenagent/controller/KnowledgeBaseController.java`
- `src/main/java/com/xuan/xuanopenagent/rag/RagIngestionService.java`
- `src/main/java/com/xuan/xuanopenagent/rag/model/KnowledgeFile.java`
- `src/main/java/com/xuan/xuanopenagent/rag/store/KnowledgeFileRepository.java`
- `src/main/java/com/xuan/xuanopenagent/rag/document/DocumentReaderRouter.java`

## 5. 处理流程
1. 接收上传文件并做基础校验。
2. 生成 `fileId`，保存原始文件到 `upload-dir`。
3. 在 `kb_file` 写入 PROCESSING。
4. 根据扩展名路由到对应 Spring AI Reader。
5. 读取失败则置为 FAILED 并记录原因。
6. 成功读取后返回标准化 `Document` 列表给下游模块。

## 6. 验收标准
- 三种文件类型都能成功读取为 `Document`。
- 非法文件类型和超限文件会被正确拒绝。
- 文件状态可在列表接口看到完整生命周期。
- 错误日志可定位到 `fileId`。

## 7. 风险与处理
- 风险：PDF 文档结构复杂导致提取质量差。
- 处理：1.1 只接受可提取文本 PDF，失败直接标记并给出原因。
- 风险：大文件读取耗时。
- 处理：1.1 先同步，日志打点保留后续异步改造依据。
