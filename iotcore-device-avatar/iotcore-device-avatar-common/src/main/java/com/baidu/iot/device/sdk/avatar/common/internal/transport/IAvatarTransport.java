// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.device.sdk.avatar.common.internal.transport;

import com.baidu.iot.device.sdk.avatar.common.internal.CommandMessage;
import com.baidu.iot.device.sdk.avatar.common.internal.Message;
import com.baidu.iot.device.sdk.avatar.common.internal.Topic;
import com.baidu.iot.device.sdk.avatar.common.internal.UserMessage;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 *  Transport interface for avatar.
 * Author zhangxiao18
 * Date 2020/9/22
 */
public interface IAvatarTransport {

    /**
     * Close the transport.
     */
    Completable close();

    /**
     * Publish the message to specific topic.
     * Note that the Completable complete means publishing success at least once(qos1).
     *
     * @param topic topic for avatar
     * @param userMessage user message for transfer
     */
    Completable pub(Topic topic, UserMessage userMessage);

    /**
     * Subscribe to specific topic.
     * The returned BehaviorSubject will not throw Exception, it will try to recover forever.
     * The returned BehaviorSubject may emmit some {@link CommandMessage}s in the following situations:
     *  CommandMessage.READY: When internal resources ready.
     *  CommandMessage.ERROR: When some error happened.
     * Because of recovering, READY message may emmit several times.
     * The messages arrive in the following order:
     *   ('-' means {@link UserMessage} , '...' means waiting for ready)
     *   ...READY ------------- ERROR...READY --------------
     *
     * @param topic specific topic
     * @return BehaviorSubject which will emmit {@link CommandMessage} or {@link UserMessage}
     */
    BehaviorSubject<Message> sub(Topic topic);
}
