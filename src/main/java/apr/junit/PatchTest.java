package apr.junit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore.Entry;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/*
 * This is to run junit tests of buggy program. This external java file can be 
 * invoked via java -cp <classpath> (i.e., specify all dependencies), therefore it's more stable
 * than internel tests which seems more difficult to specify these deps. 
 */
public class PatchTest{
	private static boolean printTrace = true;
	private static List<String> testsToRun;
	private static List<String> failedTestMethods = new ArrayList<>();
	
	public static void main(String args[]){
		// get all tests
		Map<String, String> parameters = setParameters(args);
//		if (parameters.size() != 1){
//			for (Map.Entry<String, String> entry : parameters.entrySet()){
//				System.out.format("paras: %s %s\n", entry.getKey(), entry.getValue());
//			}
//			System.err.println("The paramter should only contain testFile or testStr");
//		}else 
		if (parameters.containsKey("testFile")){
			String filePath = parameters.get("testFile");
			testsToRun = readFile(filePath);
		}else if (parameters.containsKey("testStr")){
			String testStr = parameters.get("testStr");
			testsToRun = Arrays.asList(testStr.trim().split(File.pathSeparator));
		}
		
		if (parameters.containsKey("runTestMethods") && parameters.get("runTestMethods").equals("true")){
			runTestMethods(testsToRun);
		}else{
			runTests(testsToRun);
		}
		
		// save failed methods
		if (parameters.containsKey("savePath")){
			saveFailedMethods(parameters.get("savePath"));
		}
		
		System.exit(0);
	}
	
	/** @Description 
	 * @author apr
	 * @version Apr 5, 2020
	 *
	 */
	private static void saveFailedMethods(String savePath) {
		writeLinesToFile(savePath, failedTestMethods, false);
	}
	
	/**
	 * @Description  copy from my APR project
	 * @author apr
	 * @version Apr 5, 2020
	 *
	 * @param path
	 * @param lines
	 * @param append
	 */
	public static void writeLinesToFile(String path, List<String> lines, boolean append){
		// get dir
		String dirPath = path.substring(0, path.lastIndexOf("/"));
		File dir = new File(dirPath);
		if (!dir.exists()){
			dir.mkdirs();
			System.out.println(String.format("%s does not exists, and are created now via mkdirs()", dirPath));
		}
		
		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new FileWriter(path, append));
			for(String line : lines){
				output.write(line + "\n");
			}
//			output.write(content);
			output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
	}

	/*
	 * run tests with method name
	 */
	public static void runTestMethods(List<String> testMethods){
//		List<String> failedTestMethods = new ArrayList<>();
		
		System.out.format("test methods size for execution: %d\n", testMethods.size());
		long startT = System.currentTimeMillis();
		
		// debug usage
//		Collections.reverse(testMethods);
		// face a weird bug: 
		// /home/apr/env/jdk1.7.0_80/bin/java -cp /home/apr/d4j/Chart/Chart_17/build/:/home/apr/d4j/Chart/Chart_17/build-tests/:/home/apr/d4j/Chart/Chart_17/lib/servlet.jar:/home/apr/d4j/Chart/Chart_17/lib/itext-2.0.6.jar:/home/apr/apr_tools/tbar-ori/TBar-dale/externel/target/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar apr.junit.PatchTest -testFile /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Chart_17/Dale_APR/FL/test_methods.txt -runTestMethods true
		// extra failed test methods.
		
		for (String testMethod : testMethods){
			String className = testMethod.split("#")[0];
			String methodName = testMethod.split("#")[1];
			try {
//				long startT = System.currentTimeMillis();
				Request request = Request.method(Class.forName(className), methodName);
				Result result = new JUnitCore().run(request);
				
				if (!result.wasSuccessful()){
					failedTestMethods.add(className + "#" + methodName);
					if (printTrace){
						for (Failure failure : result.getFailures()){
							System.out.format("failed trace info: %s\n", failure.getTrace());
							System.out.format("failed trace description: %s\n", failure.getDescription());
							// testIssue820(com.google.javascript.jscomp.CollapseVariableDeclarationsTest)
//							String failedTestClassName = failure.getDescription().toString().trim().split("\\(")[1].split("\\)")[0];
//							String failedTestMethodName = failure.getDescription().toString().trim().split("\\(")[0];
//							failedTestMethods.add(failedTestClassName + "#" + failedTestMethodName);
						}
					}
					DecimalFormat dF = new DecimalFormat("0.0000");
					System.out.format("failed method execution time cost: %s\n", dF.format((float) result.getRunTime()/1000));
				}
				
//				DecimalFormat dF = new DecimalFormat("0.0000");
//				System.out.format("number of executed tests: %d, time cost: %s\n", result.getRunCount(), dF.format((float) result.getRunTime()/1000));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.format("[Patch test] failed test methods size after execution: %d\n", failedTestMethods.size());
		for (String failed : failedTestMethods){
			System.out.format("[Patch test] failed test method: %s\n", failed);
		}
		
		DecimalFormat dF = new DecimalFormat("0.0000");
		System.out.format("Total time cost for run the test method(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT)/1000));
	}
	
	/*
	 * run tests without # (method name)
	 */
	public static void runTests(List<String> tests){
		List<String> failedTests = new ArrayList<>();
		List<String> failedTestMethods = new ArrayList<>();
		
		System.out.format("tests size for execution: %d\n", tests.size());
		long startT = System.currentTimeMillis();
		
		for (String test : tests){
			String className = test;
			
			try {
				Request request = Request.aClass(Class.forName(className));
				Result result = new JUnitCore().run(request);
				
				if (!result.wasSuccessful()){
					failedTests.add(test);
					if (printTrace){
						for (Failure failure : result.getFailures()){
							System.out.format("failed trace info: %s\n", failure.getTrace());
							System.out.format("failed trace description: %s\n", failure.getDescription());
							// testIssue820(com.google.javascript.jscomp.CollapseVariableDeclarationsTest)
							String failedTestClassName = failure.getDescription().toString().trim().split("\\(")[1].split("\\)")[0];
							String failedTestMethodName = failure.getDescription().toString().trim().split("\\(")[0];
							failedTestMethods.add(failedTestClassName + "#" + failedTestMethodName);
						}
					}
				}
				
				DecimalFormat dF = new DecimalFormat("0.0000");
				System.out.format("number of executed tests: %d, time cost: %s\n", result.getRunCount(), dF.format((float) result.getRunTime()/1000));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.format("[Patch test] failed tests size after execution: %d\n", failedTests.size());
		for (String failed : failedTests){
			System.out.format("[Patch test] failed test: %s\n", failed);
		}
		System.out.format("[Patch test] failed test methods size after execution: %d\n", failedTestMethods.size());
		for (String failed : failedTestMethods){
			System.out.format("[Patch test] failed test method: %s\n", failed);
		}
		
		DecimalFormat dF = new DecimalFormat("0.0000");
		System.out.format("Total time cost for run the test(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT)/1000));
	}
	
	
	/*
	 * run test cases (with "#") one by one
	 * not used now ...
	 */
//	public static void runTestCases(List<String> tests){
//		List<String> failedTests = new ArrayList<>();
//		
//		System.out.format("tests size for execution: %d\n", tests.size());
//		
//		for (String test : tests){
//			String className = test.split("#")[0];
//			String methodName = test.split("#")[1];
//			
//			try {
//				Request request = Request.method(Class.forName(className), methodName);
//				Result result = new JUnitCore().run(request);
//				
//				if (!result.wasSuccessful()){
//					failedTests.add(test);
//					
//					if (printTrace){
//						for (Failure failure : result.getFailures()){
//							System.out.format("failed trace info: %s\n", failure.getTrace());
////							System.out.format("failed trace description: %s\n", failure.getDescription());
//						}
//					}
//				}
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		System.out.format("failed tests size after execution: %d\n", failedTests.size());
//		for (String failed : failedTests){
//			System.out.format("failed test: %s\n", failed);
//		}
//	}
	
	/*
	 * read file
	 */
	private static List<String> readFile(String path){
		List<String> list = new ArrayList<>();
		try {
            final BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
            	if (line.length() == 0) System.err.println(String.format("Empty line in %s", path));
            	list.add(line); // add line
            }
            in.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return list;
	}
	
	/*
	 * receive parameters
	 */
	private static Map<String, String>  setParameters(String[] args) {		
		Map<String, String> parameters = new HashMap<>();
		
        Option opt1 = new Option("testFile","testFile",true,"a file containing tests like org.apache.commons.math3.stat.StatUtilsTest#testSumLog (test methods or tests)");
        opt1.setRequired(false);
        Option opt2 = new Option("testStr","testStr",true,"some tests linked with : (test methods or tests)");
        opt2.setRequired(false);   
        Option opt3 = new Option("runTestMethods","runTestMethods",true,"true if run test methods");
        opt3.setRequired(false);  
        Option opt4 = new Option("savePath","savePath",true,"save failed test methods");
        opt4.setRequired(false); 
        
        Options options = new Options();
        options.addOption(opt1);
        options.addOption(opt2);
        options.addOption(opt3);
        options.addOption(opt4);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            helpFormatter.printHelp(">>>>>> options", options);
            e.printStackTrace();
        } 

        if (cli.hasOption("testFile")){
        	parameters.put("testFile", cli.getOptionValue("testFile"));
        }
        if(cli.hasOption("testStr")){
        	parameters.put("testStr", cli.getOptionValue("testStr"));
        }
        if(cli.hasOption("runTestMethods")){
        	parameters.put("runTestMethods", cli.getOptionValue("runTestMethods"));
        }
        if(cli.hasOption("savePath")){
        	parameters.put("savePath", cli.getOptionValue("savePath"));
        }
		return parameters;
    }
}