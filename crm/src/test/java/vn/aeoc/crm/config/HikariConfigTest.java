package vn.aeoc.crm.config;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HikariConfigTest {

    @Test
    void bindsHikariDefaultsFromApplicationYaml() throws IOException {
        HikariConfig config = bindHikariConfig(new MockEnvironment());

        assertEquals(30, config.getMaximumPoolSize());
        assertEquals(10, config.getMinimumIdle());
        assertEquals(3_000, config.getConnectionTimeout());
        assertEquals(20_000, config.getLeakDetectionThreshold());
        assertEquals(1_500_000, config.getMaxLifetime());
        assertEquals("crm-hikari", config.getPoolName());
    }

    @Test
    void hikariMaximumPoolCanBeOverriddenWithEnvironmentProperty() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "testEnvironment", Map.of("HIKARI_MAX_POOL", "15")));

        HikariConfig config = bindHikariConfig(environment);

        assertEquals(15, config.getMaximumPoolSize());
    }

    private HikariConfig bindHikariConfig(MockEnvironment environment) throws IOException {
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addLast);

        HikariConfig config = new HikariConfig();
        Binder.get(environment).bind("spring.datasource.hikari", Bindable.ofInstance(config));
        return config;
    }
}
