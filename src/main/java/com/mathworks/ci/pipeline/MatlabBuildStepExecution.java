package com.mathworks.ci.pipeline;

/**
 * Copyright 2022-2024 The MathWorks, Inc.
 */

import java.io.Serial;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import hudson.model.Result;

import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabBuildAction;
import com.mathworks.ci.parameters.BuildActionParameters;

public class MatlabBuildStepExecution extends SynchronousNonBlockingStepExecution<Void> {

    @Serial
    private static final long serialVersionUID = 4771831219402275744L;

    private final MatlabActionFactory factory;
    private final RunMatlabBuildStep step;

    public MatlabBuildStepExecution(MatlabActionFactory factory, StepContext ctx, RunMatlabBuildStep step) {
        super(ctx);

        this.factory = factory;
        this.step = step;
    }

    public MatlabBuildStepExecution(StepContext ctx, RunMatlabBuildStep step) {
        this(new MatlabActionFactory(), ctx, step);
    }

    @Override
    public Void run() throws Exception {
        BuildActionParameters params = new BuildActionParameters(
                getContext(),
                step.getStartupOptions(),
                step.getTasks(),
                step.getBuildOptions());

        RunMatlabBuildAction action = factory.createAction(params);
        try {
            action.run();
        } catch (Exception e) {
            // throw an exception if return code is non-zero
            stop(e);
        }

        getContext().setResult(Result.SUCCESS);
        return null;
    }

    @Override
    public void stop(Throwable cause) {
        getContext().onFailure(cause);
    }
}
