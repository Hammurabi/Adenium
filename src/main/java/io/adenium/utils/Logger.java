package io.adenium.utils;

import org.fusesource.jansi.Ansi;
import io.adenium.core.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {
    public static final class Levels {
        public static final int
            None = 0,
            Important = 1,
            AlertMessage = 2,
            NotificationMessage = 3,
            Journaling = 4,
            Debugging = 5;
    }
    public static void faterr(String msg)
    {
        System.err.println(msg);
    }

    public static void alert(String msg, int level, Object ...arguments) {
        if (level <= Context.getInstance().getContextParams().getLoggingLevel()) {
            msg(Ansi.Color.GREEN, msg, arguments);
        }
    }

    public static void notify(String msg, int level, Object ...arguments) {
        if (level <= Context.getInstance().getContextParams().getLoggingLevel()) {
            msg(Ansi.Color.YELLOW, msg, arguments);
        }
    }

    public static void error(String msg, int level, Object ...arguments) {
        if (level <= Context.getInstance().getContextParams().getLoggingLevel()) {
            msg(Ansi.Color.YELLOW, msg, arguments);
        }
    }

    public static void msg(Ansi.Color chatColor, String msg, Object ...arguments) {
        Pattern pattern = Pattern.compile("\\$\\{[A-z]+\\}");
        Matcher matcher = pattern.matcher(msg);
        int index       = 0;

        while (matcher.find()) {
            msg = matcher.replaceFirst(Utils.toString(arguments[index ++]));
            matcher = pattern.matcher(msg);
        }

        System.out.println(Ansi.ansi().fg(chatColor).a(msg).reset());
    }
}
