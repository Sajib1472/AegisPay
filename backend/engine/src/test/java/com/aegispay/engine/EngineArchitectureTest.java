package com.aegispay.engine;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.aegispay.engine", importOptions = ImportOption.DoNotIncludeTests.class)
class EngineArchitectureTest {

    @ArchTest
    static final ArchRule engine_is_pure_java = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule engine_does_not_talk_to_sql = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("org.hibernate..", "org.postgresql..", "javax.sql..", "java.sql..");
}
