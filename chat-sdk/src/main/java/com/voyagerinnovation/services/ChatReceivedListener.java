package com.voyagerinnovation.services;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.id.ArchiveResultIQ;

/**
 * Created by charmanesantiago on 4/13/15.
 */
public interface ChatReceivedListener {


    public void onConnecting();

    public void onConnected(XMPPConnection xmppConnection);

    public void onDisconnected();

    public void onDisconnecting();

    public void onAuthenticated();

    public void onAuthenticationFailed();

    public void onChatReceived(Message message, boolean isRoute);

    public void onChatFileReceived(Message message, boolean isRoute);

    public void onChatVCFReceived(Message message, boolean isRoute);

    public void onChatLocationReceived(Message message, boolean isRoute);

    public void onChatStickerReceived(Message message, boolean isRoute);


    public void onSecretChatReceived(Message message, boolean isRoute);

    public void onSecretChatFileReceived(Message message, boolean isRoute);

    public void onSecretChatVCFReceived(Message message, boolean isRoute);

    public void onSecretChatLocationReceived(Message message, boolean isRoute);

    public void onSecretChatStickerReceived(Message message, boolean isRoute);


    public void onAnonymousChatReceived(Message message, boolean isRoute);

    public void onAnonymousChatFileReceived(Message message, boolean isRoute);

    public void onAnonymousChatVCFReceived(Message message, boolean isRoute);

    public void onAnonymousChatLocationReceived(Message message, boolean isRoute);

    public void onAnonymousChatStickerReceived(Message message, boolean isRoute);


    public void onVGCChatReceived(Message message, boolean isRoute);

    public void onVGCChatFileReceived(Message message, boolean isRoute);

    public void onVGCChatVCFReceived(Message message, boolean isRoute);

    public void onVGCChatLocationReceived(Message message, boolean isRoute);

    public void onVGCChatStickerReceived(Message message, boolean isRoute);

    public void onVGCInvitationReceived(Message message);

    public void onVGCSubjectChanged(Message message);

    public void onAnonymousVGCChatReceived(Message message, boolean isRoute);

    public void onAnonymousVGCChatFileReceived(Message message, boolean isRoute);

    public void onAnonymousVGCChatVCFReceived(Message message, boolean isRoute);

    public void onAnonymousVGCChatLocationReceived(Message message, boolean isRoute);

    public void onAnonymousVGCChatStickerReceived(Message message, boolean isRoute);


    public void onPublicChatReceived(Message message);

    public void onPublicChatFileReceived(Message message);

    public void onPublicChatVCFReceived(Message message);

    public void onPublicChatLocationReceived(Message message);

    public void onPublicChatStickerReceived(Message message);

    public void onEventReceived(Message message);


    /**
     * This is received when one contact has upgraded to SSO.
     * @param msisdn The original msisdn of the user while not yet SSO
     * @param ssoJID The new jid of the sso user.
     *
     */
    public void onUpdateContactReceived(String msisdn, String ssoJID);

    public void onChatStateReceived(Message message);

    public void onArchiveResultReceived(ArchiveResultIQ archiveResultIQ);
}
