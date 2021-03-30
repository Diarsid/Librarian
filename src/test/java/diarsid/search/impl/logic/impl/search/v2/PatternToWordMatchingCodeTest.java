package diarsid.search.impl.logic.impl.search.v2;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;

import static diarsid.search.impl.logic.impl.search.v2.PatternToWordMatchingCode.evaluate;
import static diarsid.search.impl.logic.impl.search.v2.PatternToWordMatchingCode.logln;
import static org.junit.jupiter.api.Assertions.fail;

public class PatternToWordMatchingCodeTest {

    public void testMatching(String pattern, String word, boolean expectMatching) {
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

    @Test
    public void test_tolknebyjr() {
        testMatching("tolknebyjr", "newborn", false);
    }

    @Test
    public void test_tolknebyjr_torah() {
        testMatching("tolknebyjr", "torah", false);
    }

    @Test
    public void test_tolknebyjr_tower() {
        testMatching("tolknebyjr", "tower", false);
    }

    @Disabled
    @Test
    public void test_tolknebyjr_tokyo() {
        testMatching("tolknebyjr", "tokyo", false);
    }

    @Test
    public void test_tolknebyjr_lnemerow() {
        testMatching("tolknebyjr", "lnemerow", false);
    }

    @Test
    public void test_tolknebyjr_jrr() {
        testMatching("tolknebyjr", "jrr", true);
    }

    @Test
    public void test_tolknebyjr_newton() {
        testMatching("tolknebyjr", "newton", false);
    }

    @Test
    public void test_tolknebyjr_knots() {
        testMatching("tolknebyjr", "knots", false);
    }

    @Test
    public void test_tolknebyjr_ebola() {
        testMatching("tolknebyjr", "ebola", false);
    }

    @Test
    public void test_tolknebyjr_knitter() {
        testMatching("tolknebyjr", "knitter", false);
    }

    @Test
    public void test_srvr_servers() {
        testMatching("srvr", "servers", true);
    }

    @Test
    public void test_srvrs_servers() {
        testMatching("srvrs", "servers", true);
    }

    @Test
    public void test_srv_servers() {
        testMatching("srv", "servers", true);
    }

    @Test
    public void test_srvcs_services() {
        testMatching("srvcs", "services", true);
    }

    @Test
    public void test_prj_projects() {
        testMatching("prj", "projects", true);
    }

    @Test
    public void test_porj_projects() {
        testMatching("porj", "projects", true);
    }

    @Test
    public void test_3toolsserv_colonels() {
        testMatching("3toolsserv", "colonels", false);
    }

    @Test
    public void test_3toolsserv_servers() {
        testMatching("3toolsserv", "servers", true);
    }

    @Test
    public void test_3toolsserv_sisemore() {
        testMatching("3toolsserv", "sisemore", false);
    }

    @Test
    public void test_3toolsserv_operator() {
        testMatching("3toolsserv", "operator", false);
    }

    @Test
    public void test_toolssevrirtl_tools() {
        testMatching("toolssevrirtl", "tools", true);
    }

    @Test
    public void test_toolssevrirtl_lovers() {
        testMatching("toolssevrirtl", "lovers", false);
    }

    @Test
    public void test_toolssevrirtl_tudor() {
        testMatching("toolssevrirtl", "tudor", false);
    }

    @Test
    public void test_toolssevrirtl_servers() {
        testMatching("toolssevrirtl", "servers", true);
    }

    @Test
    public void test_toolssevrirtl_virtualization() {
        testMatching("toolssevrirtl", "virtualization", true);
    }

    @Test
    public void test_3toolssevrirtl_virtualization() {
        testMatching("3toolssevrirtl", "virtualization", true);
    }

    @Test
    public void test_toolssevritrl_virtualization() {
        testMatching("toolssevritrl", "virtualization", true);
    }

    @Test
    public void test_3toolssevritrl_virtualization() {
        testMatching("3toolssevritrl", "virtualization", true);
    }

    @Test
    public void test_kroedc_docker() {
        testMatching("kroedc", "docker", false);
    }

    @Test
    public void test_roedck_docker() {
        testMatching("roedck", "docker", true);
    }

    @Test
    public void test_docekr_docker() {
        testMatching("docekr", "docker", true);
    }

    @Test
    public void test_roeck_docker() {
        testMatching("roeck", "docker", false);
    }

    @Test
    public void test_roeckd_docker() {
        testMatching("roeckd", "docker", false);
    }

    @Test
    public void test_dckre_docker() {
        testMatching("dckre", "docker", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_lord() {
        testMatching("lorofrngbyjrrtolk", "lord", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_of() {
        testMatching("lorofrngbyjrrtolk", "of", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_rings() {
        testMatching("lorofrngbyjrrtolk", "rings", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_by() {
        testMatching("lorofrngbyjrrtolk", "by", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_jrr() {
        testMatching("lorofrngbyjrrtolk", "jrr", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien() {
        testMatching("lorofrngbyjrrtolk", "tolkien", true);
    }

    @Test
    public void test_byjrrtolk_tolkien() {
        testMatching("byjrrtolk", "tolkien", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_orthodoxy() {
        testMatching("lorofrngbyjrrtolk", "orthodoxy", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_robertson() {
        testMatching("lorofrngbyjrrtolk", "robertson", false);
    }


    @Test
    public void test_lorofrngbyjrrtolk_loving() {
        testMatching("lorofrngbyjrrtolk", "loving", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_frontal() {
        testMatching("lorofrngbyjrrtolk", "frontal", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_tower() {
        testMatching("lorofrngbyjrrtolk", "tower", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_torrey() {
        testMatching("lorofrngbyjrrtolk", "torrey", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_long() {
        testMatching("lorofrngbyjrrtolk", "long", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_lotions() {
        testMatching("lorofrngbyjrrtolk", "lotions", false);
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkiens() {
        testMatching("lorofrngbyjrrtolk", "tolkiens", true);
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien1s() {
        testMatching("lorofrngbyjrrtolk", "tolkien1s", true);
    }

    @Test
    public void test_lorofrngbyjrrtlok_tolkiens() {
        testMatching("lorofrngbyjrrtlok", "tolkiens", true);
    }

    @Disabled
    @Test
    public void test_lorofrngbyjrrtlok_tolkien1s() {
        testMatching("lorofrngbyjrrtlok", "tolkien1s", true);
    }

    @Test
    public void test_toolssevrirtl_3tools() {
        testMatching("toolssevrirtl", "3tools", true);
    }

    @Disabled("how to distinguish mismatch from abbreviation?")
    @Test
    public void test_wh_warhammer() {
        testMatching("wh", "warhammer", true);
    }

    @Test
    public void test_byjrrtlok_tolkien() {
        testMatching("byjrrtlok", "tolkien", true);
    }

    @Test
    public void test_guide_guide() {
        testMatching("guide", "guide", true);
    }

    @Test
    public void test_whltwhitmn_whalt() {
        testMatching("whltwhitmn", "whalt", true);
    }

    @Test
    public void test_whaltwhitmn_whitman() {
        testMatching("whaltwhitmn", "whitman", true);
    }

    @Test
    public void test_whltwhtmn_whitman() {
        testMatching("whltwhtmn", "whitman", true);
    }

    @Test
    public void test_whltwhtmn_whitmans() {
        testMatching("whltwhtmn", "whitmans", true);
    }

    @Test
    public void test_whltwhtmn_whitman1s() {
        testMatching("whltwhtmn", "whitman's", true);
    }

    @Test
    public void test_whltwhtmn_twelfth() {
        testMatching("whltwhtmn", "twelfth", false);
    }

    @Test
    public void test_88abc123abcdf88_abcdxf_synthetic() {
        testMatching("88abc123abcdf88", "abcdxf", true);
    }

    @Test
    public void test_88abc888abcde888abcdef88_abcdexf_synthetic() {
        testMatching("88abc888abcde888abcdef88", "abcdexf", true);
    }

    @Test
    public void test_jeschrstpassn_jesus() {
        testMatching("jeschrstpassn", "jesus", true);
    }

    @Test
    public void test_jeschrstpassn_assessment() {
        testMatching("jeschrstpassn", "assessment", true);
    }

    @Test
    public void test_virtl_immortality() {
        testMatching("virtl", "immortality", false);
    }

    @Test
    public void test_virtl_victory() {
        testMatching("virtl", "victory", false);
    }

    @Disabled("cannot imaging criteria")
    @Test
    public void test_tolos_llosa() {
        testMatching("tolos", "llosa", false);
    }

    @Disabled
    @Test
    public void test_tolos_tortoise() {
        testMatching("tolos", "tortoise", false);
    }

    @Test
    public void test_tolos_textbooks() {
        testMatching("tolos", "textbooks", false);
    }

    @Test
    public void test_tolos_tropicals() {
        testMatching("tolos", "tropicals", false);
    }

    @Test
    public void test_tolos_totalitarianism() {
        testMatching("tolos", "totalitarianism", false);
    }

    @Test
    public void test_tolos_tocquevilles() {
        testMatching("tolos", "tocquevilles", false);
    }

    @Disabled
    @Test
    public void test_tolos_loves() {
        testMatching("tolos", "loves", false);
    }

    @Test
    public void test_tols_trilogy() {
        testMatching("tols", "trilogy", false);
    }

    @Test
    public void test_tolos_trilogy() {
        testMatching("tolos", "trilogy", false);
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
