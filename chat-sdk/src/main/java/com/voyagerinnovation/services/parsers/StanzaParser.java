package com.voyagerinnovation.services.parsers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.voyagerinnovation.constants.Constants;
import com.voyagerinnovation.environment.Environment;
import com.voyagerinnovation.model.Event;
import com.voyagerinnovation.services.ChatReceivedListener;
import com.voyagerinnovation.util.BabbleImageProcessorUtil;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Route;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.id.ArchiveResultIQ;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.packet.VGCUser;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

/**
 * Created by charmanesantiago on 4/13/15.
 */
public class StanzaParser {

    private static final String TAG = StanzaParser.class.getSimpleName();

    /**
     * Parses packet by identifying its type i.e Message, IQ or Route
     *
     * @param packet
     */
    public static void processPacket(Stanza packet, XMPPTCPConnection xmpptcpConnection,
                                     ChatReceivedListener chatReceivedListener) {

        if (packet instanceof Presence) {

            Presence presence = (Presence) packet;
            //TODO process presence
            if (presence.getExtension(new VGCUser().getNamespace()) != null) {
                VGCUser vgcUser = (VGCUser) presence
                        .getExtension(new VGCUser().getNamespace());

                String jid = vgcUser.getItem().getJid();
                String from = presence.getFrom();
                if (from.contains("/")) from = from.split("/")[0];
                Set<VGCUser.Status> setStatus = vgcUser.getStatus();
                if (presence.isAvailable()) {
                    if (setStatus.contains(VGCUser.Status.create(110))) {
                        chatReceivedListener.onVGCUserJoined(presence, from, jid);
                    }
                } else {
                    /**
                     * In presence unavailable, status code 321 means the user was removed by
                     * other user.
                     * status code 110 means, the user removes itself to the group.
                     */
                    if (setStatus.contains(VGCUser.Status.create(110))) {
                        chatReceivedListener.onVGCUserLeft(presence, from, jid);
                    } else if (setStatus.contains(VGCUser.Status.create(321))) {
                        chatReceivedListener.onVGCUserRemoved(presence, from, jid);
                    }
                }
            } else {
                Log.d("StanzaParser", "not VGCUser");
            }
        } else if (packet instanceof Message) {

            Message messagePacket = (Message) packet;

            chatReceivedListener.onTsReceived(messagePacket.getSource(), messagePacket.getTS());

            if (messagePacket.getType() == Message.Type.error) {
                chatReceivedListener.onErrorMessageReceived(messagePacket);
                return;
            }

            if (messagePacket.getBody() == null
                    && messagePacket.getExtension(Constants.JABBERXEVENT) != null) {
                chatReceivedListener.onEventReceived(messagePacket);
            }

//            if (messagePacket.getExtension(Constants.JABBERXCONFERENCE) != null) {
//                chatReceivedListener.onVGCInvitationReceived(messagePacket);
//            }

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
//            Timber.d("Archive endpoint = " + ((ArchiveResultIQ) packet).getEndpoint());
            chatReceivedListener.onArchiveResultReceived((ArchiveResultIQ) packet);
        } else if (packet instanceof IQ) {
            IQ iq = (IQ) packet;
            if (IQ.Type.error.equals(iq.getType().toString())) {
                chatReceivedListener.onErrorIQReceived(iq);
            }

        } else if (packet instanceof Route) {
            Route route = (Route) packet;
            Message message = route.getMessage();
            if (Message.Type.vgc.equals(message.getType())) {
                processPacket(message, xmpptcpConnection, chatReceivedListener);
            } else if (Message.Type.secret.equals(message.getType())) {
                //Ignore. Since this is a secret (ticking) message
                //and history must not be recovered.
                Log.w(TAG, "secret archive recovered but ignored");
            } else {
                String to = message.getTo();
                if (to != null && to.contains(Environment.IM_CHATROOM_SUFFIX)) {
                    Log.w(TAG, "private msg from chatroom archive recovered but ignored");
                } else {
                    Log.d("SMACK", "sending true route xml " + message.toXML().toString());
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
     * @param message
     * @param isRoute
     */
    private static void identifyMessagePacket(Message message, XMPPTCPConnection
            xmpptcpConnection, ChatReceivedListener chatReceivedListener, boolean isRoute) {

        String from[] = message.getFrom().split("/");
        String ts = message.getTS();


        String currentTimeDate = "" + System.currentTimeMillis();
        DelayInformation delayInfo = (DelayInformation) message
                .getExtension(Constants.JABBERXDELAY);

        String nickname = message.getNickname();
        if (nickname != null) {
            nickname = Html.fromHtml(nickname).toString();
        }

        if (delayInfo != null) {
            currentTimeDate = "" + delayInfo.getStamp().getTime();
        }

        DataForm form = (DataForm) message
                .getExtension(Constants.JABBERXDATA);

        if (form != null) {


            for (FormField field : form.getFields()) {
                if (Constants.VCARD.equals(field.getVariable())) {
                    processVCardAttachment(message, form.getFields(), chatReceivedListener, isRoute);
                    break;
                } else if (Constants.ATTACHMENT.equals(field
                        .getVariable())) {
                    processFileAttachment(message, field, chatReceivedListener, isRoute);
                } else if (Constants.LOCATION.equals(field
                        .getVariable())) {
                    processLocationAttachment(message, field, chatReceivedListener, isRoute);
                } else if (Constants.STICKER.equals(field
                        .getVariable())) {
                    processStickerAttachment(message, field, chatReceivedListener, isRoute);
                } else if (Constants.THUMBNAIL.equals(field
                        .getVariable())) {
                    processFileThumbnail(message, field);
                }
            }

        } else {
            if (!TextUtils.isEmpty(message.getSubject())) {
                chatReceivedListener.onVGCSubjectChanged(message);
            } else {
                // plain String message body
                processPlainMessage(message, chatReceivedListener, isRoute);
            }
        }

    }


    /**
     * Method to parse the thumbnail of an image sent as Base64 value in message.
     * Note that when the image has been generated it will be persisted in phone's
     * external storage.
     *
     * @param message
     * @param field
     */
    private static void processFileThumbnail(Message message, FormField field) {

        for (String value : field.getValues()) {
            byte[] decodedString = Base64.decode(value, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString,
                    0, decodedString.length);
            BabbleImageProcessorUtil.persistThumbnail(decodedByte,
                    message.getStanzaId());
            // ThumbnailUtil.getInstance().persistThumbnail(decodedByte,
            // messagePacket.getPacketID());
        }
    }


    public static void processFileAttachment(Message message, FormField formField,
                                             ChatReceivedListener
                                                     chatReceivedListener,
                                             boolean isRoute) {

        if (message.getType() == Message.Type.chat) {
            chatReceivedListener.onChatFileReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatFileReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatFileReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatFileReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatFileReceived(message, formField);
        } else if (message.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatFileReceived(message, formField, isRoute);
        }

    }


    private static void processVCardAttachment(Message message, List<FormField> formFields,
                                               ChatReceivedListener chatReceivedListener, boolean
                                                       isRoute) {
        if (message.getType() == Message.Type.chat) {
            chatReceivedListener.onChatVCFReceived(message, formFields, isRoute);
        } else if (message.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatVCFReceived(message, formFields, isRoute);
        } else if (message.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatVCFReceived(message, formFields, isRoute);
        } else if (message.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatVCFReceived(message, formFields, isRoute);
        } else if (message.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatVCFReceived(message, formFields, isRoute);
        } else if (message.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatVCFReceived(message, formFields);
        }

    }

    public static void processLocationAttachment(Message message, FormField formField,
                                                 ChatReceivedListener
                                                         chatReceivedListener,
                                                 boolean isRoute) {

        if (message.getType() == Message.Type.chat) {
            chatReceivedListener.onChatLocationReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatLocationReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatLocationReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatLocationReceived(message, formField,
                    isRoute);
        } else if (message.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatLocationReceived(message, formField);
        } else if (message.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatLocationReceived(message, formField, isRoute);
        }

    }

    public static void processStickerAttachment(Message message, FormField formField,
                                                ChatReceivedListener
                                                        chatReceivedListener, boolean isRoute) {

        if (message.getType() == Message.Type.chat) {
            chatReceivedListener.onChatStickerReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.vgc) {
            chatReceivedListener.onVGCChatStickerReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.secret_vgc) {
            chatReceivedListener.onAnonymousVGCChatStickerReceived(message, formField,
                    isRoute);
        } else if (message.getType() == Message.Type.groupchat) {
            chatReceivedListener.onPublicChatStickerReceived(message, formField);
        } else if (message.getType() == Message.Type.secret_chat) {
            chatReceivedListener.onAnonymousChatStickerReceived(message, formField, isRoute);
        } else if (message.getType() == Message.Type.secret) {
            chatReceivedListener.onSecretChatStickerReceived(message, formField, isRoute);
        }

    }


    public static void processPlainMessage(Message message, ChatReceivedListener
            chatReceivedListener,
                                           boolean isRoute) {

        if (message.getType() == Message.Type.chat) {
            Log.d("SMACK", " plainMessage type chat isRoute " + isRoute);
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
            chatReceivedListener.onErrorMessageReceived(message);
        }

    }


    public static Event.Type getEventType(Message message) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(message.getExtension(
                    Constants.JABBERXEVENT).toXML().toString()));
            int eventType = xpp.getEventType();
            String eventTag = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    if ("de".equals(tag)) {
                        return Event.Type.delivered;
                    } else if ("di".equals(tag)) {
                        return Event.Type.displayed;
                    } else if ("o".equals(tag)) {
                        return Event.Type.offline;
                    } else if ("sms".equals(tag)) {
                        return Event.Type.sms;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Event.Type.unknown;
    }


}
