package com.voyagerinnovation.services;

import android.text.TextUtils;

import com.voyagerinnovation.constants.Constants;
import com.voyagerinnovation.environment.Environment;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Route;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.ArchiveResultIQ;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import timber.log.Timber;

/**
 * Created by charmanesantiago on 4/13/15.
 */
public class StanzaParser {

    /**
     * Parses packet by identifying its type i.e Message, IQ or Route
     *
     * @param packet
     */
    public void processPacket(Stanza packet, XMPPTCPConnection xmpptcpConnection) {
        if (packet instanceof Presence) {
            //TODO process presence
        } else if (packet instanceof Message) {

            Message messagePacket = (Message) packet;

            if (messagePacket.getBody() == null
                    && messagePacket.getExtension(Constants.JABBERXEVENT) != null) {
                //TODO process Event
            }

            if (messagePacket.getExtension(Constants.JABBERXCONFERENCE) != null) {
                //TODO process VGC Invite
            }

            if (messagePacket.getExtension(Constants.JABBERXDATA) != null) {
                // Process Message Attachment
                //TODO identifyMessagePacket(connection, messagePacket, false);
            } else if (!TextUtils.isEmpty(messagePacket.getSubject())) {
                // Process VGC Subject Change
//                TODO VGCParser.processGroupSubjectChangePacket(context, messagePacket,
//                        multiUserChatManager);
            } else if (messagePacket.getBody().trim().length() == 0) {
                // Process Chat State Notification
//                TODO P2PParser.processChatStateNotification(context, messagePacket,
//                        messageManager);
            } else {
                //TODO identifyMessagePacket(connection, messagePacket, false);
            }

        } else if (packet instanceof ArchiveResultIQ) {
            //TODO
            Timber.d("Archive endpoint = " + ((ArchiveResultIQ) packet).getEndpoint());
//            String endpoint = iq.getEndpoint();
//            String count = iq.getCount();
//
//            if (endpoint != null || count != null) {
//                // then this is a successful iq of archive
//                preferencesHelper.setFRM("0");
//            }

//                TODO parse message archive from API
//                if(endpoint != null){
//                    Intent intent = new Intent(this, MessageArchiveIntentService.class);
//                    intent.putExtra(MessageArchiveIntentService.KEY_ENDPOINT, endpoint);
//                    startService(intent);
//                }
        }

        else if (packet instanceof IQ) {
            IQ iq = (IQ) packet;
            if ("error".equals(iq.getType().toString())) {
                if (iq.getError().toString().contains("403") || iq.getError().toString().contains
                        ("404")) {
                    Timber.d("delete start");
                    //TODO delete all entry of this vgc group
                }
            }

        } else if (packet instanceof Route) {
            Route route = (Route) packet;
            Message message = route.getMessage();
            if (Message.Type.vgc.equals(message.getType())) {
                processPacket(message, xmpptcpConnection);
            } else if (Message.Type.secret.equals(message.getType())) {
                //Ignore. Since this is a secret (ticking) message
                //and history must not be recovered.
                Timber.w("secret archive recovered but ignored");
            } else {
                String to = message.getTo();
                if (to != null && to.contains(Environment.IM_CHATROOM_SUFFIX)) {
                    Timber.w("private msg from chatroom archive recovered but ignored");
                } else {
                    //TODO identifyMessagePacket(connection, message, true);
                }
            }
        }
    }

}
