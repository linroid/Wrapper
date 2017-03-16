package com.linroid.wrapper.compiler;

import java.util.List;

/**
 * @author linroid <linroid@gmail.com>
 * @since 10/03/2017
 */
class Utils {
    public static String implode(String separator, List<String> data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(data.get(i));
        }
        return sb.toString();
    }
}
