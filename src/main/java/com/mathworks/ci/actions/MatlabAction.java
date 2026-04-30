package com.mathworks.ci.actions;

/**
 * Copyright 2024-2025, The MathWorks Inc.
 */

import com.mathworks.ci.BuildArtifactAction;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.TestResultsViewAction;
import com.mathworks.ci.parameters.MatlabActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;

import hudson.FilePath;
import hudson.model.Run;

import org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.io.IOException;

public class MatlabAction {
    MatlabCommandRunner runner;
    BuildConsoleAnnotator annotator;
    String actionID;

    public String getActionID() {
        return (this.actionID == null) ? "" : this.actionID;
    }

    public MatlabAction(MatlabCommandRunner runner) {
        this.runner = runner;
    }

    public MatlabAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator) {
        this.runner = runner;
        this.actionID = RandomStringUtils.randomAlphanumeric(8);
        this.annotator = annotator;
    }

    public void copyBuildPluginsToTemp() throws IOException, InterruptedException {
        // Copy BuildRunner plugins and override default plugins function
        if(this.annotator != null) {
            runner.copyFileToTempFolder(MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
            runner.copyFileToTempFolder(MatlabBuilderConstants.BUILD_REPORT_PLUGIN, MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
            runner.copyFileToTempFolder(MatlabBuilderConstants.PAR_BUILD_REPORT_PLUGIN, MatlabBuilderConstants.PAR_BUILD_REPORT_PLUGIN);
            runner.copyFileToTempFolder(MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN, MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);
        }

        // Copy TestRunner plugins and services
        runner.copyFileToTempFolder(MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN, MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN);
        runner.copyFileToTempFolder(MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN_SERVICE, MatlabBuilderConstants.TEST_RESULTS_VIEW_PLUGIN_SERVICE);
    }

    public void setBuildEnvVars() throws IOException, InterruptedException {
        // Set environment variable
        runner.addEnvironmentVariable(
                "MW_MATLAB_TEMP_FOLDER",
                runner.getTempFolder().toString());
        runner.addEnvironmentVariable("MW_MATLAB_ACTION_ID", this.getActionID());

        if(this.annotator != null) {
            runner.addEnvironmentVariable(
                    "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                    "ciplugins.jenkins.getDefaultPlugins");
        }
    }

    public void teardownAction(MatlabActionParameters params) {
        // Handle build result
        if(this.annotator != null) {
            moveBuildArtifactToBuildRoot(params);
        }

        moveTestResultsToBuildRoot(params);

        try {
            this.runner.removeTempFolder();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private void moveBuildArtifactToBuildRoot(MatlabActionParameters params) {
        try {
            FilePath file = new FilePath(this.runner.getTempFolder(), MatlabBuilderConstants.BUILD_ARTIFACT + ".json");
            if (file.exists()) {
                Run<?, ?> build = params.getBuild();

                FilePath rootLocation = new FilePath(
                        new File(
                                build.getRootDir().getAbsolutePath(),
                                MatlabBuilderConstants.BUILD_ARTIFACT + this.getActionID() + ".json"));
                file.copyTo(rootLocation);
                file.delete();
                build.addAction(new BuildArtifactAction(build, this.getActionID()));
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private void moveTestResultsToBuildRoot(MatlabActionParameters params) {
        try {
            FilePath tempFolder = this.runner.getTempFolder();
            FilePath[] sessionFiles = tempFolder.list(
                    MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + this.getActionID() + "_*.json");

            if (sessionFiles.length > 0) {
                Run<?, ?> build = params.getBuild();
                FilePath workspace = params.getWorkspace();
                String buildRootPath = build.getRootDir().getAbsolutePath();

                for (FilePath sessionFile : sessionFiles) {
                    FilePath rootLocation = new FilePath(new File(buildRootPath, sessionFile.getName()));
                    sessionFile.copyTo(rootLocation);
                    sessionFile.delete();
                }
                build.addAction(new TestResultsViewAction(build, workspace, this.getActionID()));
            }
        } catch (Exception e) {
            // Don't want to override more important error
            // thrown in catch block
            System.err.println(e.toString());
        }
    }
}
