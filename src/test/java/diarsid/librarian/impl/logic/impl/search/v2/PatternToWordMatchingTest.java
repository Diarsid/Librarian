package diarsid.librarian.impl.logic.impl.search.v2;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching.CURRENT_VERSION;
import static diarsid.support.misc.Misc.methodName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PatternToWordMatchingTest {

    private static class Statistic {
        int negative;
        int positive;
    }

    public static Statistic statistic;

    private boolean isOnlyDesirable = false;
    private boolean isExperimental = false;
    private String comment;

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
        String pattern = methodDeclarationWords[1];
        String word = methodDeclarationWords[2];

        if ( nonNull(comment) ) {
            System.out.println("[TEST] comment: " + comment);
        }
        long code = CURRENT_VERSION.evaluate(pattern, word);

        System.out.println(code);

        boolean expectMatching = findBoolIn(methodDeclarationWords).orElseThrow(
                () -> new IllegalArgumentException("Method declaration does not contain any boolean matching!"));

        if ( expectMatching ) {
            statistic.positive++;
        }
        else {
            statistic.negative++;
        }

        boolean matching = code > -1;
        boolean mismatch = expectMatching ^ matching;

        System.out.println(format("[TEST] expect matching:%s, result:%s",
                expectMatching ? "TRUE" : "FALSE",
                mismatch ? "FAIL" : "PASS"));
        if ( this.isExperimental ) {
            System.out.println("[TEST] experimental");
        }
        if ( this.isOnlyDesirable ) {
            System.out.println("[TEST] is desirable, but not mandatory");
        }

        if ( mismatch ) {
            if ( ! this.isOnlyDesirable ) {
                assumeTrue( ! this.isExperimental, "[TEST] Test is an experimental behavior");
                fail();
            }
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

    @AfterAll
    public static void after() {
        System.out.println("=== Statistic ===");
        System.out.println(" negative tests: " + statistic.negative);
        System.out.println(" positive tests: " + statistic.positive);
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
    public void test_virtlservs_variants_false() {
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
    public void test_drsprojs_diarsid_true() {
        doTest();
    }

    @Test
    public void test_projsdrs_diarsid_true() {
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
    public void test_kwisahaderh_haderach_true() {
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
    public void test_adfg0123_abcdefgh_false() {
        comment("not sure about it is really false");
        doTest();
    }

    @Test
    public void test_adfg_abcdefgh_false() {
        comment("really negative pattern?");
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
    public void test_tolos_lowndes_false() {
        experimental();
        doTest();
    }

    @Test
    public void test_tolos_looking_false() {
        experimental();
        doTest();
    }

    @Test
    public void test_tolos_blossom_false() {
        experimental();
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
        experimental();
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
        experimental("?");
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
        experimental();
        doTest();
    }

    @Test
    public void test_servs_sayers_false() {
        experimental();
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
        experimental("found 4?????");
        doTest();
    }

    @Test
    public void test_tolsvirtl_orville_false() {
        experimental("WHAT???");
        doTest();
    }

    @Test
    public void test_virtual_via_false() {
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
        experimental("not sure how to balance P:ac == W:abc");
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
        experimental();
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
    public void test_upshstnoftftclnt_status_true() {
        onlyDesirable();
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
    public void test_maximums() {
        List.of(
                "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg")
                .stream()
                .map(word -> CURRENT_VERSION.evaluate(word, word))
                .collect(toList())
                .forEach(System.out::println);
    }

}
