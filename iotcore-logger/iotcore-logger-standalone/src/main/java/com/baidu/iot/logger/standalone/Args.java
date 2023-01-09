// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.logger.standalone;


import com.beust.jcommander.Parameter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mafei01 in 5/8/21 10:29 AM
 */
@Data
@Slf4j
public class Args {

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = "--help", description = "Show help", help = true)
    private boolean help;

    @Parameter(names = {"--iotCoreId"}, description = "Receive logs from this iotCore", required = true)
    private String iotCoreId;

    @Parameter(names = {"--username"}, description = "Mqtt client username", required = true)
    private String username;

    @Parameter(names = {"--password"}, description = "Mqtt client password", required = true)
    private String password;

    @Parameter(names = {"--uri"}, description = "Mqtt broker uri")
    private String uri;

    @Parameter(names = {"--level"}, description = "Level of the received logs: ERROR, WARN, INFO, DEBUG")
    private String level = "INFO";

    @Parameter(names = {"--includeLowerLevel"}, description = "Whether the log level should include the lower levels, "
            + "such as INFO leven would include WARN and ERROR")
    private boolean includeLowerLevel = true;

    @Parameter(names = {"--deviceKeys"}, description = "Logs only from the specific devices would arrive")
    private String[] deviceKeys = new String[0];

    @Parameter(names = {"--clientCount"}, description = "Total test client count")
    private int clientCount = 3;

}
