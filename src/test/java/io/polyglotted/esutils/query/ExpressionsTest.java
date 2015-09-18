package io.polyglotted.esutils.query;

import io.polyglotted.esutils.query.request.Expressions;
import org.elasticsearch.index.query.FilterBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.polyglotted.esutils.query.ExpressionType.*;
import static org.elasticsearch.common.xcontent.XContentHelper.convertToJson;
import static org.testng.Assert.assertEquals;

public class ExpressionsTest extends Expressions {

    @DataProvider
    public static Object[][] expressionInputs() {
        return new Object[][]{
           {Eq.buildFrom(equalsTo("hello", "world")), "{\"term\":{\"hello\":\"world\"}}"},
           {Gte.buildFrom(greaterThanEquals("hello", "world")),
              "{\"range\":{\"hello\":{\"from\":\"world\",\"to\":null,\"include_lower\":true,\"include_upper\":true}}}"},
           {Gt.buildFrom(greaterThan("hello", "world")),
              "{\"range\":{\"hello\":{\"from\":\"world\",\"to\":null,\"include_lower\":false,\"include_upper\":true}}}"},
           {Lte.buildFrom(lessThanEquals("hello", "world")),
              "{\"range\":{\"hello\":{\"from\":null,\"to\":\"world\",\"include_lower\":true,\"include_upper\":true}}}"},
           {Lt.buildFrom(lessThan("hello", "world")),
              "{\"range\":{\"hello\":{\"from\":null,\"to\":\"world\",\"include_lower\":true,\"include_upper\":false}}}"},
           {Prefix.buildFrom(prefix("hello", "world")), "{\"query\":{\"prefix\":{\"hello\":\"world\"}}}"},
           {Ne.buildFrom(notEquals("hello", "world")), "{\"not\":{\"filter\":{\"term\":{\"hello\":\"world\"}}}}"},
           {In.buildFrom(in("hello", "foo", "bar")), "{\"terms\":{\"hello\":[\"foo\",\"bar\"]}}"},
           {In.buildFrom(in("hello", 25, 32)), "{\"terms\":{\"hello\":[25,32]}}"},
           {Between.buildFrom(between("hello", "foo", "bar")),
              "{\"range\":{\"hello\":{\"from\":\"foo\",\"to\":\"bar\",\"include_lower\":true,\"include_upper\":false}}}"},
           {Text.buildFrom(textAnywhere("hello")),
              "{\"query\":{\"match\":{\"_all\":{\"query\":\"hello\",\"type\":\"phrase_prefix\"}}}}"},
           {Text.buildFrom(textAnywhere("a", "hello")),
              "{\"query\":{\"match\":{\"a\":{\"query\":\"hello\",\"type\":\"phrase_prefix\"}}}}"},
           {Regex.buildFrom(regex("hello", "wor*")), "{\"regexp\":{\"hello\":\"wor*\"}}"},
           {Missing.buildFrom(missing("hello")),
              "{\"missing\":{\"field\":\"hello\",\"null_value\":true,\"existence\":true}}"},
           {Json.buildFrom(json("{\"query\":{\"match_phrase\":{\"_all\":{\"query\":\"commodity\",\"slop\":20}}}}")),
              "{\"wrapper\":{\"filter\":\"eyJxdWVyeSI6eyJtYXRjaF9waHJhc2UiOnsiX2FsbCI6eyJxdWVyeSI6ImNvbW1vZGl0eSIsInNsb3AiOjIwfX19fQ==\"}}"},
           {And.buildFrom(and(equalsTo("hello", "world"))), "{\"and\":{\"filters\":[{\"term\":{\"hello\":\"world\"}}]}}"},
           {Or.buildFrom(or(equalsTo("hello", "world"))), "{\"or\":{\"filters\":[{\"term\":{\"hello\":\"world\"}}]}}"},
           {Not.buildFrom(not(equalsTo("hello", "world"))), "{\"not\":{\"filter\":{\"term\":{\"hello\":\"world\"}}}}"},
           {Nested.buildFrom(nested("foo.bar", equalsTo("hello", "world"))),
              "{\"nested\":{\"filter\":{\"term\":{\"hello\":\"world\"}},\"path\":\"foo.bar\"}}"},
        };
    }

    @Test(dataProvider = "expressionInputs")
    public void expressionToFilter(FilterBuilder filterBuilder, String json) throws Exception {
        assertEquals(convertToJson(filterBuilder.buildAsBytes(), false, false), json);
    }

    @Test
    public void expressionToString() {
        assertEquals(equalsTo("hello", "world").toString(), "hello Eq {_val=world}");
        assertEquals(not(equalsTo("hello", "world")).toString(), "Not {}");
    }
}