package com.example.annotation


import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types


@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedOptions()
@SupportedAnnotationTypes("*")
class PerlaProcessor : AbstractProcessor() {

    private var mTypeUtil: Types? = null
    private var mElementUtil: Elements? = null
    private var mFiler: Filer? = null
    private var mMessager: Messager? = null
    private val aptSourceBook = HashMap<TypeElement, AptManInfo>()


    override fun init(processingEnvironment: ProcessingEnvironment?) {
        super.init(processingEnvironment)
        mTypeUtil = processingEnvironment?.getTypeUtils()
        mElementUtil = processingEnvironment?.getElementUtils()
        mFiler = processingEnvironment?.getFiler()
        mMessager = processingEnvironment?.getMessager()

    }

    override fun process(
        set: MutableSet<out TypeElement>,
        processingEnvironment: RoundEnvironment
    ): Boolean {


        try {

            for (element in processingEnvironment.getElementsAnnotatedWith(Man::class.java)) {
                parseAnnotation(aptSourceBook, element as TypeElement)
            }


            write()


        } catch (ex: Exception) {

        }
        return true
    }

    private fun write() {


        for ((element, info) in aptSourceBook) {

            val fields = ArrayList<FieldSpec>()
            val methods = ArrayList<MethodSpec>()

            val keyField = FieldSpec.builder(ClassName.get(String::class.java), "mKey")
                .addModifiers(Modifier.PRIVATE).build()

            val nameField = FieldSpec.builder(String::class.java, "name")
                .addModifiers(Modifier.PRIVATE)
                .build()

            val ageField = FieldSpec.builder(Int::class.java, "age")
                .addModifiers(Modifier.PRIVATE)
                .build()

            val countryField = FieldSpec.builder(String::class.java, "country")
                .addModifiers(Modifier.PRIVATE)
                .build()

            val weightField = FieldSpec.builder(Int::class.java, "weight")
                .addModifiers(Modifier.PRIVATE)
                .build()

            val heightField = FieldSpec.builder(Int::class.java, "height")
                .addModifiers(Modifier.PRIVATE)
                .build()


            val algorithmField = FieldSpec.builder(IAlgorithm::class.java, "algorithm")
                .addModifiers(Modifier.PRIVATE)
                .build()


            fields.add(keyField)
            fields.add(nameField)
            fields.add(ageField)
            fields.add(countryField)
            fields.add(weightField)
            fields.add(heightField)
            fields.add(algorithmField)

            val constructor =
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(String::class.java), "key")
                    .addStatement("mKey = key")
                    .addStatement("name = \$S", info.name)
                    .addStatement("age = \$L", info.age)
                    .addStatement("country = new \$T().name()", info.country)
                    .addStatement("algorithm = new \$T()", info.algorithm)


            val body =
                MethodSpec.methodBuilder("body")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)

            info.bodyInfo?.let {
                body.addStatement("weight = \$L", it.weight)
                body.addStatement("height = \$L", it.height)
            }

            val ce =
                MethodSpec.methodBuilder("ce")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.INT)
                    .beginControlFlow("if (algorithm != null)")
                    .addStatement("return algorithm.ce(instance())")
                    .endControlFlow()
                    .addStatement("return weight  + height")

            val getInstance =
                MethodSpec.methodBuilder("instance")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ClassName.get(IFigher::class.java))
                    .addStatement(
                        "return new \$T(String.valueOf(\$T.currentTimeMillis()))",
                        ClassName.bestGuess(element.simpleName.toString() + "$\$Impl"),
                        System::class.java
                    )




            methods.add(constructor.build())
            methods.add(body.build())
            methods.add(ce.build())
            methods.add(getInstance.build())


            val genClass =
                TypeSpec.classBuilder(element.simpleName.toString() + "$\$Impl")
                    .addSuperinterface(ClassName.get(element))
                    .addModifiers(Modifier.PUBLIC)

            for (field in fields) {
                genClass.addField(field)
            }
            for (method in methods) {
                genClass.addMethod(method)
            }

            JavaFile.builder(
                mElementUtil!!.getPackageOf(element).qualifiedName.toString(),
                genClass.build()
            )
                .addFileComment("Generated code")
                .build()
                .writeTo(mFiler)
        }
    }

    private fun parseAnnotation(
        aptSourceBook: java.util.HashMap<TypeElement, AptManInfo>,
        element: TypeElement
    ) {

        val aptManInfo = AptManInfo()
        val annotationInfo = element.getAnnotation(Man::class.java)
        aptManInfo.apply {
            name = annotationInfo.name
            age = annotationInfo.age
            country = getAnnotationClassName(element, Man::class.java, "coutry")?.toString()
                ?.let { ClassName.bestGuess(it) }
        }
        aptSourceBook[element] = aptManInfo

        val methods = mElementUtil!!.getAllMembers(element)
            .filter {
                it.kind == ElementKind.METHOD &&
                        MoreElements.isAnnotationPresent(it, GetInstance::class.java) ||
                        MoreElements.isAnnotationPresent(it, GetCE::class.java) ||
                        MoreElements.isAnnotationPresent(
                            it,
                            Body::class.java
                        )

            }.map { MoreElements.asExecutable(it) }.groupBy {
                when {
                    MoreElements.isAnnotationPresent(it, Body::class.java) -> Body::class.java
                    MoreElements.isAnnotationPresent(
                        it,
                        GetInstance::class.java
                    ) -> GetInstance::class.java
                    MoreElements.isAnnotationPresent(it, GetCE::class.java) -> GetCE::class.java
                    else -> Any::class.java
                }
            }

        methods[Body::class.java]?.forEach {
            val body = it.getAnnotation(Body::class.java)
            aptManInfo.bodyInfo = BodyInfo().apply {
                weight = body.weight
                height = body.height
            }
        }

        methods[GetInstance::class.java]?.forEach {
            val instance = it.getAnnotation(GetInstance::class.java)
            aptManInfo.getInstance = instance
        }


        methods[GetCE::class.java]?.forEach {
            aptManInfo.algorithm =
                getAnnotationClassName(it, GetCE::class.java, "algorithm").toString()
                    .let { ClassName.bestGuess(it) }
        }


    }

    private fun getAnnotationClassName(
        element: Element,
        key1: Class<out Annotation>,
        key: String
    ): Any? {
        return MoreElements.getAnnotationMirror(element, key1)
            .orNull()?.let {
                AnnotationMirrors.getAnnotationValue(it, key)?.value
            }
    }
}