package com.linroid.wrapper.compiler;

import java.util.List;

/**
 * @author linroid <linroid@gmail.com>
 * @since 10/03/2017
 */
class Utils {
    public static String implode(String separator, List<String> data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size() - 1; i++) {
            //data.length - 1 => to not add separator at the end
            if (!data.get(i).matches(" *")) {//empty string are ""; " "; "  "; and so on
                sb.append(data.get(i));
                sb.append(separator);
            }
        }
        sb.append(data.get(data.size() - 1).trim());
        return sb.toString();
    }
}
