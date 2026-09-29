/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.support;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

public final class QueryCounter {

    private static final AtomicLong RUNS = new AtomicLong();

    private QueryCounter() {
    }

    public static long count() {
        return RUNS.get();
    }

    public static Driver wrap(Driver driver) {
        return (Driver) Proxy.newProxyInstance(Driver.class.getClassLoader(), new Class[]{Driver.class},
                (proxy, method, args) -> {
                    Object result = invoke(driver, method, args);
                    return result instanceof Session ? wrap((Session) result) : result;
                });
    }

    private static Session wrap(Session session) {
        return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class[]{Session.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("run")) {
                        RUNS.incrementAndGet();
                    }
                    return invoke(session, method, args);
                });
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
