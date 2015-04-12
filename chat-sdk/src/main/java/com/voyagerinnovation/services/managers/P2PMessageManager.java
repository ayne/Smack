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

    public void sendMessage(String packetId, String message, String toJID) throws RemoteException {
        Message newMessage = new Message();
        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }
        newMessage.setBody(message);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.chat);
        newMessage.setThread(null);
        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to send an IP to CS message via XMPP
     *
     * @param packetId
     * @param message
     * @param toJID
     * @param imCsSuffix
     * @throws RemoteException
     */
    public void sendIpToCsSms(String packetId, String message, String toJID, String imCsSuffix)
            throws RemoteException {
        Message newMessage = new Message();
        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        if (!toJID.contains(imCsSuffix)) {
            toJID = toJID.split("@")[0].concat(imCsSuffix);
        }
        ;

        newMessage.setBody(message);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.chat);
        newMessage.setThread(null);
        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to send a type secret_chat message from the actual IMMessageManager
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param message     The body of the message to be sent
     * @param toAnonymous jid i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname    The nickname used in doing the anonymous message
     * @throws android.os.RemoteException
     */
    public void sendMessageAnonymously(String packetId, String message,
                                       String toAnonymous, String nickname)
            throws RemoteException {

        Message newMessage = new Message();
        if (packetId != null) {
            newMessage.setPacketID(packetId);
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
            return;
        }
        newMessage.setBody(message);
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
    }

    /**
     * This method send a message of type secret. Not secret_chat. This differs from a secret_chat
     * because the message is not anonymous. And is also different from normal chat because this
     * message
     * will disappear
     *
     * @param message The body of the message to be sent
     * @param to      The jid that will receive the secret message
     */
    public void sendSecretMessage(String message, String to) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setBody(message);
        newMessage.setTo(to);
        newMessage.setType(Message.Type.secret);
        newMessage.setThread(null);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method send a message of type secret with image. Not secret_chat. This differs from a
     * secret_chat
     * because the message is not anonymous. And is also different from normal chat because this
     * message
     * will disappear
     *
     * @param toJID the real JID to be set as "to" in message packet and to be inserted in the db
     */
    public void sendSecretImage(String packetId, String attachmentUrl,
                                String localUrl, String toJID, String mimeType)
            throws RemoteException {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret);
        newMessage.setBody("secret");

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
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
        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method send a message of type secret with sticker. Not secret_chat. This differs from a
     * secret_chat
     * because the message is not anonymous. And is also different from normal chat because this
     * message
     * will disappear
     * //TODO add sample body for sending sticker
     *
     * @param to   the real JID to be set as "to" in message packet and to be inserted in the db
     * @param body the sticker id to be sent. it is sent as a body of a message.
     */
    public void sendSecretSticker(String packetId, String body, String to) throws RemoteException {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(to);
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
    }

    /**
     * Method to send a secret message with location
     * //TODO add sample lat lng body
     *
     * @param packetId
     * @param body     the lat lng of the location
     * @param toJID
     */
    public void sendSecretLocationAttachment(String packetId, String body, String toJID) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);
        String snippet = "Location";

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to send secret message with audio
     *
     * @param packetId
     * @param attachmentUrl The url of the audio clip
     * @param toJID
     * @param mimeType
     */
    public void sendSecretAudioAttachment(String packetId, String attachmentUrl, String toJID,
                                          String mimeType) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
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

        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to send a type chat message  with attachment from the actual IMMessageManager
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The url of the attachment
     * @param toJID         The real JID of the recipient (+MSISDN@babbleim.com)
     */
    public void sendImageAttachment(String packetId, String attachmentUrl,
                                    String localUrl, String toJID,
                                    String mimeType) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.chat);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
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

        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }


    /**
     * method to send a type secret_chat message with image from the actual IMMessageManager
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The remote url of the uploaded image, that is to be attached in the
     *                      form field of the message stanza
     * @param localUrl      The local url of the selected image that was uploaded to the server
     * @param toAnonymous   i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                      this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                      but will use the original value as JID when inserting in MessagesTable
     *                      and ConversationsTable
     * @param nickname
     * @throws android.os.RemoteException
     */
    public void sendImageAttachmentAnonymously(String packetId,
                                               String attachmentUrl, String localUrl,
                                               String toAnonymous,
                                               String mimeType, String nickname) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

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
            return;
        }

        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

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

        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    /**
     * method to send a type chat message  with audio from the actual IMMessageManager
     *
     * @param packetId The packet id to be assigned in the message stanza. If this is null,
     *                 a new packet id will be generated else, the passed value is used and
     *                 will be treated as resending the message
     * @param toJID    The real JID of the recipient (+MSISDN@babbleim.com)
     * @throws android.os.RemoteException
     */
    public void sendAudioAttachment(String packetId, String attachmentUrl, String toJID,
                                    String mimeType) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.chat);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
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

        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }


    /**
     * method to send a type secret_chat message with audio from the actual IMMessageManager
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
     * @throws android.os.RemoteException
     */
    public void sendAudioAttachmentAnonymously(String packetId,
                                               String attachmentUrl,
                                               String toAnonymous,
                                               String mimeType, String nickname) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
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
            return;
        }
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to send a type chat message  with location from the actual IMMessageManager
     *
     * @param packetId The packet id to be assigned in the message stanza. If this is null,
     *                 a new packet id will be generated else, the passed value is used and
     *                 will be treated as resending the message
     * @param body     Should contain the lat,long of the location to be sent.
     * @param toJID    The real JID of the recipient (+MSISDN@babbleim.com)
     * @throws android.os.RemoteException
     */
    public void sendLocationAttachment(String packetId, String body, String toJID) {
        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.chat);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to send a type secret_chat message with location from the actual IMMessageManager
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
     * @param nickname
     * @throws android.os.RemoteException
     */
    public void sendLocationAttachmentAnonymously(String packetId, String body,
                                                  String toAnonymous,
                                                  String nickname) {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
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
            return;
        }

        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    /**
     * method to send a type chat message  with VCF file contact from the actual IMMessageManager
     *
     * @param packetId The packet id to be assigned in the message stanza. If this is null,
     *                 a new packet id will be generated else, the passed value is used and
     *                 will be treated as resending the message
     * @param toJID    The real JID of the recipient (+MSISDN@babbleim.com)
     * @throws android.os.RemoteException
     */
    public void sendVCFAttachment(String packetId, String body, String toJID) {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.chat);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);

        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    /**
     * method to send a type secret message  with VCF file contact from the actual IMMessageManager
     *
     * @param packetId The packet id to be assigned in the message stanza. If this is null,
     *                 a new packet id will be generated else, the passed value is used and
     *                 will be treated as resending the message
     * @param toJID    The real JID of the recipient (+MSISDN@babbleim.com)
     * @throws android.os.RemoteException
     */
    public void sendSecretVCFAttachment(String packetId, String body, String toJID) {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);;
        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);
        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    /**
     * method to send a type secret_chat message with vcf file contact from the actual
     * IMMessageManager
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
     * @throws android.os.RemoteException
     */
    public void sendVCFAttachmentAnonymously(String packetId, String body,
                                             String toAnonymous, String nickname) {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
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
            return;
        }

        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);
        try {
            xmpptcpConnection.sendStanza(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    public void sendChatState(String to, String nickname, int chatState)
            throws RemoteException {

        if (to.contains(Constants.JID_SECRET_CHAT_ADMIRER)) {
            to = to.split(Constants.JID_SECRET_CHAT_ADMIRER)[1];
        } else if (to.contains(Constants.JID_SECRET_CHAT_ADMIREE)) {
            to = to.split(Constants.JID_SECRET_CHAT_ADMIREE)[1];
        }

        if (xmpptcpConnection.isConnected() && xmpptcpConnection.isAuthenticated()) {
            Message newMessage = new Message();
            //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
            newMessage.setBody("");
            newMessage.setTo(to);
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

    public void sendDisplayedNotification(String to, String id)
            throws RemoteException {
        // MessageEventManager messageEventManager = new MessageEventManager(
        // mPacketManager.getConnection());

        if (xmpptcpConnection.isConnected()
                && xmpptcpConnection.isAuthenticated()) {
            MessageEventManager messageEventManager = new MessageEventManager(
                    xmpptcpConnection);
            try {
                messageEventManager.sendDisplayedNotification(to, id);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendSticker(String packetId, String body, String to,
                            String mimeType, String timestamp) throws RemoteException {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);
        newMessage.setTo(to);
        newMessage.setType(Message.Type.chat);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
        }

        if (timestamp == null) {
            timestamp = "" + System.currentTimeMillis();
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
    }


    /**
     * method to send a type secret_chat message with vcf file contact from the actual
     * IMMessageManager
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        Body should contain the sticker ID
     * @param toAnonymous i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname
     * @throws android.os.RemoteException
     */
    public void sendStickerAnonymously(String packetId, String body, String toAnonymous, String
            nickname)
            throws RemoteException {

        Message newMessage = new Message();
        //TODO add similar method with SSO
        //insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
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
            return;
        }

        newMessage.setTo(toJID);
        newMessage.setType(Message.Type.secret_chat);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

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

    }

    /**
     * Method to insert SSO profile details in message stanza
     *
     * @param message
     * @param msisdn
     * @param ssoFirstname
     * @param ssoLastname
     * @param skey
     */
    private void insertMsisdnAndNameIntoMessageIfHasSkey(Message message, String msisdn, String
            ssoFirstname,
                                                         String ssoLastname, String skey) {
        if (!TextUtils.isEmpty(skey)) {
            message.setMsisdn(msisdn);
            message.setName(ssoFirstname + " " + ssoLastname);
        }
    }
}
