/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.iot.log.sdk.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mafei01 in 6/4/21 11:26 AM
 */
public enum LogLevel {

    ERROR(0, "$sys/log/error/"),
    WARN(1, "$sys/log/warn/"),
    INFO(2, "$sys/log/info/"),
    DEBUG(3, "$sys/log/debug/");

    private static Pattern logPattern = Pattern.compile("\\$sys/log/(?<level>[^/]+)/.*");

    public final int code;

    public final String topic;

    LogLevel(int code, String topic) {
        this.topic = topic;
        this.code = code;
    }

    public static boolean isLogTopic(String topic) {
        return topic.startsWith("$sys/log");
    }

    public static int codeOf(String topic) {
        Matcher matcher = logPattern.matcher(topic);
        if (matcher.matches()) {
            String level = matcher.group("level");
            switch (level) {
                case "error":
                    return 0;
                case "warn":
                    return 1;
                case "info":
                    return 2;
                case "debug":
                    return 3;
                default:
                    return -1;
            }
        }
        return -1;
    }

    public static LogLevel fromCode(int code) {
        switch (code) {
            case 0:
                return ERROR;
            case 1:
                return WARN;
            case 2:
                return INFO;
            case 3:
                return DEBUG;
            default:
                return null;
        }
    }
}
