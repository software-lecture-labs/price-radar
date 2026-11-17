package com.priceradar;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Tag("integration")
@Import(TestDatabaseConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

}
