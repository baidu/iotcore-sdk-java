/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.common.internal.method;

import io.reactivex.rxjava3.core.Completable;

/**
 * Avatar Method is an abstraction for transport.
 *
 * Use method instead of transport to request to server.
 *
 * Author zhangxiao18
 * Date 2020/10/12
 */
public interface IAvatarMethod {

    Completable close();

}
