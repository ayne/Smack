package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Route extends Stanza {

    public static final String ELEMENT = "route";
    private Message message;


    public void setMessage(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return this.message;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder buf = new XmlStringBuilder();
        buf.halfOpenElement(ELEMENT);
        addCommonAttributes(buf);
        if (getTo() != null) {
            buf.attribute("to", StringUtils.escapeForXML(getTo()).toString());
        }
        buf.rightAngleBracket();

        if (getFrom() != null) {
            buf.attribute("from", StringUtils.escapeForXML(getFrom()).toString());
        }
        buf.rightAngleBracket();

        if (message != null) {
            buf.append(message.toXML());
        }

        buf.closeElement(ELEMENT);

        return buf;
    }

}
