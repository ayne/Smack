package com.voyagerinnovation.services.parsers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;

import com.voyagerinnovation.constants.Constants;
import com.voyagerinnovation.environment.Environment;
import com.voyagerinnovation.services.ChatReceivedListener;
import com.voyagerinnovation.util.BabbleImageProcessorUtil;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Route;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.ArchiveResultIQ;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xevent.MessageEventManager;

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
    public static void processPacket(Stanza packet, XMPPTCPConnection xmpptcpConnection,
                                     ChatReceivedListener chatReceivedListener) {
        if (packet instanceof Presence) {
            //TODO process presence
        } else if (packet instanceof Message) {

            Message messagePacket = (Message) packet;

            chatReceivedListener.onTsReceived(messagePacket.getSource(), messagePacket.getTS());

            if (messagePacket.getBody() == null
                    && messagePacket.getExtension(Constants.JABBERXEVENT) != null) {
                chatReceivedListener.onEventReceived(messagePacket);
            }

            if (messagePacket.getExtension(Constants.JABBERXCONFERENCE) != null) {
                chatReceivedListener.onVGCInvitationReceived(messagePacket);
            }

            if (messagePacket.getExtension(Constants.JABBERXDATA) != null) {
                // Process Message Attachment
                identifyMessagePacket(messagePacket, xmpptcpConnection, chatReceivedListener,
                        false);

            } else if (!TextUtils.isEmpty(messagePacket.getSubject())) {
                // Process VGC Subject Change
                chatReceivedListener.onVGCSubjectChanged(messagePacket);

            } else if (TextUtils.isEmpty(messagePacket.getBody())) {
                // Process Chat State Notification
                chatReceivedListener.onChatStateReceived(messagePacket);
            } else {
                identifyMessagePacket(messagePacket, xmpptcpConnection, chatReceivedListener,
                        false);
            }

        } else if (packet instanceof ArchiveResultIQ) {
            //TODO
            Timber.d("Archive endpoint = " + ((ArchiveResultIQ) packet).getEndpoint());
            chatReceivedListener.onArchiveResultReceived((ArchiveResultIQ) packet);
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
        } else if (packet instanceof IQ) {
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
                processPacket(message, xmpptcpConnection, chatReceivedListener);
            } else if (Message.Type.secret.equals(message.getType())) {
                //Ignore. Since this is a secret (ticking) message
                //and history must not be recovered.
                Timber.w("secret archive recovered but ignored");
            } else {
                String to = message.getTo();
                if (to != null && to.contains(Environment.IM_CHATROOM_SUFFIX)) {
                    Timber.w("private msg from chatroom archive recovered but ignored");
                } else {
                    identifyMessagePacket(message, xmpptcpConnection, chatReceivedListener, true);
                }
            }
        }
    }


    /**
     * Identifies the form of Message ie. with attachment: FileAttachmentProcessor,
     * LocationAttachmentProcessor, PlainMessageProcessor, StickerAttachmentProcessor,
     * VCardAttachmentProcessor, Group Subject Change (VGCParser.processGroupSubjectChangePacket)
     *
     * @param messagePacket
     * @param isRoute
     */
    private static void identifyMessagePacket(Message messagePacket, XMPPTCPConnection
            xmpptcpConnection,
                                              ChatReceivedListener chatReceivedListener, boolean
                                                      isRoute) {

        String from[] = messagePacket.getFrom().split("/");
        String ts = messagePacket.getTS();


        String currentTimeDate = "" + System.currentTimeMillis();
        DelayInformation delayInfo = (DelayInformation) messagePacket
                .getExtension(Constants.JABBERXDELAY);

        String nickname = messagePacket.getNickname();
        if (nickname != null) {
            nickname = Html.fromHtml(nickname).toString();
        }

        if (delayInfo != null) {
            currentTimeDate = "" + delayInfo.getStamp().getTime();
        }

        DataForm form = (DataForm) messagePacket
                .getExtension(Constants.JABBERXDATA);

        if (form != null) {


            for (FormField field : form.getFields()) {
                if (Constants.VCARD.equals(field.getVariable())) {
                    processVCardAttachment(messagePacket, field, chatReceivedListener, isRoute);
                } else if (Constants.ATTACHMENT.equals(field
                        .getVariable())) {
                    processFileAttachment(messagePacket, field, chatReceivedListener, isRoute);
                } else if (Constants.LOCATION.equals(field
                        .getVariable())) {
                    processLocationAttachment(messagePacket, field, chatReceivedListener, isRoute);
                } else if (Constants.STICKER.equals(field
                        .getVariable())) {
                    processStickerAttachment(messagePacket, field, chatReceivedListener, isRoute);
                } else if (Constants.THUMBNAIL.equals(field
                        .getVariable())) {
                    processFileThumbnail(messagePacket, field);
                }
            }

        } else {
            if (!TextUtils.isEmpty(messagePacket.getSubject())) {
                chatReceivedListener.onVGCSubjectChanged(messagePacket);
            } else {
                // plain String message body
                processPlainMessage(messagePacket, chatReceivedListener, isRoute);
            }
        }

        // DO NOT REMOVE!!! THIS IS FOR DELIVERING DISPLAYED NOTIFICATION ON
        // TYPE CHAT MESSAGES
        processDeliveredMessage(xmpptcpConnection, messagePacket);

    }


    /**
     * Method to parse the thumbnail of an image sent as Base64 value in message.
     * Note that when the image has been generated it will be persisted in phone's
     * external storage.
     *
     * @param messagePacket
     * @param field
     */
    private static void processFileThumbnail(Message messagePacket, FormField field) {

        for (String value : field.getValues()) {
            byte[] decodedString = Base64.decode(value, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString,
                    0, decodedString.length);
            BabbleImageProcessorUtil.persistThumbnail(decodedByte,
                    messagePacket.getStanzaId());
            // ThumbnailUtil.getInstance().persistThumbnail(decodedByte,
            // messagePacket.getPacketID());
        }
    }


    public static void processFileAttachment(Message messagePacket, FormField formField,
                                             ChatReceivedListener
            chatReceivedListener,
                                             boolean isRoute) {

        if (messagePacket.getType() == Message.Type.chat) {
            chatReceivedListener.onChatFileReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatFileReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatFileReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatFileReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatFileReceived(messagePacket, formField);
        } else if (messagePacket.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatFileReceived(messagePacket, formField, isRoute);
        }

    }


    private static void processVCardAttachment(Message messagePacket, FormField formField,
                                               ChatReceivedListener chatReceivedListener, boolean
                                                       isRoute) {
        if (messagePacket.getType() == Message.Type.chat) {
            //TODO onChatReceived(isRoute)
            chatReceivedListener.onChatVCFReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret) {
            //TODO onSecretChatReceived(isRoute)
            chatReceivedListener.onSecretChatVCFReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatVCFReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatVCFReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatVCFReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatVCFReceived(messagePacket, formField);
        }

    }

    public static void processLocationAttachment(Message messagePacket, FormField formField,
                                                 ChatReceivedListener
            chatReceivedListener,
                                                 boolean isRoute) {

        if (messagePacket.getType() == Message.Type.chat) {
            chatReceivedListener.onChatLocationReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatLocationReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatLocationReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatLocationReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatLocationReceived(messagePacket, formField);
        } else if (messagePacket.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatLocationReceived(messagePacket, formField, isRoute);
        }

    }

    public static void processStickerAttachment(Message messagePacket, FormField formField,
                                                ChatReceivedListener
            chatReceivedListener, boolean isRoute) {

        if (messagePacket.getType() == Message.Type.chat) {
            chatReceivedListener.onChatStickerReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatStickerReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatStickerReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatStickerReceived(messagePacket, formField);
        } else if (messagePacket.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatStickerReceived(messagePacket, formField, isRoute);
        } else if (messagePacket.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatStickerReceived(messagePacket, formField, isRoute);
        }

    }


    public static void processPlainMessage(Message message, ChatReceivedListener
            chatReceivedListener,
                                           boolean isRoute) {

        if (message.getType() == Message.Type.chat) {
            chatReceivedListener.onChatReceived(message, isRoute);
        } else if (message.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatReceived(message, isRoute);
        } else if (message.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatReceived(message, isRoute);
        } else if (message.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatReceived(message, isRoute);
        } else if (message.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatReceived(message);
        } else if (message.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatReceived(message, isRoute);
        } else if (message.getType() == Message.Type.info) {
            if (!isRoute) {
                // update old msisdn jid to new sso jid
                chatReceivedListener.onUpdateContactReceived(message.getMsisdn(), message.getFrom
                        ());
            }
        } else if (message.getType() == Message.Type.error) {
            Timber.e("I got an error");
//            TODO if (message.getError() != null) {
//                if (message.getError().getCode() == 406) {
//                    Log.d(TAG, "############## code  error 406");
//                    VGCParser.processErrorPacket(context, packetId, from,
//                            message.getError().getMessage(), currentTimeDate, status, subject,
//                            serverTime,
//                            MultiUserMessagesTable.MEMBER_PRESENCE);
//                }
//            }
        }

    }


    public static void processDeliveredMessage(XMPPTCPConnection xmpptcpConnection,
                                               Message messagePacket) {
        if (messagePacket.getType() == Message.Type.chat ||
                messagePacket.getType() == Message.Type.secret
                || messagePacket.getType() == Message.Type.secret_chat) {
            MessageEventManager msgEventMgr = new MessageEventManager(
                    xmpptcpConnection);

            try {
                msgEventMgr.sendDisplayedNotification(
                        messagePacket.getFrom(),
                        messagePacket.getStanzaId());
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }

        } else if (messagePacket.getType() == Message.Type.error) {
            //TODO
//            if (messagePacket.getError().getCode() == 406) {
//                // current user not occupant of room
//
//            } else if (messagePacket.getError().getCode() == 404) {
//                // recipient has left the room
//
//            }
        }
    }


}
