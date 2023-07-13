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
        fixture.putVariableType("var1", EMPTY, 0);
        assertEquals(EMPTY, fixture.getVariableType("var1"));
    }

    @Test
    void getBuriedVariableTest() {
        fixture.putVariableType("var1", EMPTY, 0);
        fixture.push();
        fixture.putVariableType("v2", EMPTY, 0);
        assertEquals(EMPTY, fixture.getVariableType("var1"));
    }

    @Test
    void getReusedVariableTest() {
        fixture.putVariableType("var1", EMPTY, 0);
        assertEquals(EMPTY, fixture.getVariableType("var1"));
        fixture.push();
        fixture.putVariableType("var1", INT, 0);
        assertEquals(INT, fixture.getVariableType("var1"));
        fixture.pop();
        assertEquals(EMPTY, fixture.getVariableType("var1"));
    }

    @Test
    void containsVariable() {
        fixture.putVariableType("var1", EMPTY, 0);
        fixture.push();
        fixture.putVariableType("v2", EMPTY, 0);
        assertTrue(fixture.containsVariable("v2"));
    }

    @Test
    void getFunctionTest() {
        fixture.putFunctionTypes("f1", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), FLOAT));
        assertEquals(FLOAT, fixture.getFunctionTypes("f1").returnType());
    }

    @Test
    void getPoppedFunctionTest() {
        fixture.putFunctionTypes("f1", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), FLOAT));
        fixture.push();
        fixture.putFunctionTypes("f2", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), INT));
        fixture.push();
        fixture.putFunctionTypes("f3", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), STR));
        assertEquals(STR, fixture.getFunctionTypes("f3").returnType());
        fixture.pop();
        assertEquals(FunctionTypeInfo.EMPTY, fixture.getFunctionTypes("f3"));
        assertEquals(FLOAT, fixture.getFunctionTypes("f1").returnType());
    }
}