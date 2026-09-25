---
id: interview-guide-spring-mvc-request-validation-errors-v1
title: Spring MVC 请求链、参数校验与统一异常边界
source: interview-guide/spring/mvc-request-validation-errors.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_reference: app/src/main/resources/skills/_shared/references/spring.md
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
official_references:
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html
license: AGPL-3.0
transformation: 基于上游 Spring MVC 高频点和 Java 后端 Skill 追问顺序独立重写，用 Spring Framework 官方语义校准请求、校验和异常边界，并结合本项目真实 Controller、Filter 与 Advice 形成案例；未复制上游原文、Prompt 或源码
skill: SPRING
usage: 知识库阅读、RAG 问答、SPRING 题库、专项面试与复习
---

# Spring MVC 请求链、参数校验与统一异常边界

## 学习目标

学完后应能完成四件事：从 Servlet Filter 画到 Controller 返回响应的完整调用链；区分反序列化、类型转换、对象校验和方法校验；说明局部 `@ExceptionHandler` 与受限范围 `@RestControllerAdvice` 如何选择；结合本项目解释为什么 requestId、业务错误和异步请求完成指标属于不同层次。

面试回答不要停在“`DispatcherServlet` 是前端控制器”。应说明每层拿到什么输入、负责什么决策、失败后由谁转成 HTTP 响应，以及哪些事情不应该塞进拦截器或统一异常处理器。

## 一、一次请求如何到达 Controller

Servlet 容器先执行 Filter 链。Filter 看到的是 `HttpServletRequest` 和 `HttpServletResponse`，此时通常还不知道最终会匹配哪个 Controller 方法，适合做跨框架、尽量靠前的横切工作，例如编码、CORS、安全入口、requestId 和基础指标。

请求进入 Spring MVC 后，`DispatcherServlet` 协调以下组件：

1. `HandlerMapping` 根据路径、HTTP 方法和映射条件找到 handler，并带回匹配的 Interceptor 链。
2. Interceptor 的 `preHandle` 在目标 handler 执行前运行，可以使用 handler 元数据做 MVC 范围内的检查或上下文准备。
3. `HandlerAdapter` 负责以正确方式调用 handler。对注解 Controller，`RequestMappingHandlerAdapter` 会解析方法参数、执行类型转换与校验，再调用 Controller 方法。
4. 返回值处理器把 Java 返回值转换为响应。`@RestController` 通常经 `HttpMessageConverter` 序列化 JSON；视图 Controller 则进入视图解析。
5. 异常先沿调用栈抛出，再由 `HandlerExceptionResolver` 链处理。`@ExceptionHandler` 和 `@ControllerAdvice` 的支持建立在这个机制上。

因此，HandlerMapping 负责“找谁处理”，HandlerAdapter 负责“怎么调用”，ArgumentResolver 负责“某类参数怎么构造”，HttpMessageConverter 负责“请求体和响应体怎样转换”。把它们混成一个“DispatcherServlet 自动完成”会丢失排障所需的边界。

## 二、参数绑定、转换和校验不是同一步

### 请求体反序列化

`@RequestBody` 通常先由 `HttpMessageConverter` 把 JSON 转为 Java 对象。JSON 语法错误、字段类型完全不匹配时，可能在 Bean Validation 之前就失败，因为对象尚未成功构造。

### 简单参数转换

`@RequestParam`、`@PathVariable` 等字符串值要转换成目标类型。枚举、日期或数字格式非法时属于绑定或类型转换错误，不等同于业务规则校验失败。

### 对象校验与方法校验

当 `@Valid` 或 `@Validated` 作用于复杂对象时，常见失败是 `MethodArgumentNotValidException`。当约束直接声明在 Controller 方法参数或返回值上，例如 `@Min`、`@NotBlank`，Spring MVC 可能执行方法级校验并抛出 `HandlerMethodValidationException`。

Spring 官方文档要求应用考虑这两类异常，因为具体抛出哪一种取决于方法签名和约束位置。统一错误协议不能只处理其中一种，也不能把“反序列化失败”“类型转换失败”“校验失败”全部写成同一个含糊消息。

本项目不少 Controller 目前在方法内执行明确的领域边界校验，例如 `MarkdownDocumentController` 检查资料来源、文件类型和 UTF-8 编码，`QuestionBankController` 检查题目版本。这种写法直接且可读，但它不是 Bean Validation；回答时应按真实实现说明，不要声称项目已经全面使用 `@Valid`。

## 三、异常如何变成 HTTP 响应

业务异常需要先分类，再映射 HTTP 状态：

- 请求形状、格式或调用方可修正的参数错误使用 `400 Bad Request`。
- 目标资源不存在使用 `404 Not Found`。
- 版本冲突、幂等冲突或当前状态不允许该动作使用 `409 Conflict`。
- 队列或容量暂时无法接收新工作可使用 `429 Too Many Requests`。
- 下游依赖暂时不可用使用 `503 Service Unavailable`；明确的下游超时可以使用 `504 Gateway Timeout`。
- 未预期异常保留为 `500 Internal Server Error`，响应不泄漏堆栈、密钥、SQL 或第三方原始错误体。

不要为了“前端好处理”让所有接口都返回 HTTP 200 再在 JSON 中放错误码。这样会破坏浏览器、代理、告警、重试和 Micrometer 对成功/失败的通用语义。也不要把所有异常都映射为 500，否则调用方无法区分修改请求、重新加载版本、稍后重试和联系维护者。

局部 `@ExceptionHandler` 适合一个 Controller 的特有错误语义，例如题库重复题映射为 409。`@RestControllerAdvice(assignableTypes = ...)` 适合若干明确 Controller 共享的协议，例如检索超时与不可用。完全全局的 Advice 只有在错误结构和分类真正统一时才值得引入；过宽捕获会把编程错误误包装成可恢复业务错误。

## 四、Filter、Interceptor、ArgumentResolver 和 Advice 如何选择

| 扩展点 | 所在层次 | 适合 | 不适合 |
| --- | --- | --- | --- |
| Servlet Filter | DispatcherServlet 之前/之外 | requestId、CORS、安全入口、跨框架指标、请求包装 | 依赖 Controller 注解的细粒度业务判断 |
| HandlerInterceptor | HandlerMapping 之后、Controller 前后 | 使用 handler 元数据、MVC 上下文、轻量审计或页面区域逻辑 | 代替 Spring Security、读取并重复消费请求体、承载核心业务 |
| HandlerMethodArgumentResolver | Controller 调用前的参数构造 | 从请求上下文稳定构造当前用户、分页对象或领域参数 | 全局错误映射、长耗时业务、随意查多个远程服务 |
| `@ControllerAdvice` / `@ExceptionHandler` | MVC 异常解析与响应阶段 | 统一同类错误结构、状态码和低敏消息 | 吞掉未知异常、执行补偿业务、伪装所有请求成功 |

Spring 官方明确提醒：Interceptor 并不是理想的安全层，因为它与注解 Controller 的路径匹配可能出现偏差。认证授权优先使用 Spring Security 或集成在 Servlet Filter 链中的安全机制，并尽量靠前执行。

## 五、同步与异步请求的完成时点

同步 Controller 返回后，Filter 的 `finally` 通常可以观察最终状态。但异步 Servlet 请求启动后，初始线程会先退出，真正完成、超时或报错发生在之后。如果在初始线程结束时就记录“请求成功”，指标会提前且可能重复。

本项目 `BusinessRequestMetricsFilter` 继承 `OncePerRequestFilter`，为 `/api/` 请求生成 `X-Request-Id` 并写入 MDC。同步请求直接完成计数；异步请求注册 `AsyncListener`，在 `onComplete`、`onTimeout` 或 `onError` 中通过原子完成标记只记录一次。它还从 `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` 读取模板化路由，避免把真实 ID 写进指标标签造成高基数。

这个案例同时说明 Filter 可以在请求进入 MVC 前生成 requestId，但匹配后的模板路由只有在 MVC 完成映射后才可获得。横切逻辑必须考虑数据在调用链中的可见时点。

## 六、本项目代码追踪

### 资料导入：参数错误、资源缺失和容量错误分开

`MarkdownDocumentController` 在 Controller 边界检查空文件、扩展名、UTF-8、source 和任务查询参数。可修正输入映射为 400，资料不存在映射为 404，排队任务取消冲突映射为 409，执行器拒绝新任务映射为 429。这样前端可以分别提示重新选文件、刷新资料、停止冲突操作或稍后重试。

### 题库：乐观版本与业务冲突不能伪装成参数错误

`QuestionBankController` 把非法输入映射为 400、缺失题目映射为 404、会话或版本冲突映射为 409、存储暂不可用映射为 503。相同题目的数据库唯一键冲突也返回 409。Controller 只做协议适配，题目证据、状态和会话规则仍由 `QuestionBankService` 与 Store 决定。

### 检索：受限范围 Advice 保留失败语义

`KnowledgeRetrievalExceptionHandler` 只作用于 Dense、Sparse 和 Hybrid 三个检索 Controller，把检索总 deadline 超时映射为 504，把存储不可用映射为 503。它没有全局捕获所有 `RuntimeException`，因此不会把无关编程错误误报成“检索暂不可用”。

### 当前实现的取舍

项目目前多个 Controller 各自重复少量 `IllegalArgumentException` 映射。这降低了早期抽象成本，也让局部语义清楚；代价是错误体和异常分类需要持续审查。没有出现真实协议漂移或维护阻塞前，不应只为“统一”引入宽泛全局 Advice。出现稳定的重复错误协议后，可先抽取窄范围 Advice 和显式业务异常，再迁移相关 Controller。

## 七、常见错误与排查顺序

| 现象 | 先检查 | 原因或处理方向 |
| --- | --- | --- |
| Controller 没进入就返回 400 | JSON、Content-Type、参数类型转换 | 对象可能尚未构造，不要只查业务校验 |
| 写了 `@Valid` 但约束没执行 | 注解位置、校验依赖、方法签名 | 区分对象校验和方法校验，确认异常类型 |
| Advice 没接住异常 | Advice 范围、异常包装、响应是否已提交 | 核对 assignableTypes/basePackages 和 cause 链 |
| Interceptor 看不到请求 | 路径匹配、HandlerMapping 类型、Filter 是否提前拒绝 | 安全与跨框架入口优先放 Filter/Security |
| 指标出现大量不同 URI | 是否使用原始 URI 作为 tag | 使用匹配模板，未知路由归一化 |
| SSE/异步请求过早记成功 | 是否在初始线程 finally 结束计时 | 使用 AsyncListener 且保证只完成一次 |

## 八、两分钟面试回答模板

1. **入口**：容器先过 Filter，再进入 DispatcherServlet。
2. **定位**：HandlerMapping 找 handler 和 Interceptor，HandlerAdapter 负责调用。
3. **参数**：消息转换、类型转换、ArgumentResolver 和校验按方法签名工作。
4. **业务**：Controller 做协议适配，领域规则在 Service，持久化约束在 Store/数据库。
5. **失败**：异常由 HandlerExceptionResolver 链交给局部 handler 或受限 Advice，按 400/404/409/429/503/504 语义返回。
6. **横切**：requestId/安全入口放 Filter，依赖 handler 元数据的轻量逻辑放 Interceptor；异步请求用真实完成回调记录结果。

## 九、练习题

### 主问题 1｜框架机制与实现

请从 Servlet Filter 开始，说明一个 JSON 请求如何经过 Spring MVC 到达 `@RestController`，并指出 HandlerMapping、HandlerAdapter、ArgumentResolver 和 HttpMessageConverter 各自负责什么。

参考回答：Filter 先处理跨框架横切逻辑，请求进入 DispatcherServlet；HandlerMapping 根据映射条件找到 handler 与 Interceptor，HandlerAdapter 组织方法调用，ArgumentResolver 构造各参数，HttpMessageConverter 负责请求体反序列化与响应体序列化。参数准备完成后调用 Controller，返回值处理器生成响应；异常交给 HandlerExceptionResolver 链。各组件职责分离，便于扩展和排障。

回答检查点：Filter、DispatcherServlet、HandlerMapping、Interceptor、HandlerAdapter、ArgumentResolver、HttpMessageConverter、返回值和异常解析。

追问：为什么在 Filter 最开始通常拿不到 `BEST_MATCHING_PATTERN_ATTRIBUTE`？

追问参考：Controller 路由模板尚未由 HandlerMapping 匹配；可在 MVC 匹配完成后或请求完成阶段读取，未知路由需要归一化。

### 主问题 2｜事务与故障边界

Spring MVC 中请求体反序列化、类型转换、对象校验和方法校验分别可能在哪一步失败？统一异常处理为什么要同时考虑 `MethodArgumentNotValidException` 和 `HandlerMethodValidationException`？

参考回答：JSON 先经消息转换器构造对象，语法或字段类型错误可能在校验前失败；请求参数还要做目标类型转换。复杂对象的 `@Valid` 常产生 MethodArgumentNotValidException，直接参数或返回值约束可能进入方法校验并产生 HandlerMethodValidationException。异常处理应按失败层和字段信息生成 400，不能只处理一种异常，也不能把业务冲突或下游不可用错误都压成校验失败。

回答检查点：反序列化、类型转换、Bean Validation、方法校验、两类异常、400 与业务异常分层、低敏错误体。

追问：为什么请求返回 400 且 Controller 断点没进入，不代表 `@Valid` 已经执行？

追问参考：JSON 解析或类型转换可能先失败，对象或方法参数尚未进入 Bean Validation。

### 主问题 3｜调用链表达

本项目如何选择 Filter、Controller 局部 `@ExceptionHandler` 和受限范围 `@RestControllerAdvice`？请结合 requestId/指标、题库冲突和检索超时说明完整调用链。

参考回答：BusinessRequestMetricsFilter 在 /api/ 入口生成 requestId、维护 MDC 和低基数指标，异步请求通过 AsyncListener 在真实完成时只记录一次。QuestionBankController 的参数、缺失、冲突和暂不可用语义只属于题库协议，使用局部 handler 映射 400/404/409/503。KnowledgeRetrievalExceptionHandler 只覆盖三类检索 Controller，将 deadline 超时映射 504、存储不可用映射 503。这样横切入口、局部业务协议和共享检索故障各在合适层次，不用宽泛全局捕获掩盖未知错误。

回答检查点：Filter 执行层、异步完成、模板路由、局部异常语义、受限 Advice、409/503/504、未知异常不吞掉。

追问：为什么不直接写一个捕获所有 `Exception` 的全局 Advice 并统一返回 200？

追问参考：会丢失 HTTP 失败语义、掩盖编程错误、破坏代理与指标判断，并让客户端无法区分修正请求、解决冲突和稍后重试。

## 十、掌握标准

- 能不看文档画出 Filter 到 Controller 再到异常解析和响应转换的调用链。
- 能解释为什么 JSON 解析、类型转换、对象校验和方法校验会产生不同失败。
- 能根据认证安全、requestId、handler 元数据、参数构造和错误映射选择正确扩展点。
- 能用 400、404、409、429、503、504 说明调用方下一步动作，而不是只背状态码名称。
- 能在本项目定位 `BusinessRequestMetricsFilter`、`MarkdownDocumentController`、`QuestionBankController` 和 `KnowledgeRetrievalExceptionHandler`，并如实说明当前没有全面采用 Bean Validation 或全局错误协议。
