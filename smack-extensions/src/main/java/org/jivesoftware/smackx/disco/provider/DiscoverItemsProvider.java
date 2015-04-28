/**
 *
 * Copyright 2003-2007 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.disco.provider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
* The DiscoverInfoProvider parses Service Discovery items packets.
*
* @author Gaston Dombiak
*/
public class DiscoverItemsProvider extends IQProvider<DiscoverItems> {

    @Override
    public DiscoverItems parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException {
        DiscoverItems discoverItems = new DiscoverItems();
        boolean done = false;
        DiscoverItems.Item item;
        String jid = "";
        String name = "";
        String action = "";
        String node = "";
        //Added for Babble's vgc disco support.
        String affiliation = "";
        String msisdn = "";
        String first_name = "";
        String last_name = "";
        discoverItems.setNode(parser.getAttributeValue("", "node"));
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                // Initialize the variables from the parsed XML
                jid = parser.getAttributeValue("", "jid");
                name = parser.getAttributeValue("", "name");
                node = parser.getAttributeValue("", "node");
                action = parser.getAttributeValue("", "action");
                affiliation = parser.getAttributeValue("", "affiliation");
                msisdn = parser.getAttributeValue("", "msisdn");
                first_name = parser.getAttributeValue("", "first_name");
                last_name = parser.getAttributeValue("", "last_name");
            }
            else if (eventType == XmlPullParser.END_TAG && "item".equals(parser.getName())) {
                // Create a new Item and add it to DiscoverItems.
                item = new DiscoverItems.Item(jid);
                item.setName(name);
                item.setNode(node);
                item.setAction(action);
                item.setAffiliation(affiliation);
                item.setMsisdn(msisdn);
                item.setFirstName(first_name);
                item.setLastName(last_name);
                discoverItems.addItem(item);
            }
            else if (eventType == XmlPullParser.END_TAG && "query".equals(parser.getName())) {
                done = true;
            }
        }

        return discoverItems;
    }
}
