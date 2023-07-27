package com.assia.metrics.domain.service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class ExternalClassLoader {

    public static boolean compileJavaFile(String filePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int compilationResult = compiler.run(null, null, null, filePath);

        return compilationResult == 0;
    }

    public static Class<?> loadExternalClass(String filePath, String className) {
        try {
            File file = new File(filePath);
            URL url = file.toURI().toURL();

            URLClassLoader classLoader = new URLClassLoader(new URL[]{url});



            Class<?> externalClass = classLoader.loadClass(className);
            return externalClass;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
