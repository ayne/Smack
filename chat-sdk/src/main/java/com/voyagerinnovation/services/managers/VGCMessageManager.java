package com.voyagerinnovation.services.managers;

import android.os.RemoteException;
import android.text.TextUtils;

import com.voyagerinnovation.constants.Constants;
import com.voyagerinnovation.util.BabbleImageProcessorUtil;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.List;

import timber.log.Timber;

/**
 * Created by charmanesantiago on 4/12/15.
 */
public class VGCMessageManager {

    XMPPTCPConnection xmpptcpConnection;
    private Message newMessage;

    public VGCMessageManager(XMPPTCPConnection xmpptcpConnection) {
        this.xmpptcpConnection = xmpptcpConnection;
    }

    /**
     * Method to join a VGC (private group) room.
     *
     * @param nickname The default nickname (i.e MSISDN) to be used when joining the room.
     * @param groupJid the name of the room in the form "roomName@service", where "service" is
     *                 the hostname at which the
     *                 multi-user chat service is running. Make sure to provide a valid JID.
     */
    public void joinRoom(String nickname, String groupJid){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);

        try {
            multiUserChat.join(nickname);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method to leave a group
     *
     * @param groupJID  The jid of the group the user wishes to leave.
     */
    public void leaveRoom(String groupJID) throws RemoteException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

        Presence quitPresence = new Presence(Presence.Type.unavailable);
        quitPresence.setTo(groupJID);
        try {
            xmpptcpConnection.sendStanza(quitPresence);
            multiUserChat.leave();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to send a message to a vgc group.
     *
     * @param packetId     The packet id to be used in the message. Usually you should pass
     *                     null here. But for the purpose of resending the same message, you
     *                     can pass the id of the said message.
     * @param body         The body or message content of an XMPP message.
     * @param groupJid     The jid of the receiving vgc group.
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendMessage(String packetId, String groupJid, String body, String senderMsisdn,
                               String senderName){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);

        Message message = new Message(groupJid);
        message.setType(Message.Type.vgc);
        insertMsisdnAndName(message, senderMsisdn, senderName);
        if (packetId != null) {
            message.setStanzaId(packetId);
        }
        message.setBody(body);

        try {
            multiUserChat.sendMessage(message, Message.Type.vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to send an anonymous message to a group
     *
     *@param packetId    The packet id to be used in the message. Usually you should pass
     *                    null here. But for the purpose of resending the same message, you
     *                    can pass the id of the said message.
     * @param body        The body of the message to be sent
     * @param groupJid    The jid of the receiving vgc group.
     * @param nickname    The nickname used in doing the anonymous message
     */
    public Message sendMessageAnonymously(String packetId, String groupJid, String body,String nickname){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        Message message = new Message(groupJid);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);
        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        message.setBody(body);
        message.setType(Message.Type.secret_vgc);
        try {
            multiUserChat.sendMessage(message, Message.Type.secret_vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to check if user is a member of a group
     *
     * @param groupJid  The jid of the vgc group to be queried.
     * @return true if joined. false otherwise
     */
    public boolean isJoined(String groupJid) {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);

        return multiUserChat.isJoined();
    }

    /**
     * Method to get joined rooms.
     * @Return List<String> of joined rooms.
     */
    public List<String> getJoinedRooms(){
        Timber.d("getting joined rooms...");
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.
                getInstanceFor(xmpptcpConnection);
        try {
            return multiUserChatManager.getJoinedRooms("vgc.babbleim.com");
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Method to create a vgc group.
     *
     * @param groupJid      The jid to be used for room creation (non descriptive, UUID).
     * @param roomName      The name of the group to be used.
     * @return null if room is successfully created. Exact error spiel if failed.
     */
    public String createRoom(String groupJid, String roomName, String username){

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);

        try {
            multiUserChat.create(username);

            Form form = new Form(DataForm.Type.submit);
            FormField ff = new FormField("vgc#roomname");
            ff.addValue(roomName);
            form.addField(ff);
            multiUserChat.sendConfigurationForm(form, "vgc#owner");
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            if (e.getXMPPError() != null
                    && e.getXMPPError().getConditionText() != null) {
                return e.getXMPPError().getConditionText();
            } else {
                return Constants.IMMULTIUSERCHAT_CREATE_ROOM_ERROR_SPIEL;
            }
        } catch (SmackException e) {
            e.printStackTrace();
            return Constants.IMMULTIUSERCHAT_CREATE_ROOM_ERROR_SPIEL;
        }
        return null;
    }

    /**
     * Method to automatically join room when an invitation is received. Use the other
     * addInvitationListener to use your own listener
     *
     * @param username Username not jid of the user.
     */
    public void addInvitationListener(final String username)  {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);

        multiUserChatManager.addInvitationListener(new InvitationListener() {
            @Override
            public void invitationReceived(XMPPConnection conn, MultiUserChat room, String
                    inviter, String reason, String password, Message message) {
                Timber.d("inviter: " + inviter + " |to: " + room);
                joinRoom(username, room.getRoom());
            }
        });
    }

    /**
     * Method to add group invitation listener
     *
     * @param invitationListener
     */
    public void addInvitiationListener(InvitationListener invitationListener) {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        multiUserChatManager.addInvitationListener(invitationListener);
    }

    /**
     * Method to send group message with sticker
     *
     * @param packetId     The packet id to be used in the message. Usually you should pass
     *                     null here. But for the purpose of resending the same message, you
     *                     can pass the id of the said message.
     * @param body         The body or message content of an XMPP message.
     * @param groupJid     The jid of the vgc group that will receive the message.
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendSticker(String packetId, String body, String groupJid, String senderMsisdn,
                               String senderName) {
        Message message = new Message(groupJid);
        insertMsisdnAndName(message, senderMsisdn, senderName);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);
        message.addExtension(form);
        message.setType(Message.Type.vgc);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Message to send an anonymous message with sticker to a group.
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        Body should contain the sticker ID
     * @param groupJid    The jid of the group the will receive the message.
     * @param nickname    The nickname to be used when sending the message ("alias").
     * @return Message    The actual Message that was sent.
     */
    public Message sendStickerAnonymously(String packetId, String body, String groupJid,String nickname){

        Message message = new Message(groupJid);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);
        message.addExtension(form);
        message.setType(Message.Type.secret_vgc);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.secret_vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to send a group message with image attachment
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
     * @param groupJID         The real JID of the recipient (+MSISDN@babbleim.com)
     * @param senderMsisdn  The msisdn of the sender (logged in user). If null, no msisdn will be
     *                      inserted.
     * @param senderName    The name of the sender (logged in user). If null, no name will be
     *                      inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendImageAttachment(String packetId, String attachmentUrl,
                                       String localUrl, String groupJID, String mimeType,
                                       String senderMsisdn, String senderName) {

        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        message.setTo(groupJID);
        message.setType(Message.Type.vgc);

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

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(message, Message.Type.vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * *
     * Method to send an anonymous  vgc message with image attachment
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The url to be inserted in the message, inwhich the receiver can
     *                      download the image
     * @param localUrl      The local url of the image attached. This is needed for the base 64
     *                      thumbnail generation
     *                      of the image which will be embedded in the mes
     * @param groupJid      the jid of the vgc group that will receive the message.
     * @param nickname      The nickname or "alias" to be used when sending the message
     * @return Message      The actual Message that was sent.
     */
    public Message sendImageAttachmentAnonymously(String packetId, String attachmentUrl,
                                                  String localUrl, String groupJid, String mimeType,
                                               String nickname) {

        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }
        message.setTo(groupJid);
        message.setType(Message.Type.secret_vgc);
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
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.secret_vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     *  Method to send a type vgc message  with audio from the actual IMMessageManager
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The url to be included in the message stanza, inwhich the receiving
     *                      party can download the audio attachment.
     * @param groupJid       The jid of the receiving party.
     * @param mimeType
     * @param senderMsisdn  The msisdn of the sender (logged in user). If null, no msisdn will be
     *                      inserted.
     * @param senderName    The name of the sender (logged in user). If null, no name will be
     *                      inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendAudioAttachment(String packetId, String attachmentUrl,
                                       String groupJid, String mimeType,
                                       String senderMsisdn, String senderName) {

        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        message.setTo(groupJid);
        message.setType(Message.Type.vgc);

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
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to send anonymous group message with audio attachment
     *
     * @param packetId      The packet id to be assigned in the message stanza. If this is null,
     *                      a new packet id will be generated else, the passed value is used and
     *                      will be treated as resending the message
     * @param attachmentUrl The remote url of the uploaded image, that is to be attached in the
     *                      form field of the message stanza
     * @param groupJid      The jid of the group what will receive the message.
     *                      and ConversationsTable
     * @param nickname
     * @return Message      The actual Message that was sent.
     */
    public Message sendAudioAttachmentAnonymously(String packetId, String attachmentUrl,
                                                  String groupJid, String mimeType,String nickname) {
        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        message.setTo(groupJid);
        message.setType(Message.Type.secret_vgc);
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
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.secret_vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     *  Method to send a type vgc message  with VCF file contact
     *
     * @param packetId     The packet id to be assigned in the message stanza. If this is null,
     *                     a new packet id will be generated else, the passed value is used and
     *                     will be treated as resending the message
     * @param groupJid        The real JID of the recipient (+MSISDN@babbleim.com)
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendVCFAttachment(String packetId, String body, String groupJid,
                                     String senderMsisdn, String senderName){

        Message newMessage = new Message();
        insertMsisdnAndName(newMessage, senderMsisdn, senderName);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(groupJid);
        newMessage.setType(Message.Type.vgc);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(newMessage, Message.Type.vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }


    /**
     * Method to send a anonymouse vgc message with VCF Contact File attachment.
     * IMMessageManager
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        Empty *double check
     * @param groupJid i.e secret_chat&MSISDN@babbleim.com or secret_chat#MSISDN@babbleim.com
     *                    this will be parsed to get the real JID: MSISDN@babbleim.com,
     *                    but will use the original value as JID when inserting in MessagesTable
     *                    and ConversationsTable
     * @param nickname
     * @return Message          The actual Message that was sent.
     */
    public Message sendVCFAttachmentAnonymously(String packetId, String body,
                                             String groupJid, String nickname){


        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(groupJid);
        newMessage.setType(Message.Type.secret_vgc);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(newMessage, Message.Type.secret_vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send group message with location
     *
     * @param packetId     The packet id to be assigned in the message stanza. If this is null,
     *                     a new packet id will be generated else, the passed value is used and
     *                     will be treated as resending the message
     * @param body         Should contain the lat,long of the location to be sent.
     * @param groupJid        The real JID of the recipient (+MSISDN@babbleim.com)
     * @param senderMsisdn The msisdn of the sender (logged in user). If null, no msisdn will be
     *                     inserted.
     * @param senderName   The name of the sender (logged in user). If null, no name will be
     *                     inserted.
     * @return Message          The actual Message that was sent.
     */
    public Message sendLocationAttachment(String packetId, String body, String groupJid,
                                          String senderMsisdn, String senderName){

        Message message = new Message();
        insertMsisdnAndName(message, senderMsisdn, senderName);

        if (packetId != null) {
            message.setStanzaId(packetId);
        }

        message.setTo(groupJid);
        message.setType(Message.Type.vgc);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);
        message.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to send anonymous group message with location
     *
     * @param packetId    The packet id to be assigned in the message stanza. If this is null,
     *                    a new packet id will be generated else, the passed value is used and
     *                    will be treated as resending the message
     * @param body        The message body should be a String containing the lat,
     *                    long of the location
     * @param groupJid    The jid of the receiving vgc group.
     * @param nickname    The nickname to be used when sending the anonymous message.
     * @return Message    The actual Message that was sent.
     */
    public Message sendLocationAttachmentAnonymously(String packetId, String body,
                                                  String groupJid, String nickname){

        Message message = new Message();

        if (packetId != null) {
            message.setStanzaId(packetId);
        }
        message.setTo(groupJid);
        message.setType(Message.Type.secret_vgc);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        message.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);
        message.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            multiUserChat.sendMessage(message, Message.Type.secret_vgc);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to get the owner of a group.
     * @param groupJid
     * @return
     * @throws RemoteException
     */
    public List<Affiliate> getOwner(String groupJid)
            throws RemoteException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJid);
        try {
            return multiUserChat.getOwners();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method to get the disco items of a vgc group. Used for querying members of a group
     * The result will return disco items with affiliations (owner, member) and other info.
     *
     * @param groupJid      The jid of the vgc group to be queried.
     * @return List<DiscoverItems.Item> members of the group.
     */
    public List<DiscoverItems.Item> getRoomItems(String groupJid){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        try {
             return multiUserChatManager.getRoomItems(groupJid);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method to query info of a vgc group (i.e subject or name of group).
     * @param groupJid  The jid of the vgc group to be queried.
     * @return RoomInfo
     */
    public RoomInfo getRoomInfo(String groupJid){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        try {
            return multiUserChatManager.getRoomInfo(groupJid);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method to invite or set participants of a group
     * @param groupJID
     * @param userName
     * @return null if successful and no error. Error if failed to invite.
     * @throws RemoteException
     */
    public MUCAdmin inviteUsers(String groupJID, String[] userName)
            throws RemoteException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

        MUCAdmin setMembers = new MUCAdmin("vgc#owner");
        setMembers.setTo(groupJID);
        setMembers.setType(IQ.Type.set);

        if (userName != null) {
            for (String invitee : userName) {
                MUCItem member = new MUCItem(MUCAffiliation.member, invitee);
                setMembers.addItem(member);
            }
        }

        try {
            xmpptcpConnection.sendStanza(setMembers);
            return setMembers;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Method to remove user in a group
     * @param group_jid
     * @param jid
     * @return true if user has been successfully removed.
     * @throws RemoteException
     */
    public boolean kickUser(String group_jid, String jid)
            throws RemoteException {

        MUCAdmin setMembers = new MUCAdmin("vgc#owner");
        setMembers.setTo(group_jid);
        setMembers.setType(IQ.Type.set);

        MUCItem member = new MUCItem(MUCRole.none, jid);
        // member.setReason("kick");
        setMembers.addItem(member);

        try {
            xmpptcpConnection.sendStanza(setMembers);
            return true;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Method to change subject of a VGC group.
     * @param groupJID
     * @param subject
     * @return true if group's subject has been successfully changed.
     */
    public boolean changeSubject(String groupJID, String subject) throws RemoteException{

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.changeSubject(subject, Message.Type.vgc);
            return true;
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Return the subject or name of vgc group
     * @param groupJID
     * @return Name of the group. Null if error or not found
     * @throws RemoteException
     */
    public String getRoomSubject(String groupJID) throws RemoteException {
        try {
            MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                    (xmpptcpConnection);
            MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
            RoomInfo info = multiUserChatManager.getRoomInfo(groupJID);
            return info.getVGCSubject();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        }
        return null;
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
