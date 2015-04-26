package com.voyagerinnovation.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.voyagerinnovation.environment.Environment;
import com.voyagerinnovation.services.managers.MUCMessageManager;
import com.voyagerinnovation.services.managers.P2PMessageManager;
import com.voyagerinnovation.services.managers.VGCMessageManager;
import com.voyagerinnovation.services.parsers.StanzaParser;
import com.voyagerinnovation.smack.security.authentication.DummySSLSocketFactory;
import com.voyagerinnovation.smack.security.authentication.XSKEYTokenMechanism;
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
public  class ChatService extends Service implements ConnectionListener,
        StanzaListener, StanzaFilter {


    public XMPPTCPConnection xmpptcpConnection;
    private final IBinder mBinder = new LocalBinder();
    private String yapToken;
    private String tts;
    public P2PMessageManager p2PMessageManager;
    public VGCMessageManager vgcMessageManager;
    public MUCMessageManager mucMessageManager;
    private ChatReceivedListener chatReceivedListener;


    public class LocalBinder extends Binder {
        public ChatService getService() {
            return ChatService.this;
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
    public void onCreate() {
        Timber.i("New ChatService instance created");
        super.onCreate();
        configureConnection();

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

        //Unregister all preloaded mechanism except for PLAIN and register XYAPTOKENMechanism
        Map<String, String> registeredMechs = SASLAuthentication.getRegisterdSASLMechanisms();
        for (Map.Entry<String, String> entry : registeredMechs.entrySet()) {
            if (!SASLMechanism.PLAIN.equals(entry.getValue())) {
                SASLAuthentication.blacklistSASLMechanism(entry.getValue());
            }
        }


        p2PMessageManager = new P2PMessageManager(xmpptcpConnection);
        vgcMessageManager = new VGCMessageManager(xmpptcpConnection);
        mucMessageManager = new MUCMessageManager(xmpptcpConnection);

    }

    public P2PMessageManager getP2PMessageManager(){
        return this.p2PMessageManager;
    }

    public VGCMessageManager getVgcMessageManager(){ return this.vgcMessageManager; }

    public MUCMessageManager getMucMessageManager(){ return this.mucMessageManager; }

    public XMPPTCPConnection getXMPPTCPConnection() { return this.xmpptcpConnection; }

    /**
     * Method to login to Babble using jid and password
     *
     * @param jid      full jid of the user (i.e tes1@babbleim.com)
     * @param password password of the passed jid
     */
    public void loginPlain(String jid, String password) {
        Timber.d("Logging in using plain jid " + jid + " password: " + password);

        SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());
        SASLAuthentication.blacklistSASLMechanism(XYAPTokenMechanism.MECHANISM_NAME);
        SASLAuthentication.unBlacklistSASLMechanism(SASLMechanism.PLAIN);

        try {
            xmpptcpConnection.login(jid, password,
                    SASLPlainMechanism.NAME);
        } catch (XMPPException e) {
            e.printStackTrace();
            if (e instanceof SASLErrorException) {
                SASLErrorException saslErrorException = (SASLErrorException) e;
                if (saslErrorException.getSASLFailure() != null) {
                    if (SASLError.not_authorized == saslErrorException.getSASLFailure()
                            .getSASLError()) {
                        Timber.e("Not authorized. Please login again!");
                    }
                    else if(SASLError.token_expired  == saslErrorException.getSASLFailure()
                            .getSASLError()) {
                        Timber.e("TOKEN EXPIRED WHILE DOING PLAIN");
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
    public void loginXyap(String username, String password, String yapToken) {
        Timber.d("Logging in using xyap " + username + " token: " + yapToken);
        SASLAuthentication.registerSASLMechanism(new XYAPTokenMechanism());
        SASLAuthentication.blacklistSASLMechanism(SASLMechanism.PLAIN);
        SASLAuthentication.unBlacklistSASLMechanism(XYAPTokenMechanism.MECHANISM_NAME);

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
                        if(chatReceivedListener != null){
                            chatReceivedListener.onTokenExpired();
                        }
                    }
                }
            }
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loginSkey(String jid, String password, String skey) {
        SASLAuthentication.registerSASLMechanism(new XSKEYTokenMechanism(skey));
        SASLAuthentication.blacklistSASLMechanism(SASLMechanism.PLAIN);
        SASLAuthentication.unBlacklistSASLMechanism(XSKEYTokenMechanism.MECHANISM_NAME);

        Timber.d("Logging in using skey " + skey);
        try {
            xmpptcpConnection.login(jid, skey,
                    XSKEYTokenMechanism.MECHANISM_NAME);
        } catch (XMPPException e) {
            e.printStackTrace();
            if (e instanceof SASLErrorException) {
                SASLErrorException saslErrorException = (SASLErrorException) e;
                if (saslErrorException.getSASLFailure() != null) {
                    if (SASLError.token_expired == saslErrorException.getSASLFailure()
                            .getSASLError()) {
                        Timber.e("Token expired. Logign in PLAIN ...");
                        loginPlain(jid, password);
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
        if(chatReceivedListener != null){
            chatReceivedListener.onConnecting();
        }
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

        //loginPlain("test1" + Environment.IM_SUFFIX, "vvtest1vv");
        //loginPlain(jid, password);

//        loginXyap("test1", "u49CByVnYkzw2NerzAIRIoj+8q9C5QQRdGjIqLtRIuJ6m" +
//                "/LWEEGS0dqzXP6jixUOTIkDZEb/0E/ZzGEzDRcRis/DTicAZ8vq9myQj3rsl06XlApP7hrR1a4VTe6Y");
    }



    @Override
    public boolean accept(Stanza stanza) {
        Timber.i("Stanza " + stanza.toXML().toString());
        return true;
    }


    public void setOnChatReceivedListener(ChatReceivedListener chatReceivedListener){
        this.chatReceivedListener = chatReceivedListener;
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        Timber.d("Stanza " + packet.toXML().toString());
//        if (packet instanceof ArchiveResultIQ) {
//            Timber.d("Archive endpoint = " + ((ArchiveResultIQ) packet).getEndpoint());
//        }

        if(chatReceivedListener != null){
            StanzaParser.processPacket(packet, xmpptcpConnection, chatReceivedListener);
        }
    }


    @Override
    public void connected(final XMPPConnection connection) {
        Timber.d("Connected");
        if(chatReceivedListener != null){
            chatReceivedListener.onConnected(connection);
        }
//        new Thread() {
//            @Override
//            public void run() {
//                if (!xmpptcpConnection.isAuthenticated()) {
//                    authenticate();
//                }
//            }
//        }.start();

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        if(chatReceivedListener!=null){
            chatReceivedListener.onAuthenticated(connection);
        }

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
    public void connectionClosed() {
        Timber.d("connection closed");
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
        Timber.d("reconnected failed ");
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
