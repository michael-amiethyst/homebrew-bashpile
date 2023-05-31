package com.bashpile;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class BashpileMain {

    public static void main(String[] args) throws IOException {
        BashpileMain bashpile = new BashpileMain();
        bashpile.processArgs(args);
    }

    public static String[] processArg(String filename) throws IOException {
        return processArgs(filename.split(" "));
    }

    public static String[] processArgs(String[] args) throws IOException {
        // stream is either stdin or the first argument
        InputStream is = System.in;
        boolean argsExist = args.length > 0;
        if (argsExist) {
            String inputFile = args[0];
            is = new FileInputStream(inputFile);
        }

        return parse(is);
    }

    private static String[] parse(InputStream is) throws IOException {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.DEBUG);
        builder.setConfigurationName("BuilderTest");
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                .addAttribute("level", Level.DEBUG));
        AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addAttribute("marker", "FLOW"));
        builder.add(appenderBuilder);
        builder.add(builder.newLogger("com.bashpile", Level.DEBUG)
                .add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.DEBUG).add(builder.newAppenderRef("Stdout")));
        LoggerContext ctx = Configurator.initialize(builder.build());
        Logger log = ctx.getLogger(BashpileMain.class);
        log.debug("debug");
        log.error("error");
        // lexer
        CharStream input = CharStreams.fromStream(is);
        BashpileLexer lexer = new BashpileLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // parser
        BashpileParser parser = new BashpileParser(tokens);
        ParseTree tree = parser.prog();

        return applyBashpileLogic(tree);
    }

    private static String[] applyBashpileLogic(ParseTree tree) throws IOException {
        // visitor
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
             BashpileVisitor bashpileLogic = new BashpileVisitor(byteOutput)) {

            bashpileLogic.visit(tree);  // writes to byteOutput here

            return byteOutput.toString().split("\r?\n");
        }
    }
}
