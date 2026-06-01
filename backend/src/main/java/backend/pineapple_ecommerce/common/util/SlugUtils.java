package backend.pineapple_ecommerce.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tạo URL slug từ tiếng Việt.
 * "Dứa mật vàng Cầu Đúc" → "dua-mat-vang-cau-duc"
 */
public class SlugUtils {

    private static final Pattern NON_ASCII   = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern WHITESPACE  = Pattern.compile("[\\s]+");
    private static final Pattern NON_ALPHA   = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern MULTI_DASH  = Pattern.compile("\\-{2,}");

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";

        String replaced = input.replace('đ', 'd').replace('Đ', 'd');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String ascii      = NON_ASCII.matcher(normalized).replaceAll("");
        String lower      = ascii.toLowerCase(Locale.ROOT);
        String dashed     = WHITESPACE.matcher(lower).replaceAll("-");
        String clean      = NON_ALPHA.matcher(dashed).replaceAll("");
        String result     = MULTI_DASH.matcher(clean).replaceAll("-");

        return result.replaceAll("^-|-$", "");
    }

    /**
     * Tạo slug unique bằng cách thêm số đuôi.
     * "dua-mat" → "dua-mat-1", "dua-mat-2", ...
     */
    public static String toUniqueSlug(String input, int suffix) {
        String base = toSlug(input);
        return suffix == 0 ? base : base + "-" + suffix;
    }
}
