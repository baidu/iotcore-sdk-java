// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

/**
 * Determine whether the local shadow is consistent with the cloud.
 *
 * Author zhangxiao18
 * Date 2020/10/6
 */
public interface IAvatarConsistenceStrategy {

    void recordReportResult(boolean success);

    void recordLatestDesiredVersion(int version);

    void force(boolean isConsistent);

    boolean isConsistent();

}
