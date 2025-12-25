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

    // 存储已生成的注册器类名列表
    private val generatedRegistrars = mutableListOf<String>()

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
                                    
                                    addStatement("")
                                    addStatement("// 注册 %L", func.simpleName.asString())
                                    // 为每个函数生成注册代码
                                    beginControlFlow("channel.filterIsInstance<%T>().filter", eventType)
                                    
                                    // 这里我们需要将注解信息硬编码或通过某种方式传递
                                    // 简单起见，我们重新构造一个临时的注解对象或直接在代码里写逻辑
                                    // 由于 MessageAuthorize 比较复杂，我们通常会生成一个辅助方法来获取它
                                    addStatement("val annotation = %T::class.java.getDeclaredMethod(%S, %T::class.java).getAnnotation(%T::class.java)", 
                                        classDec.toClassName(), 
                                        func.simpleName.asString(), 
                                        eventType,
                                        ClassName("cn.chahuyun.authorize", "MessageAuthorize")
                                    )
                                    
                                    // 应用权限过滤和消息过滤
                                    addStatement("filter.permFilter(it, annotation, %T::class.java) && filter.messageFilter(it, annotation)", eventType)
                                    endControlFlow()
                                    // 订阅事件并处理
                                    beginControlFlow(".subscribeAlways<%T>", eventType)
                                    beginControlFlow("try")
                                    // 根据函数参数数量调用函数
                                    if (func.parameters.size == 1) {
                                        addStatement("instance.%L(it)", func.simpleName.asString())
                                    } else {
                                        addStatement("instance.%L(it, plugin)", func.simpleName.asString())
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