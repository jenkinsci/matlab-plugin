function plugins = getDefaultPlugins(pluginProviderData)
%

%   Copyright 2024-26 The MathWorks, Inc.
arguments
    pluginProviderData (1,1) struct = struct();
end

plugins = [ ...
    matlab.buildtool.internal.getFactoryDefaultPlugins(pluginProviderData) ...
    ciplugins.jenkins.TaskRunProgressPlugin() ...
];

if strcmp(getenv("MW_INPUT_GENERATE_SUMMARY"), "true")
    if isMATLABReleaseOlderThan("R2026a")
        reportPlugin = ciplugins.jenkins.BuildReportPlugin();
    else
        reportPlugin = ciplugins.jenkins.ParallelizableBuildReportPlugin();
    end
    plugins = [plugins reportPlugin];
end
end
