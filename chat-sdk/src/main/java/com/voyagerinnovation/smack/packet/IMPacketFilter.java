package com.voyagerinnovation.smack.packet;

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

public class IMPacketFilter implements StanzaFilter {

	private static final String TAG = IMPacketFilter.class.getSimpleName();


    @Override
    public boolean accept(Stanza stanza) {
        return true;
    }
}
