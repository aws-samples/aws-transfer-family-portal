// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.socalcat.lambda.transferauth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class LambdaFunctionHandlerTest {

    private static Object input;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: Set up your sample input object here.
        input = null;
    }

    private Context createContext() {
        TestContext ctx = new TestContext();
        ctx.setFunctionName("Your Function Name");
        return ctx;
    }

    @Test
    public void testLambdaFunctionHandler() {
/*
        LambdaFunctionHandler handler = new LambdaFunctionHandler();
        Context ctx = createContext();
        Map<String, String> positivePassword = new HashMap<>();
        positivePassword.put("username", "admin");
        positivePassword.put("protocol", "FTPS");
	*/
         //positivePassword.put("password", "J2rGx#3mn");
        // positivePassword.put("password", "P&yee5(FV7");
        // positivePassword.put("password", "114Dc-A'oQ");
      //  handler.handleRequest(positivePassword, ctx);
        // Map< String output = handler.handleRequest(input, ctx);
        // LambdaFunctionHandler handler = new LambdaFunctionHandler();
        // handler.handleRequest(positivePassword, new Context());

        // String output = handler.handleRequest(input, ctx);

        // Assert.assertEquals("Hello from Lambda!", output);
    }
}
