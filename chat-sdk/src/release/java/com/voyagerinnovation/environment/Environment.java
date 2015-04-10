package com.voyagerinnovation.environment;

/**
 * Created by charmanesantiago on 3/25/15.
 */
public class Environment {
    public static final String APP_NAME = "Babble";

    public static final String IM_RESOURCE = "BA";

    public static final String IM_SERVICE_NAME = "babbleim.com";
    public static final String IM_SUFFIX = "@" + IM_SERVICE_NAME;
    public static final String IM_GROUP_JID = "vgc." + IM_SERVICE_NAME;
    public static final String IM_GROUP_SUFFIX = "@" + IM_GROUP_JID;
    public static final String IM_CHATROOM_JID = "muc." + IM_SERVICE_NAME;
    public static final String IM_CHATROOM_SUFFIX = "@" + IM_CHATROOM_JID;
    public static final String IM_ARCHIVE_HOST = "archive." + IM_SERVICE_NAME;


    public static boolean IS_DEBUG = true;

    /**
     * Babble PROD URLS
     */
    public static final String IM_HOST = "chat.babbleim.com";
    public static final int IM_PORT = 5223;
    public static final String API_HOST = "http://register.api.babbleim.com/v1/user/";

    public static final String UPLOAD_SERVER = "http://media.api.babbleim.com/v1/media/upload";
    //TODO must set default url to staging url
    public static final String TOKEN_API = "http://register.api.babbleim.com/v1/user/token";
    public static final String BUDDY_MATCH_API = "http://buddy.api.babbleim.com/v2/buddy/match";

    public static final String GCM_REGISTRATION_ENDPOINT = "http://api.cm.voyagerinnovation" +
            ".com/v1/gcm/register";
    public static final String GCM_SENDER_ID = "225764556636";
    public static final String STICKERS_API = "http://stickers.staging.babbleim.com/";
}
