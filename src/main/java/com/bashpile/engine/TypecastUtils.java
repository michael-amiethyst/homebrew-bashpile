package com.bashpile.engine;

import com.bashpile.engine.strongtypes.Type;
import com.bashpile.exceptions.TypeError;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bashpile.engine.strongtypes.TranslationMetadata.CALCULATION;
import static com.bashpile.engine.strongtypes.Type.*;
import static com.bashpile.engine.strongtypes.Type.INT_TYPE;

/** Computations not checked for parsability or anything that starts with $ (Bash variable) */
public class TypecastUtils {
    // TODO consistent C style number casts, we can't check for correctness of non-literals (but we allow them)

    public static BashTranslationEngine engine = null;

    /* package */ static @Nonnull Translation typecastFromBool(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            @Nonnull final TypeError typecastError) {
        switch (castTo.mainTypeName()) {
            case BOOL -> {}
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no cast to int, float or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromNumber(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError
    ) {
        switch (castTo.mainTypeName()) {
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> expression = expression.type(FLOAT_TYPE);
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromInt(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        if (!expression.metadata().contains(CALCULATION)) {
            // parse expression to a BigInteger
            try {
                new BigInteger(expression.body());
            } catch (final NumberFormatException e) {
                // TODO allow for non-literals like for typecastFromFloat
                String message = "Couldn't parse '%s' to an INT.  " +
                        "Typecasts only work on literals, was this an ID or function call?";
                throw new TypeError(message.formatted(expression.body()), lineNumber);
            }
        }

        // Cast
        switch (castTo.mainTypeName()) {
            case INT -> {}
            case FLOAT -> expression = expression.type(FLOAT_TYPE);
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no typecast to bool or list
            default -> throw typecastError;
        }
        return expression;
    }

    /* package */ static @Nonnull Translation typecastFromFloat(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {

        // cast
        switch (castTo.mainTypeName()) {
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> {}
            case STR -> expression = expression.quoteBody().type(STR_TYPE);
            // no typecast to bool or list
            default -> throw typecastError;
        }
        return expression;
    }

    // helper
    private static @Nonnull Translation typecastToInt(@Nonnull Translation expression, final int lineNumber) {
        // parse expression as a BigDecimal to check for literal float
        expression = expression.unquoteBody();
        BigDecimal expressionValue = null;
        try {
            expressionValue = new BigDecimal(expression.body());
        } catch (final NumberFormatException e) {
            // expressionValue is still null if it is not a literal (e.g. variable or function call)
        }
        if (expressionValue != null) {
            return expression.body(expressionValue.toBigInteger().toString()).type(INT_TYPE).unquoteBody();
        } else {
            // if a variable reference then typecast to int (round down) with printf
            String varName = StringUtils.stripStart(expression.body(), "${");
            varName = StringUtils.stripEnd(varName, "}");
            if (!varName.matches("\\d")) {
                String setupStatementText = """
                        %s="$(printf '%%d' "%s" 2>/dev/null || true)"
                        """.formatted(varName, expression);
                engine.addExpressionSetup(new Translation(setupStatementText));
                return expression.addPreamble(setupStatementText)
                        .type(INT_TYPE);
            } else {
                // trying to reassign $1, $2, etc
                // too complex to set an individual varable with the command 'set', throw
                final String message = "Could not cast $1, $2, etc.  Assign to a local variable and typecast that.";
                throw new TypeError(message, lineNumber);
            }
        }
    }

    /* package */ static @Nonnull Translation typecastFromStr(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo.mainTypeName()) {
            case BOOL -> {
                expression = expression.unquoteBody();
                if (Type.isNumberString(expression.body())) {
                    expression = typecastFromFloat(expression, castTo, lineNumber, typecastError);
                } else if (expression.body().equalsIgnoreCase("true")
                        || expression.body().equalsIgnoreCase("false")) {
                    expression = expression.body(expression.body().toLowerCase()).type(castTo);
                } else {
                    throw new TypeError("""
                            Could not cast STR to BOOL.
                            Only 'true' and 'false' allowed (capitalization ignored) or numbers for a C style cast.
                            Text was %s.""".formatted(expression.body()), lineNumber);
                }
            }
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> {
                expression = expression.unquoteBody().type(castTo);
                // verify the body parses as a valid number for non-variables
                if (!expression.body().startsWith("$")) {
                    try {
                        Type.parseNumberString(expression.body());
                    } catch (NumberFormatException e) {
                        throw new TypeError("""
                                    Could not cast STR to FLOAT.  Is not a FLOAT.  Text was %s."""
                                .formatted(expression.body()), lineNumber);
                    }
                }
            }
            case STR -> {}
            // TODO allow typecasting to LIST for all conversions
            case LIST -> expression = expression.type(castTo); // trust the user, may be a bad idea
            default -> throw typecastError;
        }
        return expression;
    }

    /** Casts to a different kind of list (i.e. a different subtype), but no other conversions */
    /* package */ static @Nonnull Translation typecastFromList(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            @Nonnull final TypeError typecastError) {
        if (castTo.isList()) {
            // Allow a typecast to any subtype
            return expression.type(castTo);
        } else {
            // cannot cast to bool, int, float or str
            throw typecastError;
        }
    }

    /* package */ static @Nonnull Translation typecastFromUnknown(
            @Nonnull Translation expression,
            @Nonnull final Type castTo,
            final int lineNumber,
            @Nonnull final TypeError typecastError) {
        switch (castTo.mainTypeName()) {
            case BOOL, STR, LIST -> expression = expression.type(castTo);
            case INT -> expression = typecastToInt(expression, lineNumber);
            case FLOAT -> expression = expression.unquoteBody().type(castTo);
            default -> throw typecastError;
        }
        return expression;
    }
}
