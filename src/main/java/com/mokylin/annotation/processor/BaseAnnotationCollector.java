package com.mokylin.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

public class BaseAnnotationCollector extends AbstractProcessor{

    private final Class<? extends Annotation> annotationToCollect;

    private final String pkg;

    private final String relativeName;

    private final Set<TypeElement> locations;

    protected BaseAnnotationCollector(Class<? extends Annotation> annotation,
            String pkg, String relativeName){
        this.annotationToCollect = annotation;
        this.pkg = pkg;
        this.relativeName = relativeName;
        this.locations = new HashSet<TypeElement>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes(){
        Set<String> result = new HashSet<String>();
        result.add(annotationToCollect.getCanonicalName());
        return result;
    }

    @Override
    public SourceVersion getSupportedSourceVersion(){
        return SourceVersion.RELEASE_7;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv){
        if (roundEnv.processingOver()){
            writeFile();
            return false;
        }

        for (Element element : roundEnv
                .getElementsAnnotatedWith(annotationToCollect)){
            TypeElement typeElement = getEnclosingType(element);
            if (typeElement != null){
                boolean unique = locations.add(typeElement);
                if (unique){
                    try{
                        doProcessType(typeElement);
                    } catch (Exception ex){
                        processingEnv.getMessager().printMessage(Kind.ERROR,
                                "Error processing type: " + ex.getMessage(),
                                typeElement);
                    }
                }
            }
        }

        return false;
    }

    /**
     * hook for subtype to process the TypeElement
     * @param cls
     */
    protected void doProcessType(TypeElement cls) throws Exception{

    }

    private static TypeElement getEnclosingType(Element element){
        ElementKind type = element.getKind();
        if (type == null){
            return null;
        }
        switch (type){
            case CLASS:
                return (TypeElement) element;

            default:{
                return getEnclosingType(element.getEnclosingElement());
            }
        }
    }

    private void writeFile(){
        try{
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                    pkg + "." + relativeName);
            Writer writer = new BufferedWriter(jfo.openWriter());

            StringBuilder sb = new StringBuilder();
            sb.append("package " + pkg + ";\n\n");
            sb.append("import com.mokylin.annotation.processor.SkipObfuscation;\n\n");
            sb.append("@SkipObfuscation\n");
            sb.append("public class " + relativeName + "{\n\n");
            sb.append("    public static final String CONTENT = \"");

            for (TypeElement e : locations){
                sb.append(e.getQualifiedName().toString());
                sb.append("|||");
            }

            sb.append("\";\n\n");
            sb.append("}\n");

            writer.write(sb.toString());
            writer.close();
        } catch (IOException ex){
            processingEnv.getMessager()
                    .printMessage(
                            Kind.ERROR,
                            "Error creating file info for "
                                    + annotationToCollect.getCanonicalName()
                                    + " to " + pkg + "." + relativeName + ": "
                                    + ex.getMessage());
        }
    }
}
