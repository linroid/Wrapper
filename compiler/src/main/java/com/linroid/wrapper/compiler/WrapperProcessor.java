package com.linroid.wrapper.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.linroid.wrapper.annotations.WrapperClass;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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
    public static final String FIELD_HANDLER = "_handler";
    public static final String METHOD_SETTER = "set";
    public static final String METHOD_GETTER = "get";
    public static final String HANDLER_CLASS_NAME = "android.os.Handler";
    public static final String LOOPER_CLASS_NAME = "android.os.Looper";
    private Messager logger;
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private TypeMirror handlerType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        handlerType = elementUtils.getTypeElement(HANDLER_CLASS_NAME).asType();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        System.out.println("start process...");
        for (Element element : env.getElementsAnnotatedWith(WrapperClass.class)) {
            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                System.out.println("found: " + element.toString());
                if (element.getKind() == ElementKind.INTERFACE) {
                    processInterface(element);
                } else {
                    processClass(element);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void processClass(Element element) {
        logger.printMessage(Diagnostic.Kind.NOTE, "a class: " + element.getSimpleName());
    }

    private void processInterface(Element element) {
        TypeElement typeElement = (TypeElement) element;
        String wrapperClassName = typeElement.getSimpleName().toString() + "Wrapper";
        String qualifyName = typeElement.getQualifiedName().toString();
        String packageName = qualifyName.substring(0, qualifyName.lastIndexOf('.'));
        boolean isAllUiThread = isAnnotatedUiThread(typeElement);
        boolean hasUiThread = isAllUiThread;
        TypeName delegateType = TypeName.get(typeElement.asType());
        logger.printMessage(Diagnostic.Kind.NOTE, "a interface: " + typeElement.getSimpleName());
        TypeSpec.Builder typeBuilder = createNewWrapper(wrapperClassName, delegateType);
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
                typeBuilder.addMethod(createDelegateMethod(methodElement, true, isMethodUiThread));
            }
        }
        if (hasUiThread) {
            typeBuilder.addField(TypeName.get(handlerType), FIELD_HANDLER, Modifier.PRIVATE);
        }
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(delegateType, FIELD_DELEGATE)
                .addStatement("this.$N = $N", FIELD_DELEGATE, FIELD_DELEGATE);
        if (hasUiThread) {
            constructorBuilder.addStatement("this.$N = new $N($N.getMainLooper())", FIELD_HANDLER, HANDLER_CLASS_NAME, LOOPER_CLASS_NAME);
        }
        typeBuilder.addMethod(constructorBuilder.build());
        if (hasUiThread) {
            MethodSpec.Builder secondConstructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(delegateType, FIELD_DELEGATE)
                    .addParameter(TypeName.get(handlerType), FIELD_HANDLER)
                    .addStatement("this.$N = $N", FIELD_DELEGATE, FIELD_DELEGATE)
                    .addStatement("this.$N = $N", FIELD_HANDLER, FIELD_HANDLER);
            typeBuilder.addMethod(secondConstructorBuilder.build());
        }
        if (hasUiThread) {
            constructorBuilder.addStatement("this.$N = new $N($N.getMainLooper())", FIELD_HANDLER, HANDLER_CLASS_NAME, LOOPER_CLASS_NAME);
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

    private MethodSpec createDelegateMethod(ExecutableElement methodElement, boolean isInterface, boolean isUiThread) {
        String methodName = methodElement.getSimpleName().toString();
        MethodSpec.Builder methodBuilder;
        methodBuilder = MethodSpec.methodBuilder(methodName);
        if (isInterface) {
            methodBuilder.addAnnotation(Override.class);
        }
        boolean hasReturnType = methodElement.getReturnType().getKind() != TypeKind.VOID;
        if (isUiThread && hasReturnType) {
            throw new IllegalArgumentException(methodName + " with annotated UiThread shouldn't has a return type");
        }

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
        methodBuilder.beginControlFlow("if(this.$N != null)", FIELD_DELEGATE);
        if (isUiThread) {
            methodBuilder.beginControlFlow("if($N.myLooper() == null || $N.myLooper() != $N.getMainLooper())", LOOPER_CLASS_NAME, LOOPER_CLASS_NAME, LOOPER_CLASS_NAME);
            methodBuilder.addCode("$N.post(new Runnable(){\n\t@Override public void run() {\n", FIELD_HANDLER);
            methodBuilder.beginControlFlow("if($N != null)", FIELD_DELEGATE);
        }
        methodBuilder.addStatement("$N $N.$N($N)", hasReturnType ? "return" : "", FIELD_DELEGATE, methodName, Utils.implode(", ", argNames));
        if (isUiThread) {
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.addCode("\n});\n}");
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("$N this.$N.$N($N)", hasReturnType ? "return" : "", FIELD_DELEGATE, methodName, Utils.implode(", ", argNames));
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
        if (hasReturnType) {
            methodBuilder.addStatement("return $N", defaultValue(methodElement.getReturnType()));
        }
        return methodBuilder.build();
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

    private TypeSpec.Builder createNewWrapper(String wrapperClassName, TypeName delegateType) {
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

        return TypeSpec.classBuilder(wrapperClassName)
                .addSuperinterface(delegateType)
                .addMethod(setter)
                .addMethod(getter)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(delegateType, FIELD_DELEGATE, Modifier.PRIVATE);
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

        return annotations;
    }

}
