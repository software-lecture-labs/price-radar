package com.priceradar;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for tests that need the full application plus real backing
 * services. Subclasses share a single Spring context — and therefore a single
 * set of containers — for the whole test run.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

}
