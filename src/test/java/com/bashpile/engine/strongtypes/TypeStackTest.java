package com.bashpile.engine.strongtypes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bashpile.engine.strongtypes.Type.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeStackTest {

    private TypeStack fixture;

    @BeforeEach
    public void init() {
        fixture = new TypeStack();
    }

    @AfterEach
    public void teardown() {
        fixture = null;
    }

    @Test
    void getVariableTest() {
        fixture.putVariableType("var1", EMPTY_TYPE, 0);
        assertEquals(EMPTY_TYPE, fixture.getVariableType("var1"));
    }

    @Test
    void getBuriedVariableTest() {
        fixture.putVariableType("var1", EMPTY_TYPE, 0);
        fixture.push();
        fixture.putVariableType("v2", EMPTY_TYPE, 0);
        assertEquals(EMPTY_TYPE, fixture.getVariableType("var1"));
    }

    @Test
    void getReusedVariableTest() {
        fixture.putVariableType("var1", EMPTY_TYPE, 0);
        assertEquals(EMPTY_TYPE, fixture.getVariableType("var1"));
        fixture.push();
        fixture.putVariableType("var1", INT_TYPE, 0);
        assertEquals(INT_TYPE, fixture.getVariableType("var1"));
        fixture.pop();
        assertEquals(EMPTY_TYPE, fixture.getVariableType("var1"));
    }

    @Test
    void containsVariable() {
        fixture.putVariableType("var1", EMPTY_TYPE, 0);
        fixture.push();
        fixture.putVariableType("v2", EMPTY_TYPE, 0);
        assertTrue(fixture.containsVariable("v2"));
    }

    @Test
    void getFunctionTest() {
        List<ParameterInfo> parameterInfos = List.of(
                new ParameterInfo("p1", FLOAT_TYPE, ""),
                new ParameterInfo("p2", FLOAT_TYPE, ""),
                new ParameterInfo("p3", FLOAT_TYPE, ""));
        fixture.putFunctionTypes("f1", new FunctionTypeInfo(parameterInfos, FLOAT_TYPE));
        assertEquals(FLOAT_TYPE, fixture.getFunctionTypes("f1").returnType());
    }

    @Test
    void getPoppedFunctionTest() {
        List<ParameterInfo> parameterInfos = List.of(
                new ParameterInfo("p1", FLOAT_TYPE, ""),
                new ParameterInfo("p2", FLOAT_TYPE, ""),
                new ParameterInfo("p3", FLOAT_TYPE, ""));
        fixture.putFunctionTypes("f1", new FunctionTypeInfo(parameterInfos, FLOAT_TYPE));
        fixture.push();
        fixture.putFunctionTypes("f2", new FunctionTypeInfo(parameterInfos, INT_TYPE));
        fixture.push();
        fixture.putFunctionTypes("f3", new FunctionTypeInfo(parameterInfos, STR_TYPE));
        assertEquals(STR_TYPE, fixture.getFunctionTypes("f3").returnType());
        fixture.pop();
        assertEquals(FunctionTypeInfo.EMPTY, fixture.getFunctionTypes("f3"));
        assertEquals(FLOAT_TYPE, fixture.getFunctionTypes("f1").returnType());
    }
}