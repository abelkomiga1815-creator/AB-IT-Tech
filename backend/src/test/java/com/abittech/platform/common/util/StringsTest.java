package com.abittech.platform.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringsTest {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertThat(Strings.normalizeEmail("  John@Example.COM ")).isEqualTo("john@example.com");
    }

    @Test
    void trimToNullConvertsBlankToNull() {
        assertThat(Strings.trimToNull("   ")).isNull();
        assertThat(Strings.trimToNull(null)).isNull();
        assertThat(Strings.trimToNull("  Nairobi ")).isEqualTo("Nairobi");
    }
}
