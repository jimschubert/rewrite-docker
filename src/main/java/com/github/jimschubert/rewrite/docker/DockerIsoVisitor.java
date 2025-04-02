package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;

/**
 * An isomorphic visitor for Dockerfile ASTs.
 * Each visit method returns the same type as the input, but with the children visited.
 * This visitor should be preferred over {@link DockerVisitor} when visiting the Dockerfile LST.
 * @see <a href="https://docs.openrewrite.org/concepts-and-explanations/visitors#isomorphic-vs-non-isomorphic-visitors">OpenRewrite docs: Visitor</a>
 * @param <T>
 */
public class DockerIsoVisitor<T> extends DockerVisitor<T> {
    @Override
    public Docker.Document visitDocument(Docker.Document dockerfile, T ctx) {
        return (Docker.Document)super.visitDocument(dockerfile, ctx);
    }

    @Override
    public @Nullable Docker visit(@Nullable Tree tree, T t, Cursor parent) {
        tree = super.visit(tree, t, parent);
        if (tree == null) {
            return null;
        }
        if (tree instanceof Docker.Document) {
            return visitDocument((Docker.Document) tree, t);
        }

        return (Docker)tree;
    }

    @Override
    public Space visitSpace(Space space, T t) {
        return super.visitSpace(space, t);
    }

    @Override
    public Docker.Stage visitStage(Docker.Stage stage, T t) {
        return (Docker.Stage)super.visitStage(stage, t);
    }

    @Override
    public Docker.From visitFrom(Docker.From from, T t) {
        return (Docker.From)super.visitFrom(from, t);
    }

    @Override
    public Docker.Comment visitComment(Docker.Comment comment, T t) {
        return (Docker.Comment)super.visitComment(comment, t);
    }

    @Override
    public Docker.Directive visitDirective(Docker.Directive directive, T t) {
        return (Docker.Directive)super.visitDirective(directive, t);
    }

    @Override
    public Docker.Run visitRun(Docker.Run run, T t) {
        return (Docker.Run)super.visitRun(run, t);
    }

    @Override
    public Docker.Cmd visitCmd(Docker.Cmd cmd, T t) {
        return (Docker.Cmd)super.visitCmd(cmd, t);
    }

    @Override
    public Docker.Label visitLabel(Docker.Label label, T t) {
        return (Docker.Label)super.visitLabel(label, t);
    }

    @Override
    public Docker.Maintainer visitMaintainer(Docker.Maintainer maintainer, T t) {
        return (Docker.Maintainer)super.visitMaintainer(maintainer, t);
    }

    @Override
    public Docker.Expose visitExpose(Docker.Expose expose, T t) {
        return (Docker.Expose)super.visitExpose(expose, t);
    }

    @Override
    public Docker.Env visitEnv(Docker.Env env, T t) {
        return (Docker.Env)super.visitEnv(env, t);
    }

    @Override
    public Docker.Add visitAdd(Docker.Add add, T t) {
        return (Docker.Add)super.visitAdd(add, t);
    }

    @Override
    public Docker.Copy visitCopy(Docker.Copy copy, T t) {
        return (Docker.Copy)super.visitCopy(copy, t);
    }

    @Override
    public Docker.Entrypoint visitEntrypoint(Docker.Entrypoint entrypoint, T t) {
        return (Docker.Entrypoint)super.visitEntrypoint(entrypoint, t);
    }

    @Override
    public Docker.Volume visitVolume(Docker.Volume volume, T t) {
        return (Docker.Volume)super.visitVolume(volume, t);
    }

    @Override
    public Docker.User visitUser(Docker.User user, T t) {
        return (Docker.User)super.visitUser(user, t);
    }

    @Override
    public Docker.Workdir visitWorkdir(Docker.Workdir workdir, T t) {
        return (Docker.Workdir)super.visitWorkdir(workdir, t);
    }

    @Override
    public Docker.Arg visitArg(Docker.Arg arg, T t) {
        return (Docker.Arg)super.visitArg(arg, t);
    }

    @Override
    public Docker.OnBuild visitOnBuild(Docker.OnBuild onBuild, T t) {
        return (Docker.OnBuild)super.visitOnBuild(onBuild, t);
    }

    @Override
    public Docker.StopSignal visitStopSignal(Docker.StopSignal stopSignal, T t) {
        return (Docker.StopSignal)super.visitStopSignal(stopSignal, t);
    }

    @Override
    public Docker.Healthcheck visitHealthcheck(Docker.Healthcheck healthcheck, T t) {
        return (Docker.Healthcheck)super.visitHealthcheck(healthcheck, t);
    }

    @Override
    public Docker.Shell visitShell(Docker.Shell shell, T t) {
        return (Docker.Shell)super.visitShell(shell, t);
    }
}
