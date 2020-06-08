package io.github.wazoakarapace;

import static com.lowagie.text.Element.ALIGN_CENTER;
import static com.lowagie.text.Rectangle.LEFT;
import static com.lowagie.text.Rectangle.NO_BORDER;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReportExtension implements TestWatcher, BeforeTestExecutionCallback, AfterTestExecutionCallback,
        AfterAllCallback, BeforeAllCallback {

    private static final String START_TIME = "start time";
    private String PROJECT = "";
    private String VERSION = "";

    private static Map<String, Map<String, List<TestResult>>> testResults = new HashMap<>();

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            Method method = testMethod.get();
            TestResult result = logReport(testMethod.get());
            result.setState("Disabled");
            result.setDuration(0);
            addResult(result);
            log.info("Test Disabled");
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            Method method = testMethod.get();
            TestResult result = logReport(testMethod.get());
            log.info("Test OK !");
            long duration = (long) getStore(context).get(
                    method.getClass().getPackage() + "." + method.getClass().getSimpleName() + "#" + method.getName());
            log.info("Duration : " + duration + "ms");
            result.setState("OK");
            result.setDuration(duration);
            addResult(result);
        }
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            TestResult result = logReport(testMethod.get());
            log.info("Test Aborted");
            result.setState("Aborted");
            result.setDuration(0);
            addResult(result);
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            TestResult result = logReport(testMethod.get());
            log.info("Test failed");
            result.setState("Failed");
            result.setDuration(0);
            addResult(result);
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        getStore(context).put(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        Method testMethod = context.getRequiredTestMethod();
        long startTime = getStore(context).remove(START_TIME, long.class);
        long duration = System.currentTimeMillis() - startTime;
        getStore(context).put(testMethod.getClass().getPackage() + "." + testMethod.getClass().getSimpleName() + "#"
                + testMethod.getName(), duration);
    }

    private TestResult logReport(Method testMethod) {
        Class<?> declaringClass = testMethod.getDeclaringClass();
        TestResult result = new TestResult();
        result.setTestPackage(declaringClass.getPackage().getName());
        result.setClazz(declaringClass.getSimpleName());
        result.setMethod(testMethod.getName());
        log.info(testMethod.getName());
        log.info(testMethod.getAnnotations());
        Title titleAnnotation = testMethod.getAnnotation(Title.class);
        if (titleAnnotation != null) {
            log.info("Title : " + titleAnnotation.value());
            result.setTitle(titleAnnotation.value());
        }
        Description descriptionAnnotation = testMethod.getAnnotation(Description.class);
        if (descriptionAnnotation != null) {
            log.info("Description : " + descriptionAnnotation.value());
            result.setDescription(descriptionAnnotation.value());
        }
        RegleGestion rgAnnotation = testMethod.getAnnotation(RegleGestion.class);
        if (rgAnnotation != null) {
            log.info("RGs : " + Arrays.toString(rgAnnotation.value()));
            result.setRGs(rgAnnotation.value());
        } else {
            result.setRGs(new String[] { "N/A" });
        }
        return result;
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(
                "target\\FTU_" + PROJECT.toUpperCase() + "-" + VERSION + "_" + LocalDate.now() + ".pdf"));
        document.open();
        writeFirstPage(document);
        writeRgContent(document);

        Font packageFont = FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD, new Color(8, 73, 117));
        Font classFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.NORMAL, new Color(17, 121, 191));
        Font durationFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(182, 187, 219));
        Font cellTestFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(182, 187, 219));
        Font okStateFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, new Color(5, 163, 0));
        Font koStateFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, new Color(163, 16, 0));
        Font rgTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);

        for (Map.Entry<String, Map<String, List<TestResult>>> packageEntrySet : testResults.entrySet()) {
            document.add(new Paragraph(packageEntrySet.getKey(), packageFont));
            Map<String, List<TestResult>> classMap = packageEntrySet.getValue();
            for (Map.Entry<String, List<TestResult>> classEntrySet : classMap.entrySet()) {
                document.add(new Paragraph(classEntrySet.getKey(), classFont));
                document.add(new Paragraph("\n"));
                for (TestResult testResult : classEntrySet.getValue()) {
                    PdfPTable table = new PdfPTable(1);
                    PdfPCell headerCell = new PdfPCell();
                    PdfPTable titleTable = new PdfPTable(2);
                    PdfPCell cell = new PdfPCell();
                    cell.setBorder(NO_BORDER);
                    cell.addElement(new Paragraph("Test : " + testResult.getTitle(), cellTestFont));
                    titleTable.addCell(cell);
                    titleTable.setWidths(new float[] { 0.85f, 0.15f });
                    titleTable.setWidthPercentage(100);
                    PdfPTable subTitleTable = new PdfPTable(2);
                    subTitleTable.setWidths(new float[] { 0.85f, 0.15f });
                    subTitleTable.setWidthPercentage(100);
                    Paragraph subTitle = new Paragraph("[" + testResult.getMethod() + "]", subTitleFont);
                    PdfPCell subTitleCell = new PdfPCell();
                    subTitleCell.setBorder(NO_BORDER);
                    subTitleCell.addElement(subTitle);
                    subTitleTable.addCell(subTitleCell);
                    if (testResult.getState().equals("OK")) {
                        Paragraph duration = new Paragraph(testResult.getDuration() + "ms", durationFont);
                        duration.setAlignment(ALIGN_CENTER);

                        PdfPCell durationCell = new PdfPCell();
                        durationCell.addElement(duration);
                        durationCell.setBorder(NO_BORDER);
                        subTitleTable.addCell(durationCell);
                        Paragraph status = new Paragraph(testResult.getState(), okStateFont);
                        PdfPCell okStateCell = new PdfPCell();
                        okStateCell.addElement(status);
                        okStateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        okStateCell.setBorder(NO_BORDER);
                        titleTable.addCell(okStateCell);
                    } else {
                        subTitleTable.addCell(new PdfPCell());
                        Paragraph status = new Paragraph(testResult.getState(), koStateFont);
                        PdfPCell koStateCell = new PdfPCell();
                        koStateCell.addElement(status);
                        koStateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        koStateCell.setBorder(NO_BORDER);
                        titleTable.addCell(koStateCell);
                    }
                    headerCell.addElement(titleTable);
                    headerCell.addElement(subTitleTable);
                    table.addCell(headerCell);
                    Paragraph p = new Paragraph();
                    p.add(String.valueOf(testResult.getDescription()));
                    p.add("\n\n");
                    p.add(new Phrase("RG testées : \n", rgTitleFont));

                    for (String rg : testResult.getRGs()) {
                        p.add(" • " + rg + "\n");
                    }
                    table.addCell(p);
                    table.setWidthPercentage(100);
                    document.add(table);
                    document.add(new Paragraph("\n"));
                }

            }
        }

        document.close();
    }

    private void writeFirstPage(Document document) {
        Font coverTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 24, Font.BOLD);
        Paragraph paragraph = new Paragraph("Rapport de tests unitaires\n" + PROJECT.toUpperCase() + " " + VERSION
                + "\nExécuté le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), coverTitleFont);
        paragraph.setAlignment(ALIGN_CENTER);
        document.add(paragraph);
        document.newPage();
    }

    private void writeRgContent(Document document) {
        Set<String> Rgs = new HashSet<>();
        for (Map<String, List<TestResult>> value : testResults.values()) {
            for (List<TestResult> results : value.values()) {
                Rgs.addAll(
                        results.stream().map(TestResult::getRGs).flatMap(Arrays::stream).collect(Collectors.toSet()));
            }
        }
        Paragraph rgList = new Paragraph();
        rgList.setAlignment(LEFT);
        List<String> RgsOrdered = new ArrayList<>(Rgs);
        Collections.sort(RgsOrdered);
        for (String rg : RgsOrdered) {
            if (!rg.equals("N/A"))
                rgList.add("- " + rg + "\n");
        }
        Font rgTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 15, Font.BOLD);
        document.add(new Paragraph("Règles de gestions couvertes dans ce cahier : ", rgTitleFont));
        document.add(rgList);
        document.newPage();
    }

    private void addResult(TestResult result) {
        Map<String, List<TestResult>> classMap = testResults.getOrDefault(result.getTestPackage(), new HashMap<>());
        List<TestResult> testList = classMap.getOrDefault(result.getClazz(), new ArrayList<>());
        testList.add(result);
        classMap.put(result.getClazz(), testList);
        testResults.put(result.getTestPackage(), classMap);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Properties prop = new Properties();
        String propFileName = "report.properties";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
        if (inputStream != null) {
            prop.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
        }

        PROJECT = prop.getProperty("projectname");
        VERSION = prop.getProperty("version");
    }
}
