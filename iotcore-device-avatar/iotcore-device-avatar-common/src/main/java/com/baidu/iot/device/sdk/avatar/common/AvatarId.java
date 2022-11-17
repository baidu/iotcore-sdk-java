// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Author zhangxiao18
 * Date 2020/9/23
 */
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class AvatarId {

    private String iotCoreId;

    private String id;

    public EntityId getDeviceSideEntityId() {
        return new EntityId(iotCoreId, id);
    }

}
