package vn.aeoc.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan({"vn.aeoc.base", "vn.aeoc.crm"})
@EnableScheduling
@SpringBootApplication(exclude = {JooqAutoConfiguration.class})
public class CrmCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmCoreApplication.class, args);
    }

}
