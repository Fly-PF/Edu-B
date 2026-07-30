# 郭阳方向接入说明

## 1. 作用

本方向负责的是 **AI 教育安全评测前置网关**。

它不是单独给学生或老师看的页面，而是一个 **插在所有 AI 调用前后的安全路由**：

1. 先做输入安全评判。
2. 再调用业务方自己的大模型接口。
3. 再做输出安全评判。
4. 最后决定放行、拦截、提示、脱敏或改写。

核心目标是：**平台里所有 AI 能力先过安全评测，再进入业务流程。**

---

## 2. 推荐接入方式

其他方向如果要接入 AI，统一走前端公共封装：

- 前端封装文件：`D:\file\qian\Edu-F\src\utils\safetyAiRoute.js`
- 后端安全接口：`POST /api/safety/gateway`

推荐做法不是让各方向自己重复写检测逻辑，而是：

1. 业务方先准备自己的 `callAi`。
2. 先传给安全路由。
3. 由安全路由统一处理前置检测、AI 调用、后置检测。

---

## 3. 前端封装入口

### 3.1 公共入口

前端已经提供了统一封装：

```js
import { createSafetyRoute } from '@/utils/safetyAiRoute'
```

### 3.2 最常用的三种调用

```js
import {
  runSafetyRoute,
  runStudentSafetyRoute,
  runTeacherSafetyRoute,
} from '@/utils/safetyAiRoute'
```

- `runSafetyRoute`：通用版
- `runStudentSafetyRoute`：学生版，默认更保守
- `runTeacherSafetyRoute`：教师版，默认允许弹出确认提示

---

## 4. 标准调用流程

其他方向只要按这个流程接：

1. 组装安全参数。
2. 调用安全路由。
3. 在 `callAi` 里写你自己的大模型调用。
4. 根据返回结果决定是否继续展示或发布。

### 4.1 必填参数

- `sourceModule`：来源模块，如 `TEACHER_PREP`、`EDUCATION_RAG`、`PROJECT_CASE`
- `userRole`：`STUDENT` / `TEACHER` / `ADMIN`
- `gradeLevel`：`PRIMARY` / `JUNIOR` / `SENIOR`
- `inputText`：用户输入
- `callAi`：你自己的 AI 调用函数

### 4.2 选填参数

- `classId`
- `courseId`
- `chapterId`
- `metadata`
- `showDialog`
- `confirmBeforeContinue`
- `extractOutputText`

---

## 5. 返回结果说明

安全路由会返回一个统一结果：

```js
{
  passed: true,
  stopped: false,
  canceled: false,
  stage: 'input' | 'output',
  inputSafety: {...},
  outputSafety: {...},
  aiResult: {...},
  outputText: 'AI原始输出',
  safeOutputText: '可展示/可发布内容'
}
```

### 5.1 `stage`

- `input`：在调用 AI 前就被拦下了
- `output`：AI 已经调用完，但输出阶段还会再审一次

### 5.2 `passed`

- `true`：可以继续往下走
- `false`：当前流程已停

### 5.3 `safeOutputText`

最终推荐展示或发布的文本。

- 如果是脱敏结果，就用脱敏后的文本
- 如果是正常结果，就用原始输出

---

## 6. 接入顺序

### 6.1 先过输入

输入阶段主要防：

- 诱导作弊
- 隐私泄露
- 提示词攻击
- 不适龄内容

### 6.2 再调 AI

只有输入通过，才会进入你自己的 `callAi`。

### 6.3 再过输出

输出阶段主要防：

- 幻觉
- 不适龄内容
- 违规建议
- 不可直接发布的成品内容

---

## 7. 两种典型场景

### 7.1 学生场景

建议使用：

```js
runStudentSafetyRoute({
  sourceModule: 'AI_COMPANION',
  gradeLevel: 'JUNIOR',
  inputText,
  callAi: async ({ inputText }) => {
    return await callStudentModel(inputText)
  },
})
```

特点：

- 默认不弹“继续确认”
- 直接按安全结果放行或拦截
- 更适合学习问答

### 7.2 教师发布场景

建议使用：

```js
runTeacherSafetyRoute({
  sourceModule: 'TEACHER_PREP',
  gradeLevel: 'PRIMARY',
  inputText,
  callAi: async ({ inputText }) => {
    return await callTeacherModel(inputText)
  },
})
```

特点：

- 输入/输出都审
- 风险内容可弹窗确认
- 适合教案、题目、评语、课堂材料等发布前检测

---

## 8. 如果只想做前置检测

如果某个方向暂时还没有完整 AI 输出，只想先做输入过滤，可以直接调用：

```js
import { runSafetyBeforeAi } from '@/utils/safetyAiRoute'
```

这适合：

- 草稿检查
- 发布前检查
- 提交前检查

---

## 9. 如果只想做输出复审

如果业务方已经拿到了 AI 输出，只想复审输出内容，可以调用：

```js
import { runSafetyAfterAi } from '@/utils/safetyAiRoute'
```

这适合：

- 教师生成结果的最终发布前检查
- RAG 答案输出检查
- 项目案例生成结果检查

---

## 10. 后端接口参数

安全网关统一接收这些字段：

```json
{
  "sourceModule": "TEACHER_PREP",
  "scene": "TEACHER_COURSE",
  "userRole": "TEACHER",
  "gradeLevel": "JUNIOR",
  "userId": 10001,
  "classId": 1,
  "courseId": 2,
  "chapterId": 3,
  "inputText": "用户输入",
  "outputText": "AI输出",
  "recordLog": true,
  "metadata": {
    "moduleName": "教师备课"
  }
}
```

说明：

- `inputText`：输入内容
- `outputText`：AI 输出内容
- `recordLog`：是否落库
- `metadata`：各方向自己的补充信息

---

## 11. 推荐接入原则

1. 任何 AI 调用都不要绕过安全路由。
2. 业务方只负责自己的模型调用，不负责安全规则细节。
3. 安全评测只做一套，统一复用。
4. 先接最核心的调用链，再慢慢补其它模块。
5. 评测日志要保留，后续才能做科研和统计。

---

## 12. 你这条线的定位

一句话版：

**我负责把安全评测插进所有 AI 调用前后，做成平台级安全网关，而不是单点检测页面。**

---

## 13. 三种角色到底怎么分

这部分是最容易混的，我直接按“谁在用、用来干什么、默认怎么处理”讲。

### 13.1 学生

学生的场景是“学习问答”。

常见模块：

- 智能学伴
- 作业辅导
- 学习路径推荐
- 题目讲解

推荐入口：

```js
runStudentSafetyRoute({
  sourceModule: 'AI_COMPANION',
  gradeLevel: 'JUNIOR',
  inputText,
  callAi,
})
```

学生模式的特点：

1. 默认更保守。
2. 不弹“是否继续”这类复杂确认。
3. 遇到明显风险直接拦截。
4. 遇到脱敏内容就直接返回可展示文本。
5. 适合“学而不是发”的场景。

学生模式不是给学生看安全后台，而是给学生的 AI 问答入口套一层门禁。

---

### 13.2 教师

教师的场景是“生成和发布”。

常见模块：

- 教师备课
- 教案生成
- 题目生成
- 评语生成
- 课堂资源发布
- AI 生成后准备发给学生的内容

推荐入口：

```js
runTeacherSafetyRoute({
  sourceModule: 'TEACHER_PREP',
  gradeLevel: 'PRIMARY',
  inputText,
  callAi,
})
```

教师模式的特点：

1. 输入和输出都要审。
2. 风险不一定立刻拦死，很多情况会先提示再决定。
3. 对适龄性、幻觉、诱导作弊要更敏感。
4. 适合“先生成，再考虑发不发”的流程。
5. 如果老师最终要发布，最好再走一遍输出复审。

教师模式的核心不是“老师更宽松”，而是“老师更容易进入发布链路，所以复审要完整”。

---

### 13.3 管理员

管理员的场景是“治理和测试”。

常见模块：

- 安全看板
- 风险统计
- 全局记录
- 人工复审
- 批量评测
- 策略验证
- 沙箱测试

推荐入口：

```js
runSafetyRoute({
  sourceModule: 'MANUAL_TEST',
  userRole: 'ADMIN',
  gradeLevel: 'JUNIOR',
  inputText,
  callAi,
})
```

管理员模式的特点：

1. 更偏全局视角。
2. 主要看全平台风险和样本评测结果。
3. 可以人工复审全局记录。
4. 适合做策略验证和演示测试。
5. 不是学生业务流程的一部分，而是平台治理入口。

---

## 14. 其他同学怎么直接接

如果别的方向不想自己研究底层细节，直接照这个模板接就够了。

### 14.1 最简模板

```js
import { runStudentSafetyRoute, runTeacherSafetyRoute } from '@/utils/safetyAiRoute'

const result = await runTeacherSafetyRoute({
  sourceModule: 'TEACHER_PREP',
  gradeLevel: 'JUNIOR',
  inputText: '生成一份课堂练习题',
  callAi: async ({ inputText }) => {
    return await myModelApi(inputText)
  },
})

if (!result.passed) {
  return
}

return result.safeOutputText || result.outputText
```

### 14.2 三句话判断该用哪个入口

1. 给学生用的 AI 问答，走 `runStudentSafetyRoute`。
2. 给老师生成教案、题目、评语、发布内容，走 `runTeacherSafetyRoute`。
3. 只是做测试、批跑、看板、策略验证，走 `runSafetyRoute`。

### 14.3 不要自己重复写的东西

其他方向不需要自己再写：

- 敏感词判断
- 学段分级
- 幻觉复审
- 风险弹窗
- 结果脱敏
- 记录落库

这些都已经在安全路由里了。

---

## 15. 接口最小闭环

如果你要告诉别人“这条链路到底长什么样”，就直接说这 5 步：

1. 前端收集输入。
2. 先调用安全路由。
3. 安全路由内部调 `POST /api/safety/gateway`。
4. 业务方自己的 AI 接口只负责生成。
5. 生成后再回来做输出复审。

也就是：

**输入先审 -> 调 AI -> 输出再审 -> 决定展示/拦截/脱敏/改写。**

---

## 16. 给别人交接时的一句话

你可以直接这么说：

**你只管写自己的 AI 能力，真正发给用户前先过我这层安全路由；学生、老师、管理员三个角色分别走不同入口，但底层共用同一套安全评判。**
