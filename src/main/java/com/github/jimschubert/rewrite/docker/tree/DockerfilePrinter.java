package com.github.jimschubert.rewrite.docker.tree;

import com.github.jimschubert.rewrite.docker.DockerVisitor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

public class DockerfilePrinter<P> extends DockerVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    protected void beforeSyntax(Docker t, PrintOutputCapture<P> p) {
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

    protected void afterSyntax(Docker t, PrintOutputCapture<P> p) {
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
    public @Nullable Docker visit(@Nullable Tree tree, PrintOutputCapture<P> p, Cursor parent) {
        if (tree == null) {
            return null;
        }
        Docker.Document target = null;
        if (tree instanceof Docker.Document) {
            target = (Docker.Document) tree;
        } else if (tree instanceof Docker) {
            target = (Docker.Document) tree;
        } else {
            return null;
        }

        List<Docker.Stage> stages = target.getStages();
        for (int i = 0; i < stages.size(); i++) {
            Docker.Stage stage = stages.get(i);
            visit(stage, p);
            if (i < stages.size() - 1) {
                p.append("\n");
            }
        }
        return super.visit(tree, p, parent);
    }

    @Override
    public Docker visitStage(Docker.Stage stage, PrintOutputCapture<P> p) {
        beforeSyntax(stage, p);
        List<Docker.Instruction> children = stage.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Docker.Instruction instruction = children.get(i);
            visit(instruction, p);
            if (i < children.size() - 1) {
                p.append("\n");
            }
        }
        afterSyntax(stage, p);
        return stage;
    }

    @Override
    public Docker visitFrom(Docker.From from, PrintOutputCapture<P> p) {
        beforeSyntax(from, p);
        p.append("FROM");
        if (from.getPlatform() != null && from.getPlatform().getElement().getText() != null) {
            p.append(" --platform=");
            p.append(from.getPlatform().getElement().getText());

            visitSpace(from.getPlatform().getAfter(), p);
        }

        appendRightPaddedLiteral(from.getImage(), p);
        appendRightPaddedLiteral(from.getVersion(), p);

        if (from.getAlias() != null && from.getAlias().getElement().getText() != null && !from.getAlias().getElement().getText().isEmpty()) {
            visitSpace(from.getAs().getPrefix(), p);
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
    public Docker visitComment(Docker.Comment comment, PrintOutputCapture<P> p) {
        Docker.Literal literal = comment.getText().getElement();
        if (literal != null) {
            beforeSyntax(comment, p);
            p.append("#");
            visitSpace(literal.getPrefix(), p);
            p.append(literal.getText().replace("\n", "\n# "));
            visitSpace(comment.getText().getAfter(), p);
            afterSyntax(comment, p);
        }
        return comment;
    }

    @Override
    public Docker visitDirective(Docker.Directive directive, PrintOutputCapture<P> p) {
        beforeSyntax(directive, p);
        p.append("#");
        DockerRightPadded<Docker.KeyArgs> padded = directive.getDirective();
        Docker.KeyArgs keyArgs = padded.getElement();
        visitSpace(keyArgs.getPrefix(), p);
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
        visitSpace(padded.getAfter(), p);
        afterSyntax(directive, p);
        return directive;
    }

    @Override
    public Docker visitRun(Docker.Run run, PrintOutputCapture<P> p) {
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
    public Docker visitCmd(Docker.Cmd cmd, PrintOutputCapture<P> p) {
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
            DockerRightPadded<Docker.Literal> padded = cmd.getCommands().get(i);
            Docker.Literal literal = padded.getElement();
            String text = literal.getText();
            if (form == Form.EXEC) {
                text = trimDoubleQuotes(text);
                visitSpace(literal.getPrefix(), p);
                p.append("\"").append(text).append("\"");
            } else {
                p.append(text);
            }
            visitSpace(padded.getAfter(), p);
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
    public Docker visitLabel(Docker.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        p.append("LABEL");
        for (DockerRightPadded<Docker.KeyArgs> padded : label.getArgs()) {
            Docker.KeyArgs kvp = padded.getElement();
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
    public Docker visitMaintainer(Docker.Maintainer maintainer, PrintOutputCapture<P> p) {
        beforeSyntax(maintainer, p);
        p.append("MAINTAINER ");
        p.append(maintainer.getName());
        afterSyntax(maintainer, p);
        return maintainer;
    }

    @Override
    public Docker visitExpose(Docker.Expose expose, PrintOutputCapture<P> p) {
        beforeSyntax(expose, p);
        p.append("EXPOSE");
        for (int i = 0; i < expose.getPorts().size(); i++) {
            DockerRightPadded<Docker.Port> padded = expose.getPorts().get(i);
            Docker.Port port = padded.getElement();
            visitSpace(port.getPrefix(), p);
            p.append(port.getPort());
            if (port.isProtocolProvided() && port.getProtocol() != null && !port.getProtocol().isEmpty()) {
                p.append("/");
                p.append(port.getProtocol());
            }
            visitSpace(padded.getAfter(), p);
        }
        afterSyntax(expose, p);
        return expose;
    }

    @Override
    public Docker visitEnv(Docker.Env env, PrintOutputCapture<P> p) {
        beforeSyntax(env, p);
        p.append("ENV");

        for (DockerRightPadded<Docker.KeyArgs> padded : env.getArgs()) {
            Docker.KeyArgs kvp = padded.getElement();
            visitSpace(kvp.getPrefix(), p);
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

            visitSpace(padded.getAfter(),p);
        }
        afterSyntax(env, p);
        return env;
    }

    @Override
    public Docker visitAdd(Docker.Add add, PrintOutputCapture<P> p) {
        beforeSyntax(add, p);
        p.append("ADD");

        if (add.getOptions() != null) {
            add.getOptions().forEach(o -> {
                    Docker.Option element = o.getElement();
                    if (element.getName() != null && !element.getName().isEmpty()) {
                        visitSpace(element.getPrefix(), p);
                        p.append("--");
                        p.append(element.getName());
                        if (element.getKeyArgs() != null) {
                            p.append("=");
                            for (Docker.KeyArgs kvp : element.getKeyArgs()) {
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
                DockerRightPadded<Docker.Literal> literalPadded = add.getSources().get(i);
                Docker.Literal literal = literalPadded.getElement();
                visitSpace(literal.getPrefix(), p);
                p.append(literal.getText());
                visitSpace(literalPadded.getAfter(), p);
            }
        }

        if (add.getDestination() != null) {
            DockerRightPadded<Docker.Literal> literalPadded = add.getDestination();
            Docker.Literal literal = literalPadded.getElement();
            visitSpace(literal.getPrefix(), p);
            p.append(literal.getText());
            visitSpace(literalPadded.getAfter(), p);
        }

        afterSyntax(add, p);
        return add;
    }

    @Override
    public Docker visitCopy(Docker.Copy copy, PrintOutputCapture<P> p) {
        beforeSyntax(copy, p);
        p.append("COPY");

        if (copy.getOptions() != null) {
            copy.getOptions().forEach(o -> {
                        Docker.Option element = o.getElement();
                        if (element.getName() != null && !element.getName().isEmpty()) {
                            visitSpace(element.getPrefix(), p);
                            p.append("--");
                            p.append(element.getName());
                            if (element.getKeyArgs() != null) {
                                p.append("=");
                                for (Docker.KeyArgs kvp : element.getKeyArgs()) {
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
                DockerRightPadded<Docker.Literal> literalPadded = copy.getSources().get(i);
                Docker.Literal literal = literalPadded.getElement();
                visitSpace(literal.getPrefix(), p);
                p.append(literal.getText());
                visitSpace(literalPadded.getAfter(), p);
            }
        }

        if (copy.getDestination() != null) {
            DockerRightPadded<Docker.Literal> literalPadded = copy.getDestination();
            Docker.Literal literal = literalPadded.getElement();
            visitSpace(literal.getPrefix(), p);
            p.append(literal.getText());
            visitSpace(literalPadded.getAfter(), p);
        }


        afterSyntax(copy, p);
        return copy;
    }

    @Override
    public Docker visitEntrypoint(Docker.Entrypoint entrypoint, PrintOutputCapture<P> p) {
        beforeSyntax(entrypoint, p);
        p.append("ENTRYPOINT [");
        for (int i = 0; i < entrypoint.getCommands().size(); i++) {
            DockerRightPadded<Docker.Literal> padded = entrypoint.getCommands().get(i);
            Docker.Literal literal = padded.getElement();
            String text = literal.getText();
            text = trimDoubleQuotes(text);
            visitSpace(literal.getPrefix(), p);
            p.append("\"").append(text).append("\"");
            visitSpace(literal.getTrailing(), p);
            if (i < entrypoint.getCommands().size() - 1) {
                p.append(",");
            }
            visitSpace(padded.getAfter(), p);
        }
        p.append("]");
        visitSpace(entrypoint.getExecFormSuffix(), p);
        afterSyntax(entrypoint, p);
        return entrypoint;
    }

    @Override
    public Docker visitVolume(Docker.Volume volume, PrintOutputCapture<P> p) {
        beforeSyntax(volume, p);
        p.append("VOLUME");

        if (volume.getForm() == Form.EXEC) {
            Space before = volume.getExecFormPrefix();
            if (before == null || before.isEmpty()) {
                before = Space.build(" ");
            }
            visitSpace(before, p);
            p.append("[");
        }

        for (int i = 0; i < volume.getPaths().size(); i++) {
            DockerRightPadded<Docker.Literal> padded = volume.getPaths().get(i);
            Docker.Literal literal = padded.getElement();
            visitSpace(literal.getPrefix(), p);

            if (volume.getForm() == Form.EXEC) {
                String text = literal.getText();
                text = trimDoubleQuotes(text);
                p.append("\"").append(text).append("\"");
            } else {
                p.append(literal.getText());
            }

            visitSpace(padded.getAfter(), p);
            if (i < volume.getPaths().size() - 1) {
                if (volume.getForm() == Form.EXEC) {
                    p.append(",");
                } else {
                    p.append(" ");
                }
            }
        }

        if (volume.getForm() == Form.EXEC) {
            p.append("]");
            visitSpace(volume.getExecFormSuffix(), p);
        }

        afterSyntax(volume, p);
        return volume;
    }

    @Override
    public Docker visitUser(Docker.User user, PrintOutputCapture<P> p) {
        beforeSyntax(user, p);
        p.append("USER");
        visitSpace(user.getUsername().getPrefix(), p);
        p.append(user.getUsername().getText());
        visitSpace(user.getUsername().getTrailing(), p);
        if (user.getGroup() != null && user.getGroup().getText() != null && !user.getGroup().getText().isEmpty()) {
            p.append(":");
            visitSpace(user.getGroup().getPrefix(), p);
            p.append(user.getGroup().getText());
            visitSpace(user.getGroup().getTrailing(), p);
        }
        afterSyntax(user, p);
        return user;
    }

    @Override
    public Docker visitWorkdir(Docker.Workdir workdir, PrintOutputCapture<P> p) {
        beforeSyntax(workdir, p);
        p.append("WORKDIR");
        visitSpace(workdir.getPath().getPrefix(), p);
        if (!workdir.getPath().getText().isEmpty()) {
            p.append(workdir.getPath().getText());
        }
        visitSpace(workdir.getPath().getTrailing(), p);
        afterSyntax(workdir, p);
        return workdir;
    }

    @Override
    public Docker visitArg(Docker.Arg arg, PrintOutputCapture<P> p) {
        beforeSyntax(arg, p);
        p.append("ARG");
        arg.getArgs().forEach(padded -> {
            Docker.KeyArgs kvp = padded.getElement();
            visitSpace(kvp.getPrefix(), p);
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
            visitSpace(padded.getAfter(), p);
        });
        afterSyntax(arg, p);
        return arg;
    }

    @Override
    public Docker visitOnBuild(Docker.OnBuild onBuild, PrintOutputCapture<P> p) {
        beforeSyntax(onBuild, p);
        p.append("ONBUILD ");
        visit(onBuild.getInstruction(), p);
        afterSyntax(onBuild, p);
        return onBuild;
    }

    @Override
    public Docker visitStopSignal(Docker.StopSignal stopSignal, PrintOutputCapture<P> p) {
        beforeSyntax(stopSignal, p);
        p.append("STOPSIGNAL ");
        p.append(stopSignal.getSignal());
        afterSyntax(stopSignal, p);
        return stopSignal;
    }

    @Override
    public Docker visitHealthcheck(Docker.Healthcheck healthcheck, PrintOutputCapture<P> p) {
        beforeSyntax(healthcheck, p);
        p.append("HEALTHCHECK ");
        p.append(healthcheck.getCommand());
        afterSyntax(healthcheck, p);
        return healthcheck;
    }

    @Override
    public Docker visitShell(Docker.Shell shell, PrintOutputCapture<P> p) {
        beforeSyntax(shell, p);
        p.append("SHELL ");
        p.append("[");
        visitSpace(shell.getExecFormPrefix(), p);

        for (int i = 0; i < shell.getCommands().size(); i++) {
            DockerRightPadded<Docker.Literal> padded = shell.getCommands().get(i);
            Docker.Literal literal = padded.getElement();
            String text = literal.getText();
            text = trimDoubleQuotes(text);
            visitSpace(literal.getPrefix(), p);
            p.append("\"").append(text).append("\"");
            visitSpace(literal.getTrailing(), p);
            if (i < shell.getCommands().size() - 1) {
                p.append(",");
            }
            visitSpace(padded.getAfter(), p);
        }

        p.append("]");

        visitSpace(shell.getExecFormSuffix(), p);
        afterSyntax(shell, p);
        return shell;
    }

    private void appendRightPaddedLiteral(DockerRightPadded<Docker.Literal> padded, PrintOutputCapture<P> p) {
        if (padded == null) {
            return;
        }

        Docker.Literal literal = padded.getElement();
        if (literal == null || literal.getText() == null || literal.getText().isEmpty()) {
            return;
        }
        visitSpace(literal.getPrefix(), p);
        p.append(literal.getText());
        visitSpace(padded.getAfter(), p);
    }
}