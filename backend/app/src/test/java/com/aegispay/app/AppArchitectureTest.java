package com.aegispay.app;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.aegispay.app", importOptions = ImportOption.DoNotIncludeTests.class)
class AppArchitectureTest {

    @ArchTest
    static final ArchRule no_mapstruct = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("org.mapstruct..");

    @ArchTest
    static final ArchRule payroll_does_not_put_wage_math_in_sql = noClasses()
            .that().resideInAPackage("com.aegispay.app.payroll..")
            .should().dependOnClassesThat().resideInAnyPackage("org.postgresql.jdbc..");
}
