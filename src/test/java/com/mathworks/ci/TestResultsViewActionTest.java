package com.mathworks.ci;

/**
 * Copyright 2025-26 The MathWorks, Inc.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.mathworks.ci.TestResultsViewAction.*;
import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class TestResultsViewActionTest {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;

    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    public TestResultsViewActionTest() {}

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.scriptBuilder = new RunMatlabBuildBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/" + version + "/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo))
                .orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(File.separator + VERSION_INFO_XML_FILE, "")
                .replace("R2017a", version);
    }

    private URL getResource(String resource) {
        return TestResultsViewAction.class.getClassLoader().getResource(resource);
    }

    /**
     *  Verify if all test results are returned from artifact
     *
     */

    @Test
    public void verifyAllTestsReturned() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        int actualTestSessions = ta.size();
        Assert.assertEquals("Incorrect test sessions",2,actualTestSessions);
        int actualTestFiles1 = ta.get(0).size();
        Assert.assertEquals("Incorrect test files",1,actualTestFiles1);
        int actualTestFiles2 = ta.get(1).size();
        Assert.assertEquals("Incorrect test files",1,actualTestFiles2);
        int actualTestResults1 = ta.get(0).get(0).getMatlabTestCases().size();
        Assert.assertEquals("Incorrect test results",9,actualTestResults1);
        int actualTestResults2 = ta.get(1).get(0).getMatlabTestCases().size();
        Assert.assertEquals("Incorrect test results",1,actualTestResults2);
    }

    /**
     *  Verify if total test results count is correct
     *
     */

    @Test
    public void verifyTotalTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        int actualCount = ac.getTotalCount();
        Assert.assertEquals("Incorrect total tests count",10,actualCount);
    }

    /**
     *  Verify if passed tests count is correct
     *
     */

    @Test
    public void verifyPassedTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        int actualCount = ac.getPassedCount();
        Assert.assertEquals("Incorrect passed tests count",4,actualCount);
    }

    /**
     *  Verify if failed tests count is correct
     *
     */

    @Test
    public void verifyFailedTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        int actualCount = ac.getFailedCount();
        Assert.assertEquals("Incorrect failed tests count",3,actualCount);
    }

    /**
     *  Verify if incomplete tests count is correct
     *
     */

    @Test
    public void verifyIncompleteTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        int actualCount = ac.getIncompleteCount();
        Assert.assertEquals("Incorrect incomplete tests count",2,actualCount);
    }

    /**
     *  Verify if not run tests count is correct
     *
     */

    @Test
    public void verifyNotRunTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        int actualCount = ac.getNotRunCount();
        Assert.assertEquals("Incorrect not run tests count",1,actualCount);
    }

    /**
     *  Verify if test file path is correct
     *
     */

    @Test
    public void verifyMatlabTestFilePath() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();

        String os = System.getProperty("os.name").toLowerCase();
        String expectedParentPath = "";
        if (os.contains("win")) {
            expectedParentPath = "visualization\\";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            expectedParentPath = "visualization/";
        } else if (os.contains("mac")) {
            expectedParentPath = "visualization/";
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }

        List<List<MatlabTestFile>> ta = ac.getTestResults();
        String actualPath1 = ta.get(0).get(0).getPath();
        Assert.assertEquals("Incorrect test file path",expectedParentPath + "tests" + File.separator + "TestExamples1",actualPath1);
        String actualPath2 = ta.get(1).get(0).getPath();
        Assert.assertEquals("Incorrect test file path",expectedParentPath + "duplicate_tests" + File.separator + "TestExamples2",actualPath2);
    }

    /**
     *  Verify if test file path for the view is correct
     *
     */

    @Test
    public void verifyMatlabTestFileLinuxStylePath() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        String expectedParentPath = "visualization/";

        List<List<MatlabTestFile>> ta = ac.getTestResults();
        String actualPath1 = ta.get(0).get(0).getLinuxStylePath();
        Assert.assertEquals("Incorrect test file path",expectedParentPath + "tests/" + "TestExamples1",actualPath1);
        String actualPath2 = ta.get(1).get(0).getLinuxStylePath();
        Assert.assertEquals("Incorrect test file path",expectedParentPath + "duplicate_tests/" + "TestExamples2",actualPath2);
    }

    /**
     *  Verify if test file name is correct
     *
     */

    @Test
    public void verifyMatlabTestFileName() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        String actualName1 = ta.get(0).get(0).getName();
        Assert.assertEquals("Incorrect test file name","TestExamples1",actualName1);
        String actualName2 = ta.get(1).get(0).getName();
        Assert.assertEquals("Incorrect test file name","TestExamples2",actualName2);
    }

    /**
     *  Verify if test file duration is correct
     *
     */

    @Test
    public void verifyMatlabTestFileDuration() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        BigDecimal actualDuration1 = ta.get(0).get(0).getDuration();
        Assert.assertEquals("Incorrect test file duration",new BigDecimal("1.73"),actualDuration1);
        BigDecimal actualDuration2 = ta.get(1).get(0).getDuration();
        Assert.assertEquals("Incorrect test file duration",new BigDecimal("0.10"),actualDuration2);
    }

    /**
     *  Verify if test file status is correct
     *
     */

    @Test
    public void verifyMatlabTestFileStatus() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        TestStatus actualStatus1 = ta.get(0).get(0).getStatus();
        Assert.assertEquals("Incorrect test file status",TestStatus.FAILED,actualStatus1);
        TestStatus actualStatus2 = ta.get(1).get(0).getStatus();
        Assert.assertEquals("Incorrect test file status",TestStatus.INCOMPLETE,actualStatus2);
    }

    /**
     *  Verify if test case name is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseName() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        String actualName1_1 = ta.get(0).get(0).getMatlabTestCases().get(0).getName();
        Assert.assertEquals("Incorrect test case name","testNonLeapYear",actualName1_1);
        String actualName1_5 = ta.get(0).get(0).getMatlabTestCases().get(4).getName();
        Assert.assertEquals("Incorrect test case name","testLeapYear",actualName1_5);
        String actualName1_8 = ta.get(0).get(0).getMatlabTestCases().get(7).getName();
        Assert.assertEquals("Incorrect test case name","testValidDateFormat",actualName1_8);
        String actualName1_9 = ta.get(0).get(0).getMatlabTestCases().get(8).getName();
        Assert.assertEquals("Incorrect test case name","testInvalidDateFormat",actualName1_9);
        String actualName2 = ta.get(1).get(0).getMatlabTestCases().get(0).getName();
        Assert.assertEquals("Incorrect test case name","testNonLeapYear",actualName2);
    }

    /**
     *  Verify if test case status is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseStatus() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        TestStatus actualStatus1_1 = ta.get(0).get(0).getMatlabTestCases().get(0).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.PASSED,actualStatus1_1);
        TestStatus actualStatus1_5 = ta.get(0).get(0).getMatlabTestCases().get(4).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.FAILED,actualStatus1_5);
        TestStatus actualStatus1_9 = ta.get(0).get(0).getMatlabTestCases().get(8).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.NOT_RUN,actualStatus1_9);
        TestStatus actualStatus2 = ta.get(1).get(0).getMatlabTestCases().get(0).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.INCOMPLETE,actualStatus2);
    }

    /**
     *  Verify if test case duration is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseDuration() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        BigDecimal actualDuration1_1 = ta.get(0).get(0).getMatlabTestCases().get(0).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.10"),actualDuration1_1);
        BigDecimal actualDuration1_2 = ta.get(0).get(0).getMatlabTestCases().get(1).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.11"),actualDuration1_2);
        BigDecimal actualDuration1_3 = ta.get(0).get(0).getMatlabTestCases().get(2).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.11"),actualDuration1_3);
        BigDecimal actualDuration1_5 = ta.get(0).get(0).getMatlabTestCases().get(4).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.40"),actualDuration1_5);
        BigDecimal actualDuration1_9 = ta.get(0).get(0).getMatlabTestCases().get(8).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.00"),actualDuration1_9);
        BigDecimal actualDuration2 = ta.get(1).get(0).getMatlabTestCases().get(0).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.10"),actualDuration2);
    }

    /**
     *  Verify if test case diagnostics is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseDiagnostics() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        List<List<MatlabTestFile>> ta = ac.getTestResults();

        MatlabTestDiagnostics diagnostics1 = ta.get(0).get(0).getMatlabTestCases().get(4).getDiagnostics().get(0);
        String actualDiagnosticsEvent1 = diagnostics1.getEvent();
        Assert.assertEquals("Incorrect test diagnostics event","SampleDiagnosticsEvent1",actualDiagnosticsEvent1);
        String actualDiagnosticsReport1 = diagnostics1.getReport();
        Assert.assertEquals("Incorrect test diagnostics report","SampleDiagnosticsReport1",actualDiagnosticsReport1);

        MatlabTestDiagnostics diagnostics2 = ta.get(1).get(0).getMatlabTestCases().get(0).getDiagnostics().get(0);
        String actualDiagnosticsEvent2 = diagnostics2.getEvent();
        Assert.assertEquals("Incorrect test diagnostics event","SampleDiagnosticsEvent2",actualDiagnosticsEvent2);
        String actualDiagnosticsReport2 = diagnostics2.getReport();
        Assert.assertEquals("Incorrect test diagnostics report","SampleDiagnosticsReport2",actualDiagnosticsReport2);
    }

    /**
     *  Verify if actionID is set correctly
     *
     */

    @Test
    public void verifyActionID() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewAction();
        String actualActionID = ac.getActionID();
        Assert.assertEquals("Incorrect action ID","abc123",actualActionID);
    }

    @Test
    public void verifyLegacySingleFileFormat() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewActionLegacy();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        Assert.assertEquals("Incorrect test sessions", 2, ta.size());
        Assert.assertEquals("Incorrect total tests count", 10, ac.getTotalCount());
        Assert.assertEquals("Incorrect passed tests count", 4, ac.getPassedCount());
        Assert.assertEquals("Incorrect failed tests count", 3, ac.getFailedCount());
    }

    @Test
    public void verifySingleSessionReturnsOneSession() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewActionFromFiles("t1",
                MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + "abc123_20260101_120000_001.json");
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        Assert.assertEquals("Should return exactly 1 session", 1, ta.size());
        Assert.assertEquals("Incorrect test files", 1, ta.get(0).size());
        Assert.assertEquals("Incorrect test results", 9, ta.get(0).get(0).getMatlabTestCases().size());
        Assert.assertEquals("Incorrect total count", 9, ac.getTotalCount());
        Assert.assertEquals("Incorrect passed count", 4, ac.getPassedCount());
        Assert.assertEquals("Incorrect failed count", 3, ac.getFailedCount());
        Assert.assertEquals("Incorrect incomplete count", 1, ac.getIncompleteCount());
        Assert.assertEquals("Incorrect not run count", 1, ac.getNotRunCount());
    }

    @Test
    public void verifySingleObjectSessionFile() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewActionFromFiles("t1",
                MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + "abc123_20260101_120000_002.json");
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        Assert.assertEquals("Should return exactly 1 session", 1, ta.size());
        Assert.assertEquals("Incorrect test files", 1, ta.get(0).size());
        Assert.assertEquals("Incorrect test results", 1, ta.get(0).get(0).getMatlabTestCases().size());

        MatlabTestFile testFile = ta.get(0).get(0);
        Assert.assertEquals("Incorrect test file name", "TestExamples2", testFile.getName());
        Assert.assertEquals("Incorrect test file status", TestStatus.INCOMPLETE, testFile.getStatus());

        MatlabTestCase testCase = testFile.getMatlabTestCases().get(0);
        Assert.assertEquals("Incorrect test case name", "testNonLeapYear", testCase.getName());
        Assert.assertEquals("Incorrect test case status", TestStatus.INCOMPLETE, testCase.getStatus());
        Assert.assertEquals("Incorrect test case duration", new BigDecimal("0.10"), testCase.getDuration());
        Assert.assertEquals("Should have 1 diagnostic", 1, testCase.getDiagnostics().size());
        Assert.assertEquals("Incorrect diagnostic event", "SampleDiagnosticsEvent2", testCase.getDiagnostics().get(0).getEvent());
        Assert.assertEquals("Incorrect diagnostic report", "SampleDiagnosticsReport2", testCase.getDiagnostics().get(0).getReport());
    }

    @Test
    public void verifyMultipleDiagnosticsAndDurationRounding() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewActionFromFiles("t3",
                MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + "abc123_20260101_120000_001.json");
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        Assert.assertEquals("Should return exactly 1 session", 1, ta.size());
        Assert.assertEquals("Incorrect test files", 1, ta.get(0).size());
        Assert.assertEquals("Incorrect test results", 5, ta.get(0).get(0).getMatlabTestCases().size());

        MatlabTestCase multiDiagCase = ta.get(0).get(0).getMatlabTestCases().get(0);
        Assert.assertEquals("Incorrect test case name", "testMultipleDiags", multiDiagCase.getName());
        Assert.assertEquals("Incorrect test case status", TestStatus.FAILED, multiDiagCase.getStatus());
        Assert.assertEquals("Should have 2 diagnostics", 2, multiDiagCase.getDiagnostics().size());
        Assert.assertEquals("Incorrect first diagnostic event", "DiagEvent1", multiDiagCase.getDiagnostics().get(0).getEvent());
        Assert.assertEquals("Incorrect first diagnostic report", "First diagnostic report", multiDiagCase.getDiagnostics().get(0).getReport());
        Assert.assertEquals("Incorrect second diagnostic event", "DiagEvent2", multiDiagCase.getDiagnostics().get(1).getEvent());
        Assert.assertEquals("Incorrect second diagnostic report", "Second diagnostic report", multiDiagCase.getDiagnostics().get(1).getReport());

        MatlabTestCase rounding1 = ta.get(0).get(0).getMatlabTestCases().get(2);
        Assert.assertEquals("Duration should round to 2 decimal places", new BigDecimal("0.11"), rounding1.getDuration());
        MatlabTestCase rounding2 = ta.get(0).get(0).getMatlabTestCases().get(3);
        Assert.assertEquals("Duration should round to 2 decimal places", new BigDecimal("1.00"), rounding2.getDuration());
        MatlabTestCase rounding3 = ta.get(0).get(0).getMatlabTestCases().get(4);
        Assert.assertEquals("Duration should round to 2 decimal places", new BigDecimal("0.01"), rounding3.getDuration());

        Assert.assertEquals("Incorrect total count", 5, ac.getTotalCount());
        Assert.assertEquals("Incorrect passed count", 4, ac.getPassedCount());
        Assert.assertEquals("Incorrect failed count", 1, ac.getFailedCount());
    }

    @Test
    public void verifyTestResultWithMissingDetails() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        TestResultsViewAction ac = setupTestResultsViewActionWithMissingDetails();
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        Assert.assertEquals("Incorrect test sessions", 1, ta.size());
        Assert.assertEquals("Incorrect test files", 1, ta.get(0).size());
        Assert.assertEquals("Incorrect test results", 1, ta.get(0).get(0).getMatlabTestCases().size());

        MatlabTestCase testCase = ta.get(0).get(0).getMatlabTestCases().get(0);
        Assert.assertEquals("Incorrect test case name", "testNonLeapYear", testCase.getName());
        Assert.assertEquals("Incorrect test case status", TestStatus.PASSED, testCase.getStatus());
        Assert.assertTrue("Diagnostics should be empty", testCase.getDiagnostics().isEmpty());

        Assert.assertEquals("Incorrect total count", 1, ac.getTotalCount());
        Assert.assertEquals("Incorrect passed count", 1, ac.getPassedCount());
        Assert.assertEquals("Incorrect failed count", 0, ac.getFailedCount());
    }

    private TestResultsViewAction setupTestResultsViewActionWithMissingDetails() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        return setupTestResultsViewActionFromFiles("t2",
                MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + "abc123_20260101_120000_001.json");
    }

    private TestResultsViewAction setupTestResultsViewAction() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        return setupTestResultsViewActionFromFiles("t1",
                MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + "abc123_20260101_120000_001.json",
                MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + "abc123_20260101_120000_002.json");
    }

    private TestResultsViewAction setupTestResultsViewActionLegacy() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        String osName = getOsName();
        final FilePath workspace = new FilePath(new File(getWorkspaceParent() + "workspace"));
        copyFileInWorkspace("testArtifacts/t1/" + osName + "/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json", targetFile, artifactRoot);
        return new TestResultsViewAction(build, workspace, actionID);
    }

    private TestResultsViewAction setupTestResultsViewActionFromFiles(String testDir, String... fileNames) throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        String osName = getOsName();
        final FilePath workspace = new FilePath(new File(getWorkspaceParent() + "workspace"));
        String resourcePrefix = "testArtifacts/" + testDir + "/" + osName + "/";
        for (String fileName : fileNames) {
            copyFileInWorkspace(resourcePrefix + fileName, fileName, artifactRoot);
        }
        return new TestResultsViewAction(build, workspace, actionID);
    }

    private String getOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "linux";
        } else if (os.contains("mac")) {
            return "mac";
        }
        throw new RuntimeException("Unsupported OS: " + os);
    }

    private String getWorkspaceParent() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "C:\\";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "/home/user/";
        } else if (os.contains("mac")) {
            return "/Users/username/";
        }
        throw new RuntimeException("Unsupported OS: " + os);
    }

    private FreeStyleBuild getFreestyleBuild() throws ExecutionException, InterruptedException, URISyntaxException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        return build;
    }

    private void copyFileInWorkspace(String sourceFile, String targetFile, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, targetFile);
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0777);
    }
}
