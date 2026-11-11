package com.bookmuse.app.mapper;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ViewsTest {
    @Test void shouldNormalizeGenresBeforeRemovingDuplicates() {
        assertEquals("classics|fiction",Views.genres(Set.of(" Fiction ","fiction","Classics")));
        assertEquals(Set.of("fiction","classics"),Views.genres("classics|fiction"));
    }
}
