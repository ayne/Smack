package com.voyagerinnovation.services.managers;

import com.voyagerinnovation.constants.Constants;
import com.voyagerinnovation.environment.Environment;
import com.voyagerinnovation.util.BabbleImageProcessorUtil;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.List;

/**
 * Created by charmanesantiago on 4/14/15.
 */
public class MUCMessageManager {

    XMPPTCPConnection xmpptcpConnection;

    public MUCMessageManager(XMPPTCPConnection xmpptcpConnection) {
        this.xmpptcpConnection = xmpptcpConnection;
    }

    public class RoomObject {

        public String jid;
        public String name;

        RoomObject(String jid, String name) {
            this.jid = jid;
            this.name = name;
        }
    }

    /**
     * Method to join a public chatroom.
     * @param nickname      The nickname to be used when joining.
     * @param chatroomJid   The jid of the chatroom to be joined in to.
     * @return  true if successfully joined. false otherwise.
     */
    public boolean joinRoom(String nickname, String chatroomJid){


        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        try {
            multiUserChat.join(nickname);
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
     * Method to leave a public chatroom.
     * @param chatroomJid
     * @return true if succefully left the room. false otherwise.
     */
    public boolean leaveRoom(String chatroomJid) {

        if (!chatroomJid.contains("@")) {
            chatroomJid = chatroomJid.concat(Environment.IM_CHATROOM_SUFFIX);
        }
        // mMultiUserChatMap.get(roomName).leave();
        Presence quitPresence = new Presence(Presence.Type.unavailable);
        quitPresence.setTo(chatroomJid);
        try {
            xmpptcpConnection.sendStanza(quitPresence);
            return true;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Method to send a message in public chatroom
     * @param chatroomJid The jid of the chatroom which will receive the message.
     * @param body        The body or content of the message
     * @return Message    The actual Message that was sent.
     */
    public Message sendMessage(String chatroomJid, String body){
        Message message = new Message(chatroomJid, Message.Type.groupchat);
        message.setBody(body);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        try {
            multiUserChat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;
    }

    /**
     * Method to get all available public chatrooms
     * @return List<String> of public chatrooms.
     */
    public List<String> getChatrooms() {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        try {
            return multiUserChatManager.getJoinedRooms("muc.babbleim.com");
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
     * Method to get the information about a chatroom. i.e list of participants
     * @param chatroomJid   The jid of the chatroom to be queried.
     * @return RoomInfo
     */
    public RoomInfo getChatroomInfo(String chatroomJid){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        try {
            return multiUserChatManager.getRoomInfo(chatroomJid);
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
     * Method to send a sticker to a public chatroom.
     * @param body          The body of the message which contains the sticker id.
     * @param chatroomJid   The jid of the chatroom that will receive the sticker.
     * @return Message      The actual Message that was sent.
     */
    public Message sendSticker(String body, String chatroomJid){
        Message message = new Message(chatroomJid, Message.Type.groupchat);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);

        message.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        try {
            multiUserChat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return message;

    }


    /**
     * Method to get the participants of a chatroom.
     * @param chatroomJid   The jid of the chatroom to be queried.
     * @return List<String> of particpants.
     */
    public List<String> getChatroomParticipants(String chatroomJid){

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);
        return multiUserChat.getOccupants();
    }

    /**
     * Method to check if currently joined to the chatroom.
     * @param chatroomJid   The jid of the chatroom to be queried.
     * @return true if joined. false otherwise.
     */
    public boolean isJoined(String chatroomJid){


        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        return multiUserChat.isJoined();
    }

    /**
     * Method to send an image to a public chatroom.
     * @param packetId      Could be null. But usually has value since message with image is usually
     *                      displayed in listviews alreasy before completion of the upload.
     * @param attachmentUrl The url to be inserted in the message, inwhich the receiver can
     *                      download the image
     * @param localUrl      The local url of the image attached. This is needed for the base 64
     *                      thumbnail generation
     *                      of the image which will be embedded in the me
     * @param chatroomJid   The jid of the chatroom that will receive the message.
     * @param mimeType      Type of image.
     * @return Message      The actual Message that was sent.
     */
    public Message sendImageAttachment(String packetId, String attachmentUrl, String localUrl, String chatroomJid,
                                       String mimeType){

        Message message = new Message();
        if(packetId != null){
            message.setStanzaId(packetId);
        }
        message.setTo(chatroomJid);
        message.setType(Message.Type.groupchat);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        String base64 = BabbleImageProcessorUtil.generateThumbnail(message.getStanzaId(),
                localUrl, mimeType);
        FormField thumbnailField = new FormField(
                Constants.THUMBNAIL);
        thumbnailField.addValue(base64);
        form.addField(thumbnailField);

        message.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        try {
            multiUserChat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Message to send an audio recording to a public chatroom.
     * @param packetId      Could be null. But usually has value since message with image is usually
     *                      displayed in listviews alreasy before completion of the upload.
     * @param attachmentUrl The url to be included in the message stanza, inwhich the receiving
     *                      party can download the audio attachment.
     * @param chatroomJid         The jid of the receiving party.
     * @param mimeType
     * @return Message  The actual Message that was sent.
     */
    public Message sendAudioAttachment(String packetId, String attachmentUrl,
                                    String chatroomJid, String mimeType){

        Message message = new Message();
        if(packetId != null){
            message.setStanzaId(packetId);
        }
        message.setTo(chatroomJid);
        message.setType(Message.Type.groupchat);

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
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        try {
            multiUserChat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return message;

    }

    /**
     * Method to send a type groupchat message  with location.
     * @param body        body content of message. should contain the lat,long of the location to be sent.
     * @param chatroomJid The jid of the chatroom that will receive the Message.
     * @return Message  The actual Message that was sent.
     */
    public Message sendLocationAttachment(String body,
                                       String chatroomJid) {

        Message newMessage = new Message();

        newMessage.setTo(chatroomJid);
        newMessage.setType(Message.Type.groupchat);
        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        newMessage.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJid);

        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send VCF contact in public chatroom
     * @param body       The body of the message.
     * @param chatroomJid the jid of the chatroom that will receive the Message.
     * @return Message  The actual Message that was sent.
     */
    public Message sendVCFAttachment(String body,
                                  String chatroomJid){

        Message message = new Message();
        message.setTo(chatroomJid);
        message.setType(Message.Type.groupchat);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);
        message.addExtension(form);
        return message;
    }

}
