package com.gateway.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(Connection connection) throws SQLException;
    }

    public static <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException {
        try (Connection connection = DatabaseConnectionPool.getConnection()) {
            connection.setAutoCommit(false);

            try {
                T result = callback.execute(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                logger.error("Transaction rolled back due to error", e);
                throw e;
            }
        }
    }

    public static void executeInTransactionVoid(TransactionCallbackVoid callback) throws SQLException {
        try (Connection connection = DatabaseConnectionPool.getConnection()) {
            connection.setAutoCommit(false);

            try {
                callback.execute(connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                logger.error("Transaction rolled back due to error", e);
                throw e;
            }
        }
    }

    @FunctionalInterface
    public interface TransactionCallbackVoid {
        void execute(Connection connection) throws SQLException;
    }
}