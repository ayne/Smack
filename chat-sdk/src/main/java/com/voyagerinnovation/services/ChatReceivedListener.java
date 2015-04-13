package com.voyagerinnovation.services;

import org.jivesoftware.smack.packet.Message;

/**
 * Created by charmanesantiago on 4/13/15.
 */
public interface ChatReceivedListener {

    public void onP2PMessageReceived(Message message);
}
