package com.voyagerinnovation.smack.security.authentication;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements;

import javax.security.auth.callback.CallbackHandler;

public class XSKEYTokenMechanism extends SASLMechanism {

    public static final String MECHANISM_NAME = "X-SKEY-TOKEN";

    private String resource;

    public XSKEYTokenMechanism(String resource){
        this.resource = resource;
    }

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        connection.send(new SaslStreamElements.AuthMechanism(MECHANISM_NAME, this.password, resource));
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
