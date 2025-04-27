/*
 * Copyright (c) 2025 Jim Schubert
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jimschubert.rewrite.docker.format;

import com.github.jimschubert.rewrite.docker.DockerIsoVisitor;
import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.InstructionName;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

@Value
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class UppercaseInstructionNames extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Uppercase instruction names";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Uppercase instruction names in Dockerfile to match Dockerfile best practices.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext executionContext) {
                return super.visitFrom(from.withMarkers(from.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Run visitRun(Docker.Run run, ExecutionContext executionContext) {
                return super.visitRun(run.withMarkers(run.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Cmd visitCmd(Docker.Cmd cmd, ExecutionContext executionContext) {
                return super.visitCmd(cmd.withMarkers(cmd.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Label visitLabel(Docker.Label label, ExecutionContext executionContext) {
                return super.visitLabel(label.withMarkers(label.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Maintainer visitMaintainer(Docker.Maintainer maintainer, ExecutionContext executionContext) {
                return super.visitMaintainer(maintainer.withMarkers(maintainer.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Expose visitExpose(Docker.Expose expose, ExecutionContext executionContext) {
                return super.visitExpose(expose.withMarkers(expose.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Env visitEnv(Docker.Env env, ExecutionContext executionContext) {
                return super.visitEnv(env.withMarkers(env.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Add visitAdd(Docker.Add add, ExecutionContext executionContext) {
                return super.visitAdd(add.withMarkers(add.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Copy visitCopy(Docker.Copy copy, ExecutionContext executionContext) {
                return super.visitCopy(copy.withMarkers(copy.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Entrypoint visitEntrypoint(Docker.Entrypoint entrypoint, ExecutionContext executionContext) {
                return super.visitEntrypoint(entrypoint.withMarkers(entrypoint.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Volume visitVolume(Docker.Volume volume, ExecutionContext executionContext) {
                return super.visitVolume(volume.withMarkers(volume.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.User visitUser(Docker.User user, ExecutionContext executionContext) {
                return super.visitUser(user.withMarkers(user.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Workdir visitWorkdir(Docker.Workdir workdir, ExecutionContext executionContext) {
                return super.visitWorkdir(workdir.withMarkers(workdir.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Arg visitArg(Docker.Arg arg, ExecutionContext executionContext) {
                return super.visitArg(arg.withMarkers(arg.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.OnBuild visitOnBuild(Docker.OnBuild onBuild, ExecutionContext executionContext) {
                return super.visitOnBuild(onBuild.withMarkers(onBuild.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.StopSignal visitStopSignal(Docker.StopSignal stopSignal, ExecutionContext executionContext) {
                return super.visitStopSignal(stopSignal.withMarkers(stopSignal.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Healthcheck visitHealthcheck(Docker.Healthcheck healthcheck, ExecutionContext executionContext) {
                return super.visitHealthcheck(healthcheck.withMarkers(healthcheck.getMarkers().removeByType(InstructionName.class)), executionContext);
            }

            @Override
            public Docker.Shell visitShell(Docker.Shell shell, ExecutionContext executionContext) {
                return super.visitShell(shell.withMarkers(shell.getMarkers().removeByType(InstructionName.class)), executionContext);
            }
        };
    }
}
