package com.voyagerinnovation.services;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.id.ArchiveResultIQ;
import org.jivesoftware.smackx.xdata.FormField;

import java.util.List;

/**
 * This class is the interface for listening to ChatService's XMPP connection and authentication states.
 * This also provides callbacks for receiving various messages like chat, secretchat, chatLocation,
 * VGCChat, publicChat, etc.
 * Created by charmanesantiago on 4/13/15.
 */
public interface ChatReceivedListener {


    /**
     * Invoked when ChatService is connecting to the XMPP server.
     */
    public void onConnecting();

    /**
     * Invoked when the ChatService has connected to the XMPP server.
     * @param xmppConnection The XMPPConnection used to connect to the server.
     *                       You may use this to get the xmpp host, port, service, etc.
     *                       You may also use this to get the ts (time to sleep) and token (xyap token)
     *                       passed  by the server, on successful authentication
     *                       (but this must come from onAuthenticated() method)
     */
    public void onConnected(XMPPConnection xmppConnection);

    /**
     * Invoked when ChatService has disconnected from the XMPP server.
     */
    public void onDisconnected();

    /**
     * Invoked when ChatService is disconnecting from the XMPP server.
     */
    public void onDisconnecting();

    /**
     * Invoked when authentication was successful.
     * @param xmppConnection The XMPPConnection used to connect to the server.
     *                       You may use this to get the xmpp host, port, service, etc.
     *                       You may also use this to get the tts (time to sleep) and token (xyap token)
     *                       passed  by the server, on successful authentication
     *                       (but this must come from onAuthenticated() method)
     */
    public void onAuthenticated(XMPPConnection xmppConnection);

    /**
     * Invoked when user must be logged out and login again.
     * i.e Resource conflict (signed in to another device)
     * i.e Wrong password or authentication details.
     */
    public void onAuthenticationFailed();

    /**
     * Invoked when XYAP token used for logging in is expired.
     */
    public void onTokenExpired();

    /**
     * Invoked when the SKEY used for logging in is expired.
     */
    public void onSkeyExpired();

    /**
     * Invoked when user is not authorized to login.
     */
    public void onNotAuthorized();

    /**
     * Invoked when a ts (not tts but server time) has been received
     * @param source        The source of the message
     * @param servertime    The server time in unix time.
     */
    public void onTsReceived(String source, String servertime);

    /**
     * Invoked when a message of type error is received.
     * @param message The error message. Typical babble error codes that need to be handled:
     *                error code 406 - current user not occupant of room
     *                error code 404 - user has left the room
     */
    public void onErrorMessageReceived(Message message);

    /**
     * Invoked when a message of type error is received.
     * @param iq The iq of type error. Typical Babble IQ error that needs to be handled:
     *           403,404 - user is not a member of the VGC group. In Babble, this VGC group
     *           must be deleted in db.
     */
    public void onErrorIQReceived(IQ iq);

    /**
     * Invoked when a p2p chat (type chat) is received
     * @param message   The chat message.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     *
     */
    public void onChatReceived(Message message, boolean isRoute);

    /**
     * Invoked when a chat with file attachment is received.
     * @param message   The chat message.
     * @param formField The FormField which contains the url of the file.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onChatFileReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a chat with vcf contact attachment is received.
     * @param message   The chat message.
     * @param formField The FormField which contains the vcf file of the contact.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onChatVCFReceived(Message message, List<FormField> formFields, boolean isRoute);

    /**
     * Invoked when a chat with location attachment is received.
     * @param message   The chat message.
     * @param formField The FormField which contains the info (lat,lng) of the location attached.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onChatLocationReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a chat with sticker attachment is received.
     * @param message   The chat message.
     * @param formField The FormField which contains id of the sticker attached.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onChatStickerReceived(Message message, FormField formField, boolean isRoute);


    /**
     * Invoked when a secret chat (disappering message) is received.
     * @param message   The secret message.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onSecretChatReceived(Message message, boolean isRoute);

    /**
     * Invoked when a secret (disappearing message) with file attachment is received.
     * @param message   The secret message.
     * @param formField The FormField which contains the url of the file attached.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onSecretChatFileReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a secret (disappearing message) with vcf contact attachment is received.
     * @param message   The secret message.
     * @param formField The FormField which contains the vcf file of the contact attached.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onSecretChatVCFReceived(Message message, List<FormField> formFields, boolean isRoute);

    /**
     * Invoked when a secret (disappearing message) with location attachment is received.
     * @param message   The secret message.
     * @param formField The FormField which contains the lat, lng of the location attached.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onSecretChatLocationReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a secret (disappearing message) with sticker attachment is received.
     * @param message   The secret message.
     * @param formField The FormField which contains the id of the sticker.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onSecretChatStickerReceived(Message message, FormField formField, boolean isRoute);


    /*
     * Invoked when an anonymous chat (type secret_chat) is received.
     */
    public void onAnonymousChatReceived(Message message, boolean isRoute);

    /**
     * Invoked when an anonymous chat (type secret_chat) with file attachment is received.
     * @param message       The anonymous_chat.
     * @param formField     The FormField which contains the url of the file attached.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousChatFileReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when an anonymous chat (type secret_chat) with vcf contact attachment is received.
     * @param message       The anonymous_chat.
     * @param formField     The FormField which contains the vcf file of the contact.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousChatVCFReceived(Message message, List<FormField> formField, boolean isRoute);

    /**
     * Invoked when an anonymous chat (type secret_chat) with location attachment is received.
     * @param message       The anonymous_chat.
     * @param formField     The FormField which contains the lat, lng of the location.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousChatLocationReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when an anonymous chat (type secret_chat) with sticker attachment is received.
     * @param message       The anonymous_chat.
     * @param formField     The FormField which contains the id of the sticker
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousChatStickerReceived(Message message, FormField formField, boolean isRoute);


    /**
     * Invoked when a chat from a private group is received (type vgc).
     * @param message       The vgc message.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onVGCChatReceived(Message message, boolean isRoute);

    /**
     * Invoked when a chat with file attachment from a private group is received (type vgc).
     * @param message       The vgc message
     * @param formField     The FormField which contains the url of the file attached.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onVGCChatFileReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a chat with contact attachment from a private group is received (type vgc).
     * @param message       The vgc message
     * @param formField     The FormField which contains the vcf file of the shared contact.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onVGCChatVCFReceived(Message message, List<FormField> formField, boolean isRoute);

    /**
     * Invoked when a chat with location  from a private group is received (type vgc).
     * @param message       The vgc message
     * @param formField     The FormField which contains the lat, lng of the location.
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onVGCChatLocationReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a chat with sticker  from a private group is received (type vgc).
     * @param message       The vgc message
     * @param formField     The FormField which contains the sticker id of the location
     * @param isRoute       true if this is a route message. Meaning,
     *                      the message originated from the logged in user, but received it via archive.
     *                      Or in short, an archived outgoing message. false otherwise.
     */
    public void onVGCChatStickerReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when a user has joined a VGC group or has been added to a VGC group.
     * @param presence   The actual message that contains the invite.
     * @param groupJid The id of the vgc group
     * @param jid The id of the user who joined the vgc group
     */
    public void onVGCUserJoined(Presence presence, String groupJid, String jid);

    /**
     * Invoked when a user has left a VGC group.
     * @param presence   The actual message that contains the invite.
     * @param groupJid The id of the vgc group
     * @param jid The id of the user who left the vgc group
     */
    public void onVGCUserLeft(Presence presence, String groupJid, String jid);

    /**
     * Invoked when a user has been removed from a VGC group.
     * @param presence   The actual message that contains the invite.
     * @param groupJid The id of the vgc group
     * @param jid The id of the user who was removed from the vgc group
     */
    public void onVGCUserRemoved(Presence presence, String groupJid, String jid);

    /**
     * Invoked when the subject (known as name) of a private vgc group has been changed.
     * @param message   The actual message that contains the change subject info
     */
    public void onVGCSubjectChanged(Message message);

    /**
     * Invoked when anonymous message (type secret_vgc) in private vgc group is received,
     * @param message   The secret_vgc message
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousVGCChatReceived(Message message, boolean isRoute);

    /**
     * Invoked when anonymous message (type secret_vgc) with image in private vgc group is received,
     * @param message   The secret_vgc message
     * @param formField The FormField that contains the url of the file.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousVGCChatFileReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when anonymous message (type secret_vgc) with vcf contact in private vgc group is received,
     * @param message   The secret_vgc message
     * @param formField The FormField that contains the vcf contact file.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousVGCChatVCFReceived(Message message, List<FormField> formField, boolean isRoute);

    /**
     * Invoked when anonymous message (type secret_vgc) with location in private vgc group is received,
     * @param message   The secret_vgc message
     * @param formField The FormField that contains the lat,lng of the location.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousVGCChatLocationReceived(Message message, FormField formField, boolean isRoute);

    /**
     * Invoked when anonymous message (type secret_vgc) with sticker in private vgc group is received,
     * @param message   The secret_vgc message
     * @param formField The FormField that contains the sticker id of the sticker.
     * @param isRoute   true if this is a route message. Meaning,
     *                  the message originated from the logged in user, but received it via archive.
     *                  Or in short, an archived outgoing message. false otherwise.
     */
    public void onAnonymousVGCChatStickerReceived(Message message, FormField formField, boolean isRoute);


    /**
     * Invoked when a public chatroom message is received (type groupchat)
     * @param message   The groupchat message.
     */
    public void onPublicChatReceived(Message message);

    /**
     * Invoked when a public chatroom message with file attachment is received (type groupchat)
     * @param message   The groupchat message.
     * @param formField The FormField that contains the url of the attached image or file.
     */
    public void onPublicChatFileReceived(Message message, FormField formField);

    /**
     * Invoked when a public chatroom message with contact attachment is received (type groupchat)
     * @param message   The groupchat message.
     * @param formField The FormField that contains the vcf file of the contact shared.
     */
    public void onPublicChatVCFReceived(Message message, List<FormField> formField);

    /**
     * Invoked when a public chatroom message with location attachment is received (type groupchat)
     * @param message   The groupchat message.
     * @param formField The FormField that contains the lat,lng of the location.
     */
    public void onPublicChatLocationReceived(Message message, FormField formField);

    /**
     * Invoked when a public chatroom message with sticker attachment is received (type groupchat)
     * @param message   The groupchat message.
     * @param formField The FormField that contains the id of the sticker
     */
    public void onPublicChatStickerReceived(Message message, FormField formField);

    /**
     * Invoked when a message event is received (i.e de for delivered, di for displayed),
     * @param message   The message containing the event.
     */
    public void onEventReceived(Message message);


    /**
     * Invoked when a contact has upgraded to SSO.
     * @param msisdn The original msisdn of the user while not yet SSO
     * @param ssoJID The new jid of the sso user.
     *
     */
    public void onUpdateContactReceived(String msisdn, String ssoJID);

    /**
     * Invoked when chat state is received: composing(typing), paused.
     * @param message   The message containing the chat state.
     */
    public void onChatStateReceived(Message message);

    /**
     * Invoked when an ArchiveResultIQ is received.
     * @param archiveResultIQ The ArchiveResultIQ which contains the endpoint (http url)
     *                        where the entire archive of the user can be downloaded.
     */
    public void onArchiveResultReceived(ArchiveResultIQ archiveResultIQ);
}
