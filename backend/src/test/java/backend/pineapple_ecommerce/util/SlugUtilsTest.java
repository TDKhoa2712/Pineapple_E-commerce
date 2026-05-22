package backend.pineapple_ecommerce.util;

import backend.pineapple_ecommerce.common.util.SlugUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlugUtils")
class SlugUtilsTest {

    // ─────────────────────────────────────────────────────────────────
    // toSlug
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toSlug()")
    class ToSlug {

        @ParameterizedTest(name = "blank input ''{0}'' → empty string")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void givenBlankOrNull_shouldReturnEmpty(String input) {
            assertThat(SlugUtils.toSlug(input)).isEmpty();
        }

        @Test
        @DisplayName("tiếng Việt có dấu → slug ASCII thuần")
        void givenVietnameseText_shouldConvertToAsciiSlug() {
            assertThat(SlugUtils.toSlug("Dứa mật vàng Cầu Đúc"))
                    .isEqualTo("dua-mat-vang-cau-duc");
        }

        @Test
        @DisplayName("chữ hoa → chữ thường")
        void givenUpperCase_shouldBeLowerCase() {
            assertThat(SlugUtils.toSlug("Hello World")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("khoảng trắng nhiều → dấu gạch nối đơn")
        void givenMultipleSpaces_shouldCollapseToSingleDash() {
            assertThat(SlugUtils.toSlug("hello   world")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("ký tự đặc biệt bị loại bỏ")
        void givenSpecialChars_shouldBeRemoved() {
            assertThat(SlugUtils.toSlug("hello@world!")).isEqualTo("helloworld");
        }

        @Test
        @DisplayName("dấu gạch nối ở đầu/cuối bị loại bỏ")
        void givenLeadingTrailingDashes_shouldBeTrimmed() {
            assertThat(SlugUtils.toSlug("--hello world--")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("số được giữ nguyên")
        void givenNumbers_shouldBePreserved() {
            assertThat(SlugUtils.toSlug("product 123 abc")).isEqualTo("product-123-abc");
        }

        @ParameterizedTest(name = "''{0}'' → ''{1}''")
        @CsvSource({
                "Thơm Sumo Hòa Lộc,    thom-sumo-hoa-loc",
                "Dứa Queen,             dua-queen",
                "Rau sạch 100%,         rau-sach-100",
                "Xoài cát Hòa Lộc,     xoai-cat-hoa-loc",
        })
        @DisplayName("các trường hợp tiếng Việt thực tế")
        void givenRealWorldVietnamese_shouldProduceExpectedSlug(String input, String expected) {
            assertThat(SlugUtils.toSlug(input.strip())).isEqualTo(expected.strip());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // toUniqueSlug
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toUniqueSlug()")
    class ToUniqueSlug {

        @Test
        @DisplayName("suffix = 0 → trả về slug không có hậu tố")
        void givenSuffixZero_shouldReturnBaseSlug() {
            assertThat(SlugUtils.toUniqueSlug("Dứa mật", 0)).isEqualTo("dua-mat");
        }

        @Test
        @DisplayName("suffix > 0 → thêm hậu tố số")
        void givenPositiveSuffix_shouldAppendSuffix() {
            assertThat(SlugUtils.toUniqueSlug("Dứa mật", 3)).isEqualTo("dua-mat-3");
        }

        @Test
        @DisplayName("suffix = 1 → hậu tố '-1'")
        void givenSuffixOne_shouldAppendOne() {
            assertThat(SlugUtils.toUniqueSlug("hello world", 1)).isEqualTo("hello-world-1");
        }
    }
}