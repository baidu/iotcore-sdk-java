// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.deviceside.internal;

import com.baidu.iot.device.sdk.avatar.common.PropertyKey;
import com.baidu.iot.device.sdk.avatar.common.PropertyValue;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/**
 * Ensure that the local shadow is consistent with the cloud.
 *
 * Author zhangxiao18
 * Date 2020/10/6
 */
public interface ILocalAvatarHolder {

    BehaviorProcessor<PropertyValue> observeDesired(PropertyKey key);

}
