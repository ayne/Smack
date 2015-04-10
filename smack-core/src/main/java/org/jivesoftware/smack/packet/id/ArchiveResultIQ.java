package org.jivesoftware.smack.packet.id;

import org.jivesoftware.smack.packet.IQ;

/**
 * Result IQ Packet containing the endpoint of message archive.
 * Created by charmanesantiago on 4/10/15.
 */
public class ArchiveResultIQ extends IQ {


    private String endpoint;

    public ArchiveResultIQ(String endpoint) {
        super(null, null);
        setType(IQ.Type.result);
        this.endpoint = endpoint;
    }

    public ArchiveResultIQ(IQ request, String endpoint) {
        this(endpoint);
        if (!(request.getType() == Type.get || request.getType() == Type.set)) {
            throw new IllegalArgumentException(
                    "IQ must be of type 'set' or 'get'. Original IQ: " + request.toXML());
        }
        setStanzaId(request.getStanzaId());
        setFrom(request.getTo());
        setTo(request.getFrom());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.append("<endpoint xmlns=\'archive\'>" + endpoint + "</endpoint>");
        return xml;
    }
}
