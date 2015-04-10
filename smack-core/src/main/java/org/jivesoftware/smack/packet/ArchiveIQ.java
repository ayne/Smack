/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smack.packet;

/**
 * IQ packet that serves querying message archive in Babble and revoking administrative
 * privileges and destroying a room. All these operations are scoped by the
 * 'http://jabber.org/protocol/muc#owner' namespace.
 * 
 * @author Charmane Santiago
 */
public class ArchiveIQ extends IQ {

    public static final String ARCHIVE = "archive";
    public static final String QUERY_ELEMENT = "query";

    private String LAM;
    private String FRM;
    private String LRM;

    public ArchiveIQ(String LAM, String FRM, String LRM) {
        super(QUERY_ELEMENT, ARCHIVE);
        this.LAM = LAM;
        this.FRM = FRM;
        this.LRM = LRM;
    }


    private String getChildElementString() {
        Float floatLAM = Float.parseFloat(LAM);
        Float floatLRM = Float.parseFloat(LRM);
        Float floatFRM = Float.parseFloat(FRM);
        if(floatFRM == 0) {
            FRM = LRM;
        }
        if(floatLAM == 0) {
            LAM = FRM;
        }
        if(floatLAM > floatFRM) {
            FRM = LAM;
        }
        if(floatFRM > floatLRM) {
            LRM = FRM;
        }
        StringBuilder buf = new StringBuilder();
        buf.append("<messages lam=\"" + LAM + "\" frm=\"" + FRM + "\" lrm=\""
                + LRM + "\">");
        buf.append("<jid>*</jid>");
        buf.append("</messages>");
        return buf.toString();
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.append("<messages lam=\"" + LAM + "\" frm=\"" + FRM + "\" lrm=\""
                + LRM + "\">");
        xml.append("<jid>*</jid>");
        xml.append("</messages>");
        return xml;
    }
}