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

package com.baidu.iot.device.sdk.avatar.common.internal.transport;

import com.baidu.iot.device.sdk.avatar.common.internal.CommandMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 *  Transport interface for avatar.
 *
 * @Author zhangxiao18
 * @Date 2020/9/22
 */
public interface IAvatarTransport {

    /**
     * Close the transport.
     *
     * @return
     */
    Completable close();

    /**
     * Publish the message to specific topic.
     *
     * Note that the Completable complete means publishing success at least once(qos1).
     *
     * @param topic
     * @param userMessage
     * @return
     */
    Completable pub(Topic topic, UserMessage userMessage);

    /**
     * Subscribe to specific topic.
     *
     * The returned BehaviorSubject will not throw Exception, it will try to recover forever.
     *
     * The returned BehaviorSubject may emmit some {@link CommandMessage}s in the following situations:
     *  CommandMessage.READY: When internal resources ready.
     *  CommandMessage.ERROR: When some error happened.
     *
     * Because of recovering, READY message may emmit several times.
     * The messages arrive in the following order:
     *
     *   ('-' means {@link UserMessage} , '...' means waiting for ready)
     *   ...READY ------------- ERROR...READY --------------
     *
     * @param topic specific topic
     * @return BehaviorSubject which will emmit {@link CommandMessage} or {@link UserMessage}
     */
    BehaviorSubject<Message> sub(Topic topic);
}
