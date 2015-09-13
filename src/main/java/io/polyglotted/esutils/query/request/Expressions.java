package io.polyglotted.esutils.query.request;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.polyglotted.esutils.query.ExpressionType;

import static io.polyglotted.esutils.query.request.Expression.*;
import static java.util.Arrays.asList;

public abstract class Expressions {

    public static Expression equalsTo(String field, Object value) {
        return withValue(ExpressionType.Eq, field, value);
    }

    public static Expression greaterThanEquals(String field, Object value) {
        return withValue(ExpressionType.Gte, field, value);
    }

    public static Expression greaterThan(String field, Object value) {
        return withValue(ExpressionType.Gt, field, value);
    }

    public static Expression lessThanEquals(String field, Object value) {
        return withValue(ExpressionType.Lte, field, value);
    }

    public static Expression lessThan(String field, Object value) {
        return withValue(ExpressionType.Lt, field, value);
    }

    public static Expression prefix(String field, Object value) {
        return withValue(ExpressionType.Prefix, field, value);
    }

    public static Expression notEquals(String field, Object value) {
        return withValue(ExpressionType.Ne, field, value);
    }

    public static <E extends Comparable<E>> Expression in(String field, E... values) {
        return withArray(ExpressionType.In, field, ImmutableList.copyOf(asList(values)));
    }

    public static Expression between(String field, Object from, Object to) {
        return between(field, from, to, true, false);
    }

    public static Expression between(String field, Object from, Object to, boolean fromIncl, boolean toIncl) {
        return withMap(ExpressionType.Between.name(), field,
           ImmutableMap.of("from", from, "to", to, "fromIncl", fromIncl, "toIncl", toIncl));
    }

    public static Expression textAnywhere(Object value) {
        return textAnywhere("", value);
    }

    public static Expression textAnywhere(String field, Object value) {
        return withValue(ExpressionType.Text, field, value);
    }

    public static Expression regex(String field, String expr) {
        return withValue(ExpressionType.Regex, field, expr);
    }

    public static Expression missing(String field) {
        return new Expression(ExpressionType.Missing.name(), field, ImmutableMap.of(), ImmutableList.of());
    }

    public static Expression json(String json) {
        return withValue(ExpressionType.Json, null, json);
    }


    public static Expression and(Expression... expressions) {
        return withOnlyChildren(ExpressionType.And, null, asList(expressions));
    }

    public static Expression or(Expression... expressions) {
        return withOnlyChildren(ExpressionType.Or, null, asList(expressions));
    }

    public static Expression not(Expression expression) {
        return withOnlyChildren(ExpressionType.Not, null, asList(expression));
    }

    public static Expression nested(String path, Expression expression) {
        return withOnlyChildren(ExpressionType.Nested, path, asList(expression));
    }
}
