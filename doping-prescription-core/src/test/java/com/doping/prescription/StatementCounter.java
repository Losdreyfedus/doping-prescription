package com.doping.prescription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Counts the SQL statements the test thread prepares, JPA and JDBC alike, to prove that an endpoint runs a fixed number
 * of queries (docs/kod-standartlari.md §2, no N+1). Register it with {@code @Import(StatementCounter.class)}.
 */
public class StatementCounter implements BeanPostProcessor {

    private static final Set<String> STATEMENT_METHODS = Set.of("prepareStatement", "prepareCall", "createStatement");

    private final AtomicInteger statements = new AtomicInteger();
    private volatile Thread counted;

    /** Starts counting from zero, on the calling thread only (background listeners do not count). */
    public void start() {
        statements.set(0);
        counted = Thread.currentThread();
    }

    public int count() {
        return statements.get();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean instanceof DataSource dataSource && !(bean instanceof CountingDataSource)
                ? new CountingDataSource(dataSource)
                : bean;
    }

    private final class CountingDataSource extends DelegatingDataSource {

        CountingDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws java.sql.SQLException {
            return counting(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws java.sql.SQLException {
            return counting(super.getConnection(username, password));
        }

        private Connection counting(Connection connection) {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                        if (STATEMENT_METHODS.contains(method.getName()) && Thread.currentThread() == counted) {
                            statements.incrementAndGet();
                        }
                        try {
                            return method.invoke(connection, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }
}
