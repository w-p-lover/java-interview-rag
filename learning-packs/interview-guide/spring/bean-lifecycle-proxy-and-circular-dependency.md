---
id: interview-guide-spring-bean-lifecycle-proxy-circular-v1
title: Spring Bean 生命周期、代理、扩展点与循环依赖
source: interview-guide/spring/bean-lifecycle-proxy-and-circular-dependency.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/spring.md]
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游 Spring 核心知识点独立扩展为容器调用链课程，并结合本项目 Profile、Configuration、Store 适配与可观测装饰；未复制上游原文、Prompt 或源码
skill: SPRING
usage: 知识库阅读、RAG 问答、SPRING 题库、专项面试与复习
---

# Spring Bean 生命周期、代理、扩展点与循环依赖

## 一、从 BeanDefinition 到可用 Bean

容器先读取配置形成 BeanDefinition，再实例化、属性填充，调用 Aware 回调、BeanPostProcessor 前置处理、初始化方法和后置处理，最终可能返回代理对象。销毁时执行 `@PreDestroy`、DisposableBean 或配置的 destroyMethod。InstantiationPostProcessor、BeanFactoryPostProcessor 和 BeanPostProcessor 所在阶段不同，不能混用。

构造器注入让依赖和不可变性清晰，也会直接暴露设计循环；字段注入隐藏依赖并降低可测试性。本项目的配置类通过构造参数或 `@Bean` 参数显式组装 Store、Service、执行器和模型客户端，local/storage Profile 选择不同实现。

## 二、代理决定增强入口

Spring AOP 通常使用 JDK 动态代理或 CGLIB 子类代理。外部调用经过代理，事务、缓存或重试拦截器才有机会执行；同对象自调用直接调用 `this`，绕过代理。final 类/方法、private 方法和对象不是容器 Bean 也会限制代理增强，具体边界与 Spring 版本和代理方式有关。

代理链顺序会改变行为：重试包在事务外可能每次重试新开事务，事务包在重试外可能让所有尝试共享已标记回滚的事务。回答时要画调用链，而不是只报注解。

## 三、循环依赖不是功能

单例 setter/字段循环依赖在特定条件下可借助三级缓存提前暴露引用，但构造器循环无法先得到可用实例；prototype 也没有相同解决路径。代理对象需要提前引用时还会增加一致性复杂度。现代 Spring Boot 默认更倾向暴露循环依赖问题，工程上应拆分职责、提取第三方服务或改用事件，而不是依赖容器技巧掩盖耦合。

三级缓存的核心不是“有三个 Map”，而是区分完整单例、早期对象和用于创建早期引用的工厂，使需要 AOP 的 Bean 尽量获得一致代理引用。

## 四、条件装配与生命周期资源

`@Profile`、`@ConditionalOnProperty` 和条件 Bean 用于选择实现，但相同接口不能意外出现多个无限定候选。线程池、调度器、客户端等资源 Bean 要声明关闭方法，避免上下文关闭后线程继续存活。配置属性必须在启动时验证容量和超时边界，不能让非法值运行到业务请求才暴露。

## 五、本项目调用链映射

local Profile 使用内存/Noop 实现保持零外部依赖，storage Profile 组装 MySQL、Qdrant、Lucene、Redis 和模型客户端。`MarkdownImportConfiguration`、`QuestionBankConfiguration`、`RagAnswerConfiguration` 把线程池、Store 与 Service 明确组装，并为执行器声明 shutdown。

评价链使用接口与装饰器：核心 evaluator 负责评分，observable evaluator 记录版本、token、耗时和状态。Store 接口使内存和 MySQL 实现可替换。这些模式依赖容器装配，但业务正确性仍在领域服务和数据库约束中，不能把 `@Bean` 当成架构本身。

## 六、练习题

### 主问题 1｜框架机制与实现

描述 Spring Bean 从定义到代理对象可用的主要生命周期，BeanPostProcessor 在哪里发挥作用？

参考回答：配置解析形成 BeanDefinition，容器实例化、填充依赖，执行 Aware 和初始化前回调，再执行初始化方法和初始化后回调；AOP 常由后置处理器在初始化后包装代理。最终单例进入缓存，关闭时调用销毁回调。BeanFactoryPostProcessor 修改定义，BeanPostProcessor 处理实例，阶段不同。

回答检查点：BeanDefinition、实例化、属性填充、Aware、初始化、后置处理、代理和销毁。

追问：`@PostConstruct` 时拿到的一定是最终代理对象吗？

追问参考：初始化回调通常发生在初始化后代理包装之前，当前对象自身不应假定已经是最终代理；外部依赖拿到的引用由后处理过程决定。

### 主问题 2｜事务与故障边界

为什么构造器循环依赖不能像部分 setter 单例循环那样通过三级缓存解决？

参考回答：构造 A 必须先得到完整 B，而构造 B 又必须先得到 A，此时 A 实例尚未创建，无法提前暴露引用。setter 循环至少可以先实例化对象再填充属性，但仍有代理、初始化未完成和设计耦合风险。应拆分职责或引入中间服务，而不是开启循环依赖作为常规方案。

回答检查点：实例化时点、早期引用、三级缓存作用、代理风险、重构方向。

追问：为什么能启动不代表循环依赖设计合理？

追问参考：消费者可能拿到未初始化或代理不一致对象，测试和演进困难，容器版本策略变化也会暴露问题。

### 主问题 3｜调用链表达

本项目为什么用 Profile 和接口分别组装 local 与 storage 实现，如何排查装配错了实现？

参考回答：接口稳定业务依赖，Profile 让 local 使用内存/Noop、storage 使用 MySQL/Redis/Qdrant 等真实实现。排查先确认 active profiles 和条件报告，再看 BeanDefinition 来源、候选数量、Qualifier 和启动日志；最后沿 Controller-Service-Store 调用验证实际类型。不能只看配置文件猜测。

回答检查点：接口隔离、Profile、条件报告、候选冲突、实际 Bean 类型和调用链。

追问：线程池 Bean 为什么需要 destroyMethod？

追问参考：上下文关闭时应停止接收任务并释放线程；否则重启、测试或部署关闭可能残留非守护线程和未完成工作。
