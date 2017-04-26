package org.ops4j.api;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

/**
 * Created by nmw on 26-04-2017.
 */
public class Interceptor {
    @RuntimeType
    public static Object intercept(@Origin String method, @AllArguments Object[] args) {
        System.out.println("I have intercepted a call");
        return "Hello";
    }
}