package com.voyagerinnovation.util;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by charmanesantiago on 3/26/15.
 */
public class PasswordUtil {

    public static String encryptPassword(String input) {

        if (input == null) {
            return null;
        } else {
            if (input.length() < 14) {
                return null;
            }
        }

        // Get
        String a = input.substring(0, 6); // Get the first six digits (TAC):
        // 123456
        String b = input.substring(6, 8); // Get the next two digits (FAC): 78
        String c = input.substring(8, 14);// Get the next six digits (Phone
        // Serial Number): 654321
        String d = input.substring(14);

        // Transform
        a = new StringBuffer(a).reverse().toString();
        b = new StringBuffer(b).reverse().toString();
        c = new StringBuffer(c).reverse().toString();
        if (TextUtils.isEmpty(d)) {
            d = "";
        } else {
            d = ((char) ('j' - Integer.parseInt(d))) + "";
        }

        String pass = "voyager" + d + c + b + a + System.currentTimeMillis();

        try {
            pass = computeHash(pass);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
        return pass;
    }

    public static String computeHash(String input) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();

        byte[] byteData = digest.digest(input.getBytes("UTF-8"));
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return sb.toString();
    }
}
