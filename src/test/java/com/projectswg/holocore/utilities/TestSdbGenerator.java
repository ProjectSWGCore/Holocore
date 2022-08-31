package com.projectswg.holocore.utilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSdbGenerator {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
    private StringWriter stringWriter;
    private SdbGenerator generator;

    @BeforeEach
    public void setup() {
        stringWriter = new StringWriter();
        generator = new SdbGenerator(new BufferedWriter(stringWriter));
    }

    @Test
    public void columnNamesRetainOrder() throws IOException {
        String col1 = "col1";
        String col2 = "col2";
        String separator = "\t";

        generator.writeColumnNames(col1, col2);
        generator.close();  // Flushes changes to the StringWriter
		
        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");
        String expected = col1 + separator + col2;

        assertEquals(expected, result);
    }

    @Test
    public void canWriteString() throws IOException {
        String cell = "cell";

        generator.writeLine(cell);
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");

        assertEquals(cell, result);
    }

    @Test
    public void canWriteInt() throws IOException {
        int cell = 1234;

        generator.writeLine(cell);
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");
        String expected = String.valueOf(cell);

        assertEquals(expected, result);
    }

    @Test
    public void nullIsWrittenAsEmptyString() throws IOException {
        generator.writeLine(new Object[]{null});
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");
        String expected = "";

        assertEquals(expected, result);
    }

    @Test
    public void multipleValuesInCollection() throws IOException {
        String valSeparator = ";";
        String val1 = "1";
        String val2 = "2";

        Collection<String> collection = Arrays.asList(
                val1,
                val2
        );

        generator.writeLine(collection);
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");
        String expected = val1 + valSeparator + val2;

        assertEquals(expected, result);
    }

    @Test
    public void singleValueInCollection() throws IOException {
        String val = "1";

        Collection<String> collection = Collections.singletonList(val);

        generator.writeLine(collection);
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");

        assertEquals(val, result);
    }

    @Test
    public void collectionOfCollections() throws IOException {
        String val1 = "1";
        String val2 = "2";
        String val3 = "3";
        String val4 = "4";

        String valSeparator = ";";

        Collection<String> collection1 = Arrays.asList(val1, val2);
        Collection<String> collection2 = Arrays.asList(val3, val4);
        Collection<Collection<String>> collectionCollection = Arrays.asList(collection1, collection2);

        generator.writeLine(collectionCollection);
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");
        String expected = val1 + valSeparator + val2 + valSeparator + val3 + valSeparator + val4;

        assertEquals(expected, result);
    }

    @Test
    public void multipleValuesInMap() throws IOException {
        String k1 = "k1";
        String k2 = "k2";
        String v1 = "v1";
        String v2 = "v2";
        String pairSeparator = "=";
        String entrySeparator = ",";

        Map<String, String> map = new TreeMap<>(Map.of( // TreeMap ensures the entries are always ordered the same way
                k1, v1,
                k2, v2
        ));

        generator.writeLine(map);
        generator.close();

        String result = stringWriter.toString().replace(LINE_SEPARATOR, "");
        String expected = k1 + pairSeparator + v1 + entrySeparator + k2 + pairSeparator + v2;

        assertEquals(expected, result);
    }

}
