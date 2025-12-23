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

class AuthorizeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val generatedRegistrars = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("cn.chahuyun.authorize.MessageAuthorize")
        val functions = symbols.filterIsInstance<KSFunctionDeclaration>()
        
        if (!functions.iterator().hasNext()) return emptyList()

        val functionsByClass = functions.groupBy { it.parentDeclaration as KSClassDeclaration }

        functionsByClass.forEach { (classDec, funcDecs) ->
            val fullName = generateRegistrar(classDec, funcDecs)
            generatedRegistrars.add(fullName)
        }

        return emptyList()
    }

    override fun finish() {
        if (generatedRegistrars.isEmpty()) return
        
        try {
            val file = codeGenerator.createNewFile(
                Dependencies(true),
                "",
                "META-INF/services/cn.chahuyun.authorize.listener.GeneratedListenerRegistrar",
                ""
            )
            file.bufferedWriter().use { writer ->
                generatedRegistrars.forEach { writer.write("$it\n") }
            }
        } catch (e: Exception) {
            // 文件可能已存在
        }
    }

    private fun generateRegistrar(classDec: KSClassDeclaration, functions: List<KSFunctionDeclaration>): String {
        val packageName = classDec.packageName.asString()
        val className = classDec.simpleName.asString()
        val registrarClassName = "${className}_AuthorizeRegistrar"
        val fullRegistrarName = if (packageName.isEmpty()) registrarClassName else "$packageName.$registrarClassName"

        val fileSpec = FileSpec.builder(packageName, registrarClassName)
            .addType(
                TypeSpec.classBuilder(registrarClassName)
                    .addSuperinterface(ClassName("cn.chahuyun.authorize.listener", "GeneratedListenerRegistrar"))
                    .addFunction(
                        FunSpec.builder("register")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("channel", ClassName("net.mamoe.mirai.event", "EventChannel").parameterizedBy(ClassName("net.mamoe.mirai.event.events", "MessageEvent")))
                            .addParameter("filter", ClassName("cn.chahuyun.authorize.listener", "ListenerFilter"))
                            .addParameter("handleApi", ClassName("cn.chahuyun.authorize.exception", "ExceptionHandleApi"))
                            .addParameter("plugin", ClassName("net.mamoe.mirai.console.plugin.jvm", "JvmPlugin"))
                            .addCode(buildCodeBlock {
                                addStatement("val instance = %T()", classDec.toClassName())
                                functions.forEach { func ->
                                    val eventType = func.parameters.first().type.resolve().toClassName()
                                    
                                    addStatement("")
                                    addStatement("// 注册 %L", func.simpleName.asString())
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
                                    
                                    addStatement("filter.permFilter(it, annotation, %T::class.java) && filter.messageFilter(it, annotation)", eventType)
                                    endControlFlow()
                                    beginControlFlow(".subscribeAlways<%T>", eventType)
                                    beginControlFlow("try")
                                    if (func.parameters.size == 1) {
                                        addStatement("instance.%L(it)", func.simpleName.asString())
                                    } else {
                                        addStatement("instance.%L(it, plugin)", func.simpleName.asString())
                                    }
                                    nextControlFlow("catch (e: Throwable)")
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

        fileSpec.writeTo(codeGenerator, false)
        return fullRegistrarName
    }
}

class AuthorizeProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AuthorizeProcessor(environment.codeGenerator, environment.logger)
    }
}

