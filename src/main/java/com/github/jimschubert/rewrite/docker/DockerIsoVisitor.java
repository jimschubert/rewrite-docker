package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;

public class DockerIsoVisitor<T> extends DockerfileVisitor<T> {
    @Override
    public Dockerfile.Document visitDocument(Dockerfile.Document dockerfile, T ctx) {
        return (Dockerfile.Document)super.visitDocument(dockerfile, ctx);
    }

    @Override
    public @Nullable Dockerfile visit(@Nullable Tree tree, T t, Cursor parent) {
        tree = super.visit(tree, t, parent);
        if (tree == null) {
            return null;
        }
        if (tree instanceof Dockerfile.Document) {
            return visitDocument((Dockerfile.Document) tree, t);
        }

        return (Dockerfile)tree;
    }

    @Override
    public Space visitSpace(Space space, T t) {
        return super.visitSpace(space, t);
    }

    @Override
    public Dockerfile.Stage visitStage(Dockerfile.Stage stage, T t) {
        return (Dockerfile.Stage)super.visitStage(stage, t);
    }

    @Override
    public Dockerfile.From visitFrom(Dockerfile.From from, T t) {
        return (Dockerfile.From)super.visitFrom(from, t);
    }

    @Override
    public Dockerfile.Comment visitComment(Dockerfile.Comment comment, T t) {
        return (Dockerfile.Comment)super.visitComment(comment, t);
    }

    @Override
    public Dockerfile.Directive visitDirective(Dockerfile.Directive directive, T t) {
        return (Dockerfile.Directive)super.visitDirective(directive, t);
    }

    @Override
    public Dockerfile.Run visitRun(Dockerfile.Run run, T t) {
        return (Dockerfile.Run)super.visitRun(run, t);
    }

    @Override
    public Dockerfile.Cmd visitCmd(Dockerfile.Cmd cmd, T t) {
        return (Dockerfile.Cmd)super.visitCmd(cmd, t);
    }

    @Override
    public Dockerfile.Label visitLabel(Dockerfile.Label label, T t) {
        return (Dockerfile.Label)super.visitLabel(label, t);
    }

    @Override
    public Dockerfile.Maintainer visitMaintainer(Dockerfile.Maintainer maintainer, T t) {
        return (Dockerfile.Maintainer)super.visitMaintainer(maintainer, t);
    }

    @Override
    public Dockerfile.Expose visitExpose(Dockerfile.Expose expose, T t) {
        return (Dockerfile.Expose)super.visitExpose(expose, t);
    }

    @Override
    public Dockerfile.Env visitEnv(Dockerfile.Env env, T t) {
        return (Dockerfile.Env)super.visitEnv(env, t);
    }

    @Override
    public Dockerfile.Add visitAdd(Dockerfile.Add add, T t) {
        return (Dockerfile.Add)super.visitAdd(add, t);
    }

    @Override
    public Dockerfile.Copy visitCopy(Dockerfile.Copy copy, T t) {
        return (Dockerfile.Copy)super.visitCopy(copy, t);
    }

    @Override
    public Dockerfile.Entrypoint visitEntrypoint(Dockerfile.Entrypoint entrypoint, T t) {
        return (Dockerfile.Entrypoint)super.visitEntrypoint(entrypoint, t);
    }

    @Override
    public Dockerfile.Volume visitVolume(Dockerfile.Volume volume, T t) {
        return (Dockerfile.Volume)super.visitVolume(volume, t);
    }

    @Override
    public Dockerfile.User visitUser(Dockerfile.User user, T t) {
        return (Dockerfile.User)super.visitUser(user, t);
    }

    @Override
    public Dockerfile.Workdir visitWorkdir(Dockerfile.Workdir workdir, T t) {
        return (Dockerfile.Workdir)super.visitWorkdir(workdir, t);
    }

    @Override
    public Dockerfile.Arg visitArg(Dockerfile.Arg arg, T t) {
        return (Dockerfile.Arg)super.visitArg(arg, t);
    }

    @Override
    public Dockerfile.OnBuild visitOnBuild(Dockerfile.OnBuild onBuild, T t) {
        return (Dockerfile.OnBuild)super.visitOnBuild(onBuild, t);
    }

    @Override
    public Dockerfile.StopSignal visitStopSignal(Dockerfile.StopSignal stopSignal, T t) {
        return (Dockerfile.StopSignal)super.visitStopSignal(stopSignal, t);
    }

    @Override
    public Dockerfile.Healthcheck visitHealthcheck(Dockerfile.Healthcheck healthcheck, T t) {
        return (Dockerfile.Healthcheck)super.visitHealthcheck(healthcheck, t);
    }

    @Override
    public Dockerfile.Shell visitShell(Dockerfile.Shell shell, T t) {
        return (Dockerfile.Shell)super.visitShell(shell, t);
    }
}
