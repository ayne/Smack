package org.jivesoftware.smackx.muc.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.muc.packet.VGCUser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by charmanesantiago on 9/27/15.
 */
public class VGCUserProvider extends ExtensionElementProvider<VGCUser> {

    /**
     * Parses a MUCUser stanza(/packet) (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Override
    public VGCUser parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException,
            IOException {
        VGCUser vgcUser = new VGCUser();
        outerloop:
        while (true) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case "invite":
                            vgcUser.setInvite(parseInvite(parser));
                            break;
                        case "item":
                            vgcUser.setItem(MUCParserUtils.parseItem(parser));
                            break;
                        case "password":
                            vgcUser.setPassword(parser.nextText());
                            break;
                        case "status":
                            String statusString = parser.getAttributeValue("", "code");
                            vgcUser.addStatusCode(VGCUser.Status.create(statusString));
                            break;
                        case "decline":
                            vgcUser.setDecline(parseDecline(parser));
                            break;
                        case "destroy":
                            vgcUser.setDestroy(MUCParserUtils.parseDestroy(parser));
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }

        return vgcUser;
    }

    private static VGCUser.Invite parseInvite(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        boolean done = false;
        VGCUser.Invite invite = new VGCUser.Invite();
        invite.setFrom(parser.getAttributeValue("", "from"));
        invite.setTo(parser.getAttributeValue("", "to"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    invite.setReason(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("invite")) {
                    done = true;
                }
            }
        }
        return invite;
    }

    private static VGCUser.Decline parseDecline(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        boolean done = false;
        VGCUser.Decline decline = new VGCUser.Decline();
        decline.setFrom(parser.getAttributeValue("", "from"));
        decline.setTo(parser.getAttributeValue("", "to"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    decline.setReason(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("decline")) {
                    done = true;
                }
            }
        }
        return decline;
    }
}
