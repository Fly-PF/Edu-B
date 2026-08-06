# AI 教育安全评测中心接入说明

## 1. 定位

安全评测中心是平台级安全网关，不是单独的文本检测页面。其他方向只要涉及 AI 调用、AI 输出、内容发布、学习总结、项目案例生成，都应该先经过安全评测中心。

核心目标：

1. 用户输入先检测，防止隐私泄露、诱导作弊、不适龄请求、提示词攻击。
2. AI 输出后再检测，防止幻觉、错误依据、不当内容、隐私暴露。
3. 系统自动给出 `PASS / WARN / BLOCK / DESENSITIZE / REWRITE` 处置动作。
4. 风险记录进入检测记录与安全看板。
5. 只有边界不确定、需要人工判断的内容进入“人工审核”。

## 2. 统一后端接口

其他方向推荐统一调用：

```text
POST /api/safety/gateway
```

请求字段：

```json
{
  "sourceModule": "TEACHER_PREP",
  "scene": "AI_OUTPUT",
  "userRole": "TEACHER",
  "gradeLevel": "JUNIOR",
  "userId": 1,
  "classId": 1,
  "courseId": 1,
  "chapterId": 1,
  "inputText": "老师输入给 AI 的要求",
  "outputText": "AI 生成结果",
  "recordLog": true,
  "metadata": {}
}
```

关键返回字段：

```json
{
  "allowed": true,
  "riskLevel": "HIGH",
  "riskTypes": ["HALLUCINATION"],
  "decision": "REWRITE",
  "reason": "AI 输出缺少明确依据",
  "suggestion": "建议补充课程来源、教材引用或知识库依据",
  "processedText": "处理后的文本",
  "evidenceLevel": "UNSUPPORTED",
  "evidenceScore": 0.35,
  "manualReviewRequired": false,
  "teacherConfirmationRequired": true,
  "recordId": 123
}
```

## 3. 前端统一封装

已提供统一封装：

```text
D:\file\qian\Edu-F\src\utils\safetyGateway.js
```

其他方向前端不要自己重复写安全逻辑，直接使用：

```js
import { runSafeAiFlow } from '@/utils/safetyGateway'

const result = await runSafeAiFlow({
  sourceModule: 'TEACHER_PREP',
  userRole: 'TEACHER',
  gradeLevel: 'JUNIOR',
  classId,
  courseId,
  inputText: teacherPrompt,
  callAi: ({ inputText }) => generateLessonPlan({ prompt: inputText }),
})

if (!result.passed) {
  return
}

const textForDisplayOrPublish = result.safeOutputText
```

这个封装会自动完成：

1. AI 调用前检测 `inputText`。
2. 输入被 `BLOCK` 时，不调用 AI，弹窗提示原因和建议。
3. 输入为 `WARN / DESENSITIZE / REWRITE` 时，老师端弹窗确认是否继续。
4. 调用业务方自己的 AI 接口。
5. AI 输出后再次调用安全网关。
6. 输出被 `BLOCK` 时，不允许展示或发布。
7. 输出为 `WARN / DESENSITIZE / REWRITE` 时，弹窗提醒，由业务页面决定是否继续发布。
8. 返回 `safeOutputText` 给业务页面展示或发布。

## 4. 老师调用 AI 的完整流程

假设李佰城的教师备课模块中，老师点击“生成教案”：

1. 教师备课页面调用 `runSafeAiFlow`。
2. 安全中心先检测老师输入。
3. 如果输入诱导代写、包含隐私、提示词攻击或不适龄内容：
   - `BLOCK`：弹窗提示“不合规”，不再调用 AI。
   - `DESENSITIZE`：先脱敏，再继续调用 AI。
   - `WARN / REWRITE`：弹窗提醒风险，老师确认后继续。
4. 教师备课模块正常调用自己的 AI 生成教案。
5. 安全中心检测 AI 生成的教案。
6. 如果 AI 输出不合规：
   - `BLOCK`：不展示或不允许发布，只展示安全建议。
   - `REWRITE`：提示补充依据或重新生成。
   - `WARN`：提示老师确认，老师确认后继续或返回修改。
   - `DESENSITIZE`：展示脱敏后的内容。
7. 检测记录自动写入安全中心，管理员可以在检测记录和看板里看到。

所以：发布前检测不用单独做页面。它应该嵌入其他同学自己的业务页面里，由统一封装完成。

## 5. 哪些进入人工审核

进入人工审核的标准不是“有风险就审核”，而是：

1. `manualReviewRequired = true`
2. `reviewStatus = PENDING`

常见进入人工审核的情况：

1. 学生侧 `WARN` 类边界内容。
2. 学生侧教育 RAG 或真实业务 `AI_OUTPUT` 中证据存疑/无据。
3. 本班学生提交内容需要老师判断是否放行或驳回。

无需人工审核的情况：

1. `PASS`：直接通过。
2. `BLOCK`：明确违规，系统直接拦截。
3. `DESENSITIZE`：系统可自动脱敏处理。
4. 教师自己的 AI 发布前内容：即使有 `WARN / DESENSITIZE / REWRITE`，也只触发老师端弹窗确认，不进入人工审核队列。
5. 沙箱测试里的普通风险演示：主要用于调试和展示，不一定进入人工审核。

检测记录页面里，以 `reviewStatus` 为准：

- `PENDING`：需要人工审核。
- `APPROVED`：已审核通过。
- `REJECTED`：已审核驳回。
- `NOT_REQUIRED`：无需人工审核。

## 6. 各方向 sourceModule

| 方向 | sourceModule | 推荐 scene |
| --- | --- | --- |
| 智能学伴 | `AI_COMPANION` | `STUDENT_AI` / `AI_OUTPUT` |
| 教师备课批改 | `TEACHER_PREP` | `TEACHER_COURSE` / `AI_OUTPUT` |
| 教育 RAG | `EDUCATION_RAG` | `AI_OUTPUT` |
| 项目式学习案例库 | `PROJECT_CASE` | `RESOURCE_SCAN` / `AI_OUTPUT` |
| 学情分析/成长档案 | `LEARNING_ANALYSIS` | `AI_OUTPUT` |
| 多模态教学 | `MULTIMODAL_TEACHING` | `AI_OUTPUT` |
| 手动沙箱 | `MANUAL_TEST` | `MANUAL_TEST` |

## 7. 接入原则

1. 其他方向不需要做安全中心页面。
2. 其他方向只在自己的 AI 调用处包一层 `runSafeAiFlow`。
3. 安全中心只保留看板、检测记录、沙箱测试、样本评测、人工审核这些治理能力。
4. 学生端不出现安全中心页面，只在业务流程中看到拦截、脱敏、改写建议。
5. 老师端不需要堆很多测试功能，只保留教师发布前确认和本班学生内容人工审核入口。
6. 管理员端负责全局看板、检测记录、人工审核、样本评测。
