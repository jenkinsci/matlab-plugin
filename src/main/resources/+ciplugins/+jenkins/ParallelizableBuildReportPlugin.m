classdef ParallelizableBuildReportPlugin < matlab.buildtool.plugins.BuildRunnerPlugin

    %   Copyright 2025 The MathWorks, Inc.

    properties
        TempFolder
    end

    methods
        function plugin = ParallelizableBuildReportPlugin()
            tempRoot = getenv("MW_MATLAB_TEMP_FOLDER");
            plugin.TempFolder = fullfile(tempRoot, "taskDetails");
        end
    end

    methods (Access=protected)
        function runBuild(plugin, pluginData)
            % Create temp folder
            mkdir(plugin.TempFolder);
            cleanup = onCleanup(@()rmdirplugin.TempFolder, "s");
 
            runBuild@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

            % Construct task details
            taskDetails = {};
            fs = what(plugin.TempFolder).mat;
            for f = fs(:)'
                s = load(fullfile(plugin.TempFolder, fs{1}));
                taskDetails = [taskDetails s.taskDetail]; %#ok<AGROW>
            end

            % Write to file
            [fID, msg] = fopen(fullfile(getenv("MW_MATLAB_TEMP_FOLDER"),"buildArtifact.json"), "w");
            if fID == -1
                warning("ciplugins:jenkins:BuildReportPlugin:UnableToOpenFile","Could not open a file for Jenkins build result table due to: %s", msg);
            else
                closeFile = onCleanup(@()fclose(fID));
                a = struct();
                a.taskDetails = taskDetails;
                s = jsonencode(a, PrettyPrint=true);
                fprintf(fID, "%s", s);
            end
        end

        function runTask(plugin, pluginData)
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

            name = fullfile(plugin.TempFolder, pluginData.Name + ".mat");
            taskDetail = getCommonTaskDetail(pluginData);
            save(name, "taskDetail");
        end

        function skipTask(plugin, pluginData)
            skipTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

            name = fullfile(plugin.TempFolder, pluginData.Name + ".mat");
            taskDetail = getCommonTaskDetail(pluginData);
            taskDetail.skipReason = pluginData.SkipReason;
            save(name, "taskDetail");
        end
    end
end

function taskDetail = getCommonTaskDetail(pluginData)
    taskDetail = struct();
    taskDetail.name = pluginData.TaskResults.Name;
    taskDetail.description = pluginData.TaskGraph.Tasks.Description;
    taskDetail.failed = pluginData.TaskResults.Failed;
    taskDetail.skipped = pluginData.TaskResults.Skipped;
    taskDetail.duration = string(pluginData.TaskResults.Duration);
end
