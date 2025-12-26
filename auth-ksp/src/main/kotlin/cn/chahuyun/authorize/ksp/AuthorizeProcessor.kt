@file:Suppress("unused")

package cn.chahuyun.authorize.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * 授权处理器，用于处理 @MessageAuthorize 注解并生成相应的监听器注册器
 * 在编译时扫描带有 @MessageAuthorize 注解的函数，并生成对应的注册代码
 */
class AuthorizeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    // 存储已生成的注册器类名（使用 Set 去重，避免多轮处理导致 service 文件出现重复 provider，进而造成重复订阅）
    private val generatedRegistrars = linkedSetOf<String>()

    /**
     * 处理编译时注解，扫描带有 @MessageAuthorize 注解的函数
     * 
     * @param resolver KSP解析器，用于访问符号信息
     * @return 未处理的符号列表（通常为空）
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 获取所有带有 MessageAuthorize 注解的符号
        val symbols = resolver.getSymbolsWithAnnotation("cn.chahuyun.authorize.MessageAuthorize")
        val functions = symbols.filterIsInstance<KSFunctionDeclaration>()
        
        // 如果没有找到任何带注解的函数，直接返回空列表
        if (!functions.iterator().hasNext()) return emptyList()

        // 按声明类分组函数
        val functionsByClass = functions.groupBy { it.parentDeclaration as KSClassDeclaration }

        // 为每个类生成注册器
        functionsByClass.forEach { (classDec, funcDecs) ->
            val fullName = generateRegistrar(classDec, funcDecs)
            generatedRegistrars.add(fullName)
        }

        return emptyList()
    }

    /**
     * 处理完成时的回调方法，生成服务注册文件
     */
    override fun finish() {
        // 如果没有生成任何注册器，直接返回
        if (generatedRegistrars.isEmpty()) return
        
        try {
            // 创建服务注册文件，用于服务加载器自动发现生成的注册器
            val file = codeGenerator.createNewFile(
                Dependencies(true),
                "",
                "META-INF/services/cn.chahuyun.authorize.listener.GeneratedListenerRegistrar",
                ""
            )
            file.bufferedWriter().use { writer ->
                // 将所有生成的注册器类名写入服务文件
                generatedRegistrars.forEach { writer.write("$it\n") }
            }
        } catch (e: Exception) {
            // 文件可能已存在
        }

        // 兼容：部分打包方式可能丢失 generated resources（META-INF/services），导致 ServiceLoader 找不到 provider。
        // 为保证“只要 registrar 类能编译进产物，就一定能被发现”，额外生成一个纯 class 的索引对象。
        // 运行时可在 ServiceLoader 为空时回退读取该索引。
        try {
            val indexPackage = "cn.chahuyun.authorize.listener"
            val indexName = "GeneratedListenerRegistrarIndex"

            val registrarsArrayInit = CodeBlock.builder()
                .add("arrayOf(\n")
                .apply {
                    generatedRegistrars.forEachIndexed { idx, name ->
                        add("    %S", name)
                        if (idx != generatedRegistrars.size - 1) add(",")
                        add("\n")
                    }
                }
                .add(")")
                .build()

            val fileSpec = FileSpec.builder(indexPackage, indexName)
                .addType(
                    TypeSpec.objectBuilder(indexName)
                        .addKdoc(
                            "KSP 生成：监听注册器索引。\n\n" +
                                "当 META-INF/services 在打包过程中丢失时，运行时可通过该索引回退发现 registrar。\n" +
                                "请勿手动修改。\n"
                        )
                        .addProperty(
                            PropertySpec.builder("REGISTRARS", ARRAY.parameterizedBy(STRING))
                                .addAnnotation(JvmField::class)
                                .initializer(registrarsArrayInit)
                                .build()
                        )
                        .addFunction(
                            FunSpec.builder("registrarClassNames")
                                .addAnnotation(JvmStatic::class)
                                .returns(ARRAY.parameterizedBy(STRING))
                                .addStatement("return REGISTRARS")
                                .build()
                        )
                        .build()
                )
                .build()

            fileSpec.writeTo(codeGenerator, true)
        } catch (e: Exception) {
            logger.warn("生成 GeneratedListenerRegistrarIndex 失败：${e.message}")
        }
    }

    /**
     * 为指定类和函数生成注册器代码
     * 
     * @param classDec 包含函数的类声明
     * @param functions 需要注册的函数列表
     * @return 生成的注册器类的完整名称
     */
    private fun generateRegistrar(classDec: KSClassDeclaration, functions: List<KSFunctionDeclaration>): String {
        val packageName = classDec.packageName.asString()
        val className = classDec.simpleName.asString()
        val registrarClassName = "${className}_AuthorizeRegistrar"
        val fullRegistrarName = if (packageName.isEmpty()) registrarClassName else "$packageName.$registrarClassName"

        // 构建生成的文件
        val fileSpec = FileSpec.builder(packageName, registrarClassName)
            .addType(
                TypeSpec.classBuilder(registrarClassName)
                    // 实现 GeneratedListenerRegistrar 接口
                    .addSuperinterface(ClassName("cn.chahuyun.authorize.listener", "GeneratedListenerRegistrar"))
                    .addFunction(
                        FunSpec.builder("register")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("channel", ClassName("net.mamoe.mirai.event", "EventChannel").parameterizedBy(ClassName("net.mamoe.mirai.event.events", "MessageEvent")))
                            .addParameter("filter", ClassName("cn.chahuyun.authorize.listener", "ListenerFilter"))
                            .addParameter("handleApi", ClassName("cn.chahuyun.authorize.exception", "ExceptionHandleApi"))
                            .addParameter("plugin", ClassName("net.mamoe.mirai.console.plugin.jvm", "JvmPlugin"))
                            .addCode(buildCodeBlock {
                                // 创建类实例
                                addStatement("val instance = %T()", classDec.toClassName())
                                functions.forEach { func ->
                                    val eventType = func.parameters.first().type.resolve().toClassName()
                                    val funcName = func.simpleName.asString()
                                    val safeName = funcName.replace(Regex("[^A-Za-z0-9_]"), "_")
                                    
                                    addStatement("")
                                    addStatement("// 注册 %L", funcName)
                                    // 先在链式调用外部获取注解（避免作用域问题；兼容 suspend 函数）
                                    addStatement(
                                        "val annotation_%L = %T::class.java.declaredMethods.first { it.name == %S && it.isAnnotationPresent(%T::class.java) }.getAnnotation(%T::class.java)",
                                        safeName,
                                        classDec.toClassName(),
                                        funcName,
                                        ClassName("cn.chahuyun.authorize", "MessageAuthorize"),
                                        ClassName("cn.chahuyun.authorize", "MessageAuthorize")
                                    )

                                    // 生成中间 channel 变量，避免链式 beginControlFlow 产出非法语法
                                    addStatement(
                                        "val channel_%L = channel.filterIsInstance<%T>().filter { filter.permFilter(it, annotation_%L, %T::class.java) && filter.messageFilter(it, annotation_%L) }",
                                        safeName,
                                        eventType,
                                        safeName,
                                        eventType,
                                        safeName
                                    )

                                    // 订阅事件并处理（并发与优先级取自注解）

                                    beginControlFlow(
                                        "channel_%L.subscribeAlways<%T>(concurrency = annotation_%L.concurrency, priority = annotation_%L.priority)",
                                        safeName,
                                        eventType,
                                        safeName,
                                        safeName
                                    )
                                    if (funcName == "test") {
                                        addStatement(
                                            "cn.chahuyun.authorize.HuYanAuthorize.log.debug(%S + java.lang.System.identityHashCode(this@%L) + %S + it.sender.id + %S + it.subject.id)",
                                            "ENTRY[test][KSP] registrar@",
                                            registrarClassName,
                                            " sender=",
                                            " subject="
                                        )
                                    }
                                    beginControlFlow("try")
                                    // 根据函数参数数量调用函数
                                    if (func.parameters.size == 1) {
                                        addStatement("instance.%L(it)", funcName)
                                    } else {
                                        addStatement("instance.%L(it, plugin)", funcName)
                                    }
                                    nextControlFlow("catch (e: Throwable)")
                                    // 捕获异常并处理
                                    addStatement("handleApi.handle(e)")
                                    endControlFlow()
                                    endControlFlow()
                                }
                            })
                            .build()
                    )
                    .build()
            )
            .build()

        // 将生成的代码写入文件
        fileSpec.writeTo(codeGenerator, false)
        return fullRegistrarName
    }
}

/**
 * 授权处理器提供者，用于创建 AuthorizeProcessor 实例
 */
class AuthorizeProcessorProvider : SymbolProcessorProvider {
    /**
     * 创建符号处理器实例
     * 
     * @param environment 符号处理器环境
     * @return 新创建的 AuthorizeProcessor 实例
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AuthorizeProcessor(environment.codeGenerator, environment.logger)
    }
}