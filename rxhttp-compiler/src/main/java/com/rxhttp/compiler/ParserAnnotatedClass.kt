package com.rxhttp.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import rxhttp.wrapper.annotation.Parser
import java.io.IOException
import java.util.*
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeMirror

class ParserAnnotatedClass {
    private val mElementMap: MutableMap<String, TypeElement>
    fun add(typeElement: TypeElement) {
        val annotation = typeElement.getAnnotation(Parser::class.java)
        val name: String = annotation.name
        require(name.length != 0) {
            String.format("methodName() in @%s for class %s is null or empty! that's not allowed",
                Parser::class.java.simpleName, typeElement.qualifiedName.toString())
        }
        mElementMap[name] = typeElement
    }

    fun getMethodList(platform: String): List<FunSpec> {
        val t = TypeVariableName("T")
        val k = TypeVariableName("K")
        val v = TypeVariableName("V")
        val responseName = ClassName("okhttp3", "Response")
        val schedulerName = ClassName("io.reactivex", "Scheduler")
        val observableName = ClassName("io.reactivex", "Observable")
        val consumerName = ClassName("io.reactivex.functions", "Consumer")
        val httpSenderName = ClassName("rxhttp", "HttpSender")
        val parserName = ClassName("rxhttp.wrapper.parse", "Parser")
        val progressName = ClassName("rxhttp.wrapper.entity", "Progress")
        val simpleParserName = ClassName("rxhttp.wrapper.parse", "SimpleParser")
        val mapParserName = ClassName("rxhttp.wrapper.parse", "MapParser")
        val listParserName = ClassName("rxhttp.wrapper.parse", "ListParser")
        val downloadParserName = ClassName("rxhttp.wrapper.parse", "DownloadParser")
        val bitmapParserName = ClassName("rxhttp.wrapper.parse", "BitmapParser")
        val okResponseName = ClassName("okhttp3", "Response")
        val headersName = ClassName("okhttp3", "Headers")
        val okResponseParserName = ClassName("rxhttp.wrapper.parse", "OkResponseParser")
        val observableOkResponseName = observableName.parameterizedBy(okResponseName)
        val observableHeadersName = observableName.parameterizedBy(headersName)
        val typeName = String::class.asTypeName()
        val classTName = Class::class.asClassName().parameterizedBy(t)
        val classKName = Class::class.asClassName().parameterizedBy(k)
        val classVName = Class::class.asClassName().parameterizedBy(v)
        val progressStringName = progressName.parameterizedBy(typeName)
        val observableTName = observableName.parameterizedBy(t)
        val observableStringName = observableName.parameterizedBy(typeName)
        val consumerProgressStringName = consumerName.parameterizedBy(progressStringName)
        val parserTName = parserName.parameterizedBy(t)
        val methodList: MutableList<FunSpec> = ArrayList()
        var method: FunSpec.Builder
        method = FunSpec.builder("execute")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(Throws::class)
                .addMember("%T::class", IOException::class).build())
            .addStatement("setConverter(param)")
            .addStatement("return %T.execute(addDefaultDomainIfAbsent(param))", httpSenderName)
            .returns(responseName)
        methodList.add(method.build())
        method = FunSpec.builder("execute")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addAnnotation(AnnotationSpec.builder(Throws::class)
                .addMember("%T::class", IOException::class).build())
            .addParameter("parser", parserTName)
            .addStatement("return parser.onParse(execute())", httpSenderName)
            .returns(t)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOn")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("scheduler", schedulerName)
            .addStatement("this.scheduler=scheduler")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOnCurrent")
            .addKdoc("设置在当前线程发请求\n")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("this.scheduler=null")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOnIo")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("this.scheduler=Schedulers.io()")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOnComputation")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("this.scheduler=Schedulers.computation()")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOnNewThread")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("this.scheduler=Schedulers.newThread()")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOnSingle")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("this.scheduler=Schedulers.single()")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("subscribeOnTrampoline")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("this.scheduler=Schedulers.trampoline()")
            .addStatement("return this as R")
            .returns(RxHttpGenerator.r)
        methodList.add(method.build())
        method = FunSpec.builder("asParser")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addParameter("parser", parserTName)
            .addStatement("setConverter(param)")
            .addStatement("var observable=%T.syncFrom(addDefaultDomainIfAbsent(param),parser)", httpSenderName)
            .beginControlFlow("if(scheduler!=null)")
            .addStatement("observable=observable.subscribeOn(scheduler)")
            .endControlFlow()
            .addStatement("return observable")
            .returns(observableTName)
        methodList.add(method.build())
        method = FunSpec.builder("asObject")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addParameter("type", classTName)
            .addStatement("return asParser(%T(type))", simpleParserName)
        methodList.add(method.build())


        if ("Android" == platform) {
            method = FunSpec.builder("asBitmap")
                .addModifiers(KModifier.PUBLIC)
                .addStatement("return asParser(%T())", bitmapParserName)
            methodList.add(method.build())
        }
        method = FunSpec.builder("asString")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(String::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asBoolean")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Boolean::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asByte")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Byte::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asShort")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Short::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asInteger")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Int::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asLong")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Long::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asFloat")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Float::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asDouble")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Double::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asMap")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asObject(Map::class.java)")
        methodList.add(method.build())
        method = FunSpec.builder("asMap")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addParameter("type", classTName)
            .addStatement("return asParser(%T(type,type))", mapParserName)
        methodList.add(method.build())
        method = FunSpec.builder("asMap")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(k)
            .addTypeVariable(v)
            .addParameter("kType", classKName)
            .addParameter("vType", classVName)
            .addStatement("return asParser(%T(kType,vType))", mapParserName)
        methodList.add(method.build())
        method = FunSpec.builder("asList")
            .addModifiers(KModifier.PUBLIC)
            .addTypeVariable(t)
            .addParameter("type", classTName)
            .addStatement("return asParser(%T(type))", listParserName)
        methodList.add(method.build())

        method = FunSpec.builder("asHeaders")
            .addKdoc("调用此方法，订阅回调时，返回 {@link okhttp3.Headers} 对象\n")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asOkResponse().map(Response::headers)")
            .returns(observableHeadersName)
        methodList.add(method.build())

        method = FunSpec.builder("asOkResponse")
            .addKdoc("调用此方法，订阅回调时，返回 {@link okhttp3.Response} 对象\n")
            .addModifiers(KModifier.PUBLIC)
            .addStatement("return asParser(%T())", okResponseParserName)
            .returns(observableOkResponseName)
        methodList.add(method.build())

        //获取自定义解析器，并生成对应的方法
        for ((key, typeElement) in mElementMap) {
            var returnType: TypeMirror? = null //获取onParse方法的返回类型
            for (element in typeElement.enclosedElements) {
                if (element !is ExecutableElement) continue
                if (!element.getModifiers().contains(Modifier.PUBLIC)
                    || element.getModifiers().contains(Modifier.STATIC)) continue
                val executableElement = element
                if (executableElement.simpleName.toString() == "onParse" && executableElement.parameters.size == 1 && executableElement.parameters[0].asType().toString() == "okhttp3.Response") {
                    returnType = executableElement.returnType
                    break
                }
            }
            if (returnType == null) continue
            val typeVariableNames: MutableList<TypeVariableName> = ArrayList()
            val parameterSpecs: MutableList<ParameterSpec> = ArrayList()
            val typeParameters: List<TypeParameterElement> = typeElement.getTypeParameters()
            for (element in typeParameters) {

                val typeVariableName = element.asTypeVariableName()
                typeVariableNames.add(typeVariableName)
                val parameterSpec: ParameterSpec = ParameterSpec.builder(
                    element.asType().toString().toLowerCase() + "Type",
                    Class::class.asClassName().parameterizedBy(typeVariableName)).build()
                parameterSpecs.add(parameterSpec)
            }

            //自定义解析器对应的asXxx方法里面的语句
            //自定义解析器对应的asXxx方法里面的语句
            val statementBuilder = StringBuilder("return asParser(%T")
            if (typeVariableNames.size > 0) { //添加泛型
                statementBuilder.append("<")
                var i = 0
                val size = typeVariableNames.size
                while (i < size) {
                    val variableName = typeVariableNames[i]
                    statementBuilder.append(variableName.name)
                        .append(if (i == size - 1) ">" else ",")
                    i++
                }
            }

            statementBuilder.append("(")
            if (parameterSpecs.size > 0) { //添加参数
                var i = 0
                val size = parameterSpecs.size
                while (i < size) {
                    val parameterSpec = parameterSpecs[i]
                    statementBuilder.append(parameterSpec.name)
                    if (i < size - 1) statementBuilder.append(",")
                    i++
                }
            }
            statementBuilder.append("))")
            method = FunSpec.builder("as$key")
                .addModifiers(KModifier.PUBLIC)
                .addParameters(parameterSpecs)
                .addStatement(statementBuilder.toString(), typeElement.asClassName())

            typeVariableNames.forEach {
                method.addTypeVariable(it.toKClassTypeName() as TypeVariableName)
            }
            methodList.add(method.build())
        }
        method = FunSpec.builder("asDownload")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("destPath", String::class)
            .addStatement("return asParser(%T(destPath))", downloadParserName)
        methodList.add(method.build())
        method = FunSpec.builder("asDownload")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("destPath", String::class)
            .addParameter("progressConsumer", consumerProgressStringName)
            .addStatement("return asDownload(destPath, 0, progressConsumer, null)")
//            .returns(observableStringName)
        methodList.add(method.build())
        method = FunSpec.builder("asDownload")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("destPath", String::class)
            .addParameter("progressConsumer", consumerProgressStringName)
            .addParameter("observeOnScheduler", schedulerName)
            .addStatement("return asDownload(destPath, 0, progressConsumer, observeOnScheduler)")
//            .returns(observableStringName)
        methodList.add(method.build())
        method = FunSpec.builder("asDownload")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("destPath", String::class)
            .addParameter("offsetSize", Long::class)
            .addParameter("progressConsumer", consumerProgressStringName)
            .addStatement("return asDownload(destPath, offsetSize, progressConsumer, null)")
//            .returns(observableStringName)
        methodList.add(method.build())


        val offsetSize = ParameterSpec.builder("offsetSize", Long::class)
//            .defaultValue("0")
            .build()
        val observeOnScheduler = ParameterSpec.builder("observeOnScheduler", schedulerName.copy(nullable = true))
//            .defaultValue("null")
            .build()
        method = FunSpec.builder("asDownload")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("destPath", String::class)
            .addParameter(offsetSize)
            .addParameter("progressConsumer", consumerProgressStringName)
            .addParameter(observeOnScheduler)
            .addStatement("setConverter(param)")
            .addStatement("var observable = %T\n" +
                ".downloadProgress(addDefaultDomainIfAbsent(param),destPath,offsetSize,scheduler)", httpSenderName)
            .beginControlFlow("if(observeOnScheduler != null)")
            .addStatement("observable=observable.observeOn(observeOnScheduler)")
            .endControlFlow()
            .addStatement("return observable.doOnNext(progressConsumer)\n" +
                ".filter { it.isCompleted }\n" +
                ".map { it.result }")
            .returns(observableStringName)
        methodList.add(method.build())
        return methodList
    }

    init {
        mElementMap = LinkedHashMap()
    }
}