package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Dockerfile;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.TreeVisitor;

import java.util.List;

public class DockerfileVisitor<P> extends TreeVisitor<Dockerfile, P> {

    public Dockerfile visitDocument(Dockerfile.Document dockerfile, P ctx) {
        return dockerfile.withStages(dockerfile.getStages().stream()
                .map(s -> visitAndCast(s, ctx))
                .map(s -> (Dockerfile.Stage)s)
                .toList());
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Dockerfile visitStage(Dockerfile.Stage stage, P p) {
        List<Dockerfile.Instruction> children = stage.getChildren().stream()
                .map(c -> visitAndCast(c, p))
                .map(c -> (Dockerfile.Instruction)c)
                .toList();

        return stage.withChildren(children)
                .withMarkers(visitMarkers(stage.getMarkers(), p));
    }

    public Dockerfile visitFrom(Dockerfile.From from, P p) {
        return from.withMarkers(visitMarkers(from.getMarkers(), p));
    }
    public Dockerfile visitComment(Dockerfile.Comment comment, P p) {
        return comment.withMarkers(visitMarkers(comment.getMarkers(), p));
    }

    public Dockerfile visitRun(Dockerfile.Run run, P p) {
        return run.withMarkers(visitMarkers(run.getMarkers(), p));
    }

    public Dockerfile visitCmd(Dockerfile.Cmd cmd, P p) {
        return cmd.withMarkers(visitMarkers(cmd.getMarkers(), p));
    }

    public Dockerfile visitLabel(Dockerfile.Label label, P p) {
        return label.withMarkers(visitMarkers(label.getMarkers(), p));
    }

    public Dockerfile visitMaintainer(Dockerfile.Maintainer maintainer, P p) {
        return maintainer.withMarkers(visitMarkers(maintainer.getMarkers(), p));
    }

    public Dockerfile visitExpose(Dockerfile.Expose expose, P p) {
        return expose.withMarkers(visitMarkers(expose.getMarkers(), p));
    }

    public Dockerfile visitEnv(Dockerfile.Env env, P p) {
        return env.withMarkers(visitMarkers(env.getMarkers(), p));
    }

    public Dockerfile visitAdd(Dockerfile.Add add, P p) {
        return add.withMarkers(visitMarkers(add.getMarkers(), p));
    }

    public Dockerfile visitCopy(Dockerfile.Copy copy, P p) {
        return copy.withMarkers(visitMarkers(copy.getMarkers(), p));
    }

    public Dockerfile visitEntrypoint(Dockerfile.Entrypoint entrypoint, P p) {
        return entrypoint.withMarkers(visitMarkers(entrypoint.getMarkers(), p));
    }

    public Dockerfile visitVolume(Dockerfile.Volume volume, P p) {
        return volume.withMarkers(visitMarkers(volume.getMarkers(), p));
    }

    public Dockerfile visitUser(Dockerfile.User user, P p) {
        return user.withMarkers(visitMarkers(user.getMarkers(), p));
    }

    public Dockerfile visitWorkdir(Dockerfile.Workdir workdir, P p) {
        return workdir.withMarkers(visitMarkers(workdir.getMarkers(), p));
    }

    public Dockerfile visitArg(Dockerfile.Arg arg, P p) {
        return arg.withMarkers(visitMarkers(arg.getMarkers(), p));
    }

    public Dockerfile visitOnBuild(Dockerfile.OnBuild onBuild, P p) {
        return onBuild.withMarkers(visitMarkers(onBuild.getMarkers(), p));
    }

    public Dockerfile visitStopSignal(Dockerfile.StopSignal stopSignal, P p) {
        return stopSignal.withMarkers(visitMarkers(stopSignal.getMarkers(), p));
    }

    public Dockerfile visitHealthcheck(Dockerfile.Healthcheck healthcheck, P p) {
        return healthcheck.withMarkers(visitMarkers(healthcheck.getMarkers(), p));
    }

    public Dockerfile visitShell(Dockerfile.Shell shell, P p) {
        return shell.withMarkers(visitMarkers(shell.getMarkers(), p));
    }

    public Dockerfile visitDirective(Dockerfile.Directive directive, P p) {
        return directive.withMarkers(visitMarkers(directive.getMarkers(), p));
    }

    public Dockerfile visitLiteral(Dockerfile.Literal literal, P p) {
        return literal.withMarkers(visitMarkers(literal.getMarkers(), p));
    }

    public Dockerfile visitOption(Dockerfile.Option option, P p) {
        return option.withMarkers(visitMarkers(option.getMarkers(), p));
    }
}
