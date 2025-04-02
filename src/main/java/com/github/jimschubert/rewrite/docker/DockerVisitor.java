package com.github.jimschubert.rewrite.docker;

import com.github.jimschubert.rewrite.docker.tree.Docker;
import com.github.jimschubert.rewrite.docker.tree.Space;
import org.openrewrite.TreeVisitor;

import java.util.List;

/**
 * A visitor for Docker LSTs.
 * Each visit method returns an abstract type. This visitor allows for rewriting the Dockerfile (e.g. replace an ARG instruction with an ENV instruction).
 * For the most part, you'll want to use {@link DockerIsoVisitor} when visiting the Dockerfile AST.
 * @see <a href="https://docs.openrewrite.org/concepts-and-explanations/visitors#isomorphic-vs-non-isomorphic-visitors">OpenRewrite docs: Visitor</a>
 * @param <P>
 */
public class DockerVisitor<P> extends TreeVisitor<Docker, P> {

    public Docker visitDocument(Docker.Document dockerfile, P ctx) {
        return dockerfile.withStages(dockerfile.getStages().stream()
                .map(s -> visitAndCast(s, ctx))
                .map(s -> (Docker.Stage)s)
                .toList());
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Docker visitStage(Docker.Stage stage, P p) {
        List<Docker.Instruction> children = stage.getChildren().stream()
                .map(c -> visitAndCast(c, p))
                .map(c -> (Docker.Instruction)c)
                .toList();

        return stage.withChildren(children)
                .withMarkers(visitMarkers(stage.getMarkers(), p));
    }

    public Docker visitFrom(Docker.From from, P p) {
        return from.withMarkers(visitMarkers(from.getMarkers(), p));
    }
    public Docker visitComment(Docker.Comment comment, P p) {
        return comment.withMarkers(visitMarkers(comment.getMarkers(), p));
    }

    public Docker visitRun(Docker.Run run, P p) {
        return run.withMarkers(visitMarkers(run.getMarkers(), p));
    }

    public Docker visitCmd(Docker.Cmd cmd, P p) {
        return cmd.withMarkers(visitMarkers(cmd.getMarkers(), p));
    }

    public Docker visitLabel(Docker.Label label, P p) {
        return label.withMarkers(visitMarkers(label.getMarkers(), p));
    }

    public Docker visitMaintainer(Docker.Maintainer maintainer, P p) {
        return maintainer.withMarkers(visitMarkers(maintainer.getMarkers(), p));
    }

    public Docker visitExpose(Docker.Expose expose, P p) {
        return expose.withMarkers(visitMarkers(expose.getMarkers(), p));
    }

    public Docker visitEnv(Docker.Env env, P p) {
        return env.withMarkers(visitMarkers(env.getMarkers(), p));
    }

    public Docker visitAdd(Docker.Add add, P p) {
        return add.withMarkers(visitMarkers(add.getMarkers(), p));
    }

    public Docker visitCopy(Docker.Copy copy, P p) {
        return copy.withMarkers(visitMarkers(copy.getMarkers(), p));
    }

    public Docker visitEntrypoint(Docker.Entrypoint entrypoint, P p) {
        return entrypoint.withMarkers(visitMarkers(entrypoint.getMarkers(), p));
    }

    public Docker visitVolume(Docker.Volume volume, P p) {
        return volume.withMarkers(visitMarkers(volume.getMarkers(), p));
    }

    public Docker visitUser(Docker.User user, P p) {
        return user.withMarkers(visitMarkers(user.getMarkers(), p));
    }

    public Docker visitWorkdir(Docker.Workdir workdir, P p) {
        return workdir.withMarkers(visitMarkers(workdir.getMarkers(), p));
    }

    public Docker visitArg(Docker.Arg arg, P p) {
        return arg.withMarkers(visitMarkers(arg.getMarkers(), p));
    }

    public Docker visitOnBuild(Docker.OnBuild onBuild, P p) {
        return onBuild.withMarkers(visitMarkers(onBuild.getMarkers(), p));
    }

    public Docker visitStopSignal(Docker.StopSignal stopSignal, P p) {
        return stopSignal.withMarkers(visitMarkers(stopSignal.getMarkers(), p));
    }

    public Docker visitHealthcheck(Docker.Healthcheck healthcheck, P p) {
        return healthcheck.withMarkers(visitMarkers(healthcheck.getMarkers(), p));
    }

    public Docker visitShell(Docker.Shell shell, P p) {
        return shell.withMarkers(visitMarkers(shell.getMarkers(), p));
    }

    public Docker visitDirective(Docker.Directive directive, P p) {
        return directive.withMarkers(visitMarkers(directive.getMarkers(), p));
    }

    public Docker visitLiteral(Docker.Literal literal, P p) {
        return literal.withMarkers(visitMarkers(literal.getMarkers(), p));
    }

    public Docker visitOption(Docker.Option option, P p) {
        return option.withMarkers(visitMarkers(option.getMarkers(), p));
    }
}
