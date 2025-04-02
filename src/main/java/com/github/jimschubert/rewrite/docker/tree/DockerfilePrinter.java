package com.github.jimschubert.rewrite.docker.tree;

import com.github.jimschubert.rewrite.docker.DockerfileVisitor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
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

        List<Dockerfile.Stage> stages = target.getStages();
        for (int i = 0; i < stages.size(); i++) {
            Dockerfile.Stage stage = stages.get(i);
            visit(stage, p);
            if (i < stages.size() - 1) {
                p.append("\n");
            }
        }
        return super.visit(tree, p, parent);
    }

    @Override
    public Dockerfile visitStage(Dockerfile.Stage stage, PrintOutputCapture<P> p) {
        beforeSyntax(stage, p);
        List<Dockerfile.Instruction> children = stage.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Dockerfile.Instruction instruction = children.get(i);
            visit(instruction, p);
            if (i < children.size() - 1) {
                p.append("\n");
            }
        }
        afterSyntax(stage, p);
        return stage;
    }

    @Override
    public Dockerfile visitFrom(Dockerfile.From from, PrintOutputCapture<P> p) {
        beforeSyntax(from, p);
        p.append("FROM");
        if (from.getPlatform() != null && from.getPlatform().getElement().getText() != null) {
            p.append(" --platform=");
            p.append(from.getPlatform().getElement().getText());
            p.append(from.getPlatform().getAfter().getWhitespace());
        }

        appendRightPaddedLiteral(from.getImage(), p);
        appendRightPaddedLiteral(from.getVersion(), p);

        if (from.getAlias() != null && from.getAlias().getElement().getText() != null && !from.getAlias().getElement().getText().isEmpty()) {
            p.append(from.getAs().getPrefix().getWhitespace());
            p.append(from.getAs().getText());
            appendRightPaddedLiteral(from.getAlias(), p);
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
        Dockerfile.Literal literal = comment.getText().getElement();
        if (literal != null) {
            beforeSyntax(comment, p);
            p.append("#");
            p.append(literal.getPrefix().getWhitespace());
            p.append(literal.getText().replace("\n", "\n# "));
            p.append(comment.getText().getAfter().getWhitespace());
            afterSyntax(comment, p);
        }
        return comment;
    }

    @Override
    public Dockerfile visitDirective(Dockerfile.Directive directive, PrintOutputCapture<P> p) {
        beforeSyntax(directive, p);
        p.append("#");
        DockerfileRightPadded<Dockerfile.KeyArgs> padded = directive.getDirective();
        Dockerfile.KeyArgs keyArgs = padded.getElement();
        p.append(keyArgs.getPrefix().getWhitespace());
        p.append(keyArgs.getKey());
        if (keyArgs.isHasEquals()) {
            p.append("=");
        }
        if (keyArgs.getQuoting() == Quoting.SINGLE_QUOTED) {
            p.append("'").append(keyArgs.getValue()).append("'");
        } else if (keyArgs.getQuoting() == Quoting.DOUBLE_QUOTED) {
            p.append("\"").append(keyArgs.getValue()).append("\"");
        } else {
            p.append(keyArgs.getValue());
        }
        p.append(padded.getAfter().getWhitespace());
        afterSyntax(directive, p);
        return directive;
    }

    @Override
    public Dockerfile visitRun(Dockerfile.Run run, PrintOutputCapture<P> p) {
        beforeSyntax(run, p);
        p.append("RUN ");
        if (run.getMounts() != null) {
            run.getMounts().forEach(m -> {
                p.append("--mount=type=");
                p.append(m.getType());
                if (m.getOptions() != null) {
                    m.getOptions().forEach(
                            o -> {
                                p.append(",");
                                p.append(o.getKey());
                                p.append("=");
                                p.append(o.getValue());
                            }
                    );
                }
            });
        }

        if (run.getNetworkOption() != null) {
            p.append("--network=");
            p.append(run.getNetworkOption().getName());
        }

        if (run.getCommands() != null) {
            for (int i = 0; i < run.getCommands().size(); i++) {
                p.append(run.getCommands().get(i));
                if (i < run.getCommands().size() - 1) {
                    // TODO: retain multiline and indents
                    p.append(" && ");
                }
            }
        }

        if (run.getHeredoc() != null && !run.getHeredoc().isEmpty()) {
            p.append(" <<" + run.getHeredocName() + "\n");
            p.append(run.getHeredoc());
            p.append("\n");
            p.append(run.getHeredocName());
        }

        afterSyntax(run, p);
        return run;
    }

    @Override
    public Dockerfile visitCmd(Dockerfile.Cmd cmd, PrintOutputCapture<P> p) {
        beforeSyntax(cmd, p);
        p.append("CMD");
        Form form = cmd.getForm();
        if (form == null) {
            form = Form.EXEC;
        }
        if (form == Form.EXEC) {
            p.append(" [");
        }

        for (int i = 0; i < cmd.getCommands().size(); i++) {
            DockerfileRightPadded<Dockerfile.Literal> padded = cmd.getCommands().get(i);
            Dockerfile.Literal literal = padded.getElement();
            String text = literal.getText();
            if (form == Form.EXEC) {
                text = trimDoubleQuotes(text);
                p.append(literal.getPrefix().getWhitespace());
                p.append("\"").append(text).append("\"");
            } else {
                p.append(text);
            }
            p.append(padded.getAfter().getWhitespace());
        }

        if (form == Form.EXEC) {
            p.append("]");
        }
        afterSyntax(cmd, p);
        return cmd;
    }

    private static @NotNull String trimDoubleQuotes(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public Dockerfile visitLabel(Dockerfile.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        p.append("LABEL");
        for (Dockerfile.KeyArgs kvp : label.getArgs()) {
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
        p.append("EXPOSE");
        for (int i = 0; i < expose.getPorts().size(); i++) {
            DockerfileRightPadded<Dockerfile.Port> padded = expose.getPorts().get(i);
            Dockerfile.Port port = padded.getElement();
            p.append(port.getPrefix().getWhitespace());
            p.append(port.getPort());
            if (port.isProtocolProvided() && port.getProtocol() != null && !port.getProtocol().isEmpty()) {
                p.append("/");
                p.append(port.getProtocol());
            }
            p.append(padded.getAfter().getWhitespace());
        }
        afterSyntax(expose, p);
        return expose;
    }

    @Override
    public Dockerfile visitEnv(Dockerfile.Env env, PrintOutputCapture<P> p) {
        beforeSyntax(env, p);
        p.append("ENV");

        for (DockerfileRightPadded<Dockerfile.KeyArgs> padded : env.getArgs()) {
            Dockerfile.KeyArgs kvp = padded.getElement();
            p.append(kvp.getPrefix().getWhitespace());
            p.append(kvp.getKey());
            if (kvp.getValue() != null && !kvp.getValue().isEmpty()) {
                if (kvp.isHasEquals()) {
                    p.append("=");
                } else {
                    p.append(" ");
                }

                if (kvp.getQuoting() == Quoting.SINGLE_QUOTED) {
                    p.append("'").append(kvp.getValue()).append("'");
                } else if (kvp.getQuoting() == Quoting.DOUBLE_QUOTED) {
                    p.append("\"").append(kvp.getValue()).append("\"");
                } else {
                    p.append(kvp.getValue());
                }
            }

            p.append(padded.getAfter().getWhitespace());
        }
        afterSyntax(env, p);
        return env;
    }

    @Override
    public Dockerfile visitAdd(Dockerfile.Add add, PrintOutputCapture<P> p) {
        beforeSyntax(add, p);
        p.append("ADD");

        if (add.getOptions() != null) {
            add.getOptions().forEach(o -> {
                    Dockerfile.Option element = o.getElement();
                    if (element.getName() != null && !element.getName().isEmpty()) {
                        p.append(element.getPrefix().getWhitespace());
                        p.append("--");
                        p.append(element.getName());
                        if (element.getKeyArgs() != null) {
                            p.append("=");
                            for (Dockerfile.KeyArgs kvp : element.getKeyArgs()) {
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
                        }
                    }
                }
            );
        }

        if (add.getSources() != null ) {
            for (int i = 0; i < add.getSources().size(); i++) {
                DockerfileRightPadded<Dockerfile.Literal> literalPadded = add.getSources().get(i);
                Dockerfile.Literal literal = literalPadded.getElement();
                p.append(literal.getPrefix().getWhitespace());
                p.append(literal.getText());
                p.append(literalPadded.getAfter().getWhitespace());
            }
        }

        if (add.getDestination() != null) {
            DockerfileRightPadded<Dockerfile.Literal> literalPadded = add.getDestination();
            Dockerfile.Literal literal = literalPadded.getElement();
            p.append(literal.getPrefix().getWhitespace());
            p.append(literal.getText());
            p.append(literalPadded.getAfter().getWhitespace());
        }

        afterSyntax(add, p);
        return add;
    }

    @Override
    public Dockerfile visitCopy(Dockerfile.Copy copy, PrintOutputCapture<P> p) {
        beforeSyntax(copy, p);
        p.append("COPY");

        if (copy.getOptions() != null) {
            copy.getOptions().forEach(o -> {
                        Dockerfile.Option element = o.getElement();
                        if (element.getName() != null && !element.getName().isEmpty()) {
                            p.append(element.getPrefix().getWhitespace());
                            p.append("--");
                            p.append(element.getName());
                            if (element.getKeyArgs() != null) {
                                p.append("=");
                                for (Dockerfile.KeyArgs kvp : element.getKeyArgs()) {
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
                            }
                        }
                    }
            );
        }

        if (copy.getSources() != null ) {
            for (int i = 0; i < copy.getSources().size(); i++) {
                DockerfileRightPadded<Dockerfile.Literal> literalPadded = copy.getSources().get(i);
                Dockerfile.Literal literal = literalPadded.getElement();
                p.append(literal.getPrefix().getWhitespace());
                p.append(literal.getText());
                p.append(literalPadded.getAfter().getWhitespace());
            }
        }

        if (copy.getDestination() != null) {
            DockerfileRightPadded<Dockerfile.Literal> literalPadded = copy.getDestination();
            Dockerfile.Literal literal = literalPadded.getElement();
            p.append(literal.getPrefix().getWhitespace());
            p.append(literal.getText());
            p.append(literalPadded.getAfter().getWhitespace());
        }


        afterSyntax(copy, p);
        return copy;
    }

    @Override
    public Dockerfile visitEntrypoint(Dockerfile.Entrypoint entrypoint, PrintOutputCapture<P> p) {
        beforeSyntax(entrypoint, p);
        p.append("ENTRYPOINT [");
        for (int i = 0; i < entrypoint.getCommand().size(); i++) {
            DockerfileRightPadded<Dockerfile.Literal> padded = entrypoint.getCommand().get(i);
            Dockerfile.Literal literal = padded.getElement();
            String text = literal.getText();
            text = trimDoubleQuotes(text);
            p.append(literal.getPrefix().getWhitespace());
            p.append("\"").append(text).append("\"");
            p.append(padded.getAfter().getWhitespace());
            if (i < entrypoint.getCommand().size() - 1) {
                p.append(",");
            }
        }
        p.append("]");
        p.append(entrypoint.getTrailing().getWhitespace());
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
        p.append("ARG");
        arg.getArgs().forEach(padded -> {
            Dockerfile.KeyArgs kvp = padded.getElement();
            p.append(kvp.getPrefix().getWhitespace());
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
            p.append(padded.getAfter().getWhitespace());
        });
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

    private void appendRightPaddedLiteral(DockerfileRightPadded<Dockerfile.Literal> padded, PrintOutputCapture<P> p) {
        if (padded == null) {
            return;
        }

        Dockerfile.Literal literal = padded.getElement();
        if (literal == null || literal.getText() == null || literal.getText().isEmpty()) {
            return;
        }
        p.append(literal.getPrefix().getWhitespace());
        p.append(literal.getText());
        p.append(padded.getAfter().getWhitespace());
    }
}