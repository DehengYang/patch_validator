/**
 * apr
 * Dec 25, 2021
 */
package apr.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author apr
 * Dec 25, 2021
 */
public class TestMainCompile {
    /**
     * parse main args
     * date: Dec 2, 2021
     * @param mainArgs
     * @return
     */
    public static String[] parseStringArgs(String mainArgs) {
        String[] args = mainArgs.split("\\\n");
        List<String> argsList = new ArrayList<>();
        for (String arg : args) {
            arg = arg.trim();
            if (arg.endsWith("\\")) {
                arg = arg.substring(0, arg.length() - 1);
            }
            String[] argArray = arg.split("\\s+");
            argsList.addAll(Arrays.asList(argArray));
        }

        String[] realArgs = new String[argsList.size()];
        int i = 0;
        for (String arg : argsList) {
            realArgs[i] = arg;
            i++;
        }
        return realArgs;
    }

    @Test
    public void testChart6() {
        String mainArgs = "--logFilePath /mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/mcrepair/patch/patch_0/compile.txt --dependencies /mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/defects4j_Chart_6/defects4j_Chart_6/build/:/mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/defects4j_Chart_6/defects4j_Chart_6/build-tests/:/mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/defects4j_Chart_6/defects4j_Chart_6/lib/junit.jar:/mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/defects4j_Chart_6/defects4j_Chart_6/lib/servlet.jar:/mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/defects4j_Chart_6/defects4j_Chart_6/lib/itext-2.0.6.jar:/mnt/data/2021_11_multi_chunk_repair/APRConfig/datasets/defects4j/framework/projects/lib/junit-4.11.jar --outputDirPath /mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/mcrepair/patch/patch_0 --jvmPath /home/apr/env/jdk1.7.0_80/bin --tmpPatchFile /mnt/data/2021_11_multi_chunk_repair/APRConfig/results_defects4j/defects4j_Chart_6/mcrepair/patch/patch_0/candidatePatch.java";

        String[] args = parseStringArgs(mainArgs);
        MainCompile.main(args);
    }
}
