package apr.junit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

import apr.junit.utils.TimeOut;

/*
 * This is to run junit tests of buggy program. This external java file can be 
 * invoked via java -cp <classpath> (i.e., specify all dependencies), therefore it's more stable
 * than internel tests which seems more difficult to specify these deps. 
 */
public class PatchTest{
	private static boolean printTrace = true;
	private static List<String> testsToRun;
	private static List<String> failedTestMethods = new ArrayList<>();
	
	private static Result result;
	private static int testCnt = 0;
	
	private static Map<String, String> parameters = new HashMap<>();
	
	public static void main(final String args[]){
		int timeout = 30;//minutes for main

		final long startTime = System.currentTimeMillis();
		try {
			TimeOut.runWithTimeout(new Runnable() {
				@Override
				public void run() {
					mainTask(args);
				}
			}, timeout, TimeUnit.MINUTES);
		} catch (Exception e1) {
			failedTestMethods.add(String.format("test execution timeout! (%s min)", timeout));
			e1.printStackTrace();
		}catch (Error er) {
			failedTestMethods.add(String.format("test execution timeout! (%s min)", timeout));
			er.printStackTrace();
		} finally{
			System.out.format("total test size: %s, executed test size: %s\n", testsToRun.size(), testCnt);
			// save failed methods
			if (parameters.containsKey("savePath")){
				String savePath = parameters.get("savePath");
				saveFailedMethods(savePath);
			}
		}

		System.out.format("time used: %s\n", countTime(startTime));

		System.exit(0);
	}
	
	public static void mainTask(String[] args){
		// get all tests
//		Map<String, String> parameters = setParameters(args);
		setParameters(args);
		
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
	}
	
	/**
	 * @Description borrowed from my-apr framework 
	 * @author apr
	 * @version Apr 21, 2020
	 *
	 * @param startTime
	 * @return
	 */
	public static String countTime(long startTime){
		DecimalFormat dF = new DecimalFormat("0.0000");
		return dF.format((float) (System.currentTimeMillis() - startTime)/1000);
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
			output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
	}
	
	public static void writeLinesToFile(String path, String line, boolean append){
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
			output.write(line + "\n");
//			output.write(content);
			output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
	}

	/*
	 * run tests with method name, and stops once a test case fails.
	 */
	public static void runTestMethods(List<String> testMethods){
		System.out.format("test methods size for execution: %d\n", testMethods.size());
		long startT = System.currentTimeMillis();
		
		// debug usage
//		Collections.reverse(testMethods);
		// face a weird bug: 
		// /home/apr/env/jdk1.7.0_80/bin/java -cp /home/apr/d4j/Chart/Chart_17/build/:/home/apr/d4j/Chart/Chart_17/build-tests/:/home/apr/d4j/Chart/Chart_17/lib/servlet.jar:/home/apr/d4j/Chart/Chart_17/lib/itext-2.0.6.jar:/home/apr/apr_tools/tbar-ori/TBar-dale/externel/target/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar apr.junit.PatchTest -testFile /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Chart_17/Dale_APR/FL/test_methods.txt -runTestMethods true
		// extra failed test methods.
		// I tried a lot of attempts, and found that when the positions of failed methods are changed, they passed.
		// 1) change order -> passed
		// 2) find that "org.jfree.chart.axis.junit.SegmentedTimelineTests2" impacts the extra failed methods "org.jfree.chart.axis.junit.SegmentedTimelineTests"...
		// 3) I run arja on Closure 103, my patch test has 115 failed methods, while ajra has 127 failed methods...
		// The arja cmd is: /home/apr/env/jdk1.8.0_202/jre/bin/java -cp /mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/classes/:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/test/:/mnt/recursive-repairthemall/RepairThemAll-Nopol/libs/arja_external/bin:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/hamcrest-core-1.1.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/ant_deploy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/classes:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/junit4-legacy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/junit4-core.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/test:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/google_common_deploy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/protobuf_deploy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/libtrunk_rhino_parser_jarjared.jar  us.msu.cse.repair.external.junit.JUnitTestRunner   @/mnt/benchmarks/buggylocs/Defects4J/Defects4J_Closure_103/Dale_APR/FL/test_methods.txt
		// none is perfect.
		// 4) I tried to export TZ, but still failed.
		// Therefore, I decided to leave this problem as future work. This is not our focus now.
		
		// I find a solution!
		// 1) I noticed that when running test methods is much more slower than running test cases. (9.8s for closure 103 tests, but 256.9s for test cases)
		// interestingly, defects4j test uses around 33s... This is strange.
		// 2) More importantly, there is no error in test method execution.
		// Therefore, I plan to run tests rather than methods. but in apr test validation, I plan to use the following strategy:
		// 1) run failed test methods;
		// 2) run positive test methods in the failed test classes;
		// 3) run other positive test methods in the rest test classes.
		// or more simply, we can direct:
		// 1) run failed test methods;
		// 2) run all classes.
		// This is pretty cool and straightforward.
		
		int timeout = 200;//timeout for a single method execution
		DecimalFormat dF = new DecimalFormat("0.0000");
		for (String testMethod : testMethods){
			String className = testMethod.split("#")[0];
			String methodName = testMethod.split("#")[1];
			
			try {
				final Request request = Request.method(Class.forName(className), methodName);
				testCnt ++;
				
//				result = null;
				try {
					TimeOut.runWithTimeout(new Runnable() {
						@Override
						public void run() {
							result = new JUnitCore().run(request);
						}
					}, timeout, TimeUnit.SECONDS);
				} catch (Exception e1) {
					System.out.format("failed method execution timeout: %s\n", className + "#" + methodName);
					failedTestMethods.add(className + "#" + methodName);
					e1.printStackTrace();
					return;
				}
				
				if (result == null){
					failedTestMethods.add(className + "#" + methodName);
					System.out.format("failed method execution: %s (result is null)\n", className + "#" + methodName);
					return;
				}else if (!result.wasSuccessful()){
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
					System.out.format("failed method execution time cost: %s\n", dF.format((float) result.getRunTime()/1000));
					return;
				}
			} catch (ClassNotFoundException e) {
				failedTestMethods.add(className + "#" + methodName);
				e.printStackTrace();
				return;
			}catch (StackOverflowError e) {//Throwable // StackOverflowError
				failedTestMethods.add(className + "#" + methodName);
				System.out.format("StackOverflowError. Now exit.\n");
				e.printStackTrace();
				return;
			}catch (java.lang.Error e) {//Throwable // StackOverflowError
				failedTestMethods.add(className + "#" + methodName);
				System.out.format("Other error. Now exit.\n");
				e.printStackTrace();
				return;
			}
		}
		
		System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
		for (String failed : failedTestMethods){
			System.out.format("[Junit test] failed test method: %s\n", failed);
		}
		
		System.out.format("[Junit test] Total time cost for running the test method(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT)/1000));
	}
	
	/*
	 * run tests without # (method name)
	 */
	public static void runTests(List<String> tests){
		List<String> failedTests = new ArrayList<>();
		
		System.out.format("tests size for execution: %d\n", tests.size());
		long startT = System.currentTimeMillis();
		
		int cnt = 0; // test cases/methods cnt
		int size = tests.size();
		int testCnt = 0;
		
		int timeout = 600;//timeout for a test class execution
		DecimalFormat dF = new DecimalFormat("0.0000");
		
		for (String test : tests){
			String className = test;
			
			try {
				final Request request = Request.aClass(Class.forName(className));
				System.out.format("current test: %s\n", className);
				
//				result = null;
				try {
					TimeOut.runWithTimeout(new Runnable() {
						@Override
						public void run() {
							result = new JUnitCore().run(request);
						}
					}, timeout, TimeUnit.SECONDS);
				} catch (Exception e1) {
					System.out.format("failed test class execution timeout: %s\n", className);
					failedTests.add(className);
					failedTestMethods.add(className);
					e1.printStackTrace();
//					continue;
					return;
				}
				
				if (result == null){
					failedTests.add(test);
					failedTestMethods.add(className);
					System.out.format("failed test class execution: %s (result is null)\n", className);
					return;
				}else if(!result.wasSuccessful()){
					failedTests.add(test);
					if (printTrace){
						for (Failure failure : result.getFailures()){
							System.out.format("failed trace info: %s\n", failure.getTrace());
							System.out.format("failed trace description: %s\n", failure.getDescription());
							// testIssue820(com.google.javascript.jscomp.CollapseVariableDeclarationsTest)
							
							// general: failed trace description: testIODirectoryNotWritable(org.apache.flink.runtime.taskmanager.TestManagerStartupTest)
							
							// failed trace description: org.apache.flink.runtime.io.network.partition.PartialConsumePipelinedResultTest
							// java.lang.ArrayIndexOutOfBoundsException: 1
							// exposed by: Bugs.jar_Flink_06e2da35 replicateTests 
							String desp = failure.getDescription().toString().trim();
							if (desp.contains("(") && desp.contains(")")){
								String failedTestClassName = desp.split("\\(")[1].split("\\)")[0];
								String failedTestMethodName = desp.split("\\(")[0];
								failedTestMethods.add(failedTestClassName + "#" + failedTestMethodName);
							}else{
//								System.out.println("debug1: " + failure.getDescription());
//								System.out.println("debug2: " + failure.getDescription().toString());
								failedTestMethods.add(desp + "# ");
							}
						}
					}
					return;
				}
				
				cnt = cnt + result.getRunCount();
				testCnt ++;
				System.out.format("[%d/%d] number of executed tests: %d, time cost: %s\n", testCnt, size, result.getRunCount(), dF.format((float) result.getRunTime()/1000));
			} catch (ClassNotFoundException e) {
				failedTests.add(test);
				failedTestMethods.add(className);
				e.printStackTrace();
				return;
			}catch (StackOverflowError e) {//Throwable // StackOverflowError
				failedTests.add(test);
				failedTestMethods.add(className);
				System.out.format("StackOverflowError. Now exit.\n");
				e.printStackTrace();
				return;
			}catch (java.lang.Error e) {//Throwable // StackOverflowError
				failedTests.add(test);
				failedTestMethods.add(className);
				System.out.format("Other error. Now exit.\n");
				e.printStackTrace();
				return;
			}
		}
		
		System.out.format("[Junit test] total test cases in execution: %d\n", cnt);
		System.out.format("[Junit test] failed tests size after execution: %d\n", failedTests.size());
		for (String failed : failedTests){
			System.out.format("[Junit test] failed test: %s\n", failed);
		}
		System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
		for (String failed : failedTestMethods){
			System.out.format("[Junit test] failed test method: %s\n", failed);
		}
		
		System.out.format("[Junit test] Total time cost for running the test(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT)/1000));
	}
	
	/*
	 * run tests with method name
	 */
	public static void runTestMethodsWithoutStop(List<String> testMethods){
//		List<String> failedTestMethods = new ArrayList<>();
		
		System.out.format("test methods size for execution: %d\n", testMethods.size());
		long startT = System.currentTimeMillis();
		
		// debug usage
//		Collections.reverse(testMethods);
		// face a weird bug: 
		// /home/apr/env/jdk1.7.0_80/bin/java -cp /home/apr/d4j/Chart/Chart_17/build/:/home/apr/d4j/Chart/Chart_17/build-tests/:/home/apr/d4j/Chart/Chart_17/lib/servlet.jar:/home/apr/d4j/Chart/Chart_17/lib/itext-2.0.6.jar:/home/apr/apr_tools/tbar-ori/TBar-dale/externel/target/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar apr.junit.PatchTest -testFile /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Chart_17/Dale_APR/FL/test_methods.txt -runTestMethods true
		// extra failed test methods.
		// I tried a lot of attempts, and found that when the positions of failed methods are changed, they passed.
		// 1) change order -> passed
		// 2) find that "org.jfree.chart.axis.junit.SegmentedTimelineTests2" impacts the extra failed methods "org.jfree.chart.axis.junit.SegmentedTimelineTests"...
		// 3) I run arja on Closure 103, my patch test has 115 failed methods, while ajra has 127 failed methods...
		// The arja cmd is: /home/apr/env/jdk1.8.0_202/jre/bin/java -cp /mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/classes/:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/test/:/mnt/recursive-repairthemall/RepairThemAll-Nopol/libs/arja_external/bin:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/hamcrest-core-1.1.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/ant_deploy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/classes:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/junit4-legacy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/junit4-core.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/build/test:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/google_common_deploy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/protobuf_deploy.jar:/mnt/benchmarks/repairDir/Kali_Defects4J_Closure_103/lib/libtrunk_rhino_parser_jarjared.jar  us.msu.cse.repair.external.junit.JUnitTestRunner   @/mnt/benchmarks/buggylocs/Defects4J/Defects4J_Closure_103/Dale_APR/FL/test_methods.txt
		// none is perfect.
		// 4) I tried to export TZ, but still failed.
		// Therefore, I decided to leave this problem as future work. This is not our focus now.
		
		// I find a solution!
		// 1) I noticed that when running test methods is much more slower than running test cases. (9.8s for closure 103 tests, but 256.9s for test cases)
		// interestingly, defects4j test uses around 33s... This is strange.
		// 2) More importantly, there is no error in test method execution.
		// Therefore, I plan to run tests rather than methods. but in apr test validation, I plan to use the following strategy:
		// 1) run failed test methods;
		// 2) run positive test methods in the failed test classes;
		// 3) run other positive test methods in the rest test classes.
		// or more simply, we can direct:
		// 1) run failed test methods;
		// 2) run all classes.
		// This is pretty cool and straightforward.
		
		int timeout = 200;//timeout for a single method execution
		DecimalFormat dF = new DecimalFormat("0.0000");
				
		for (String testMethod : testMethods){
			String className = testMethod.split("#")[0];
			String methodName = testMethod.split("#")[1];
			
			// debugging use
//			runCmdNoOutput("TZ=\"America/New_York\"; export TZ");
			
			try {
//				long startT = System.currentTimeMillis();
				final Request request = Request.method(Class.forName(className), methodName);
//				Result result = null;
				
//				result = null;
				try {
					TimeOut.runWithTimeout(new Runnable() {
						@Override
						public void run() {
							result = new JUnitCore().run(request);
						}
					}, timeout, TimeUnit.SECONDS);
				} catch (Exception e1) {
					System.out.format("failed method execution timeout: %s\n", className + "#" + methodName);
					failedTestMethods.add(className + "#" + methodName);
					e1.printStackTrace();
					continue;
				}
				
//				Result result = new JUnitCore().run(request);
				
				if (result == null){
					failedTestMethods.add(className + "#" + methodName);
					System.out.format("failed method execution: %s (result is null)\n", className + "#" + methodName);
				}else if (!result.wasSuccessful()){
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
					System.out.format("failed method execution time cost: %s\n", dF.format((float) result.getRunTime()/1000));
				}
				
//				DecimalFormat dF = new DecimalFormat("0.0000");
//				System.out.format("number of executed tests: %d, time cost: %s\n", result.getRunCount(), dF.format((float) result.getRunTime()/1000));
			} catch (ClassNotFoundException e) {
				failedTestMethods.add(className + "#" + methodName);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (StackOverflowError e) {//Throwable // StackOverflowError
				failedTestMethods.add(className + "#" + methodName);
				//unwrap the root cause
				System.out.format("StackOverflowError. Now exit.\n");
				e.printStackTrace();
			}catch (java.lang.Error e) {//Throwable // StackOverflowError
				failedTestMethods.add(className + "#" + methodName);
				//unwrap the root cause
				System.out.format("Other error. Now exit.\n");
				e.printStackTrace();
			}
		}
		
		System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
		for (String failed : failedTestMethods){
			System.out.format("[Junit test] failed test method: %s\n", failed);
		}
		
		System.out.format("[Junit test] Total time cost for running the test method(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT)/1000));
	}
	
	/**
	 * @Description copy from my apr project 
	 * @author apr
	 * @version Apr 5, 2020
	 *
	 * @param cmd
	 */
	public static void runCmdNoOutput(String cmd) {
		try{
			String[] commands = {"bash", "-c", cmd};
			Process proc = Runtime.getRuntime().exec(commands);

			proc.getInputStream();
			
//			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//			
//			// read output
//			String line = null;
//			while ((line = stdInput.readLine()) != null){
//				System.out.println("Cmd Output: " + line);
////				output += line + "\n";
//			}
		}catch (Exception err){
			err.printStackTrace();
		}
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
	private static void setParameters(String[] args) {		
//		Map<String, String> parameters = new HashMap<>();
		
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
//		return parameters;
    }
}