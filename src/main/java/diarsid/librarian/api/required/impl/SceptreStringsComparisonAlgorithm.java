package diarsid.librarian.api.required.impl;

import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.sceptre.api.Analyze;
import diarsid.sceptre.api.WeightEstimate;
import diarsid.support.model.versioning.Version;

import static diarsid.sceptre.api.WeightEstimate.BAD;

public class SceptreStringsComparisonAlgorithm implements StringsComparisonAlgorithm {

    private final Analyze analyze;

    public SceptreStringsComparisonAlgorithm(Analyze analyze) {
        this.analyze = analyze;
    }

    @Override
    public String name() {
        return "diarsid.sceptre";
    }

    @Override
    public Version version() {
        return this.analyze.version();
    }

    @Override
    public int compare(float weight1, float weight2) {
        return Float.compare(weight1, weight2);
    }

    @Override
    public boolean isBad(float weight) {
        return WeightEstimate.of(weight).is(BAD);
    }

    @Override
    public float analyze(String pattern, String entry) {
        return this.analyze.process(pattern, entry);
    }

}
