// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

/**
 * Created by zhuchenhao at 2019/11/19
 */
@Data
@Builder
public class AvatarOperation {
    private String key;
    private JsonNode value;
}
