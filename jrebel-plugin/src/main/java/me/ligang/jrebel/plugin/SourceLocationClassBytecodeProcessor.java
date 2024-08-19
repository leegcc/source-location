package me.ligang.jrebel.plugin;

import org.zeroturnaround.bundled.javassist.ClassPool;
import org.zeroturnaround.bundled.javassist.CtClass;
import org.zeroturnaround.bundled.javassist.CtMethod;
import org.zeroturnaround.javarebel.Logger;
import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.integration.support.JavassistClassBytecodeProcessor;

import java.net.URL;

public class SourceLocationClassBytecodeProcessor extends JavassistClassBytecodeProcessor {

    private final Logger logger = LoggerFactory.getInstance();

    private static final String[] MAPPING_ANNOTATIONS = {
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping"
    };

    @Override
    public void process(ClassPool cp, ClassLoader cl, CtClass ctClass) throws Exception {
        if (!isSpringController(ctClass)) {
            return;
        }

        String sourceFilePath = getSourceFilePath(cl, ctClass);
        if (sourceFilePath == null) {
            return;
        }

        CtClass httpServletResponseClass = getHttpServletResponseClass(cp);
        if (httpServletResponseClass == null) {
            System.err.println("Neither javax.servlet.http.HttpServletResponse nor jakarta.servlet.http.HttpServletResponse found.");
            return;
        }

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            if (hasMappingAnnotation(ctMethod)) {
                int startLine = ctMethod.getMethodInfo().getLineNumber(0);
                String codeToInsert = buildCodeToInsert(httpServletResponseClass, sourceFilePath, ctClass.getName(), ctMethod.getName(), startLine);
                ctMethod.insertBefore(codeToInsert);
            }
        }
    }

    private boolean isSpringController(CtClass ctClass) throws Exception {
        return ctClass.hasAnnotation("org.springframework.web.bind.annotation.RestController")
                || ctClass.hasAnnotation("org.springframework.web.bind.annotation.Controller");
    }

    private boolean hasMappingAnnotation(CtMethod ctMethod) throws Exception {
        for (String annotation : MAPPING_ANNOTATIONS) {
            if (ctMethod.hasAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }

    private String getSourceFilePath(ClassLoader cl, CtClass ctClass) throws Exception {
        URL classUrl = cl.getResource(ctClass.getName().replace('.', '/') + ".class");
        if (classUrl == null) {
            return null;
        }
        String classPath = classUrl.getPath();
        int index = classPath.indexOf("/target/classes/");
        if (index == -1) {
            return null;
        }
        String projectRoot = classPath.substring(0, index);
        return projectRoot.replaceAll("^/", "") + "/src/main/java/" + ctClass.getName().replace('.', '/') + ".java";
    }

    private CtClass getHttpServletResponseClass(ClassPool cp) throws Exception {
        CtClass responseClass = cp.getOrNull("javax.servlet.http.HttpServletResponse");
        return responseClass != null ? responseClass : cp.getOrNull("jakarta.servlet.http.HttpServletResponse");
    }

    private String buildCodeToInsert(CtClass httpServletResponseClass, String sourceFilePath, String className, String methodName, int startLine) {
        return "{ " +
                httpServletResponseClass.getName() + " response = " +
                "((org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getResponse(); " +
                "response.addHeader(\"X-Source-Path\", \"" + sourceFilePath + "\"); " +
                "response.addHeader(\"X-Source-Class\", \"" + className + "\"); " +
                "response.addHeader(\"X-Source-Method\", \"" + methodName + "\"); " +
                "response.addHeader(\"X-Source-Line\", String.valueOf(" + startLine + ")); " +
                "}";
    }
}
