package com.voyagerinnovation.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.voyagerinnovation.environment.Environment;
import com.voyagerinnovation.smack.security.authentication.DummySSLSocketFactory;
import com.voyagerinnovation.smack.security.authentication.XYAPTokenMechanism;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ArchiveIQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.ArchiveResultIQ;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.util.Map;

import timber.log.Timber;

//import org.jivesoftware.smack.SmackAndroid;

/**
 * Service that handles connection to XMPP server as well as parsing of incoming stanzas.
 * Created by charmanesantiago on 3/25/15.
 */
public class ChatService extends Service implements ConnectionListener,
        StanzaListener, StanzaFilter {


    private XMPPTCPConnection xmpptcpConnection;
    private final IBinder mBinder = new LocalBinder();
    private String yapToken;
    private String tts;

    @Override
    public boolean accept(Stanza stanza) {
        return true;
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        Timber.d("Stanza " + packet.toXML().toString());
        if (packet instanceof ArchiveResultIQ) {
            Timber.d("Archive endpoint = " + ((ArchiveResultIQ) packet).getEndpoint());
        }
    }

    public class LocalBinder extends Binder {
        public ChatService getService() {
            return ChatService.this;
        }
    }


    private static class ChatServiceHandler extends Handler {
        @Override
        public void handleMessage(Message message) {

        }
    }


    @Override
    public void onCreate() {
        Timber.i("New ChatService instance created");
        super.onCreate();
        //SmackAndroid.init(this);
        configureConnection();
        connect();

    }

    private void configureConnection() {
        SmackConfiguration.DEBUG = true;

//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax
// .SASLCramMD5Mechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax
// .SASLDigestMD5Mechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax
// .SASLExternalMechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax
// .SASLGSSAPIMechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax
// .SASLJavaXMechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax
// .SASLJavaXSmackInitializer");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.core
// .SCRAMSHA1Mechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.core
// .SASLXOauth2Mechanism");


        XMPPTCPConnectionConfiguration mConnectionConfig = XMPPTCPConnectionConfiguration.builder()
                .setHost(Environment.IM_HOST)
                .setPort(Environment.IM_PORT)
                .setServiceName(Environment.IM_SERVICE_NAME)
                .setSendPresence(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                .setSocketFactory(new DummySSLSocketFactory())
                .setDebuggerEnabled(Environment.IS_DEBUG)
                .build();

        xmpptcpConnection = new XMPPTCPConnection(mConnectionConfig);
        xmpptcpConnection.addConnectionListener(this);
        xmpptcpConnection.addSyncStanzaListener(this, this);
        xmpptcpConnection.setRosterEnabled(false);

        Map<String, String> registeredMechs = SASLAuthentication.getRegisterdSASLMechanisms();
        for (Map.Entry<String, String> entry : registeredMechs.entrySet()) {
            if (!SASLMechanism.PLAIN.equals(entry.getValue())) {
                SASLAuthentication.blacklistSASLMechanism(entry.getValue());
            }
        }

        //SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());

    }

    /**
     * Method to login to Babble using jid and password
     *
     * @param jid      full jid of the user (i.e tes1@babbleim.com)
     * @param password password of the passed jid
     */
    public void loginPlain(String jid, String password) {

        SASLAuthentication.blacklistSASLMechanism(XYAPTokenMechanism.MECHANISM_NAME);
        SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());

        try {
            xmpptcpConnection.login(jid, password,
                    Environment.IM_RESOURCE);
        } catch (XMPPException e) {
            e.printStackTrace();
            if (e instanceof SASLErrorException) {
                SASLErrorException saslErrorException = (SASLErrorException) e;
                if (saslErrorException.getSASLFailure() != null) {
                    if (SASLError.not_authorized == saslErrorException.getSASLFailure()
                            .getSASLError()) {
                        Timber.e("Not authorized. Please login again!");
                    }
                }
            }
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to login to Babble via the XYAP Token.
     *
     * @param username The username (not full jid) of the user.
     * @param yapToken The token (x-yap-token) to be used for logging in.
     *                 This is retrieved from <success></success> response after <auth></auth>
     */
    public void loginXyap(String username, String yapToken) {
        SASLAuthentication.blacklistSASLMechanism(SASLMechanism.PLAIN);
        SASLAuthentication.registerSASLMechanism(new XYAPTokenMechanism
                (yapToken));

        Timber.d("Logging in using xyap " + yapToken);
        try {
            xmpptcpConnection.login(username, yapToken,
                    XYAPTokenMechanism.MECHANISM_NAME);
        } catch (XMPPException e) {
            e.printStackTrace();
            if (e instanceof SASLErrorException) {
                SASLErrorException saslErrorException = (SASLErrorException) e;
                if (saslErrorException.getSASLFailure() != null) {
                    if (SASLError.token_expired == saslErrorException.getSASLFailure()
                            .getSASLError()) {
                        //loginPlain();
                    }
                }
            }
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to connect to XMPP server. This method performs an automatic login to the server
     * if previous connection state was logged (authenticated).
     */
    public void connect() {
        new Thread() {
            @Override
            public void run() {
                Timber.d("Connecting... is connected " + xmpptcpConnection.isConnected());
                try {
                    if (!xmpptcpConnection.isConnected()) {
                        xmpptcpConnection.connect();
                    }
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public void disconnect() {
        new Thread() {
            @Override
            public void run() {
                Timber.d("Disconnecting...");
                xmpptcpConnection.disconnect();
            }
        }.start();
    }

    public void authenticate() {
        Timber.d("do authenticate manually");
//        if (yapToken != null) {
//            loginXyap("test1", yapToken);
//        } else {
//            loginPlain("test1" + Environment.IM_SUFFIX, "vvtest1vv");
//        }
        //u49CByVnYkzw2NerzAIRIoj+8q9C5QQRdGjIqLtRIuJ6m/LWEEGS0dqzXP6jixUOTIkDZEb/0E/ZzGEzDRcRis/DTicAZ8vq9myQj3rsl06XlApP7hrR1a4VTe6Y

        loginXyap("test1", "u49CByVnYkzw2NerzAIRIoj+8q9C5QQRdGjIqLtRIuJ6m" +
                "/LWEEGS0dqzXP6jixUOTIkDZEb/0E/ZzGEzDRcRis/DTicAZ8vq9myQj3rsl06XlApP7hrR1a4VTe6Y");
    }


    @Override
    public void connected(final XMPPConnection connection) {
        Timber.d("Connected");
        new Thread() {
            @Override
            public void run() {
                if (!xmpptcpConnection.isAuthenticated()) {
                    authenticate();
                }
            }
        }.start();

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        yapToken = connection.getToken();
        Timber.d("Authenticated xyaptoken is " + yapToken);
        Timber.d("Authenticated tts is " + connection.getTts());
        ArchiveIQ archiveIq = new ArchiveIQ("0", "0", "0");
        archiveIq.setTo(Environment.IM_ARCHIVE_HOST);
        archiveIq.setStanzaId(null);
        try {
            xmpptcpConnection.sendStanza(archiveIq);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Timber.d("connectionClosedOnError " + e.getMessage());
        if ("stream:error (conflict) text: Replaced by new connection".equals(e.getMessage())) {
            //TODO do handling of error, i.e logout user.
            Timber.d("Stream conflict error");
        }

    }

    @Override
    public void reconnectingIn(int seconds) {

    }

    @Override
    public void reconnectionSuccessful() {

    }

    @Override
    public void reconnectionFailed(Exception e) {

    }

    /**
     * This method only return whether connected to the server. Not necessarily authenticated.
     *
     * @return true if connected to the server, false otherwise.
     */
    public boolean isConnected() {
        return xmpptcpConnection.isConnected();
    }


    /**
     * @return true if authenticated to the server (logged in)
     */
    public boolean isAuthenticated() {
        return xmpptcpConnection.isAuthenticated();
    }

}
