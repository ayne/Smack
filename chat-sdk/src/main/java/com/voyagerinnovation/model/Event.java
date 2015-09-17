package com.voyagerinnovation.model;

import java.util.Locale;

/**
 * Created by charmanesantiago on 9/15/15.
 */
public class Event {

    public enum Type {
        delivered,
        displayed,
        offline,
        sms,
        unknown;

        public static Type fromString(String string) {
            return Type.valueOf(string.toLowerCase(Locale.US));
        }
    }
}
