package ua.compservice.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

    public static boolean matches(String anUniqueNumber) {

        final String regex = "\\d{2}\\/\\d{4}$";

        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(anUniqueNumber);

        return matcher.find();


    }
}
