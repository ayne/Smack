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
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.util.Map;

import timber.log.Timber;

//import org.jivesoftware.smack.SmackAndroid;

/**
 * Created by charmanesantiago on 3/25/15.
 */
public class ChatService extends Service implements ConnectionListener,
         StanzaListener, StanzaFilter {


    private XMPPTCPConnection xmppConnection;
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
        if(packet instanceof ArchiveResultIQ){
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

//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLCramMD5Mechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLDigestMD5Mechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLExternalMechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLGSSAPIMechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLJavaXMechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLJavaXSmackInitializer");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.core.SCRAMSHA1Mechanism");
//        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.core.SASLXOauth2Mechanism");


        XMPPTCPConnectionConfiguration mConnectionConfig = XMPPTCPConnectionConfiguration.builder()
                .setHost(Environment.IM_HOST)
                .setPort(Environment.IM_PORT)
                .setServiceName(Environment.IM_SERVICE_NAME)
                .setSendPresence(true)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                .setSocketFactory(new DummySSLSocketFactory())
                .setDebuggerEnabled(Environment.IS_DEBUG)
                .build();

        xmppConnection = new XMPPTCPConnection(mConnectionConfig);
        xmppConnection.addConnectionListener(this);
        xmppConnection.addSyncStanzaListener(this, this);
        xmppConnection.setRosterEnabled(false);

        Map<String, String> registeredMechs = SASLAuthentication.getRegisterdSASLMechanisms();
        for (Map.Entry<String, String> entry : registeredMechs.entrySet()){
            if(!SASLMechanism.PLAIN.equals(entry.getValue())){
                SASLAuthentication.blacklistSASLMechanism(entry.getValue());
            }
        }

        //SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());

    }

    private void loginPlain() {

//        Map<String, String> registeredMechs = SASLAuthentication.getRegisterdSASLMechanisms();
//        for (Map.Entry<String, String> entry : registeredMechs.entrySet()){
//            if(!SASLMechanism.PLAIN.equals(entry.getValue())){
//                SASLAuthentication.blacklistSASLMechanism(entry.getValue());
//            }
//        }

        try {
            xmppConnection.login("test1" + Environment.IM_SUFFIX,
                    "vvtest1vv",
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

    private void loginXyap() {
        SASLAuthentication.registerSASLMechanism(new XYAPTokenMechanism("u49CByVnYkzw2NerzAIRIoj+8q9C5QQRdGjIqLtRIuJ6m" +
                "/LWEEGS0dqzXP6jixUOTIkMa0T500XezGEzDRcRih7ozTSUnR2COJuNIftmq2baLOBSmg63A4pieZpP"));
        try {
            xmppConnection.login("test1" +
                            "u49CByVnYkzw2NerzAIRIoj+8q9C5QQRdGjIqLtRIuJ6m" +
                            "/LWEEGS0dqzXP6jixUOTIkMa0T500XezGEzDRcRih7ozTSUnR2COJuNIftmq2baLOBSmg63A4pieZpP",
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

    public void connect() {
        new Thread() {
            @Override
            public void run() {
                Timber.d("Connecting...");
                try {
                    xmppConnection.connect();
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


    @Override
    public void connected(XMPPConnection connection) {
        Timber.d("Connected");
        new Thread() {
            @Override
            public void run() {
                //loginXyap();
                loginPlain();
            }
        }.start();

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Timber.d("Authenticated xyaptoken is " + connection.getToken());
        Timber.d("Authenticated tts is " + connection.getTts());
        ArchiveIQ archiveIq = new ArchiveIQ("0", "0", "0");
        archiveIq.setTo(Environment.IM_ARCHIVE_HOST);
        archiveIq.setStanzaId(null);
        try {
            xmppConnection.sendStanza(archiveIq);
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

    public boolean isConnected() {
        return xmppConnection.isConnected();
    }

    public boolean isAuthenticated() {
        return xmppConnection.isAuthenticated();
    }

}
