// Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
// Licensed under the Apache License.

package com.baidu.iot.shared.sub.transport;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

import com.baidu.iot.shared.sub.transport.enums.TransportState;
import com.baidu.iot.shared.sub.transport.model.TransportMessage;


public interface ISharedSubTransport {

    /**
     * Start the transport
     */
    Completable start();


    /**
     * Close the transport.
     */
    Completable close();

    /**
     * Listen to the latest message from the transport.
     *
     * @return Message flow which emit messages from this transport
     */
    Observable<TransportMessage> listen();

    /**
     * Listen to the state change of the transport.
     * State enum: CONNECTING, READY, FAILURE, SHUTDOWN
     *
     * @return TransportState flow which emit last and latest state of this transport.
     */
    Observable<TransportState> transportState();
}
