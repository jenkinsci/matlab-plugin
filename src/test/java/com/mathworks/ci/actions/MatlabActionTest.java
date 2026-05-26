package com.mathworks.ci.actions;

/**
 * Copyright 2024-26, The MathWorks Inc.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.BuildArtifactAction;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.actions.RunMatlabCommandAction;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import com.mathworks.ci.parameters.CommandActionParameters;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MatlabActionTest {
    @Mock
    CommandActionParameters params;
    @Mock
    BuildConsoleAnnotator annotator;
    @Mock
    MatlabCommandRunner runner;
    @Mock
    PrintStream out;
    @Mock
    TaskListener listener;
    @Mock
    Run build;

    @Mock
    FilePath tempFolder;

    private boolean setup = false;
    private RunMatlabCommandAction action;

    // Not using @BeforeClass to avoid static fields.
    @Before
    public void init() {
        if (!setup) {
            setup = true;
            action = new RunMatlabCommandAction(runner, annotator, params);

            when(runner.getTempFolder()).thenReturn(tempFolder);
            when(tempFolder.getRemote()).thenReturn("/path/less/traveled");

            when(params.getTaskListener()).thenReturn(listener);
            when(listener.getLogger()).thenReturn(out);

            when(params.getBuild()).thenReturn(build);
            when(params.getGenerateSummary()).thenReturn(true);
        }
    }

    @Test
    public void shouldCopyPluginsToTempDirectory() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        InOrder inOrder = inOrder(runner);

        inOrder.verify(runner)
                .copyFileToTempFolder(MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
        inOrder.verify(runner)
                .copyFileToTempFolder(MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN,
                        MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);
        inOrder.verify(runner)
                .copyFileToTempFolder(MatlabBuilderConstants.BUILD_REPORT_PLUGIN,
                        MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
    }

    @Test
    public void shouldOverrideDefaultBuildtoolPlugin()
            throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).addEnvironmentVariable(
                "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                "ciplugins.jenkins.getDefaultPlugins");
    }

    @Test
    public void shouldCopyBuildResultsToRootAndAddAction()
            throws IOException, InterruptedException, MatlabExecutionException {
        File tmp = Files.createTempDirectory("temp").toFile();
        tmp.deleteOnExit();

        File dest = Files.createTempDirectory("dest").toFile();
        dest.deleteOnExit();

        File json = new File(tmp, "buildArtifact.json");
        json.createNewFile();

        doReturn(new FilePath(tmp)).when(runner).getTempFolder();
        doReturn(dest).when(build).getRootDir();

        action.run();

        // Should have deleted original file
        assertFalse(json.exists());
        // Should have copied file to root dir
        assertTrue(new File(dest, "buildArtifact" + action.getActionID() + ".json").exists());
    }

    @Test
    public void shouldNotAddActionIfNoBuildResult() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(build, never()).addAction(any(BuildArtifactAction.class));
    }

    @Test
    public void shouldRemoveTempFolder() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).removeTempFolder();
    }

    @Test
    public void shouldOnlyCopyNonVisualizationPluginsWhenGenerateSummaryFalse()
            throws IOException, InterruptedException {
        MatlabCommandRunner localRunner = mock(MatlabCommandRunner.class);
        BuildConsoleAnnotator localAnnotator = mock(BuildConsoleAnnotator.class);

        MatlabAction matlabAction = new MatlabAction(localRunner, localAnnotator);
        matlabAction.copyPluginsToTemp(false);

        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN, MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);

        verify(localRunner, never()).copyFileToTempFolder(
                MatlabBuilderConstants.BUILD_REPORT_PLUGIN, MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
        verify(localRunner, never()).copyFileToTempFolder(
                MatlabBuilderConstants.PAR_BUILD_REPORT_PLUGIN, MatlabBuilderConstants.PAR_BUILD_REPORT_PLUGIN);
        verify(localRunner, never()).copyFileToTempFolder(
                MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN, MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN);
        verify(localRunner, never()).copyFileToTempFolder(
                MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN_SERVICE, MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN_SERVICE);
    }

    @Test
    public void shouldCopyAllPluginsWhenGenerateSummaryTrue()
            throws IOException, InterruptedException {
        MatlabCommandRunner localRunner = mock(MatlabCommandRunner.class);
        BuildConsoleAnnotator localAnnotator = mock(BuildConsoleAnnotator.class);

        MatlabAction matlabAction = new MatlabAction(localRunner, localAnnotator);
        matlabAction.copyPluginsToTemp(true);

        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN, MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);
        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.BUILD_REPORT_PLUGIN, MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.PAR_BUILD_REPORT_PLUGIN, MatlabBuilderConstants.PAR_BUILD_REPORT_PLUGIN);
        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN, MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN);
        verify(localRunner).copyFileToTempFolder(
                MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN_SERVICE, MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN_SERVICE);
    }

    @Test
    public void shouldSetGenerateSummaryEnvVar()
            throws IOException, InterruptedException {
        MatlabCommandRunner localRunner = mock(MatlabCommandRunner.class);
        BuildConsoleAnnotator localAnnotator = mock(BuildConsoleAnnotator.class);
        FilePath localTempFolder = mock(FilePath.class);
        when(localRunner.getTempFolder()).thenReturn(localTempFolder);

        MatlabAction matlabAction = new MatlabAction(localRunner, localAnnotator);
        matlabAction.setBuildEnvVars(true);
        verify(localRunner).addEnvironmentVariable("MW_GENERATE_SUMMARY", "true");

        MatlabCommandRunner localRunner2 = mock(MatlabCommandRunner.class);
        FilePath localTempFolder2 = mock(FilePath.class);
        when(localRunner2.getTempFolder()).thenReturn(localTempFolder2);

        MatlabAction matlabAction2 = new MatlabAction(localRunner2, localAnnotator);
        matlabAction2.setBuildEnvVars(false);
        verify(localRunner2).addEnvironmentVariable("MW_GENERATE_SUMMARY", "false");
    }

    @Test
    public void shouldAlwaysSetDefaultPluginsOverrideEnvVar()
            throws IOException, InterruptedException {
        MatlabCommandRunner localRunner = mock(MatlabCommandRunner.class);
        BuildConsoleAnnotator localAnnotator = mock(BuildConsoleAnnotator.class);
        FilePath localTempFolder = mock(FilePath.class);
        when(localRunner.getTempFolder()).thenReturn(localTempFolder);

        MatlabAction matlabAction = new MatlabAction(localRunner, localAnnotator);
        matlabAction.setBuildEnvVars(false);

        verify(localRunner).addEnvironmentVariable(
                "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                "ciplugins.jenkins.getDefaultPlugins");
    }
}
