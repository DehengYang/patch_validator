/**
 * apr
 * Aug 23, 2021
 */
package apr.junit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author apr
 * Aug 23, 2021
 */
public class MainCompile {
    public static void main(String[] args) {
        Map<String, String> argsMap = parseCommandLine(args);
        PatchCompile pc = new PatchCompile(argsMap.get("logFilePath"), 
                Arrays.asList(argsMap.get("dependencies").split(":")), argsMap.get("jvmPath"), 
                argsMap.get("outputDirPath"));
        Boolean result = pc.compilePatchFile(argsMap.get("tmpPatchFile"));
        System.out.println("[compilation result] "+ result);
    }
    
    
    private static Map<String, String> parseCommandLine(String[] args) {
        Map<String, String> argsMap = new HashMap<>();

        /*
         * String logFilePath, List<String> dependencies, String jvmPath, String outputDirPath
         */
        
        Options options = new Options();
        options.addRequiredOption("logFilePath", "logFilePath", true,
                "log file path");
        options.addRequiredOption("dependencies", "dependencies", true,
                "dependencies string");
        options.addRequiredOption("outputDirPath", "outputDirPath", true,
                "dir to save compiled sources.");
        options.addRequiredOption("jvmPath", "jvmPath", true,
                "jvmPath");
        options.addRequiredOption("tmpPatchFile", "tmpPatchFile", true,
                "tmpPatchFile");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage() + "\n");
            formatter.printHelp(">>>>>>>>>> patch compilation: \n\n", options);

            System.exit(1);
        }

        argsMap.put("logFilePath", cmd.getOptionValue("logFilePath"));
        argsMap.put("dependencies", cmd.getOptionValue("dependencies"));
        argsMap.put("outputDirPath", cmd.getOptionValue("outputDirPath"));
        argsMap.put("jvmPath", cmd.getOptionValue("jvmPath"));
        argsMap.put("tmpPatchFile", cmd.getOptionValue("tmpPatchFile"));
        
        return argsMap;
    }
}
