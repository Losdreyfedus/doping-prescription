package com.doping.prescription.admin.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.shared.api.DemoDataLoader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class DemoDataSeederTest {

    @Test
    void runsLoadersInGivenOrder() {
        // Given
        List<String> calls = new ArrayList<>();
        DemoDataSeeder seeder =
                new DemoDataSeeder(List.of(loader("auth", calls), loader("content", calls), loader("exam", calls)));

        // When
        seeder.run(new DefaultApplicationArguments());

        // Then
        assertThat(calls).containsExactly("auth", "content", "exam");
    }

    private static DemoDataLoader loader(String name, List<String> calls) {
        return new DemoDataLoader() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void load() {
                calls.add(name);
            }
        };
    }
}
