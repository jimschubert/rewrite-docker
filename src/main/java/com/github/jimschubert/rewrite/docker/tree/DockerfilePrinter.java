package com.github.jimschubert.rewrite.docker.tree;

import com.github.jimschubert.rewrite.docker.DockerfileVisitor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.function.UnaryOperator;

public class DockerfilePrinter<P> extends DockerfileVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    protected void beforeSyntax(Dockerfile t, PrintOutputCapture<P> p) {
        beforeSyntax(t.getPrefix(), t.getMarkers(), p);
    }

    @Override
    public Markers visitMarkers(@Nullable Markers markers, PrintOutputCapture<P> p) {
        if (markers == null) {
            return Markers.EMPTY;
        }
        return super.visitMarkers(markers, p);
    }

    protected void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        if (markers == null || markers.getMarkers().isEmpty()) {
            return;
        }
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    protected void afterSyntax(Dockerfile t, PrintOutputCapture<P> p) {
        afterSyntax(t.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        if (markers == null) {
            return;
        }
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    @Override
    public @Nullable Dockerfile visit(@Nullable Tree tree, PrintOutputCapture<P> p, Cursor parent) {
        if (tree == null) {
            return null;
        }
        Dockerfile.Document target = null;
        if (tree instanceof Dockerfile.Document) {
            target = (Dockerfile.Document) tree;
        } else if (tree instanceof Dockerfile) {
            target = (Dockerfile.Document) tree;
        } else {
            return null;
        }

        for (Dockerfile.Stage stage : target.getStages()) {
            visit(stage, p);
        }
        return super.visit(tree, p, parent);
    }

    @Override
    public Dockerfile visitStage(Dockerfile.Stage stage, PrintOutputCapture<P> p) {
        beforeSyntax(stage, p);
        for (Dockerfile.Instruction instruction : stage.getChildren()) {
            visit(instruction, p);
        }
        afterSyntax(stage, p);
        return stage;
    }

    @Override
    public Dockerfile visitFrom(Dockerfile.From from, PrintOutputCapture<P> p) {
        beforeSyntax(from, p);
        p.append("FROM ");
        // TODO: update with appropriate options
        p.append(from.getImageSpec());
        if (from.getAlias() != null) {
            p.append(" AS ");
            p.append(from.getAlias());
        }
        afterSyntax(from, p);
        return from;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        if (space == null) {
            return Space.EMPTY;
        }
        if (space.getWhitespace() != null) {
            p.append(space.getWhitespace());
        }
        return space;
    }

    @Override
    public Dockerfile visitComment(Dockerfile.Comment comment, PrintOutputCapture<P> p) {
        beforeSyntax(comment, p);
        p.append("# ");
        p.append(comment.getText().replace("\n", "\n# "));
        afterSyntax(comment, p);
        return comment;
    }

    @Override
    public Dockerfile visitRun(Dockerfile.Run run, PrintOutputCapture<P> p) {
        beforeSyntax(run, p);
        p.append("RUN ");
        // TODO: update with appropriate options
        p.append(run.getCommand());
        afterSyntax(run, p);
        return run;
    }

    @Override
    public Dockerfile visitCmd(Dockerfile.Cmd cmd, PrintOutputCapture<P> p) {
        beforeSyntax(cmd, p);
        p.append("CMD ");
        p.append(cmd.getCommand());
        afterSyntax(cmd, p);
        return cmd;
    }

    @Override
    public Dockerfile visitLabel(Dockerfile.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        p.append("LABEL");
        for (KeyArgs kvp : label.getArgs()) {
            p.append(" ").append(kvp.getKey());
            if (kvp.isHasEquals()) {
                p.append("=");
            }
            if (kvp.getQuoting() == Quoting.SINGLE_QUOTED) {
                p.append("'").append(kvp.getValue()).append("'");
            } else if (kvp.getQuoting() == Quoting.DOUBLE_QUOTED) {
                p.append("\"").append(kvp.getValue()).append("\"");
            } else {
                p.append(kvp.getValue());
            }
        }
        afterSyntax(label, p);
        return label;
    }

    @Override
    public Dockerfile visitMaintainer(Dockerfile.Maintainer maintainer, PrintOutputCapture<P> p) {
        beforeSyntax(maintainer, p);
        p.append("MAINTAINER ");
        p.append(maintainer.getName());
        afterSyntax(maintainer, p);
        return maintainer;
    }

    @Override
    public Dockerfile visitExpose(Dockerfile.Expose expose, PrintOutputCapture<P> p) {
        beforeSyntax(expose, p);
        p.append("EXPOSE ");
        p.append(expose.getPort());
        afterSyntax(expose, p);
        return expose;
    }

    @Override
    public Dockerfile visitEnv(Dockerfile.Env env, PrintOutputCapture<P> p) {
        beforeSyntax(env, p);
        p.append("ENV ");
        for (KeyArgs kvp : env.getArgs()) {
            p.append(kvp.getKey());
            if (kvp.isHasEquals()) {
                p.append("=");
            }
            if (kvp.getQuoting() == Quoting.SINGLE_QUOTED) {
                p.append("'").append(kvp.getValue()).append("'");
            } else if (kvp.getQuoting() == Quoting.DOUBLE_QUOTED) {
                p.append("\"").append(kvp.getValue()).append("\"");
            } else {
                p.append(kvp.getValue());
            }
            p.append(" ");
        }
        afterSyntax(env, p);
        return env;
    }

    @Override
    public Dockerfile visitAdd(Dockerfile.Add add, PrintOutputCapture<P> p) {
        beforeSyntax(add, p);
        p.append("ADD ");
        // TODO: update with appropriate options
        p.append(add.getSource());
        p.append(" ");
        p.append(add.getDestination());
        afterSyntax(add, p);
        return add;
    }

    @Override
    public Dockerfile visitCopy(Dockerfile.Copy copy, PrintOutputCapture<P> p) {
        beforeSyntax(copy, p);
        p.append("COPY ");
        // TODO: update with appropriate options
        p.append(copy.getSource());
        p.append(" ");
        p.append(copy.getDestination());
        afterSyntax(copy, p);
        return copy;
    }

    @Override
    public Dockerfile visitEntrypoint(Dockerfile.Entrypoint entrypoint, PrintOutputCapture<P> p) {
        beforeSyntax(entrypoint, p);
        p.append("ENTRYPOINT [");
        for (int i = 0; i < entrypoint.getCommand().size(); i++) {
            p.append("\"").append(entrypoint.getCommand().get(i)).append("\"");
            if (i < entrypoint.getCommand().size() - 1) {
                p.append(", ");
            }
        }
        p.append("]");
        afterSyntax(entrypoint, p);
        return entrypoint;
    }

    @Override
    public Dockerfile visitVolume(Dockerfile.Volume volume, PrintOutputCapture<P> p) {
        beforeSyntax(volume, p);
        p.append("VOLUME ");
        // TODO: update with appropriate options
        p.append(volume.getPath());
        afterSyntax(volume, p);
        return volume;
    }

    @Override
    public Dockerfile visitUser(Dockerfile.User user, PrintOutputCapture<P> p) {
        beforeSyntax(user, p);
        p.append("USER ");
        p.append(user.getUsername());
        if (user.getGroup() != null && !user.getGroup().isEmpty()) {
            p.append(":");
            p.append(user.getGroup());
        }
        afterSyntax(user, p);
        return user;
    }

    @Override
    public Dockerfile visitWorkdir(Dockerfile.Workdir workdir, PrintOutputCapture<P> p) {
        beforeSyntax(workdir, p);
        p.append("WORKDIR ");
        p.append(workdir.getPath());
        afterSyntax(workdir, p);
        return workdir;
    }

    @Override
    public Dockerfile visitArg(Dockerfile.Arg arg, PrintOutputCapture<P> p) {
        beforeSyntax(arg, p);
        p.append("ARG ");
        for (KeyArgs kvp : arg.getArgs()) {
            p.append(kvp.getKey());
            if (kvp.isHasEquals()) {
                p.append("=");
            }
            if (kvp.getQuoting() == Quoting.SINGLE_QUOTED) {
                p.append("'").append(kvp.getValue()).append("'");
            } else if (kvp.getQuoting() == Quoting.DOUBLE_QUOTED) {
                p.append("\"").append(kvp.getValue()).append("\"");
            } else {
                p.append(kvp.getValue());
            }
            p.append(" ");
        }

        afterSyntax(arg, p);
        return arg;
    }

    @Override
    public Dockerfile visitOnBuild(Dockerfile.OnBuild onBuild, PrintOutputCapture<P> p) {
        beforeSyntax(onBuild, p);
        p.append("ONBUILD ");
        visit(onBuild.getInstruction(), p);
        afterSyntax(onBuild, p);
        return onBuild;
    }

    @Override
    public Dockerfile visitStopSignal(Dockerfile.StopSignal stopSignal, PrintOutputCapture<P> p) {
        beforeSyntax(stopSignal, p);
        p.append("STOPSIGNAL ");
        p.append(stopSignal.getSignal());
        afterSyntax(stopSignal, p);
        return stopSignal;
    }

    @Override
    public Dockerfile visitHealthcheck(Dockerfile.Healthcheck healthcheck, PrintOutputCapture<P> p) {
        beforeSyntax(healthcheck, p);
        p.append("HEALTHCHECK ");
        p.append(healthcheck.getCommand());
        afterSyntax(healthcheck, p);
        return healthcheck;
    }

    @Override
    public Dockerfile visitShell(Dockerfile.Shell shell, PrintOutputCapture<P> p) {
        beforeSyntax(shell, p);
        p.append("SHELL ");
        p.append("[");
        for (int i = 0; i < shell.getCommands().size(); i++) {
            p.append("\"").append(shell.getCommands().get(i)).append("\"");
            if (i < shell.getCommands().size() - 1) {
                p.append(", ");
            }
        }
        p.append("]");
        afterSyntax(shell, p);
        return shell;
    }
}