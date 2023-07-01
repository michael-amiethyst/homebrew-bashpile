package com.bashpile.engine.strongtypes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bashpile.engine.strongtypes.Type.*;
import static org.junit.jupiter.api.Assertions.*;

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
        fixture.putVariable("var1", EMPTY);
        assertEquals(EMPTY, fixture.getVariable("var1"));
    }

    @Test
    void getBuriedVariableTest() {
        fixture.putVariable("var1", EMPTY);
        fixture.push();
        fixture.putVariable("v2", EMPTY);
        assertEquals(EMPTY, fixture.getVariable("var1"));
    }

    @Test
    void getReusedVariableTest() {
        fixture.putVariable("var1", EMPTY);
        assertEquals(EMPTY, fixture.getVariable("var1"));
        fixture.push();
        fixture.putVariable("var1", INT);
        assertEquals(INT, fixture.getVariable("var1"));
        fixture.pop();
        assertEquals(EMPTY, fixture.getVariable("var1"));
    }

    @Test
    void containsVariable() {
        fixture.putVariable("var1", EMPTY);
        fixture.push();
        fixture.putVariable("v2", EMPTY);
        assertTrue(fixture.containsVariable("v2"));
    }

    @Test
    void getFunctionTest() {
        fixture.putFunction("f1", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), FLOAT));
        assertEquals(FLOAT, fixture.getFunction("f1").returnType());
    }

    @Test
    void getPoppedFunctionTest() {
        fixture.putFunction("f1", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), FLOAT));
        fixture.push();
        fixture.putFunction("f2", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), INT));
        fixture.push();
        fixture.putFunction("f3", new FunctionTypeInfo(List.of(FLOAT, FLOAT, FLOAT), STR));
        assertEquals(STR, fixture.getFunction("f3").returnType());
        fixture.pop();
        assertEquals(FunctionTypeInfo.EMPTY, fixture.getFunction("f3"));
        assertEquals(FLOAT, fixture.getFunction("f1").returnType());
    }
}