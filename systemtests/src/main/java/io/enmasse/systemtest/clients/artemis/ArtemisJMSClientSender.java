/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients.artemis;

import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.ClientType;
import io.enmasse.systemtest.clients.proton.java.ProtonJMSClientSender;


public class ArtemisJMSClientSender extends ProtonJMSClientSender {
    public ArtemisJMSClientSender() {
        this.setClientType(ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER);
    }

    @Override
    protected ArgumentMap transformArguments(ArgumentMap args) {
        args = javaBrokerTransformation(args);
        return args;
    }
}
