package com.nido.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/** What a class logs while a test runs, until closed: the logger is then as it was. */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final Level levelBefore;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture(Class<?> source, Level level) {
        logger = (Logger) LoggerFactory.getLogger(source);
        levelBefore = logger.getLevel();
        if (level != null) {
            logger.setLevel(level);
        }
        appender.start();
        logger.addAppender(appender);
    }

    /** At the level the class logs at in the tests. */
    public static LogCapture of(Class<?> source) {
        return new LogCapture(source, null);
    }

    /** Its debug lines too. */
    public static LogCapture atEveryLevel(Class<?> source) {
        return new LogCapture(source, Level.ALL);
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    /** The lines written, their arguments in place. */
    public List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        logger.setLevel(levelBefore);
    }
}
