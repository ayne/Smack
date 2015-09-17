package com.voyagerinnovation.services.managers;

import android.os.RemoteException;
import android.text.TextUtils;

import com.voyagerinnovation.constants.Constants;
import com.voyagerinnovation.util.BabbleImageProcessorUtil;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xevent.MessageEventManager;

/**
 * Created by charmanesantiago on 4/11/15.
 */
public class P2PMessageManager {

    XMPPTCPConnection xmpptcpConnection;

    public P2PMessageManager(XMPPTCPConnection xmpptcpConnection) {
        this.xmpptcpConnection = xmpptcpConnection;
    }

    /**
     * Method to send message of type "chat" to a contact. (P2P)
     *
     * @param packetId     The packet id to be used in the message. Usually you should pass
     *                     null here. But for the purpose of resending the same message, you
     *                     can pass the id of the said message.
     * @param body         The body or message content of an XMPP message.
     * @param toJid        The jid of the receiving party.
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendMessage(String packetId, String body, String toJid, String senderMsisdn,
                               String senderName) {
        Message message = new Message();
        if (packetId != null) {
            message.setStanzaId(packetId);
        }
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setBody(body);
        message.setTo(toJid);
        message.setType(Message.Type.chat);
        message.setThread(null);
        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return message;
    }


    /**
     * Method to send message of type "sms" to a contact. (P2P)
     *
     * @param packetId     The packet id to be used in the message. Usually you should pass
     *                     null here. But for the purpose of resending the same message, you
     *                     can pass the id of the said message.
     * @param body         The body or message content of an XMPP message.
     * @param toJid        The jid of the receiving party.
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendSms(String packetId, String body, String toJid, String senderMsisdn,
                               String senderName) {
        Message message = new Message();
        if (packetId != null) {
            message.setStanzaId(packetId);
        }
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setBody(body);
        message.setTo(toJid);
        message.setType(Message.Type.sms);
        message.setThread(null);
        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return message;
    }

    /**
     * Method to send an IP to CS message via XMPP
     *
     * @param packetId   The packet id to be used in the message. Usually you should pass
     *                   null here. But for the purpose of resending the same message, you
     *                   can pass the id of the said message.
     * @param body       The body or message content of an XMPP message.
     * @param toJid      The jid of the receiving party.
     * @param imCsSuffix
     * @return Message          The actual Message that was sent.
     */
    public Message sendIpToCsSms(String packetId, String body, String toJid, String imCsSuffix) {
        Message newMessage = new Message();
        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        if (!toJid.contains(imCsSuffix)) {
            toJid = toJid.split("@")[0].concat(imCsSuffix);
        }
        ;

        newMessage.setBody(body);
        newMessage.setTo(toJid);
        newMessage.setType(Message.Type.chat);
        newMessage.setThread(null);
        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return newMessage;
    }

    /**
     * Method to send a type secret_chat message
     *
     * @param packetId    The packet id to be used in the message. Usually you should pass
     *                    null here. But for the purpose of resending the same message, you
     *                    can pass the id of the said message.
     * @param body        The body of the message to be sent
     * @param toAnonymous jid i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname    The nickname used in doing the anonymous message
     * @return Message    The actual message that was sent.
     */
    public Message sendMessageAnonymously(String packetId, String body,
                                          String toAnonymous, String nickname) {

        Message newMessage = new Message();
        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }
        String toJID = toAnonymous;

        if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJID = toAnonymous.split("&")[1];
            //set nickname to null since this should REPLY to an anonymous user
            nickname = null;
        } else if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJID = toAnonymous.split("#")[1];
        } else if (toAnonymous.contains("%")) {
            // ignore. this is the 1st char splitter for secret chat which was
            // replaced by & due to SQLite
            return newMessage;
        }
        newMessage.setBody(body);
        //set message packet's to, to real jid of the recipient
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);
        newMessage.setThread(null);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }

    /**
     * This method send a message of type secret. Not secret_chat. This differs from a secret_chat
     * because the message is not anonymous. And is also different from normal chat because this
     * message will disappear
     *
     * @param body  The body of the message to be sent
     * @param toJid The jid of the user that will receive the secret message
     * @return Message        The actual Message that was sent.
     */
    public Message sendSecretMessage(String body, String toJid) {
        Message message = new Message();
        message.setBody(body);
        message.setTo(toJid);
        message.setType(Message.Type.secret);
        message.setThread(null);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * This method send a message of type secret with image. Not secret_chat. This differs from a
     * secret_chat
     * because the message is not anonymous. And is also different from normal chat because this
     * message
     * will disappear
     * @param packetId      Could be null. But usually has value since message with image is usually
     *                      displayed in listviews alreasy before completion of the upload.
     * @param toJid         The real JID to be set as "to" in message packet.
     * @param attachmentUrl The url to be inserted in the message, inwhich the receiver can
     *                      download the image
     * @param localUrl      The local url of the image attached. This is needed for the base 64
     *                      thumbnail generation
     *                      of the image which will be embedded in the message as well.
     * @param mimeType      The image type of the attachment.
     * @return Message          The actual Message that was sent.
     */
    public Message sendSecretImage(String packetId, String attachmentUrl,
                                   String localUrl, String toJid, String mimeType) {
        Message message = new Message();
        if(packetId != null){
            message.setStanzaId(packetId);
        }
        message.setTo(toJid);
        message.setType(Message.Type.secret);
        message.setBody("secret");

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        String base64 = BabbleImageProcessorUtil.generateThumbnail(message.getStanzaId(), localUrl,
                mimeType);
        FormField thumbnailField = new FormField(Constants.THUMBNAIL);
        thumbnailField.addValue(base64);
        form.addField(thumbnailField);
        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * This method send a message of type secret with sticker. Not secret_chat. This differs from a
     * secret_chat
     * because the message is not anonymous. And is also different from normal chat because this
     * message will disappear
     *
     * @param packetId The packet id to be used in the message. Usually you should pass
     *                 null here. But for the purpose of resending the same message, you
     *                 can pass the id of the said message.
     * @param toJid    The real JID to be set as "to" in message packet and to be inserted in the db
     * @param body     The sticker id to be sent. it is sent as a body of a message.
     * @return Message         The actual Message that was sent.
     */
    public Message sendSecretSticker(String packetId, String body, String toJid) throws
            RemoteException {

        Message newMessage = new Message();
        newMessage.setTo(toJid);
        newMessage.setType(Message.Type.secret);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);

        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }

    /**
     * Method to send a secret message with location
     *
     * @param body  The lat lng of the location which is sent as a body of a message.
     * @param toJid The real JID to be set as "to" of the message stanza.
     * @return Message  The actual Message that was sent.
     */
    public Message sendSecretLocationAttachment(String body, String toJid){
        Message newMessage = new Message();
        newMessage.setTo(toJid);
        newMessage.setType(Message.Type.secret);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }

    /**
     * Method to send secret message with audio
     *@param packetId      Could be null. But usually has value since message with image is usually
     *                      displayed in listviews alreasy before completion of the upload.
     * @param attachmentUrl The url to be included in the message stanza, inwhich the receiving
     *                      party can download the audio attachment.
     * @param toJid         The jid of the receiving party.
     * @param mimeType
     * @return Message         The actual Message that was sent.
     */
    public Message sendSecretAudioAttachment(String packetId, String attachmentUrl, String toJid,
                                             String mimeType) {
        Message message = new Message();
        if(packetId != null){
            message.setStanzaId(packetId);
        }
        message.setTo(toJid);
        message.setType(Message.Type.secret);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to send a type chat message  with image attachment
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The url to be inserted in the message, inwhich the receiver can
     *                      download the image
     * @param localUrl      The local url of the image attached. This is needed for the base 64
     *                      thumbnail generation
     *                      of the image which will be embedded in the message as well.
     * @param mimeType      The image type of the attachment.
     * @param toJid         The real JID of the recipient (+MSISDN@babbleim.com)
     * @param senderMsisdn  The msisdn of the sender (logged in user). If null, no msisdn will be
     *                      inserted.
     * @param senderName    The name of the sender (logged in user). If null, no name will be
     *                      inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendImageAttachment(String packetId, String attachmentUrl,
                                       String localUrl, String toJid,
                                       String mimeType, String senderMsisdn, String senderName) {
        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setTo(toJid);
        message.setType(Message.Type.chat);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }
        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);
        field.setMedia(mediaField);
        form.addField(field);

        String base64 = BabbleImageProcessorUtil.generateThumbnail(packetId, localUrl,
                mimeType);
        FormField thumbnailField = new FormField(Constants.THUMBNAIL);
        thumbnailField.addValue(base64);
        form.addField(thumbnailField);

        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }


    /**
     * Method to send an anonymous message with image attachment
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The url to be inserted in the message, inwhich the receiver can
     *                      download the image
     * @param localUrl      The local url of the image attached. This is needed for the base 64
     *                      thumbnail generation
     *                      of the image which will be embedded in the mes
     * @param toAnonymous   i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                      this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                      but will use the original value as JID when inserting in MessagesTable
     *                      and ConversationsTable
     * @param nickname      The nickname or "alias" to be used when sending the message
     * @return Message      The actual Message that was sent.
     */
    public Message sendImageAttachmentAnonymously(String packetId,
                                                  String attachmentUrl, String localUrl,
                                                  String toAnonymous,
                                                  String mimeType, String nickname) {
        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        String toJID = toAnonymous;
        if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJID = toAnonymous.split("&")[1];
            //set nickname to null since this should REPLY to an anonymous user
            nickname = null;
        } else if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJID = toAnonymous.split("#")[1];
        } else if (toAnonymous.contains("%")) {
            // ignore. this is the 1st char splitter for secret chat which was
            // replaced by & due to SQLite
            return message;
        }

        message.setTo(toJID);
        message.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);
        field.setMedia(mediaField);
        form.addField(field);

        String base64 = BabbleImageProcessorUtil.generateThumbnail(packetId, localUrl,
                mimeType);
        FormField thumbnailField = new FormField(Constants.THUMBNAIL);
        thumbnailField.addValue(base64);
        form.addField(thumbnailField);

        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to send a type chat message  with audio recording
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The url to be included in the message stanza, inwhich the receiving
     *                      party can download the audio attachment.
     * @param toJid         The jid of the receiving party.
     * @param mimeType
     * @param senderMsisdn  The msisdn of the sender (logged in user). If null, no msisdn will be
     *                      inserted.
     * @param senderName    The name of the sender (logged in user). If null, no name will be
     *                      inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendAudioAttachment(String packetId, String attachmentUrl, String toJid,
                                       String mimeType, String senderMsisdn, String senderName) {
        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setTo(toJid);
        message.setType(Message.Type.chat);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }


    /**
     * Method to send a type secret_chat message with audio recording.
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The remote url of the uploaded image, that is to be attached in the
     *                      form field of the message stanza
     * @param toAnonymous   i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                      this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                      but will use the original value as JID when inserting in MessagesTable
     *                      and ConversationsTable
     * @param nickname
     * @return Message      The actual Message that was sent.
     */
    public Message sendAudioAttachmentAnonymously(String packetId,
                                                  String attachmentUrl,
                                                  String toAnonymous,
                                                  String mimeType, String nickname) {
        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        String toJID = toAnonymous;
        if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJID = toAnonymous.split("&")[1];
            nickname = null;
        } else if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJID = toAnonymous.split("#")[1];
        } else if (toJID.contains("%")) {
            // ignore. this is the 1st char splitter for secret chat which was
            // replaced by & due to SQLite
            return message;
        }
        message.setTo(toJID);
        message.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to send a type chat message  with location
     *
     * @param packetId     The packet id to be assigned in the message stanza. If this is null,
     *                     a new packet id will be generated else, the passed value is used and
     *                     will be treated as resending the message
     * @param body         Should contain the lat,long of the location to be sent.
     * @param toJid        The real JID of the recipient (+MSISDN@babbleim.com)
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendLocationAttachment(String packetId, String body, String toJid, String
            senderMsisdn,
                                          String senderName) {
        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setTo(toJid);
        message.setType(Message.Type.chat);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to send a type secret_chat message with location
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        The message body should be a String containing the lat,
     *                    long of the location
     * @param toAnonymous i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname     The nickname to be used when sending the anonymous message.
     * @return Message      The actual Message that was sent.
     */
    public Message sendLocationAttachmentAnonymously(String packetId, String body,
                                                     String toAnonymous, String nickname) {

        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        String toJID = toAnonymous;
        if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJID = toAnonymous.split("&")[1];
            nickname = null;
        } else if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJID = toAnonymous.split("#")[1];
        } else if (toAnonymous.contains("%")) {
            // ignore. this is the 1st char splitter for secret chat which was
            // replaced by & due to SQLite
            return message;
        }

        message.setTo(toJID);
        message.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to send a type chat message  with VCF file contact
     *
     * @param packetId     The packet id to be assigned in the message stanza. If this is null,
     *                     a new packet id will be generated else, the passed value is used and
     *                     will be treated as resending the message
     * @param toJid        The real JID of the recipient (+MSISDN@babbleim.com)
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendVCFAttachment(String packetId, String body, String toJid, String
            senderMsisdn,
                                     String senderName) {

        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setTo(toJid);
        message.setType(Message.Type.chat);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        message.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * method to send a type secret message  with VCF file contact
     *
     * @param toJid        The real JID of the recipient (+MSISDN@babbleim.com)
     * @return Message          The actual Message that was sent.
     */
    public Message sendSecretVCFAttachment(String body, String toJid) {

        Message message = new Message();
        message.setTo(toJid);
        message.setType(Message.Type.secret);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        message.addExtension(form);
        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to send a type secret_chat message with vcf file contact
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        Empty *double check
     * @param toAnonymous i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname
     * @return Message          The actual Message that was sent.
     */
    public Message sendVCFAttachmentAnonymously(String packetId, String body,
                                                String toAnonymous, String nickname) {

        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        String toJID = toAnonymous;
        if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJID = toAnonymous.split("&")[1];
            nickname = null;
        } else if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJID = toAnonymous.split("#")[1];
        } else if (toAnonymous.contains("%")) {
            // ignore. this is the 1st char splitter for secret chat which was
            // replaced by & due to SQLite
            return message;
        }

        message.setTo(toJID);
        message.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        message.addExtension(form);
        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to send chat state (i.e composing or paused)
     *
     * @param toJid     The jid of the receiving party that will receive the chat state.
     * @param nickname  The nickname to be used when chat state is anonymous. null if normal
     *                  chat message.
     * @param chatState int value 1 for CHAT_STATE_COMPOSING, 2 for CHAT_STATE_PAUSED
     * @throws RemoteException
     */
    public void sendChatState(String toJid, String nickname, int chatState)
            throws RemoteException {

        if (toJid.contains(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJid = toJid.split(Constants.JID_SECRET_CHAT_ADMIRER)[1];
        } else if (toJid.contains(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJid = toJid.split(Constants.JID_SECRET_CHAT_ADMIREE)[1];
        }

        if (xmpptcpConnection.isConnected() && xmpptcpConnection.isAuthenticated()) {
            Message newMessage = new Message();
            //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
            newMessage.setBody("");
            newMessage.setTo(toJid);
            if (nickname != null) {
                newMessage.setType(Message.Type.secret_chat);
                nickname = TextUtils.htmlEncode(nickname);
                newMessage.setNickname(nickname);
            } else {
                newMessage.setType(Message.Type.chat);
                newMessage.setNickname(null);
            }
            ChatState rChatState;
            switch (chatState) {
                case Constants.CHAT_STATE_COMPOSING: {
                    rChatState = ChatState.composing;
                }
                break;
                default: // IMConstants.CHAT_STATE_PAUSED:
                {
                    rChatState = ChatState.paused;
                }
            }

            try {
                ChatStateExtension chatStateExtension = new ChatStateExtension(
                        rChatState);
                newMessage.addExtension(chatStateExtension);
                xmpptcpConnection.sendStanza(newMessage);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                // do nothing.
            } catch (NullPointerException e) {
                // do nothing
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Method to send a "di" displayed receipt of a message
     *
     * @param toJid The jid that will receive the "di"
     * @param id    The stanza id of the message that was displayed.
     */
    public void sendDisplayedNotification(String toJid, String id) {

        if (xmpptcpConnection.isConnected()
                && xmpptcpConnection.isAuthenticated()) {
            MessageEventManager messageEventManager = new MessageEventManager(
                    xmpptcpConnection);
            try {
                messageEventManager.sendDisplayedNotification(toJid, id);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Method to send a message of type chat with sticker.
     *
     * @param packetId     The packet id to be used in the message. Usually you should pass
     *                     null here. But for the purpose of resending the same message, you
     *                     can pass the id of the said message.
     * @param body         The body or message content of an XMPP message.
     * @param toJid        The jid of the receiving party.
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendSticker(String packetId, String body, String toJid, String senderMsisdn,
                               String senderName) {

        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);
        message.setTo(toJid);
        message.setType(Message.Type.chat);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);

        message.addExtension(form);
        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }


    /**
     * method to send a type secret_chat message with vcf file contact
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        Body should contain the sticker ID
     * @param toAnonymous i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname     The nickname to be used when sending the message ("alias").
     * @return Message      The actual Message that was sent.
     */
    public Message sendStickerAnonymously(String packetId, String body, String toAnonymous, String
            nickname) {

        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        String toJID = toAnonymous;
        if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIREE)) {
            toJID = toAnonymous.split("&")[1];
            nickname = null;
        } else if (toAnonymous.startsWith(Constants.JID_SECRET_CHAT_ADMIRER)) {
            toJID = toAnonymous.split("#")[1];
        } else if (toAnonymous.contains("%")) {
            // ignore. this is the 1st char splitter for secret chat which was
            // replaced by & due to SQLite
            return message;
        }

        message.setTo(toJID);
        message.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);

        message.addExtension(form);
        try {
            xmpptcpConnection.sendStanza(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to insert SSO profile details in message stanza
     *
     * @param message The Message to be modified.
     * @param msisdn  The msisdn to be inserted. If value is null, no msisdn will be placed.
     * @param name    The name to be inserted (i.e "SSOFirstName SSOLastName"). If value is null no
     *                name will be placed.
     */
    private void insertMsisdnAndName(Message message, String msisdn, String
            name) {
        if (!TextUtils.isEmpty(msisdn)) {
            message.setMsisdn(msisdn);
        }
        if (!TextUtils.isEmpty(name)) {
            message.setName(name);
        }
    }
}
