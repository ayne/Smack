package com.voyagerinnovation.smack.security.authentication;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements;

import javax.security.auth.callback.CallbackHandler;

public class XYAPTokenMechanism extends SASLMechanism {


    public static final String MECHANISM_NAME = "X-YAP-TOKEN";

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        connection.send(new SaslStreamElements.AuthMechanism(MECHANISM_NAME, this.password));
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        byte[] passw = toBytes(password);
        return passw;
    }

    @Override
    public String getName() {
        return MECHANISM_NAME;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {

    }

    @Override
    protected SASLMechanism newInstance() {
        return this;
    }
}

