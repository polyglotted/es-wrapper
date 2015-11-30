package io.polyglotted.eswrapper.indexing;

import io.polyglotted.eswrapper.validation.Validity;
import org.testng.annotations.Test;

import static io.polyglotted.esmodel.api.index.FieldMapping.notAnalyzedStringField;
import static io.polyglotted.eswrapper.indexing.TypeMapping.typeBuilder;
import static io.polyglotted.eswrapper.validation.Validity.valid;
import static io.polyglotted.eswrapper.validation.Validity.validityBuilder;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class DataObjectsTest {

    @Test
    public void indexActionValues() {
        asList(IndexRecord.Action.values()).contains(IndexRecord.Action.valueOf("CREATE"));
    }

    @Test
    public void indexSettingEqHash() {
        IndexSetting orig = IndexSetting.with(3, 0);
        IndexSetting copy = IndexSetting.with(3, 0);
        IndexSetting other = IndexSetting.with(1, 0);
        verifyEqualsHashCode(orig, copy, other);
    }

    @Test
    public void typeMappingEqHash() throws Exception {
        TypeMapping orig = typeBuilder().index("a").type("a").fieldMapping(notAnalyzedStringField("a")).build();
        TypeMapping copy = typeBuilder().index("a").type("a").fieldMapping(notAnalyzedStringField("a")).build();
        TypeMapping other = typeBuilder().index("b").type("b").fieldMapping(notAnalyzedStringField("a")).build();
        verifyEqualsHashCode(orig, copy, other);
    }

    @Test
    public void indexRecordEqHash() {
        IndexRecord orig = IndexRecord.createRecord("abc", "def").source("").build();
        IndexRecord copy = IndexRecord.createRecord("abc", "def").source("").build();
        IndexRecord other1 = IndexRecord.createRecord("abc", "ghi").source("").build();
        verifyEqualsHashCode(orig, copy, other1);
    }

    @Test
    public void memoEqHash() {
        Validity.Memo orig  = new Validity.Memo("a", "b");
        Validity.Memo copy  = new Validity.Memo("a", "b");
        Validity.Memo other1  = new Validity.Memo("a", "c");
        Validity.Memo other2  = new Validity.Memo("c", "b");
        Validity.Memo other3  = new Validity.Memo("c", "d");
        verifyEqualsHashCode(orig, copy, other1, other2, other3);
    }

    @Test
    public void validityEqHash() {
        Validity orig = validityBuilder().memo("a", "b").build();
        Validity copy = validityBuilder().memo("a", "b").build();
        Validity other = valid();
        verifyEqualsHashCode(orig, copy, other);
    }

    @SafeVarargs
    public static <T> void verifyEqualsHashCode(T obj, T copy, T... others) {
        assertNotNull(obj.toString());
        assertEquals(obj, obj);
        assertEquals(obj, copy);
        assertEquals(obj.hashCode(), copy.hashCode());
        assertFalse(obj.equals(null));
        assertFalse(obj.equals(""));
        for (int i = 0; i < others.length; ++i) {
            assertNotEquals(obj, others[i]);
        }
    }
}