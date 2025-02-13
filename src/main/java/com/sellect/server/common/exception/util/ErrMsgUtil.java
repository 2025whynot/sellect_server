package com.sellect.server.common.exception.util;

public class ErrMsgUtil {

    public static String parseMessage(String message, String... args) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }

        if (args == null || args.length == 0) {
            return message;
        }

        String[] splitMsgs = message.split("%");
        if (splitMsgs.length <= 1) {
            return message;
        }

        for (int i = 0; i < args.length; i++) {
            String replaceChar = "%" + (i + 1);
            message = message.replaceFirst(replaceChar, args[i]);
        }
        return message;
    }
}