package com.letsvpn.admin.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 区域类型常量：areaType 数字 → label / currency 符号 / currencyCode
 */
public final class AreaTypeConstants {

    private AreaTypeConstants() {}

    public static final class Info {
        public final String label;
        public final String currency;
        public final String currencyCode;

        private Info(String label, String currency, String currencyCode) {
            this.label        = label;
            this.currency     = currency;
            this.currencyCode = currencyCode;
        }
    }

    public static final Map<Integer, Info> MAP;

    static {
        Map<Integer, Info> m = new HashMap<>();
        m.put(1,  new Info("国内",     "¥",  "CNY"));
        m.put(2,  new Info("印度",     "₹",  "INR"));
        m.put(4,  new Info("印尼",     "Rp", "IDR"));
        m.put(5,  new Info("非洲",     "Fr", "XOF"));
        m.put(6,  new Info("泰国",     "฿",  "THB"));
        m.put(7,  new Info("墨西哥",   "$",  "MXN"));
        m.put(8,  new Info("巴西",     "R$", "BRL"));
        m.put(9,  new Info("巴基斯坦", "₨",  "PKR"));
        m.put(10, new Info("孟加拉国", "৳",  "BDT"));
        m.put(11, new Info("日本",     "¥",  "JPY"));
        m.put(12, new Info("俄罗斯",   "₽",  "RUB"));
        m.put(13, new Info("马来西亚", "RM", "MYR"));
        m.put(14, new Info("埃及",     "E£", "EGP"));
        MAP = Collections.unmodifiableMap(m);
    }

    /** 取 label，找不到返回 null */
    public static String label(Integer areaType) {
        Info info = MAP.get(areaType);
        return info == null ? null : info.label;
    }

    /** 取货币符号，找不到返回 null */
    public static String currency(Integer areaType) {
        Info info = MAP.get(areaType);
        return info == null ? null : info.currency;
    }

    /** 取货币代码，找不到返回 null */
    public static String currencyCode(Integer areaType) {
        Info info = MAP.get(areaType);
        return info == null ? null : info.currencyCode;
    }
}
