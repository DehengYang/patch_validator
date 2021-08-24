package apr.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;

import apr.junit.utils.FileUtil;

public class PatchCompile {
    List<String> compilerOpts = new ArrayList<>();
    private String jvmPath;
    private String compileLevel;
    private List<String> deps;
    private String outputDirPath;
    private String logFilePath;

    /**
     * 
     * @param dependencies
     * @param jvmPath
     * @param outputDirPath: this is a folder path, not a file path.
     */
    public PatchCompile(String logFilePath, List<String> dependencies, String jvmPath, String outputDirPath) {
        this.deps = dependencies;
        this.jvmPath = jvmPath;
        if (this.jvmPath.contains("jdk1.8")) {
            compileLevel = "1.8";
        } else {
            compileLevel = "1.7";
        }
        this.outputDirPath = outputDirPath;
        this.logFilePath = logFilePath;
        
        init();
    }

    public void init() {
        // init compiler options
        compilerOpts.add("-nowarn");
        compilerOpts.add("-source");
        compilerOpts.add(compileLevel);
        // Refer to: Nopol DynamicClassCompiler.java
        // options = asList("-nowarn", "-source", "1." + compliance, "-target", "1." + compliance);
        
        compilerOpts.add("-target");
        compilerOpts.add(compileLevel);
        compilerOpts.add("-cp");
        
        String depStr = "";
        for (String dep : deps) {
            depStr += File.pathSeparator + dep;
        }
        compilerOpts.add(depStr);
        
        setOutputPath(this.outputDirPath);
    }

    /**
     * @Description outputPath is a folder path, not a file path.
     * @author apr
     * @version Apr 20, 2020
     *
     * @param outputPath
     */
    public void setOutputPath(String outputPath) {
        File dir = new File(outputPath);
        // refer to: https://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(new File(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }

        compilerOpts.add("-d");
        compilerOpts.add(outputPath);
    }

    public Boolean compilePatchFile(String filePath) {
        // init compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "Cannot find the system Java compiler. Please check that your class path includes tools.jar");
        }

        // config
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, null, null);

        // get file object(s)
        final File file = new File(filePath);
        final Iterable<? extends JavaFileObject> compilationUnits = manager
                .getJavaFileObjectsFromFiles(Arrays.asList(file));

        // compile now
        final CompilationTask task = compiler.getTask(null, manager, diagnostics, compilerOpts, null,
                compilationUnits);
        Boolean result = task.call();

        // errors
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            // System.out.format("%s\nline %d in %s",
            // diagnostic.getMessage( null ),
            // diagnostic.getLineNumber(),
            // diagnostic.getSource().getName() );
            String error = String.format("%s (at line %d)\n",
                    diagnostic.getMessage(null),
                    diagnostic.getLineNumber());
//            System.err.println(error);
            
            FileUtil.writeToFile(logFilePath, error);
        }

        if (result == null || !result) {
            // throw new RuntimeException("Compilation failed. File path: " + filePath);
            System.out.println("Compilation failed. File path: " + filePath);
        } else {
            System.out.println("Compilation passed. File path: " + filePath);
        }

        // close manager
        try {
            manager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

        // final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // final DiagnosticCollector< JavaFileObject > diagnostics = new DiagnosticCollector<>();
        // final StandardJavaFileManager manager = compiler.getStandardFileManager(
        // diagnostics, null, null );
        //
        // final EmptyTryBlockScanner scanner = new EmptyTryBlockScanner();
        // final EmptyTryBlockProcessor processor = new EmptyTryBlockProcessor( scanner );
        // final Iterable<? extends JavaFileObject> sources = manager.getJavaFileObjectsFromFiles(
        // Arrays.asList(
        // new File(CompilerExample.class.getResource("/SampleClassToParse.java").toURI()),
        // new File(CompilerExample.class.getResource("/SampleClass.java").toURI())
        // )
        // );
        //
        // final CompilationTask task = compiler.getTask( null, manager, diagnostics,
        // null, null, sources );
        // task.setProcessors( Arrays.asList( processor ) );
        // task.call();
    }
}
