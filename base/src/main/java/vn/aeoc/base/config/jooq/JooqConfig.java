package vn.aeoc.base.config.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.logging.LogManager;

@Configuration
@RequiredArgsConstructor
public class JooqConfig {

    private final DataSource dataSource;

    @Bean
    public DSLContext dslContext() {
        LogManager.getLogManager().reset();
        return DSL.using(dataSource, SQLDialect.MYSQL);
    }
}
