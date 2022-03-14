package diarsid.librarian.api.required.impl;

import java.util.Optional;

import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.sceptre.api.WeightAnalyze;

public class SceptreStringsComparisonAlgorithm implements StringsComparisonAlgorithm {

    private final WeightAnalyze weightAnalyze;
    private final Version version;

    public SceptreStringsComparisonAlgorithm(WeightAnalyze weightAnalyze) {
        this.weightAnalyze = weightAnalyze;
        this.version = new Version() {
            @Override
            public int number() {
                return 1;
            }

            @Override
            public Optional<String> name() {
                return Optional.empty();
            }

            @Override
            public int compareTo(Version other) {
                return Integer.compare(this.number(), other.number());
            }
        };
    }

    @Override
    public String name() {
        return "diarsid.sceptre";
    }

    @Override
    public Version version() {
        return this.version;
    }

    @Override
    public int compare(float weight1, float weight2) {
        return Float.compare(weight1, weight2);
    }

    @Override
    public boolean isBad(float weight) {
        return weightAnalyze.isBad(weight);
    }

    @Override
    public float analyze(String pattern, String entry) {
        return weightAnalyze.weightString(pattern, entry);
    }

}
