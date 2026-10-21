package vn.aeoc.crm.repository.auth;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import vn.aeoc.entity.data.mysql.auth.Resource;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceRepositoryUpdateTest {
    @Test
    void updatesActiveButKeepsExistingModuleWhenModuleIdIsOmitted() {
        CapturedQuery captured = new CapturedQuery();
        ResourceRepository repository = repository(captured);
        Resource resource = resource();
        resource.setActive(0);
        resource.setModuleId(null);

        repository.updateResource(resource);

        String setClause = captured.sql().substring(0, captured.sql().toLowerCase().indexOf(" where "))
                .toLowerCase();
        assertTrue(setClause.contains("active"));
        assertFalse(setClause.contains("module_id"));
        assertTrue(captured.bindings().contains(0));
    }

    @Test
    void updatesModuleWhenProvidedButKeepsExistingActiveWhenActiveIsOmitted() {
        CapturedQuery captured = new CapturedQuery();
        ResourceRepository repository = repository(captured);
        Resource resource = resource();
        resource.setActive(null);
        resource.setModuleId(5);

        repository.updateResource(resource);

        String setClause = captured.sql().substring(0, captured.sql().toLowerCase().indexOf(" where "))
                .toLowerCase();
        assertTrue(setClause.contains("module_id"));
        assertFalse(setClause.contains("active"));
        assertTrue(captured.bindings().contains(5));
    }

    private ResourceRepository repository(CapturedQuery captured) {
        MockDataProvider provider = context -> {
            captured.capture(context);
            return new MockResult[]{new MockResult(1)};
        };
        DSLContext dsl = DSL.using(new MockConnection(provider), SQLDialect.MYSQL);
        return new ResourceRepository(dsl);
    }

    private Resource resource() {
        Resource resource = new Resource();
        resource.setId(12);
        resource.setName("Resource");
        resource.setDescription("Description");
        resource.setCode("CODE");
        resource.setUri("/resource/");
        resource.setActions("[\"VIEW\"]");
        return resource;
    }

    private static class CapturedQuery {
        private final AtomicReference<String> sql = new AtomicReference<>();
        private final AtomicReference<List<Object>> bindings = new AtomicReference<>(List.of());

        void capture(MockExecuteContext context) throws SQLException {
            sql.set(context.sql());
            bindings.set(Arrays.asList(context.bindings()));
        }

        String sql() {
            return sql.get();
        }

        List<Object> bindings() {
            return bindings.get();
        }
    }
}
