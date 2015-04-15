package com.voyagerinnovation.services.managers;

import android.os.RemoteException;

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
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.List;
import java.util.Set;

/**
 * Created by charmanesantiago on 4/14/15.
 */
public class MUCMessageManager {

    XMPPTCPConnection xmpptcpConnection;

    public MUCMessageManager(XMPPTCPConnection xmpptcpConnection) {
        this.xmpptcpConnection = xmpptcpConnection;
    }

    public boolean joinRoom(String nickname, String chatroomJID)
            throws RemoteException {


        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

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

    public void leaveRoom(String chatroomJID) throws RemoteException, SmackException
            .NotConnectedException {

        if (!chatroomJID.contains("@")) {
            chatroomJID = chatroomJID.concat(Environment.IM_CHATROOM_SUFFIX);
        }
        // mMultiUserChatMap.get(roomName).leave();
        Presence quitPresence = new Presence(Presence.Type.unavailable);
        quitPresence.setTo(chatroomJID);
        xmpptcpConnection.sendStanza(quitPresence);
    }


    public void sendMessage(String chatroomJID, String message)
            throws RemoteException {
        Message groupMessage = new Message(chatroomJID, Message.Type.groupchat);
        groupMessage.setBody(message);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        try {
            multiUserChat.sendMessage(groupMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getChatrooms() {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        return multiUserChatManager.getJoinedRooms();
    }

    public void sendSticker(String packetId, String body, String chatroomJID,
                            String mimeType, String timestamp) throws RemoteException {
        Message newMessage = new Message(chatroomJID, Message.Type.groupchat);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        if (timestamp == null) {
            timestamp = "" + System.currentTimeMillis();
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);

        newMessage.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }


    }


    public List<Occupant> getChatroomParticipants(String chatroomJID)
            throws RemoteException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        try {
            multiUserChat.getOwners();
            return multiUserChat.getParticipants();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isJoined(String chatroomJID) throws RemoteException {


        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        return multiUserChat.isJoined();
    }

    public void sendImageAttachment(String packetId, String attachmentUrl,
                                    String localUrl, String chatroomJID, String mimeType)
            throws RemoteException {

        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }
        newMessage.setTo(chatroomJID);
        newMessage.setType(Message.Type.groupchat);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.ATTACHMENT);
        FormField.Media mediaField = new FormField.Media();
        FormField.Media.Uri uri = new FormField.Media.Uri();
        uri.setValue(attachmentUrl);
        uri.setType(mimeType);
        mediaField.addUri(uri);

        field.setMedia(mediaField);
        form.addField(field);

        String base64 = BabbleImageProcessorUtil.generateThumbnail(packetId,
                localUrl, mimeType);
        FormField thumbnailField = new FormField(
                Constants.THUMBNAIL);
        thumbnailField.addValue(base64);
        form.addField(thumbnailField);

        newMessage.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    public void sendAudioAttachment(String packetId, String attachmentUrl,
                                    String chatroomJID, String mimeType)
            throws RemoteException {

        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(chatroomJID);
        newMessage.setType(Message.Type.groupchat);

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

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    public void sendLocationAttachment(String packetId, String body,
                                       String chatroomJID) throws
            RemoteException {

        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(chatroomJID);
        newMessage.setType(Message.Type.groupchat);
        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatroomJID);

        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }


    }

    public void sendVCFAttachment(String packetId, String body,
                                  String chatroomJID) throws RemoteException {

        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(chatroomJID);
        newMessage.setType(Message.Type.groupchat);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);

        // String currentTimeDate = ""+System.currentTimeMillis();
        newMessage.addExtension(form);
    }

}
