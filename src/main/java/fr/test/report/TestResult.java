package fr.test.report;

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestResult {

    private String testPackage;
    private String clazz;
    private String method;
    private String title;
    private String description;
    private String[] RGs;
    private String state;
    private long duration;

    @Override
    public String toString() {
        return "TestResult{" + "testPackage='" + testPackage + '\'' + ", clazz='" + clazz + '\'' + ", method='" + method
                + '\'' + ", title='" + title + '\'' + ", description='" + description + '\'' + ", RGs="
                + Arrays.toString(RGs) + ", state='" + state + '\'' + ", duration=" + duration + '}';
    }
}
