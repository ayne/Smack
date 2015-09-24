package com.voyagerinnovation.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
import org.jivesoftware.smackx.iqlast.LastActivityManager;

import java.io.IOException;
import java.util.Map;

//import org.jivesoftware.smack.SmackAndroid;

/**
 * Service that handles connection to XMPP server as well as parsing of incoming stanzas.
 * Created by charmanesantiago on 3/25/15.
 */
public class ChatService extends Service implements ConnectionListener,
        StanzaListener, StanzaFilter {

    private static final String TAG = ChatService.class.getSimpleName();

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
        Log.i(TAG, "New ChatService instance created");
        super.onCreate();
        configureConnection();

    }

    public void configureConnection(boolean isDebuggable, String host, int port,
                                    String serviceName, String resource, boolean sendPresence,
                                    ConnectionConfiguration.SecurityMode securityMode){
        SmackConfiguration.DEBUG = isDebuggable;

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
                .setHost(host)
                .setPort(port)
                .setServiceName(serviceName)
                .setResource(resource)
                .setSendPresence(sendPresence)
                .setSecurityMode(securityMode)
                .setSocketFactory(new DummySSLSocketFactory())
                .setDebuggerEnabled(isDebuggable)
                .build();

        xmpptcpConnection = new XMPPTCPConnection(mConnectionConfig);
        xmpptcpConnection.addConnectionListener(this);
        xmpptcpConnection.addSyncStanzaListener(this, this);
        xmpptcpConnection.setRosterEnabled(false);
        xmpptcpConnection.setUseStreamManagement(false);
        xmpptcpConnection.setUseStreamManagementResumption(false);

        //Unregister all preloaded mechanism except for PLAIN and register XYAPTOKENMechanism
        Map<String, String> registeredMechs = SASLAuthentication.getRegisterdSASLMechanisms();
        for (Map.Entry<String, String> entry : registeredMechs.entrySet()) {
            if (!SASLMechanism.PLAIN.equals(entry.getValue())) {
                SASLAuthentication.blacklistSASLMechanism(entry.getValue());
            }
        }
        SASLAuthentication.registerSASLMechanism(new XYAPTokenMechanism());
        SASLAuthentication.registerSASLMechanism(new XSKEYTokenMechanism());


        p2PMessageManager = new P2PMessageManager(xmpptcpConnection);
        vgcMessageManager = new VGCMessageManager(xmpptcpConnection);
        mucMessageManager = new MUCMessageManager(xmpptcpConnection);
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
                .setResource(Environment.IM_RESOURCE)
                .setSendPresence(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setSocketFactory(new DummySSLSocketFactory())
                .setDebuggerEnabled(Environment.IS_DEBUG)
                .build();

        xmpptcpConnection = new XMPPTCPConnection(mConnectionConfig);
        xmpptcpConnection.addConnectionListener(this);
        xmpptcpConnection.addSyncStanzaListener(this, this);
        xmpptcpConnection.setRosterEnabled(false);
        xmpptcpConnection.setUseStreamManagement(false);
        xmpptcpConnection.setUseStreamManagementResumption(false);

        //Unregister all preloaded mechanism except for PLAIN and register XYAPTOKENMechanism
        Map<String, String> registeredMechs = SASLAuthentication.getRegisterdSASLMechanisms();
        for (Map.Entry<String, String> entry : registeredMechs.entrySet()) {
            if (!SASLMechanism.PLAIN.equals(entry.getValue())) {
                SASLAuthentication.blacklistSASLMechanism(entry.getValue());
            }
        }
        SASLAuthentication.registerSASLMechanism(new XYAPTokenMechanism());
        SASLAuthentication.registerSASLMechanism(new XSKEYTokenMechanism());


        p2PMessageManager = new P2PMessageManager(xmpptcpConnection);
        vgcMessageManager = new VGCMessageManager(xmpptcpConnection);
        mucMessageManager = new MUCMessageManager(xmpptcpConnection);

    }

    public P2PMessageManager getP2PMessageManager() {
        return this.p2PMessageManager;
    }

    public VGCMessageManager getVgcMessageManager() {
        return this.vgcMessageManager;
    }

    public MUCMessageManager getMucMessageManager() {
        return this.mucMessageManager;
    }

    public XMPPTCPConnection getXMPPTCPConnection() {
        return this.xmpptcpConnection;
    }


    public void instantShutDown() {
        xmpptcpConnection.instantShutdown();
    }

    /**
     * Method to login to Babble using jid and password
     *
     * @param jid      full jid of the user (i.e tes1@babbleim.com)
     * @param password password of the passed jid
     */
    public void loginPlain(String jid, String password) {

        SASLAuthentication.blacklistSASLMechanism(XYAPTokenMechanism.MECHANISM_NAME);
        SASLAuthentication.blacklistSASLMechanism(XSKEYTokenMechanism.MECHANISM_NAME);
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
                        Log.e(TAG, "Not authorized. Please login again!");
                        if (chatReceivedListener != null) {
                            chatReceivedListener.onNotAuthorized();
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

    /**
     * Method to login to Babble via the XYAP Token.
     *
     * @param username The username (not full jid) of the user.
     * @param yapToken The token (x-yap-token) to be used for logging in.
     *                 This is retrieved from <success></success> response after <auth></auth>
     */
    public void loginXyap(String username, String yapToken) {

        SASLAuthentication.blacklistSASLMechanism(SASLMechanism.PLAIN);
        SASLAuthentication.blacklistSASLMechanism(XSKEYTokenMechanism.MECHANISM_NAME);
        SASLAuthentication.unBlacklistSASLMechanism(XYAPTokenMechanism.MECHANISM_NAME);

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
                        if (chatReceivedListener != null) {
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

    public void loginSkey(String jid, String skey) {

        SASLAuthentication.blacklistSASLMechanism(SASLMechanism.PLAIN);
        SASLAuthentication.blacklistSASLMechanism(XYAPTokenMechanism.MECHANISM_NAME);
        SASLAuthentication.unBlacklistSASLMechanism(XSKEYTokenMechanism.MECHANISM_NAME);
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
                        if (chatReceivedListener != null) {
                            chatReceivedListener.onSkeyExpired();
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

    /**
     * Method to connect to XMPP server. This method performs an automatic login to the server
     * if previous connection state was logged (authenticated).
     */
    public void connect() {
        if (chatReceivedListener != null) {
            chatReceivedListener.onConnecting();
        }
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Connecting... is connected " + xmpptcpConnection.isConnected());
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
        if(chatReceivedListener != null){
            chatReceivedListener.onDisconnecting();
        }
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Disconnecting...");
                xmpptcpConnection.disconnect();
            }
        }.start();
    }

    @Override
    public boolean accept(Stanza stanza) {
        return true;
    }


    /**
     * Method to set the ChatReceivedListener for this service.
     * @param chatReceivedListener ChatReceivedListener
     */
    public void setOnChatReceivedListener(ChatReceivedListener chatReceivedListener) {
        this.chatReceivedListener = chatReceivedListener;
    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
//        if (packet instanceof ArchiveResultIQ) {
//            Timber.d("Archive endpoint = " + ((ArchiveResultIQ) packet).getEndpoint());
//        }

        if (chatReceivedListener != null) {
            StanzaParser.processPacket(packet, xmpptcpConnection, chatReceivedListener);
        }
    }


    @Override
    public void connected(final XMPPConnection connection) {
        Log.d(TAG, "Connected");
        if (chatReceivedListener != null) {
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
        if (chatReceivedListener != null) {
            chatReceivedListener.onAuthenticated(connection);
        }

        yapToken = connection.getToken();
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
        Log.d(TAG, "connection closed");
        if(chatReceivedListener != null){
            chatReceivedListener.onDisconnected();
        }
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Log.d(TAG, "connectionClosedOnError " + e.getMessage());
        if ("stream:error (conflict) text: Replaced by new connection".equals(e.getMessage())) {
            Log.d(TAG, "Stream conflict error");
            if(chatReceivedListener != null){
                chatReceivedListener.onAuthenticationFailed();
            }
        }
        else{
            if(chatReceivedListener != null){
                chatReceivedListener.onDisconnected();
            }
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
        Log.d(TAG, "reconnection failed ");
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

    public long getIdleTime(String jid) {
        try {
            return LastActivityManager.getInstanceFor(xmpptcpConnection).getLastActivity(jid)
                    .getIdleTime();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
