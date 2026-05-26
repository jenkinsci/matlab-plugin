package com.mathworks.ci.parameters;

/**
 * Copyright 2024-26 The MathWorks, Inc.
 */

import java.io.IOException;
import hudson.FilePath;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

public class MatlabActionParameters {
    private Run build;
    private FilePath workspace;
    private EnvVars env;
    private Launcher launcher;
    private TaskListener listener;

    private String startupOptions;
    private boolean generateSummary;

    public MatlabActionParameters(StepContext context, String startupOpts, boolean generateSummary)
            throws IOException, InterruptedException {
        this.build = context.get(Run.class);
        this.workspace = context.get(FilePath.class);
        this.env = context.get(EnvVars.class);
        this.launcher = context.get(Launcher.class);
        this.listener = context.get(TaskListener.class);
        this.startupOptions = startupOpts;
        this.generateSummary = generateSummary;
    }

    public MatlabActionParameters(Run build, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener,
            String startupOpts, boolean generateSummary) {
        this.build = build;
        this.workspace = workspace;
        this.env = env;
        this.launcher = launcher;
        this.listener = listener;
        this.startupOptions = startupOpts;
        this.generateSummary = generateSummary;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public EnvVars getEnvVars() {
        return env;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public TaskListener getTaskListener() {
        return listener;
    }

    public String getStartupOptions() {
        return startupOptions;
    }

    public boolean getGenerateSummary() {
        return generateSummary;
    }
}
