// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * Class to hold current avatar version.
 *
 * Author zhangxiao18
 * Date 2020/10/13
 */
public interface IAvatarVersionHolder {

    Single<Integer> getNextReportedVersion();

    Single<Integer> getNextDesiredVersion();

    Completable triggerReload();

    Completable close();

}
