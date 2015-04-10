package com.voyagerinnovation.smack.packet;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;

public class IMPacketListener implements StanzaListener {

	private final static String TAG = IMPacketListener.class.getSimpleName();

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {

    }

//	@Override
//	public void processPacket(Packet packet) {
//		//Log.d(TAG,packet.toXML());
//	}

}
