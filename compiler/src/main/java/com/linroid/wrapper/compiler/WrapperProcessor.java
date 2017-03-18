package com.linroid.wrapper.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.linroid.wrapper.annotations.Multiple;
import com.linroid.wrapper.annotations.WrapperClass;
import com.linroid.wrapper.annotations.WrapperGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @author linroid <linroid@gmail.com>
 * @since 10/03/2017
 */
@AutoService(Processor.class)
public class WrapperProcessor extends AbstractProcessor {
    public static final String FIELD_DELEGATE = "_delegate";
    public static final String FIELD_MULTI_DELEGATE = "_delegates";
    public static final String FIELD_HANDLER = "_handler";
    public static final String METHOD_SETTER = "setWrapper";
    public static final String METHOD_GETTER = "getWrapper";
    public static final String METHOD_ADDER = "addWrapper";
    public static final String METHOD_REMOVER = "removeWrapper";

    public static final ClassName HANDLER_CLASS_NAME = ClassName.get("android.os", "Handler");
    public static final ClassName LOOPER_CLASS_NAME = ClassName.get("android.os", "Looper");
    public static final ClassName HASH_SET_CLASS_NAME = ClassName.get(HashSet.class);
    private Messager logger;
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        logger.printMessage(Diagnostic.Kind.NOTE, "start process wrapper annotations...");
        for (Element element : env.getElementsAnnotatedWith(WrapperClass.class)) {
            TypeElement typeElement = (TypeElement) element;
            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                logger.printMessage(Diagnostic.Kind.NOTE, "found: " + element.toString());
                boolean isInterface = element.getKind() == ElementKind.INTERFACE;
                boolean isMultiple = element.getAnnotation(Multiple.class) != null;
                boolean isAllUiThread = isAnnotatedUiThread(element);

                process(typeElement, isInterface, isAllUiThread, false);
                if (isMultiple) {
                    process(typeElement, isInterface, isAllUiThread, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Element generatorElement : env.getElementsAnnotatedWith(WrapperGenerator.class)) {
            if (!SuperficialValidation.validateElement(generatorElement)) continue;
            try {
                if (generatorElement.getKind() == ElementKind.CLASS) {
                    TypeElement typeElement = (TypeElement) generatorElement;
                    WrapperGenerator annotation = typeElement.getAnnotation(WrapperGenerator.class);
                    boolean isAllUiThread = isAnnotatedUiThread(generatorElement);
                    boolean isMultiple = generatorElement.getAnnotation(Multiple.class) != null;

                    try {
                        Class<?>[] values = annotation.values();
                        for (Class<?> clazz : values) {

                        }
                    } catch (MirroredTypesException mte) {
                        List<? extends TypeMirror> types = mte.getTypeMirrors();
                        for (TypeMirror tm : types) {
                            DeclaredType classTypeMirror = (DeclaredType) tm;
                            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                            boolean isInterface = classTypeElement.getKind() == ElementKind.INTERFACE;
                            process(classTypeElement, isInterface, isAllUiThread, false);
                            if (isMultiple) {
                                process(classTypeElement, isInterface, isAllUiThread, true);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void process(TypeElement typeElement, boolean isInterface, boolean isAllUiThread, boolean isMultiple) {
        PackageElement packageElement = elementUtils.getPackageOf(typeElement);
        String packageName = packageElement.getQualifiedName().toString();
        boolean hasUiThread = isAllUiThread;
        TypeName delegateType = TypeName.get(typeElement.asType());
        TypeSpec.Builder typeBuilder = createNewWrapper(typeElement, delegateType, isInterface, isMultiple);
        for (Element e : typeElement.getEnclosedElements()) { // iterate over children
            if (e.getKind() == ElementKind.METHOD) {
                boolean isMethodUiThread = isAllUiThread;
                ExecutableElement methodElement = (ExecutableElement) e;
                if (!isMethodUiThread) {
                    isMethodUiThread = isAnnotatedUiThread(methodElement);
                } else {
                    // TODO: 10/03/2017 是否在Method 和 Type 都注解了 UiThread 的时候 抛出异常?
                }
                if (isMethodUiThread) {
                    hasUiThread = true;
                }
                typeBuilder.addMethod(createDelegateMethod(typeElement, methodElement, isInterface, isMethodUiThread, isMultiple));
            }
        }
        if (hasUiThread) {
            typeBuilder.addField(HANDLER_CLASS_NAME, FIELD_HANDLER, Modifier.PRIVATE);
        }
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        if (!isMultiple) {
            constructorBuilder.addParameter(delegateType, FIELD_DELEGATE)
                    .addStatement("this.$N = $N", FIELD_DELEGATE, FIELD_DELEGATE);
        }
        if (hasUiThread) {
            constructorBuilder.addStatement("this.$N = new $T($T.getMainLooper())", FIELD_HANDLER, HANDLER_CLASS_NAME, LOOPER_CLASS_NAME);
        }
        typeBuilder.addMethod(constructorBuilder.build());
        if (hasUiThread) {
            MethodSpec.Builder secondConstructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(HANDLER_CLASS_NAME, FIELD_HANDLER)
                    .addStatement("this.$N = $N", FIELD_HANDLER, FIELD_HANDLER);
            if (!isMultiple) {
                secondConstructorBuilder.addParameter(delegateType, FIELD_DELEGATE)
                        .addStatement("this.$N = $N", FIELD_DELEGATE, FIELD_DELEGATE);
            }
            typeBuilder.addMethod(secondConstructorBuilder.build());
        }
        if (hasUiThread) {
            constructorBuilder.addStatement("this.$N = new $T($T.getMainLooper())", FIELD_HANDLER, HANDLER_CLASS_NAME, LOOPER_CLASS_NAME);
        }

        JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build())
                .build();
        try {
            javaFile.writeTo(System.out);
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private MethodSpec createDelegateMethod(TypeElement element, ExecutableElement methodElement, boolean isInterface, boolean isUiThread, boolean isMultiple) {
        String methodName = methodElement.getSimpleName().toString();
        MethodSpec.Builder methodBuilder;
        methodBuilder = MethodSpec.methodBuilder(methodName);
        if (isInterface) {
            methodBuilder.addAnnotation(Override.class);
        }
        boolean hasReturnType = methodElement.getReturnType().getKind() != TypeKind.VOID;
//        if (isUiThread && hasReturnType) {
//            throw new IllegalArgumentException(methodName + " with annotated UiThread shouldn't has a return type");
//        }

        if (hasReturnType) {
            methodBuilder.returns(TypeName.get(methodElement.getReturnType()));
        }
        if (isInterface) {
            methodBuilder.addModifiers(Modifier.PUBLIC);
        }
        List<? extends VariableElement> params = methodElement.getParameters();
        List<String> argNames = new ArrayList<>(params.size());
        for (VariableElement param : params) {
            methodBuilder.addParameter(TypeName.get(param.asType()), param.getSimpleName().toString(), Modifier.FINAL);
            argNames.add(param.getSimpleName().toString());
        }
        if (isUiThread) {
            MethodSpec.Builder runMethodBuilder = MethodSpec.methodBuilder("run")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC);
            invokeMethod(element, runMethodBuilder, methodName, argNames, isMultiple, isUiThread, hasReturnType);
            TypeSpec runnable = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(Runnable.class)
                    .addMethod(runMethodBuilder.build())
                    .build();
            methodBuilder.beginControlFlow("if($T.myLooper() == null || $T.myLooper() != $T.getMainLooper())", LOOPER_CLASS_NAME, LOOPER_CLASS_NAME, LOOPER_CLASS_NAME);
            methodBuilder.addStatement("$N.post($L)", FIELD_HANDLER, runnable);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
        }
        invokeMethod(element, methodBuilder, methodName, argNames, isMultiple, isUiThread, hasReturnType);
        if (isUiThread) {
            methodBuilder.endControlFlow();
        }

        if (hasReturnType) {
            methodBuilder.addStatement("return $N", defaultValue(methodElement.getReturnType()));
        }
        for (TypeMirror thrownType : methodElement.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }
        return methodBuilder.build();
    }

    private static void invokeMethod(TypeElement element, MethodSpec.Builder methodBuilder, String methodName, List<String> argNames, boolean isMultiple, boolean isUiThread, boolean hasReturnType) {
        if (isMultiple) {
            methodBuilder.beginControlFlow("for(final $T $N : $N)", TypeName.get(element.asType()), FIELD_DELEGATE, FIELD_MULTI_DELEGATE);
        }
        methodBuilder.beginControlFlow("if($N != null)", FIELD_DELEGATE);
        methodBuilder.addStatement("$N $N.$N($N)", (hasReturnType && !isMultiple && !isUiThread) ? "return" : "", FIELD_DELEGATE, methodName, Utils.implode(", ", argNames));
        methodBuilder.endControlFlow();
        if (isMultiple) {
            methodBuilder.endControlFlow();
        }
    }

    private boolean isAnnotatedUiThread(Element element) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if ("android.support.annotation.UiThread".equals(mirror.getAnnotationType().toString())) {
                return true;
            }
        }
        return false;
    }

    private String defaultValue(TypeMirror type) {
        final TypeKind kind = type.getKind();
        switch (kind) {
            case BOOLEAN:
                return "false";
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return "0";
        }
        return "null";
    }

    private TypeSpec.Builder createNewWrapper(TypeElement element, TypeName delegateType, boolean isInterface, boolean isMultiple) {

        String wrapperClassName;
        if (isMultiple) {
            wrapperClassName = element.getSimpleName().toString() + "MultiWrapper";
        } else {
            wrapperClassName = element.getSimpleName().toString() + "Wrapper";
        }


        TypeSpec.Builder builder = TypeSpec.classBuilder(wrapperClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (isInterface) {
            builder.addSuperinterface(delegateType);
        }
        if (isMultiple) {
            MethodSpec adder = MethodSpec.methodBuilder(METHOD_ADDER)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(delegateType, FIELD_DELEGATE)
                    .addStatement("this.$N.add($N)", FIELD_MULTI_DELEGATE, FIELD_DELEGATE)
                    .build();
            MethodSpec remover = MethodSpec.methodBuilder(METHOD_REMOVER)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(delegateType, FIELD_DELEGATE)
                    .addStatement("this.$N.remove($N)", FIELD_MULTI_DELEGATE, FIELD_DELEGATE)
                    .build();
            TypeName hashSetType = ParameterizedTypeName.get(HASH_SET_CLASS_NAME, delegateType);
            FieldSpec holders = FieldSpec.builder(hashSetType, FIELD_MULTI_DELEGATE, Modifier.PRIVATE)
                    .initializer("new $T<>()", HASH_SET_CLASS_NAME)
                    .build();
            builder.addMethod(adder)
                    .addMethod(remover)
                    .addField(holders);
        } else {
            MethodSpec setter = MethodSpec.methodBuilder(METHOD_SETTER)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(delegateType, FIELD_DELEGATE)
                    .addStatement("this.$N = $N", FIELD_DELEGATE, FIELD_DELEGATE)
                    .build();
            MethodSpec getter = MethodSpec.methodBuilder(METHOD_GETTER)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(delegateType)
                    .addStatement("return this.$N", FIELD_DELEGATE)
                    .build();

            builder.addMethod(setter)
                    .addMethod(getter)
                    .addField(delegateType, FIELD_DELEGATE, Modifier.PRIVATE);
        }
        return builder;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();

        annotations.add(WrapperClass.class);
        annotations.add(WrapperGenerator.class);

        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
