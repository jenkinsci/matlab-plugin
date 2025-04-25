function plugins = getDefaultPlugins(pluginProviderData)
%

%   Copyright 2024-2025 The MathWorks, Inc.
arguments
    pluginProviderData (1,1) struct = struct();
end

if isMATLABReleaseOlderThan("R2025b")
    reportPlugin = ciplugins.jenkins.BuildReportPlugin();
else
    reportPlugin = ciplugins.jenkins.ParallelizableBuildReportPlugin();
end

plugins = [ ...
    matlab.buildtool.internal.getFactoryDefaultPlugins(pluginProviderData) ...
    reportPlugin ...
    ciplugins.jenkins.TaskRunProgressPlugin() ...
];
end
