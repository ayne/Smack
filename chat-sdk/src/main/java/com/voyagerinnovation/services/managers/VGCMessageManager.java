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
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by charmanesantiago on 4/12/15.
 */
public class VGCMessageManager {

    XMPPTCPConnection xmpptcpConnection;

    public VGCMessageManager(XMPPTCPConnection xmpptcpConnection) {
        this.xmpptcpConnection = xmpptcpConnection;
    }

    /**
     * Method to join a VGC (private group) room.
     *
     * @param nickname
     * @param groupJID the name of the room in the form "roomName@service", where "service" is
     *                 the hostname at which the
     *                 multi-user chat service is running. Make sure to provide a valid JID.
     * @throws RemoteException
     */
    public void joinRoom(String nickname, String groupJID)
            throws RemoteException {


        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

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
     * @param groupJID
     * @throws RemoteException
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
     * Method to send a message to a group.
     *
     * @param packetId
     * @param groupJID
     * @param message
     * @throws RemoteException
     */
    public Message sendMessage(String packetId, String groupJID, String message) throws
            RemoteException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

        Message groupMessage = new Message(groupJID, Message.Type.vgc);
        insertMsisdnAndNameIntoMessageIfHasSkey(groupMessage);
        if (packetId != null) {
            groupMessage.setStanzaId(packetId);
        }
        groupMessage.setBody(message);

        try {
            multiUserChat.sendMessage(groupMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return groupMessage;
    }

    /**
     * Method to send an anonymous message to a group
     *
     * @param packetId
     * @param groupJID
     * @param message
     * @param nickname
     * @throws RemoteException
     */
    public Message sendMessageAnonymously(String packetId, String groupJID, String message,
                                       String nickname) throws RemoteException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        Message groupMessage = new Message(groupJID, Message.Type.secret_vgc);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        groupMessage.setNickname(nickname);
        if (packetId != null) {
            groupMessage.setPacketID(packetId);
        }

        groupMessage.setBody(message);
        try {
            multiUserChat.sendMessage(groupMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return groupMessage;

    }

    /**
     * Method to check if user is a member of a group
     *
     * @param groupJID
     * @return true if joined. false otherwise
     */
    public boolean isJoined(String groupJID) {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

        return multiUserChat.isJoined();
    }

    /**
     * Method to get joined rooms.
     *
     * @throws RemoteException
     */
    public Set<String> getJoinedRooms() throws RemoteException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.
                getInstanceFor(xmpptcpConnection);
        return multiUserChatManager.getJoinedRooms();
    }


    /**
     * Method to create a vgc group.
     *
     * @param groupJID
     * @param roomName
     * @param nickName
     * @return null if room is successfully created. Error spiel if failed.
     * @throws RemoteException
     */
    public String createRoom(String groupJID, String roomName, String nickName)
            throws RemoteException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

        try {
            multiUserChat.create(nickName);
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
     * @throws RemoteException
     */
    public void addInvitationListener(final String username) throws RemoteException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);

        multiUserChatManager.addInvitationListener(new InvitationListener() {
            @Override
            public void invitationReceived(XMPPConnection conn, MultiUserChat room, String
                    inviter, String reason, String password, Message message) {
                Timber.d("inviter: " + inviter + " |to: " + room);
                try {
                    joinRoom(username, room.getRoom());
                    //TODO add notification util
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
     * @param packetId
     * @param body
     * @param groupJID
     * @throws RemoteException
     */
    public Message sendSticker(String packetId, String body, String groupJID) throws RemoteException {
        Message newMessage = new Message(groupJID, Message.Type.vgc);
        insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Message to send an anonymous message with sticker to a group.
     *
     * @param packetId
     * @param body
     * @param groupJID
     * @param nickname
     * @throws RemoteException
     */
    public Message sendStickerAnonymously(String packetId, String body, String groupJID,
                                       String nickname) throws RemoteException {

        Message newMessage = new Message(groupJID, Message.Type.secret_vgc);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.STICKER);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }

    /**
     * Method to send a group message with image attachment
     *
     * @param packetId
     * @param attachmentUrl
     * @param localUrl
     * @param groupJID
     * @param mimeType
     */
    public Message sendImageAttachment(String packetId, String attachmentUrl,
                                    String localUrl, String groupJID, String mimeType) {

        Message newMessage = new Message();
        insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.vgc);

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

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send anonymous group message with image attachment
     *
     * @param packetId
     * @param attachmentUrl
     * @param localUrl
     * @param groupJID
     * @param mimeType
     */
    public Message sendImageAttachmentAnonymously(String packetId, String attachmentUrl,
                                               String localUrl, String groupJID, String mimeType,
                                               String nickname) {

        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }
        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.secret_vgc);
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
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send group message with audio attachment
     *
     * @param packetId
     * @param attachmentUrl
     * @param localUrl
     * @param groupJID
     * @param mimeType
     */
    public Message sendAudioAttachment(String packetId, String attachmentUrl,
                                     String groupJID, String mimeType) {

        Message newMessage = new Message();
        insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.vgc);

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
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send anonymous group message with audio attachment
     *
     * @param packetId
     * @param attachmentUrl
     * @param localUrl
     * @param groupJID
     * @param mimeType
     */
    public Message sendAudioAttachmentAnonymously(String packetId, String attachmentUrl,
                                               String localUrl, String groupJID, String mimeType,
                                               String nickname) {
        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.secret_vgc);
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
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send group message with VCF contact attachment
     *
     * @param packetId
     * @param body
     * @param groupJID
     */
    public Message sendVCFAttachment(String packetId, String body, String groupJID) throws RemoteException{

        Message newMessage = new Message();
        insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setPacketID(packetId);
        }

        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.vgc);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.VCARD);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }


    /**
     * Method to send anonymous group message with VCF contact attachment
     *
     * @param packetId
     * @param body
     * @param groupJID
     */
    public Message sendVCFAttachmentAnonymously(String packetId, String body,
                                             String groupJID, String timestamp, String nickname) throws  RemoteException{


        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        if (timestamp == null) {
            timestamp = "" + System.currentTimeMillis();
        }


        newMessage.setTo(groupJID);
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
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send group message with location
     *
     * @param packetId
     * @param body
     * @param groupJID
     */
    public Message sendLocationAttachment(String packetId, String body, String groupJID) throws RemoteException {

        Message newMessage = new Message();
        insertMsisdnAndNameIntoMessageIfHasSkey(newMessage);

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }

        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.vgc);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;

    }

    /**
     * Method to send anonymous group message with location
     *
     * @param packetId
     * @param body
     * @param groupJID
     */
    public Message sendLocationAttachmentAnonymously(String packetId, String body,
                                                  String groupJID, String timestamp,
                                                  String nickname) throws RemoteException{

        Message newMessage = new Message();

        if (packetId != null) {
            newMessage.setStanzaId(packetId);
        }
        newMessage.setTo(groupJID);
        newMessage.setType(Message.Type.secret_vgc);
        if (nickname != null) {
            nickname = TextUtils.htmlEncode(nickname);
        }
        newMessage.setNickname(nickname);

        DataForm form = new DataForm(DataForm.Type.form);
        FormField field = new FormField(Constants.LOCATION);
        field.addValue(body);
        form.addField(field);
        newMessage.addExtension(form);
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
            multiUserChat.sendMessage(newMessage);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

        return newMessage;
    }

    /**
     * Method to get the owner of a group.
     * @param groupJID
     * @return
     * @throws RemoteException
     */
    public List<Affiliate> getOwner(String groupJID)
            throws RemoteException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
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

    public List<Occupant> getParticipants(String groupJID){
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);
        try {
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

    /**
     * Method to invite or set participants of a group
     * @param groupJID
     * @param userName
     * @return null if successful and no error. Error if failed to invite.
     * @throws RemoteException
     */
    public String inviteUsers(String groupJID, String[] userName)
            throws RemoteException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor
                (xmpptcpConnection);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(groupJID);

        MUCAdmin setMembers = new MUCAdmin("vgc#owner");
        setMembers.setTo(groupJID);
        setMembers.setType(IQ.Type.set);

        if (userName != null) {
            for (String invitee : userName) {
                MUCItem member = new MUCItem(MUCRole.member, invitee);
                setMembers.addItem(member);
            }
        }

        try {
            xmpptcpConnection.sendStanza(setMembers);
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

    private void insertMsisdnAndNameIntoMessageIfHasSkey(Message message, String skey, String
            msisdn, String firstname, String lastname) {
        if (!TextUtils.isEmpty(skey)) {
            message.setMsisdn(msisdn);
            message.setName(firstname + " " + lastname);
        }
    }

    private void insertMsisdnAndNameIntoMessageIfHasSkey(Message message){
        //TODO
    }

}
