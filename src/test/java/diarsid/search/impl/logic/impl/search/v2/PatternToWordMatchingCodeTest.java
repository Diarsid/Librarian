package diarsid.search.impl.logic.impl.search.v2;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;

import static diarsid.search.impl.logic.impl.search.v2.PatternToWordMatchingCode.evaluate;
import static diarsid.support.misc.Misc.methodName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class PatternToWordMatchingCodeTest {

    public void testMatching(boolean expectMatching) {
        String testMethodName = methodName(1);
        String[] methodDeclarationWords = testMethodName.split("_");
        assertThat(methodDeclarationWords).hasSizeGreaterThanOrEqualTo(3);
        String test = methodDeclarationWords[0];
        assertThat(test).isEqualTo("test");
        String pattern = methodDeclarationWords[1];
        String word = methodDeclarationWords[2];
        long code = evaluate(pattern, word);
        System.out.println(code);
        if ( expectMatching ) {
            if ( code < 0 ) {
                fail();
            }
        }
        else {
            if ( code > -1 ) {
                fail();
            }
        }
    }

    public void testMatching() {
        String testMethodName = methodName(1);
        String[] methodDeclarationWords = testMethodName.split("_");
        assertThat(methodDeclarationWords).hasSizeGreaterThanOrEqualTo(3);
        String test = methodDeclarationWords[0];
        assertThat(test).isEqualTo("test");
        String pattern = methodDeclarationWords[1];
        String word = methodDeclarationWords[2];
        long code = evaluate(pattern, word);
        System.out.println(code);
        boolean expectMatching = findBoolIn(methodDeclarationWords).orElseThrow(
                () -> new IllegalArgumentException("Method declaration does not contain any boolean matching!"));
        if ( expectMatching ) {
            if ( code < 0 ) {
                fail();
            }
        }
        else {
            if ( code > -1 ) {
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

    @Test
    public void test_tolknebyjr_newborn_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_torah_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_tower_false() {
        testMatching();
    }

    @Disabled
    @Test
    public void test_tolknebyjr_tokyo_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_lnemerow_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_jrr_true() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_newton_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_knots_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_ebola_false() {
        testMatching();
    }

    @Test
    public void test_tolknebyjr_knitter_false() {
        testMatching();
    }

    @Test
    public void test_srvr_servers_true() {
        testMatching();
    }

    @Test
    public void test_srvrs_servers_true() {
        testMatching();
    }

    @Test
    public void test_srv_servers_true() {
        testMatching();
    }

    @Test
    public void test_srvcs_services_true() {
        testMatching();
    }

    @Test
    public void test_prj_projects_true() {
        testMatching();
    }

    @Test
    public void test_porj_projects_true() {
        testMatching();
    }

    @Test
    public void test_3toolsserv_colonels_false() {
        testMatching();
    }

    @Test
    public void test_3toolsserv_servers_true() {
        testMatching();
    }

    @Test
    public void test_3toolsserv_sisemore_false() {
        testMatching();
    }

    @Test
    public void test_3toolsserv_operator_false() {
        testMatching();
    }

    @Test
    public void test_toolssevrirtl_tools_true() {
        testMatching();
    }

    @Test
    public void test_toolssevrirtl_lovers_false() {
        testMatching();
    }

    @Test
    public void test_toolssevrirtl_tudor_false() {
        testMatching();
    }

    @Test
    public void test_toolssevrirtl_servers_true() {
        testMatching();
    }

    @Test
    public void test_toolssevrirtl_virtualization_true() {
        testMatching();
    }

    @Test
    public void test_3toolssevrirtl_virtualization_true() {
        testMatching();
    }

    @Test
    public void test_toolssevritrl_virtualization_true() {
        testMatching();
    }

    @Test
    public void test_3toolssevritrl_virtualization_true() {
        testMatching();
    }

    @Test
    public void test_kroedc_docker_false() {
        testMatching();
    }

    @Test
    public void test_roedck_docker_true() {
        testMatching();
    }

    @Test
    public void test_docekr_docker_true() {
        testMatching();
    }

    @Test
    public void test_roeck_docker_false() {
        testMatching();
    }

    @Test
    public void test_roeckd_docker_false() {
        testMatching();
    }

    @Test
    public void test_dckre_docker_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_lord_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_of_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_rings_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_by_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_jrr_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_true() {
        testMatching();
    }

    @Test
    public void test_byjrrtolk_tolkien_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_orthodoxy_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_robertson_false() {
        testMatching();
    }


    @Test
    public void test_lorofrngbyjrrtolk_loving_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_frontal_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tower_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_torrey_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_long_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_lotions_false() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkiens_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien1s_true() {
        testMatching();
    }

    @Test
    public void test_lorofrngbyjrrtlok_tolkiens_true() {
        testMatching();
    }

    @Disabled
    @Test
    public void test_lorofrngbyjrrtlok_tolkien1s_true() {
        testMatching();
    }

    @Test
    public void test_toolssevrirtl_3tools_true() {
        testMatching();
    }

    @Disabled("how to distinguish mismatch from abbreviation?")
    @Test
    public void test_wh_warhammer_true() {
        testMatching();
    }

    @Test
    public void test_byjrrtlok_tolkien_true() {
        testMatching();
    }

    @Test
    public void test_guide_guide_true() {
        testMatching();
    }

    @Test
    public void test_whltwhitmn_whalt_true() {
        testMatching();
    }

    @Test
    public void test_whaltwhitmn_whitman_true() {
        testMatching();
    }

    @Test
    public void test_whltwhtmn_whitman_true() {
        testMatching();
    }

    @Test
    public void test_whltwhtmn_whitmans_true() {
        testMatching();
    }

    @Test
    public void test_whltwhtmn_whitman1s_true() {
        testMatching();
    }

    @Test
    public void test_whltwhtmn_twelfth_false() {
        testMatching();
    }

    @Test
    public void test_88abc123abcdf88_abcdxf_synthetic_true() {
        testMatching();
    }

    @Test
    public void test_88abc888abcde888abcdef88_abcdexf_synthetic_true() {
        testMatching();
    }

    @Test
    public void test_jeschrstpassn_jesus_true() {
        testMatching();
    }

    @Test
    public void test_jeschrstpassn_assessment_true() {
        testMatching();
    }

    @Test
    public void test_virtl_immortality_false() {
        testMatching();
    }

    @Test
    public void test_virtl_victory_false() {
        testMatching();
    }

    @Disabled("cannot imaging criteria")
    @Test
    public void test_tolos_llosa_false() {
        testMatching();
    }

    @Disabled
    @Test
    public void test_tolos_tortoise_false() {
        testMatching();
    }

    @Test
    public void test_tolos_textbooks_false() {
        testMatching();
    }

    @Test
    public void test_tolos_tropicals_false() {
        testMatching();
    }

    @Test
    public void test_tolos_totalitarianism_false() {
        testMatching();
    }

    @Test
    public void test_tolos_tocquevilles_false() {
        testMatching();
    }

    @Disabled
    @Test
    public void test_tolos_loves_false() {
        testMatching();
    }

    @Test
    public void test_tols_trilogy_false() {
        testMatching();
    }

    @Disabled
    @Test
    public void test_tolos_trilogy_false() {
        testMatching();
    }

    @Test
    public void test_virtlservs_street_false() {
        testMatching();
    }

    @Test
    public void test_virtlservs_variants_false() {
        testMatching();
    }

    @Test
    public void test_servs_supremo_false() {
        testMatching();
    }

    @Test
    public void test_maximums() {
        List.of(
                "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg")
                .stream()
                .map(word -> evaluate(word, word))
                .collect(toList())
                .forEach(System.out::println);
    }

}
