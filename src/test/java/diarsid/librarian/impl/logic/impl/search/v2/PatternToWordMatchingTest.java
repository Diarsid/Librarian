package diarsid.librarian.impl.logic.impl.search.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.AggregationCodeV2;
import diarsid.librarian.impl.logic.impl.search.charscan.matching.MatchingCodeV2;
import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching;
import diarsid.librarian.tests.setup.transactional.AwareOfTestAnnotations;
import diarsid.librarian.tests.setup.transactional.AwareOfTestName;
import diarsid.support.tests.expectations.Expectation;
import diarsid.support.tests.expectations.Expectations;

import static java.lang.String.format;
import static java.util.Collections.sort;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching.currentVersion;
import static diarsid.support.misc.Misc.methodName;

@ExtendWith(AwareOfTestName.class)
@ExtendWith(AwareOfTestAnnotations.class)
public class PatternToWordMatchingTest {

    private static final PatternToWordMatching MATCHING = currentVersion();

    private static class Statistic {

        private static class TestInstance implements Comparable<TestInstance> {

            private String pattern;
            private String word;
            private boolean expectMatching;

            public TestInstance(PatternToWordMatchingTest test) {
                this.pattern = test.pattern;
                this.word = test.word;
                this.expectMatching = test.expectMatching;
            }

            @Override
            public String toString() {
                return "TestInstance{" +
                        "pattern='" + pattern + '\'' +
                        ", word='" + word + '\'' +
                        ", expectMatching=" + expectMatching +
                        '}';
            }

            public String report() {
                return "pattern='" + pattern + '\'' +
                        " word='" + word + '\'' +
                        " expectMatching=" + expectMatching;
            }

            @Override
            public int compareTo(TestInstance other) {
                return Boolean.compare(this.expectMatching, other.expectMatching);
            }
        }

        int negative;
        int positive;
        int experimental;
        List<TestInstance> experimentalPassed = new ArrayList<>();
        List<TestInstance> experimentalFailed = new ArrayList<>();
        int desirable;
        List<TestInstance> desirablePassed = new ArrayList<>();
        List<TestInstance> desirableFailed = new ArrayList<>();
        int success;
        int fail;
    }

    public static Statistic statistic;

    private boolean isOnlyDesirable = false;
    private boolean isExperimental = false;
    private String pattern;
    private String word;
    private boolean expectMatching;
    private String comment;
    private Expectations<MatchingCodeV2> expectations = new Expectations<>();

    public void experimental() {
        this.isExperimental = true;
    }

    public void experimental(String comment) {
        this.isExperimental = true;
        this.comment = comment;
    }

    public void onlyDesirable() {
        this.isOnlyDesirable = true;
    }

    public void comment(String comment) {
        this.comment = comment;
    }

    public void doTest() {
        String testMethodName = methodName(1);
        String[] methodDeclarationWords = testMethodName.split("_");
        assertThat(methodDeclarationWords).hasSizeGreaterThanOrEqualTo(3);
        String test = methodDeclarationWords[0];
        assertThat(test).isEqualTo("test");
        this.pattern = methodDeclarationWords[1];
        this.word = methodDeclarationWords[2];

        if ( ! this.isOnlyDesirable ) {
            this.isOnlyDesirable = AwareOfTestAnnotations
                    .oneAnnotation(OnlyDesirable.class)
                    .isPresent();
        }

        if ( nonNull(comment) ) {
            System.out.println("[TEST] comment: " + comment);
        }

        long code = MATCHING.evaluate(pattern, word);

        if ( this.expectations.areNotEmpty() ) {
            MatchingCodeV2 decomposedCode = new MatchingCodeV2(code);
            this.expectations.accept(decomposedCode);
        }

        this.expectMatching = findBoolIn(methodDeclarationWords).orElseThrow(
                () -> new IllegalArgumentException("Method declaration does not contain any boolean matching!"));

        if ( expectMatching ) {
            statistic.positive++;
        }
        else {
            statistic.negative++;
        }

        boolean matching = code > -1;
        boolean expectationFailed = this.expectations.areFailed();

        boolean testFailed = (expectMatching ^ matching) || expectationFailed;

        System.out.println(format("[TEST] expect matching:%s, result:%s",
                expectMatching ? "TRUE" : "FALSE",
                testFailed ? "FAIL" : "PASS"));

        if ( this.isExperimental ) {
            statistic.experimental++;
            if ( testFailed ) {
                statistic.experimentalFailed.add(new Statistic.TestInstance(this));
            }
            else {
                statistic.experimentalPassed.add(new Statistic.TestInstance(this));
            }
            System.out.println("[TEST] experimental");
        }

        if ( this.isOnlyDesirable ) {
            statistic.desirable++;
            if ( testFailed ) {
                statistic.desirableFailed.add(new Statistic.TestInstance(this));
            }
            else {
                statistic.desirablePassed.add(new Statistic.TestInstance(this));
            }
            System.out.println("[TEST] is desirable, but not mandatory");
        }

        if ( testFailed ) {
            if ( ! this.isOnlyDesirable ) {
                assumeTrue( ! this.isExperimental, "[TEST] Test is an experimental behavior");
                statistic.fail++;
                if ( expectationFailed ) {
                    this.expectations.assertPassed();
                }
                else {
                    fail();
                }
            }
        }
        else {
            statistic.success++;
        }
    }

    private static Optional<Boolean> findBoolIn(String[] methodDeclarationWords) {
        String word;

        for (int i = 2; i < methodDeclarationWords.length; i++) {
            word = methodDeclarationWords[i].toLowerCase().trim().strip();
            if ( word.equals("true") ) {
                return Optional.of(true);
            }
            if ( word.equals("false") ) {
                return Optional.of(false);
            }
        }

        return Optional.empty();
    }

    @BeforeAll
    public static void before() {
        statistic = new Statistic();
    }

    private static final String TEST_FORMAT = "           %-19s %-19s %s";

    @AfterAll
    public static void after() {
        System.out.println("=== Statistic ===");
        System.out.println(" negative tests   : " + statistic.negative);
        System.out.println(" positive tests   : " + statistic.positive);
        System.out.println(" desirable        : " + statistic.desirable);
        System.out.println("     passed       : " + statistic.desirablePassed.size());
        printTests(statistic.desirablePassed);
        System.out.println("     failed       : " + statistic.desirableFailed.size());
        printTests(statistic.desirableFailed);
        System.out.println(" experimental     : " + statistic.experimental);
        System.out.println("     passed       : " + statistic.experimentalPassed.size());
        printTests(statistic.experimentalPassed);
        System.out.println("     failed       : " + statistic.experimentalFailed.size());
        printTests(statistic.experimentalFailed);
        System.out.println(" fail             : " + statistic.fail);
        System.out.println(" success          : " + statistic.success);
    }

    private static void printTests(List<Statistic.TestInstance> tests) {
        sort(tests);
        if ( tests.isEmpty() ) {
            return;
        }
        System.out.println(format(TEST_FORMAT, "PATTERN", "WORD", "EXPECTED_MATCHING"));
        for ( Statistic.TestInstance test : tests ) {
            System.out.println(format(TEST_FORMAT, test.pattern, test.word, test.expectMatching));
        }
    }

    @Test
    public void test_tolknebyjr_newborn_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_torah_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_tower_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_tokyo_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_lnemerow_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_jrr_true() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_newton_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_knots_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_tolkien_true() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_by_true() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_ebola_false() {
        doTest();
    }

    @Test
    public void test_tolknebyjr_knitter_false() {
        doTest();
    }

    @Test
    public void test_srvr_servers_true() {
        doTest();
    }

    @Test
    public void test_srvrs_servers_true() {
        doTest();
    }

    @Test
    public void test_srv_servers_true() {
        doTest();
    }

    @Test
    public void test_srvcs_services_true() {
        doTest();
    }

    @Test
    public void test_prj_projects_true() {
        doTest();
    }

    @Test
    public void test_prjs_projects_true() {
        doTest();
    }

    @Test
    public void test_porj_projects_true() {
        doTest();
    }

    @Test
    public void test_porjs_projects_true() {
        doTest();
    }

    @Test
    public void test_porjcs_projects_true() {
        doTest();
    }

    @Test
    public void test_3toolsserv_colonels_false() {
        doTest();
    }

    @Test
    public void test_3toolsserv_servers_true() {
        doTest();
    }

    @Test
    public void test_3toolsserv_sisemore_false() {
        doTest();
    }

    @Test
    public void test_3toolsserv_operator_false() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_tools_true() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_lovers_false() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_tudor_false() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_servers_true() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_virtualization_true() {
        doTest();
    }

    @Test
    public void test_3toolssevrirtl_virtualization_true() {
        doTest();
    }

    @Test
    public void test_toolssevritrl_virtualization_true() {
        doTest();
    }

    @Test
    public void test_3toolssevritrl_virtualization_true() {
        doTest();
    }

    @Test
    public void test_kroedc_docker_false() {
        doTest();
    }

    @Test
    public void test_roedck_docker_true() {
        doTest();
    }

    @Test
    public void test_docekr_docker_true() {
        doTest();
    }

    @Test
    public void test_roeck_docker_false() {
        doTest();
    }

    @Test
    public void test_roeckd_docker_false() {
        doTest();
    }

    @Test
    public void test_dckre_docker_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_lord_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_of_true() {
        doTest();
    }

    @Test
    public void test_timewheldragn_time_true() {
        doTest();
    }

    @Test
    public void test_timewheldragn_wheel_true() {
        doTest();
    }

    @Test
    public void test_timewheldragn_dragon_true() {
        doTest();
    }

    @Test
    public void test_wheloftimedrgrebrn_dragon_true() {
        doTest();
    }

    @Test
    public void test_wheloftimedrgrebrn_reborn_true() {
        doTest();
    }

    @Test
    public void test_wheloftimedrgrebrn_wheel_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_rings_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_by_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_jrr_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_true() {
        doTest();
    }

    @Test
    public void test_byjrrtolk_tolkien_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_orthodoxy_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_robertson_false() {
        doTest();
    }


    @Test
    public void test_lorofrngbyjrrtolk_loving_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_frontal_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tower_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_torrey_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_long_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_lotions_false() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkiens_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_ring_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien1s_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtlok_tolkiens_true() {
        doTest();
    }

    @Test
    public void test_lorofrngbyjrrtlok_tolkien1s_true() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_3tools_true() {
        doTest();
    }

    @Test
    public void test_toolssevrirtl_virtual_true() {
        doTest();
    }

    @Disabled("how to distinguish mismatch from abbreviation?")
    @Test
    public void test_wh_warhammer_true() {
        doTest();
    }

    @Test
    public void test_byjrrtlok_tolkien_true() {
        doTest();
    }

    @Test
    public void test_byjrrtlok_jrr_true() {
        doTest();
    }

    @Test
    public void test_byjrrtlok_by_true() {
        doTest();
    }

    @Test
    public void test_guide_guide_true() {
        doTest();
    }

    @Test
    public void test_whltwhitmn_whalt_true() {
        doTest();
    }

    @Test
    public void test_whaltwhitmn_whitman_true() {
        doTest();
    }

    @Test
    public void test_whltwhtmn_whitman_true() {
        doTest();
    }

    @Test
    public void test_whltwhtmn_walt_true() {
        doTest();
    }

    @Test
    public void test_whltwhtmn_what_true() {
        doTest();
    }

    @Test
    public void test_whltwhtmn_whitmans_true() {
        doTest();
    }

    @Test
    public void test_whltwhtmn_whitman1s_true() {
        doTest();
    }

    @Test
    public void test_whltwhtmn_twelfth_false() {
        doTest();
    }

    @Test
    public void test_88abc123abcdf88_abcdxf_synthetic_true() {
        doTest();
    }

    @Test
    public void test_88abc888abcde888abcdef88_abcdexf_synthetic_true() {
        doTest();
    }

    @Test
    public void test_jeschrstpassn_jesus_true() {
        doTest();
    }

    @Test
    public void test_jeschrstpassn_christ_true() {
        doTest();
    }

    @Test
    public void test_jeschrstpassn_christianity_true() {
        doTest();
    }

    @Test
    public void test_jeschrstpassn_passion_true() {
        doTest();
    }

    @Test
    public void test_jeschrstpassn_assessment_true() {
        doTest();
    }

    @Test
    public void test_jescrst_christian_true() {
        doTest();
    }

    @Test
    public void test_jescrst_jesus_true() {
        doTest();
    }

    @Test
    public void test_virtl_immortality_false() {
        doTest();
    }

    @Test
    public void test_virtl_victory_false() {
        doTest();
    }

    @Test
    public void test_tolos_llosa_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_tolos_tortoise_false() {
        doTest();
    }

    @Test
    public void test_tolos_textbooks_false() {
        doTest();
    }

    @Test
    public void test_tolos_tropicals_false() {
        doTest();
    }

    @Test
    public void test_tolos_totalitarianism_false() {
        doTest();
    }

    @Test
    public void test_tolos_tocquevilles_false() {
        doTest();
    }

    @Test
    public void test_tolos_loves_false() {
        doTest();
    }

    @Test
    public void test_tols_trilogy_false() {
        doTest();
    }

    @Test
    public void test_tolos_trilogy_false() {
        doTest();
    }

    @Test
    public void test_virtlservs_street_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_tolsvirtl_savitch_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_tolsvirtl_little_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_tolsvirtl_tvollmann_false() {
        doTest();
    }

    @Test
    public void test_virtlservs_variants_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_servs_sorceress_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_servs_speirs_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    @OnlyDesirable
    public void test_servs_ravishing_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_servs_sorcerers_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    @OnlyDesirable
    public void test_servs_serenissima_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_servs_spears_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    @OnlyDesirable
    public void test_servs_revised_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_servs_sierras_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    @OnlyDesirable
    public void test_servs_sgerstein_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    @OnlyDesirable
    public void test_servrs_sgerstein_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_srvrs_sgerstein_false() {
        doTest();
    }

    @Test
    public void test_servs_supremo_false() {
        doTest();
    }

    @Test
    public void test_servs_services_true() {
        doTest();
    }

    @Test
    public void test_romerise_rising_true() {
        doTest();
    }

    @Test
    public void test_romerise_tristes_false() {
        doTest();
    }

    @Test
    public void test_romerise_cristea_false() {
        doTest();
    }

    @Test
    public void test_romerise_from_false() {
        doTest();
    }

    @Test
    public void test_romerise_eisenhower_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_romerise_menagerie_false() {
        doTest();
    }

    @Test
    public void test_romerise_grisetti_false() {
        doTest();
    }

    @Test
    public void test_romerise_exercises_false() {
        doTest();
    }

    @Test
    public void test_romeries_ariadnes_false() {
        doTest();
    }

    @Test
    public void test_romeries_home_false() {
        doTest();
    }

    @Test
    public void test_romeries_euripides_false() {
        doTest();
    }

    @Test
    public void test_romeries_elites_false() {
        doTest();
    }

    @Test
    public void test_romeries_richest_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_yasnrkawbata_yasunari_true() {
        doTest();
    }

    @Test
    public void test_yasnkawbata_yasunari_true() {
        doTest();
    }

    @Test
    public void test_yasnrkwabta_yasunari_true() {
        doTest();
    }

    @Test
    public void test_yansrkwabta_yasunari_true() {
        doTest();
    }

    @Test
    public void test_yasnrkwabta_kawabata_true() {
        doTest();
    }

    @Test
    public void test_waltwthmn_whitman_true() {
        doTest();
    }

    @Test
    public void test_hobt_hackabout_false() {
        doTest();
    }

    @Test
    public void test_virtl_interlinear_false() {
        doTest();
    }

    @Test
    public void test_virtl_virtual_true() {
        doTest();
    }

    @Test
    public void test_lorofrng_rings_true() {
        doTest();
    }

    @Test
    public void test_rng_rings_true() {
        doTest();
    }

    @Test
    public void test_rgns_rings_true() {
        doTest();
    }

    @Test
    public void test_lorofrgns_rings_true() {
        doTest();
    }

    @Test
    public void test_lorofrng_ring_true() {
        doTest();
    }

    @Test
    public void test_lorofrng_lord_true() {
        doTest();
    }

    @Test
    public void test_lorofrng_of_true() {
        doTest();
    }

    @Test
    public void test_jenknsinstl_jenkins_true() {
        doTest();
    }

    @Test
    public void test_jenknsinstl_install_true() {
        doTest();
    }

    @Test
    public void test_jenknsinstl_installing_true() {
        doTest();
    }

    @Test
    public void test_jenknsinstl_installation_true() {
        doTest();
    }

    @Test
    public void test_tools_costa_false() {
        doTest();
    }

    @Test
    public void test_tolosvirtl_tools_true() {
        doTest();
    }

    @Test
    public void test_tolsvirtl_tools_true() {
        doTest();
    }

    @Test
    public void test_tolosvirtl_virtualization_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_virtlzn_virtualization_true() {
        doTest();
    }

    @Test
    public void test_drsprojs_diarsid_true() {
        doTest();
    }

    @Test
    public void test_drsprojs_projects_true() {
        doTest();
    }

    @Test
    public void test_kwiszhedrah_haderach_true() {
        doTest();
    }

    @Test
    public void test_projsdrs_diarsid_true() {
        doTest();
    }

    @Test
    public void test_projsdrs_projects_true() {
        doTest();
    }

    @Test
    public void test_kwisahaderh_haderach_true() {
        doTest();
    }

    @Test
    public void test_kwistzhadrch_kwisatz_true() {
        doTest();
    }

    @Test
    public void test_kwistzhadrch_haderach_true() {
        doTest();
    }

    @Test
    public void test_kwisahaderh_kwisatz_true() {
        doTest();
    }

    @Test
    public void test_exti_exit_true() {
        doTest();
    }

    @Test
    public void test_ext_exit_true() {
        doTest();
    }

    @Test
    public void test_drsid_diarsid_true() {
        doTest();
    }

    @Test
    public void test_prjsdrs_diarsid_true() {
        doTest();
    }

    @Test
    public void test_prjsdrs_projects_true() {
        doTest();
    }

    @Test
    public void test_porjdrsd_projects_true() {
        doTest();
    }

    @Test
    public void test_drsdporj_projects_true() {
        doTest();
    }

    @Test
    public void test_drsdporj_diarsid_true() {
        doTest();
    }

    @Test
    public void test_enginsjva_engins_true() {
        doTest();
    }

    @Test
    public void test_enginsjva_java_true() {
        doTest();
    }

    @Test
    public void test_passnjss_passion_true() {
        doTest();
    }

    @Test
    public void test_passnjss_jesus_true() {
        doTest();
    }

    @Test
    public void test_passnjss_jesusx_true() {
        doTest();
    }

    @Test
    public void test_passnjssy_jesusx_true() {
        doTest();
    }

    @Test
    public void test_jsspassn_passion_true() {
        doTest();
    }

    @Test
    public void test_jsspassn_jesus_true() {
        doTest();
    }

    @Test
    public void test_jsspassn_jesusx_true() {
        doTest();
    }

    @Test
    public void test_jss_jesusx_true() {
        doTest();
    }

    @Test
    public void test_jsspassn_jesusxy_true() {
        doTest();
    }

    @Test
    public void test_sex_jesusx_false() {
        doTest();
    }

    @Test
    public void test_uposhtarch_poshta_true() {
        doTest();
    }

    @Test
    public void test_upostarch_poshta_true() {
        doTest();
    }

    @Test
    public void test_uposhtarch_ukrposhta_true() {
        doTest();
    }

    @Test
    public void test_uposhtarch_ukr_true() {
        doTest();
    }

    @Test
    public void test_ukposhtarch_ukr_true() {
        doTest();
    }

    @Test
    public void test_ukposht_ukr_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_proupsth_ukrposhta_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_proupsth_poshta_true() {
        doTest();
    }

    @Test
    public void test_uposhtarch_archive_true() {
        doTest();
    }

    @Test
    public void test_jss_jesus_true() {
        doTest();
    }

    @Test
    public void test_srvsmsg_servers_true() {
        doTest();
    }

    @Test
    public void test_srvsmsg_message_true() {
        doTest();
    }

    @Test
    public void test_msg_message_true() {
        doTest();
    }

    @Test
    public void test_srvsmesg_message_true() {
        doTest();
    }

    @Test
    public void test_srvsmesg_servers_true() {
        doTest();
    }

    @Test
    public void test_srvsmsg_messaging_true() {
        doTest();
    }

    @Test
    public void test_xxmsg_messaging_true() {
        doTest();
    }

    @Test
    public void test_xxmssg_messaging_true() {
        doTest();
    }

    @Test
    public void test_porjsdrsd_projects_true() {
        doTest();
    }

    @Test
    public void test_porjdrsd_diarsid_true() {
        doTest();
    }

    @Test
    public void test_porjsdrsd_diarsid_true() {
        doTest();
    }

    @Test
    public void test_drs_diarsid_true() {
        doTest();
    }

    @Test
    public void test_drklalver_dracula_true() {
        doTest();
    }

    @Test
    public void test_drklalver_draculas_true() {
        doTest();
    }

    @Test
    public void test_drklalver_lover_true() {
        doTest();
    }

    @Test
    public void test_ukrpsotapi_ukrposhta_true() {
        doTest();
    }

    @Test
    public void test_ukrpsotapi_api_true() {
        doTest();
    }

    @Test
    public void test_dnehrbrt_dune_true() {
        doTest();
    }

    @Test
    public void test_dnehrbrt_herbert_true() {
        doTest();
    }

    @Test
    public void test_acbd_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_acbd_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbe_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbdf_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_acbdf_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbef_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbf_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_acbf_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbdxyz0123_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_0123acbdxyz_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_acbdxyz0123_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_0123acbdxyz_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbexyz0123_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_0123acbexyz_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbdfxyz0123_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_0123acbdfxyz_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_acbdfxyz0123_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_0123acbdfxyz_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbefxyz0123_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_0123acbefxyz_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbfxyz0123_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_0123acbfxyz_abcdefg_true() {
        doTest();
    }

    @Test
    public void test_acbfxyz0123_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_0123acbfxyz_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acbd0123_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_0123acbd_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_acbd01h3_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_acdf_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_acdf_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acdf0123_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_0123acdf_abcdefghijklmn_true() {
        doTest();
    }

    @Test
    public void test_acdf0123_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_0123acdf_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_adfh0123_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_0123adfh_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_adfh_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_adfh_abcdefgh01234_false() {
        doTest();
    }

    @Test
    public void test_abcedf_abcdeafghij_true() {
        doTest();
    }

    @Test
    public void test_abcedf_abcdefghij_true() {
        doTest();
    }

    @Test
    public void test_sres_servers_true() {
        doTest();
    }

    @Test
    public void test_sers_servers_true() {
        doTest();
    }

    @Test
    public void test_ytube_youtube_true() {
        doTest();
    }

    @Test
    public void test_ytbe_youtube_true() {
        doTest();
    }

    @Test
    public void test_yutbe_youtube_true() {
        doTest();
    }

    @Test
    public void test_yuotbe_youtube_true() {
        doTest();
    }

    @Test
    public void test_yotbe_youtube_true() {
        doTest();
    }

    @Test
    public void test_ggl_google_true() {
        doTest();
    }

    @Test
    public void test_yteb_youtube_false() {
        doTest();
    }

    @Test
    public void test_adfg0123_abcdefgh_false() {
        comment("not sure about it is really false");
        doTest();
    }

    @Test
    public void test_adfg_abcdefgh_true() {
        doTest();
    }

    @Test
    public void test_adfg_abcdefgh01234_false() {
        comment("really negative pattern?");
        doTest();
    }

    @Test
    public void test_adgf0123_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_adgf_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_adgf_abcdefgh01234_false() {
        doTest();
    }

    @Test
    public void test_adgfh0123_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_adgfh_abcdefgh_false() {
        doTest();
    }

    @Test
    public void test_adgfh_abcdefgh01234_false() {
        doTest();
    }

    @Test
    public void test_msser_servers_true() {
        doTest();
    }

    @Test
    public void test_acbd_abcd_true() {
        doTest();
    }

    @Test
    public void test_abdc_abcd_true() {
        doTest();
    }

    @Test
    public void test_romeries_rise_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_tolos_its_false() {
        doTest();
    }

    @Test
    public void test_tolos_lowndes_false() {
        doTest();
    }

    @Test
    public void test_tolos_looking_false() {
        doTest();
    }

    @Test
    public void test_tolos_blossom_false() {
        doTest();
    }

    @Test
    public void test_kwizachederahatrids_caused_false() {
        doTest();
    }

    @Test
    public void test_kwizachederahatrids_haderach_true() {
        doTest();
    }

    @Test
    public void test_tools_ordeals_false() {
        doTest();
    }


    @Test
    public void test_tools_TextbOOkS_false() {
        doTest();
    }


    @Test
    public void test_tools_TrOOperS_false() {
        doTest();
    }

    @Test
    public void test_tools_tycoons_false() {
        doTest();
    }

    @Test
    public void test_tools_olmecs_false() {
        doTest();
    }

    @Test
    public void test_tools_toenails_false() {
        doTest();
    }

    @Test
    public void test_tolos_louise_false() {
        doTest();
    }

    @Test
    public void test_tolos_outposts_false() {
        doTest();
    }

    @Test
    public void test_tolos_oldies_false() {
        doTest();
    }

    @Test
    public void test_tolos_topolski_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_tolos_loftis_false() {
        doTest();
    }

    @Test
    public void test_tolos_tomorrows_false() {
        doTest();
    }

    @Test
    public void test_tolos_teukolsky_false() {
        doTest();
    }

    @Test
    public void test_tolos_troost_false() {
        experimental();
        doTest();
    }

    @Test
    public void test_tolos_toujours_false() {
        doTest();
    }

    @Test
    public void test_tolos_lowlands_false() {
        doTest();
    }

    @Test
    public void test_tolos_technologies_false() {
        doTest();
    }

    @Test
    public void test_tolos_larose_false() {
        doTest();
    }

    @Test
    public void test_tolos_lovers_false() {
        doTest();
    }

    @Test
    public void test_tolos_tortoises_false() {
        doTest();
    }

    @Test
    public void test_tolos_theologies_false() {
        doTest();
    }

    @Test
    public void test_teolg_theologies_true() {
        doTest();
    }

    @Test
    public void test_teolgs_theologies_true() {
        doTest();
    }

@Test
    public void test_servs_sanders_false() {
        doTest();
    }

//    XXXXX
//
//    log where found=3, 1-ch-miss>0 matching:true result:pass macthLength > 5/6/7
//    options - wordDiff > 2 (P:projsdrs --- W:diarsid)
    
    @Test
    public void test_servs_sonderstab_false() {
        doTest();
    }

    @Test
    public void test_servs_shofer_false() {
        doTest();
    }

    @Test
    public void test_servs_superstar_false() {
        doTest();
    }

    @Test
    public void test_servs_sayers_false() {
        doTest();
    }

    @Test
    public void test_servs_stickers_false() {
        doTest();
    }

    @Test
    public void test_lorofrng_london_false() {
        doTest();
    }

    @Test
    public void test_lorofrng_learning_false() {
        doTest();
    }

    @Test
    public void test_tolsvirtl_living_false() {
        doTest();
    }

    @Test
    public void test_tolsvirtl_orville_false() {
        doTest();
    }

    @Test
    public void test_virtual_via_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_virtual_vitality_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_virtual_vital_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_virtual_italy_false() {
        doTest();
    }

    @Test
    public void test_msser_messaging_true() {
        doTest();
    }

    @Test
    public void test_romeries_recipes_false() {
        doTest();
    }

    @Test
    public void test_romeries_others_false() {
        doTest();
    }

    @Test
    public void test_romeries_memory_false() {
        doTest();
    }

    @Test
    public void test_romeries_revised_false() {
        doTest();
    }

    @Test
    public void test_romeries_mysteries_false() {
        doTest();
    }

    @Test
    public void test_romeries_episode_false() {
        doTest();
    }

    @Test
    public void test_romeries_renaissance_false() {
        doTest();
    }

    @Test
    public void test_romeries_modern_false() {
        doTest();
    }

    @Test
    public void test_tols_tolkiens_false() {
        doTest();
    }

    @Test
    public void test_tols_torrents_false() {
        doTest();
    }

    @Test
    public void test_kwistz_kranowitz_false() {
        doTest();
    }

    @Test
    public void test_romeries_rene_false() {
        doTest();
    }

    @Test
    public void test_3toolssevrirtl_tolls_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_jeschrstpassn_asniper_false() {
        experimental();
        doTest();
    }

    @Test
    public void test_kwizachederahatrids_federations_false() {
        doTest();
    }

    @Test
    public void test_tools_tolls_false() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_npleon_napoleon_true() {
        doTest();
    }

    @Test
    public void test_naplon_napoleon_true() {
        doTest();
    }

    @Test
    public void test_tmclncy_tom_true() {
        doTest();
    }

    @Test
    public void test_tmclncy_clancy_true() {
        doTest();
    }

    @Test
    public void test_brkfst_breakfast_true() {
        doTest();
    }

    @Test
    public void test_mtrxpholspy_philosophy_true() {
        doTest();
    }

    @Test
    public void test_mtrxpholspy_matrix_true() {
        doTest();
    }

    @Test
    public void test_upshstnoftftclnt_notification_true() {
        doTest();
    }

    @Test
    public void test_upshstnoftftclnt_ukrposhta_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_upshstnoftftclnt_ukr_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_upshstnoftftclnt_ua_true() {
        doTest();
    }

    @Test
    public void test_upshstnoftftclnt_status_true() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_upshstsnoftftclnt_status_true() {
        doTest();
    }

    @Test
    public void test_sts_status_true() {
        doTest();
    }

    @Test
    public void test_upshstnoftftclnt_client_true() {
        doTest();
    }

    @Test
    public void test_ukrpsthnotfclntstats_notification_true() {
        doTest();
    }

    @Test
    public void test_ukrpsthnotfclntstats_ukrposhta_true() {
        doTest();
    }

    @Test
    public void test_ukrpsthnotfclntstats_status_true() {
        doTest();
    }

    @Test
    public void test_ukrpsthnotfclntstats_client_true() {
        doTest();
    }

    @Test
    public void test_jrtolkbylord_tolkien_true() {
        doTest();
    }

    @Test
    public void test_jrtolkbylord_jrr_true() {
        doTest();
    }

    @Test
    public void test_path_goldpath_true() {
        doTest();
    }

    @Test
    public void test_goldpath_path_true() {
        doTest();
    }

    @Test
    public void test_dth_death_true() {
        doTest();
    }

    @Test
    public void test_gml_gmail_true() {
        doTest();
    }

    @Test
    public void test_trnslt_translate_true() {
        doTest();
    }

    @Test
    public void test_trsnlt_translate_true() {
        doTest();
    }

    @Test
    public void test_tsrnlt_translate_true() {
        doTest();
    }

    @Test
    public void test_ntpd_notepad_true() {
        doTest();
    }

    @Test
    public void test_ntpd_notepadplusplus_true() {
        doTest();
    }

    @Test
    public void test_ntpdpp_notepadplusplus_true() {
        doTest();
    }

    @Test
    public void test_ntpdpp_notepad_true() {
        doTest();
    }

    @Test
    public void test_ntpdpp_plusplus_true() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_ntpdpls_notepadplusplus_true() {
        doTest();
    }

    @Test
    public void test_ntpdplspls_notepadplusplus_true() {
        doTest();
    }

    @Test
    public void test_servs_stress_false() {
        doTest();
    }

    @Test
    public void test_jvaenings_java_true() {
        doTest();
    }

    @Test
    public void test_jvaenings_engines_true() {
        doTest();
    }

    @Test
    public void test_cdrw_coreldraw_true() {
        doTest();
    }

    @Test
    public void test_crldrw_draw_true() {
        doTest();
    }

    @Test
    public void test_clrdrw_draw_true() {
        doTest();
    }

    @Test
    public void test_cdrw_corel_false() {
        doTest();
    }

    @Test
    public void test_crdrw_corel_true() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_cdrw_draw_true() {
        doTest();
    }

    @Test
    public void test_crldrw_coreldraw_true() {
        doTest();
    }

    @Test
    public void test_crldr_coreldraw_true() {
        doTest();
    }

    @Test
    public void test_crldw_coreldraw_true() {
        doTest();
    }

    @Test
    public void test_crldwr_coreldraw_true() {
        doTest();
    }

    @Test
    public void test_crldrw_corel_true() {
        doTest();
    }

    @Test
    public void test_crl_corel_true() {
        doTest();
    }

    @Test
    public void test_crl_coreld_true() {
        doTest();
    }

    @Test
    public void test_crl_coreldr_true() {
        doTest();
    }

    @Test
    public void test_crld_corel_true() {
        doTest();
    }

    @Test
    public void test_clrdrw_coreldraw_true() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_clrdrw_corel_true() {
        onlyDesirable();
        doTest();
    }

    @Test
    public void test_clrdrw_color_true() {
        doTest();
    }

    @Test
    public void test_tolsvirtl_possible_false() {
        doTest();
    }

    @Test
    public void test_tolsvirtl_worlds_false() {
        doTest();
    }

    @Test
    public void test_tolsvirtl_toussaint_false() {
        doTest();
    }

    @Test
    public void test_ilovyo_girlhood_false() {
        doTest();
    }

    @Test
    public void test_drklalver_delivering_false() {
        doTest();
    }

    @Test
    public void test_drklalver_delivered_false() {
        doTest();
    }

    @Test
    public void test_virtlservs_resurrection_false() {
        doTest();
    }

    @Test
    public void test_kwizachoderah_reached_false() {
        doTest();
    }

    @Test
    public void test_kwizachoderah_herbert_false() {
        doTest();
    }

    @Test
    public void test_drklalver_drockefeller_false() {
        doTest();
    }

    @Test
    public void test_goldpath_goddard_false() {
        doTest();
    }

    @Test
    public void test_goldpath_goodall_false() {
        doTest();
    }

    @Test
    public void test_tolosvirtl_overtreated_false() {
        doTest();
    }

    @Test
    public void test_imnlknt_montecristo_false() {
        doTest();
    }

    @Test
    public void test_imnlknt_implementation_false() {
        doTest();
    }

    @Test
    public void test_imnlknt_inmortales_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_imnlknt_intelligence_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_imnlknt_diamant_false() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnxercs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_xercsmvn_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnxyercs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvanxercs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_xercsmvan_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvenxercs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnexercs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnxyz123ercs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_xyz123ercsmvn_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_xyzm123ercsmvn_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_xyzm123evrcsmvn_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnxzrcs_maven_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnxzrcs_xerces_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_mvnxercs_xerces_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_eclpmemanl_analizer_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_eclpanlmem_analizer_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_eclpanlmem_eclipse_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_anlmemeclp_eclipse_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_eclpanlmem_memory_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_posecom_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_uposecom_ukrposhta_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_posecom_ecom_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_antrplctable_another_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_antrplctable_place_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_antrplctable_table_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_anmlpanitng_animals_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_anmlpanitng_painting_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_hofmaninctant_hoffman_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_hofmaninctant_incantation_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_brontstreflc_brontosaurus_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_brontstreflc_reflections_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_pirctstokn_pictures_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_toknpircts_pictures_true() {
        doTest();
    }

    @Test
    @Tag("V48")
    public void test_pirctstokn_tolkien_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_projupsth_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_upsthproj_ukr_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_upsthproj_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_upsthproj_projects_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_projsupsth_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_projsupsth_projects_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    @OnlyDesirable
    public void test_projsupsth_ukr_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_projsupsth_ukrposhta_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_xrojupsth_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_poshtapiukr_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_apiposhtukr_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V51")
    public void test_apiukrposht_poshta_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_poshtapiukr_api_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_poshtapiukr_ukr_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_poshtapiukr_ukrposhta_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_progsloclscl_programs_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_progsloclscl_local_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_progsloclscl_social_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_progxloclscl_social_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_progxxxxxscl_social_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_progsxxxxscl_social_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_solmnmies_assassination_false() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_solmnmies_solomon_true() {
        doTest();
    }

    @Test
    @Tag("V50")
    public void test_solmnmies_mines_true() {
        doTest();
    }

    @Test
    @Tag("V52")
    public void test_rddragnhanballectr_red_true() {
        doTest();
    }

    @Test
    @Tag("V52")
    public void test_rddragnhanballectr_dragon_true() {
        doTest();
    }

    @Test
    @Tag("V52")
    public void test_rddragnhanballectr_hannibal_true() {
        doTest();
    }

    @Test
    @Tag("V52")
    public void test_rddragnhanballectr_lecter_true() {
        doTest();
    }

    @Test
    @Tag("V53")
    public void test_urbrsedssn_ouroboros_true() {
        doTest();
    }

    @Test
    @Tag("V54")
    public void test_rrhgtphilpsy_right_true() {
        doTest();
    }

    @Test
    @Tag("V53")
    public void test_rhgtphilpsy_right_true() {
        doTest();
    }

    @Test
    @Tag("V54")
    public void test_witwidsmwinstchucrl_wisdom_true() {
        doTest();
    }

    @Test
    @Tag("V54")
    public void test_witwidsmwinstchucrl_wit_true() {
        doTest();
    }

    @Test
    @Tag("V54")
    public void test_witwidsmwinstchucrl_winston_true() {
        doTest();
    }

    @Test
    @Tag("V54")
    public void test_witwidsmwinstchucrl_churchill_true() {
        doTest();
    }

    @Test
    @Tag("V54")
    public void test_devenigs_engines_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_mvnantlr_maven_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_mvnantlr_antlr_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_mvnantle_maven_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_mvnantle_antlr_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_chenrbly_chernobyl_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_waltwitmn_wanted_false() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_waltwitmn_lattimore_false() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_wcrft_warcraft_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_wrcft_warcraft_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_wrcrft_warcraft_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    @OnlyDesirable
    public void test_tolsvirtl_literature_false() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_witchsrls_witcher_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_witchsrls_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlswworld_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlswworld_westworld_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlswsworld_westworld_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlswsworld_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srwrsmove_movies_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srwrsmove_star_true() {
        doTest();
    }

    @Test
    @Tag("V56")
    public void test_movesrwrs_star_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srwrsmove_wars_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_movesrwrs_wars_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_bkscs_books_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_bkscs_cs_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_seralsffly_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_seralsffly_firefly_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlsslcnvally_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlsslcnvally_silicon_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlsslcnvally_valley_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlslcfr_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlslcfr_lucifer_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlsaltcrbn_serials_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlsaltcrbn_altered_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_srlsaltcrbn_carbon_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_engnspthn_python_true() {
        doTest();
    }

    @Test
    @Tag("V55")
    public void test_engnspthn_engines_true() {
        doTest();
    }

    @Test
    @Tag("V56")
    public void test_jpath_java_true() {
        doTest();
    }

    @Test
    @Tag("V56")
    public void test_beitcons_bitcoins_true() {
        expectations.add(code -> {
            assertThat(code.matchSpan).isGreaterThanOrEqualTo(7);
        });
        doTest();
    }

    @Test
    @Tag("V56")
    public void test_trky_turkey_true() {
        doTest();
    }

    @Test
    @Tag("V56")
    public void test_vvm_visual_true() {
        doTest();
    }

    @Test
    @Tag("V56")
    public void test_vvm_vm_true() {
        doTest();
    }

//    @Test
//    @Tag("V55")
//    public void test_P_W_true() {
//        doTest();
//    }

    @Test
    public void test_maximums() {
        List.of(
                "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg")
                .stream()
                .map(word -> MATCHING.evaluate(word, word))
                .collect(toList())
                .forEach(System.out::println);
    }

}
