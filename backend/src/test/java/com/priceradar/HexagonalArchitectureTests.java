package com.priceradar;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import jakarta.persistence.Entity;

/**
 * Layering inside each application module (ADR 0002). Spring Modulith guards
 * the boundaries between modules; these rules guard the layers within one:
 *
 * <pre>
 * web ──────────┐
 *               ├──▶ application ──▶ domain
 * infrastructure┘
 * </pre>
 *
 * Rules match on package names, so they apply to every module as soon as a
 * class lands in the layer. Empty layers are allowed: most modules have no
 * code yet.
 */
class HexagonalArchitectureTests {

	private static final String MODULE_API = "com.priceradar.*";
	private static final String DOMAIN = "com.priceradar.*.domain..";
	private static final String APPLICATION = "com.priceradar.*.application..";
	private static final String INFRASTRUCTURE = "com.priceradar.*.infrastructure..";
	private static final String WEB = "com.priceradar.*.web..";

	/** Libraries that belong behind a port: persistence, search, messaging, cache and HTTP. */
	private static final String[] ADAPTER_TECHNOLOGIES = {
			"jakarta.persistence..",
			"org.hibernate..",
			"org.springframework.data.jpa..",
			"org.springframework.data.elasticsearch..",
			"org.springframework.data.redis..",
			"org.springframework.kafka..",
			"org.apache.kafka..",
			"co.elastic..",
			"io.lettuce..",
			"org.springframework.web.." };

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.priceradar");

	@Test
	void domainDependsOnNoOtherLayer() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE, WEB)
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void domainIsFreeOfFrameworks() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAnyPackage(
						adapterTechnologiesAnd("org.springframework..", "tools.jackson..", "com.fasterxml.jackson.."))
				.because("business rules must not depend on PostgreSQL, Kafka, Elasticsearch or Spring")
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void applicationDependsOnPortsNotOnAdapters() {
		noClasses().that().resideInAPackage(APPLICATION)
				.should().dependOnClassesThat().resideInAnyPackage(adapterTechnologiesAnd(INFRASTRUCTURE, WEB))
				.because("use cases reach the outside world only through ports declared in domain or application")
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void moduleApiDoesNotExposeAdapters() {
		// "com.priceradar.*" is exactly a module's top-level package: the API other
		// modules see. Facades and events there must not leak adapter types.
		noClasses().that().resideInAPackage(MODULE_API)
				.should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, WEB)
				.because("other modules may only see facades and events, never adapters")
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void inboundAndOutboundAdaptersStayApart() {
		noClasses().that().resideInAPackage(WEB)
				.should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
				.allowEmptyShould(true)
				.check(CLASSES);

		noClasses().that().resideInAPackage(INFRASTRUCTURE)
				.should().dependOnClassesThat().resideInAPackage(WEB)
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void jpaEntitiesLiveInInfrastructure() {
		classes().that().areAnnotatedWith(Entity.class)
				.should().resideInAPackage(INFRASTRUCTURE)
				.because("the domain model is persistence-ignorant; JPA entities are adapter details")
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void kafkaListenersLiveInInfrastructure() {
		methods().that().areAnnotatedWith(KafkaListener.class)
				.should().beDeclaredInClassesThat().resideInAPackage(INFRASTRUCTURE)
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	@Test
	void controllersStayOutOfTheCore() {
		noClasses().that().areAnnotatedWith(RestController.class)
				.should().resideInAnyPackage(DOMAIN, APPLICATION, INFRASTRUCTURE)
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	private static String[] adapterTechnologiesAnd(String... packages) {
		return Stream.concat(Arrays.stream(ADAPTER_TECHNOLOGIES), Arrays.stream(packages)).toArray(String[]::new);
	}

}
