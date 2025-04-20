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
package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.DockerRightPadded;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

/**
 * A visitor for Docker LSTs.
 * Each visit method returns an abstract type. This visitor allows for rewriting the Dockerfile (e.g. replace an ARG instruction with an ENV instruction).
 * For the most part, you'll want to use {@link DockerIsoVisitor} when visiting the Dockerfile AST.
 *
 * @param <P>
 * @see <a href="https://docs.openrewrite.org/concepts-and-explanations/visitors#isomorphic-vs-non-isomorphic-visitors">OpenRewrite docs: Visitor</a>
 */
public class DockerVisitor<P> extends TreeVisitor<Docker, P> {
    @Override
    public @Nullable String getLanguage() {
        return "Dockerfile";
    }

    public Docker visitDocument(Docker.Document dockerfile, P ctx) {
        return dockerfile.withStages(ListUtils.map(dockerfile.getStages(), s -> visitAndCast(s, ctx)))
                .withMarkers(visitMarkers(dockerfile.getMarkers(), ctx));
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Docker visitStage(Docker.Stage stage, P p) {
        return stage.withChildren(ListUtils.map(stage.getChildren(), c -> visitAndCast(c, p)))
                .withMarkers(visitMarkers(stage.getMarkers(), p));
    }

    public Docker visitFrom(Docker.From from, P p) {
        return from.withPrefix(visitSpace(from.getPrefix(), p))
                .withImage(visitAndCast(from.getImage(), p))
                .withPlatform(visitAndCast(from.getPlatform(), p))
                .withVersion(visitAndCast(from.getVersion(), p))
                .withAs(visitAndCast(from.getAs(), p))
                .withAlias(visitAndCast(from.getAlias(), p))
                .withMarkers(visitMarkers(from.getMarkers(), p));
    }

    public Docker visitComment(Docker.Comment comment, P p) {
        return comment.withPrefix(visitSpace(comment.getPrefix(), p))
                .withText(comment.getText()) // Assuming the text doesn't need visiting
                .withMarkers(visitMarkers(comment.getMarkers(), p));
    }

    public Docker visitRun(Docker.Run run, P p) {
        return run
                .withPrefix(visitSpace(run.getPrefix(), p))
                .withOptions(ListUtils.map(run.getOptions(), o -> visitDockerRightPadded(o, p)))
                .withCommands(ListUtils.map(run.getCommands(), c -> visitDockerRightPadded(c, p)))
                .withMarkers(visitMarkers(run.getMarkers(), p));
    }

    public Docker visitCmd(Docker.Cmd cmd, P p) {
        return cmd
                .withForm(cmd.getForm())
                .withPrefix(visitSpace(cmd.getPrefix(), p))
                .withExecFormPrefix(visitSpace(cmd.getExecFormPrefix(), p))
                .withCommands(ListUtils.map(cmd.getCommands(), c -> visitDockerRightPadded(c, p)))
                .withExecFormSuffix(visitSpace(cmd.getExecFormSuffix(), p))
                .withMarkers(visitMarkers(cmd.getMarkers(), p));
    }

    public Docker visitLabel(Docker.Label label, P p) {
        return label
                .withPrefix(visitSpace(label.getPrefix(), p))
                .withArgs(ListUtils.map(label.getArgs(), a -> visitDockerRightPadded(a, p)))
                .withMarkers(visitMarkers(label.getMarkers(), p));
    }

    public Docker visitMaintainer(Docker.Maintainer maintainer, P p) {
        return maintainer
                .withQuoting(maintainer.getQuoting()) // Assuming quoting doesn't need visiting
                .withPrefix(visitSpace(maintainer.getPrefix(), p))
                .withName(visitAndCast(maintainer.getName(), p))
                .withMarkers(visitMarkers(maintainer.getMarkers(), p));
    }

    public Docker visitExpose(Docker.Expose expose, P p) {
        return expose
                .withPrefix(visitSpace(expose.getPrefix(), p))
                .withPorts(ListUtils.map(expose.getPorts(), port -> visitDockerRightPadded(port, p)))
                .withMarkers(visitMarkers(expose.getMarkers(), p));
    }

    public Docker visitEnv(Docker.Env env, P p) {
        return env
                .withPrefix(visitSpace(env.getPrefix(), p))
                .withArgs(ListUtils.map(env.getArgs(), a -> visitDockerRightPadded(a, p)))
                .withMarkers(visitMarkers(env.getMarkers(), p));
    }

    public Docker visitAdd(Docker.Add add, P p) {
        return add
                .withPrefix(visitSpace(add.getPrefix(), p))
                .withOptions(ListUtils.map(add.getOptions(), o -> visitDockerRightPadded(o, p)))
                .withSources(ListUtils.map(add.getSources(), s -> visitDockerRightPadded(s, p)))
                .withDestination(visitDockerRightPadded(add.getDestination(), p))
                .withMarkers(visitMarkers(add.getMarkers(), p));
    }

    public Docker visitCopy(Docker.Copy copy, P p) {
        return copy
                .withPrefix(visitSpace(copy.getPrefix(), p))
                .withOptions(ListUtils.map(copy.getOptions(), o -> visitDockerRightPadded(o, p)))
                .withSources(ListUtils.map(copy.getSources(), s -> visitDockerRightPadded(s, p)))
                .withDestination(visitDockerRightPadded(copy.getDestination(), p))
                .withMarkers(visitMarkers(copy.getMarkers(), p));
    }

    public Docker visitEntrypoint(Docker.Entrypoint entrypoint, P p) {
        return entrypoint
                .withForm(entrypoint.getForm())
                .withPrefix(visitSpace(entrypoint.getPrefix(), p))
                .withExecFormPrefix(visitSpace(entrypoint.getExecFormPrefix(), p))
                .withCommands(ListUtils.map(entrypoint.getCommands(), c -> visitDockerRightPadded(c, p)))
                .withExecFormSuffix(visitSpace(entrypoint.getExecFormSuffix(), p))
                .withMarkers(visitMarkers(entrypoint.getMarkers(), p));
    }

    public Docker visitVolume(Docker.Volume volume, P p) {
        return volume
                .withForm(volume.getForm())
                .withPrefix(visitSpace(volume.getPrefix(), p))
                .withExecFormPrefix(visitSpace(volume.getExecFormPrefix(), p))
                .withPaths(ListUtils.map(volume.getPaths(), path -> visitDockerRightPadded(path, p)))
                .withExecFormSuffix(visitSpace(volume.getExecFormSuffix(), p))
                .withMarkers(visitMarkers(volume.getMarkers(), p));
    }

    public Docker visitUser(Docker.User user, P p) {
        return user
                .withPrefix(visitSpace(user.getPrefix(), p))
                .withUsername(visitAndCast(user.getUsername(), p))
                .withGroup(visitAndCast(user.getGroup(), p))
                .withMarkers(visitMarkers(user.getMarkers(), p));
    }

    public Docker visitWorkdir(Docker.Workdir workdir, P p) {
        return workdir
                .withPrefix(visitSpace(workdir.getPrefix(), p))
                .withPath(visitAndCast(workdir.getPath(), p))
                .withMarkers(visitMarkers(workdir.getMarkers(), p));
    }

    public Docker visitArg(Docker.Arg arg, P p) {
        return arg
                .withPrefix(visitSpace(arg.getPrefix(), p))
                .withArgs(ListUtils.map(arg.getArgs(), a -> visitDockerRightPadded(a, p)))
                .withMarkers(visitMarkers(arg.getMarkers(), p));
    }

    public Docker visitOnBuild(Docker.OnBuild onBuild, P p) {
        return onBuild
                .withPrefix(visitSpace(onBuild.getPrefix(), p))
                .withInstruction(visitAndCast(onBuild.getInstruction(), p))
                .withTrailing(visitSpace(onBuild.getTrailing(), p))
                .withMarkers(visitMarkers(onBuild.getMarkers(), p));
    }

    public Docker visitStopSignal(Docker.StopSignal stopSignal, P p) {
        return stopSignal
                .withPrefix(visitSpace(stopSignal.getPrefix(), p))
                .withSignal(visitAndCast(stopSignal.getSignal(), p))
                .withMarkers(visitMarkers(stopSignal.getMarkers(), p));
    }

    public Docker visitHealthcheck(Docker.Healthcheck healthcheck, P p) {
        return healthcheck
                .withPrefix(visitSpace(healthcheck.getPrefix(), p))
                .withType(healthcheck.getType())
                .withOptions(ListUtils.map(healthcheck.getOptions(), o -> visitDockerRightPadded(o, p)))
                .withCommands(ListUtils.map(healthcheck.getCommands(), c -> visitDockerRightPadded(c, p)))
                .withMarkers(visitMarkers(healthcheck.getMarkers(), p));
    }

    public Docker visitShell(Docker.Shell shell, P p) {
        return shell
                .withPrefix(visitSpace(shell.getPrefix(), p))
                .withExecFormPrefix(visitSpace(shell.getExecFormPrefix(), p))
                .withCommands(ListUtils.map(shell.getCommands(), c -> visitDockerRightPadded(c, p)))
                .withExecFormSuffix(visitSpace(shell.getExecFormSuffix(), p))
                .withMarkers(visitMarkers(shell.getMarkers(), p));
    }

    public Docker visitDirective(Docker.Directive directive, P p) {
        return directive.withPrefix(visitSpace(directive.getPrefix(), p))
                .withDirective(visitDockerRightPadded(directive.getDirective(), p)) // Assuming the name doesn't need visiting
                .withMarkers(visitMarkers(directive.getMarkers(), p));
    }

    public Docker visitLiteral(Docker.Literal literal, P p) {
        return literal.withPrefix(visitSpace(literal.getPrefix(), p))
                .withQuoting(literal.getQuoting()) // Assuming quoting doesn't need visiting
                .withMarkers(visitMarkers(literal.getMarkers(), p));
    }

    public Docker visitOption(Docker.Option option, P p) {
        return option.withPrefix(visitSpace(option.getPrefix(), p))
                .withKeyArgs(option.getKeyArgs()) // Assuming the key doesn't need visitingx
                .withMarkers(visitMarkers(option.getMarkers(), p));
    }

    public <T> DockerRightPadded<T> visitDockerRightPadded(DockerRightPadded<T> right, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Docker) {
            //noinspection unchecked
            t = visitAndCast((Docker) right.getElement(), p);
        } else if (t instanceof Docker.KeyArgs) {
            Docker.KeyArgs args = (Docker.KeyArgs) right.getElement();
            //noinspection unchecked
            t = (T) args.withPrefix(visitSpace(args.getPrefix(), p))
                    .withKey(visitAndCast(args.getKey(), p))
                    .withValue(visitAndCast(args.getValue(), p))
                    .withQuoting(args.getQuoting());
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space after = visitSpace(right.getAfter(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return (after == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ?
                right : new DockerRightPadded<>(t, after, markers);
    }
}
