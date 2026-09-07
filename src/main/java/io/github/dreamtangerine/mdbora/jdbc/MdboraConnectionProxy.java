/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * Wraps the internal SQL connection and exposes the public Mdbora connection behavior.
 */
final class MdboraConnectionProxy implements InvocationHandler {

  private final MdboraConnectionContext context;
  private final Connection delegate;
  private boolean closed;

  private MdboraConnectionProxy(MdboraConnectionContext context, Connection delegate) {
    this.context = context;
    this.delegate = delegate;
  }

  /**
   * Creates a public JDBC connection backed by an internal SQL connection.
   *
   * @param context Mdbora connection context
   * @param delegate internal SQL connection
   * @return wrapped JDBC connection
   */
  static Connection wrap(MdboraConnectionContext context, Connection delegate) {
    Connection result = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new MdboraConnectionProxy(context, delegate));

    return result;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
    String methodName = method.getName();
    Object result;

    if ("close".equals(methodName)) {
      close();
      result = null;
    } else if ("isClosed".equals(methodName)) {
      result = isClosed();
    } else if ("isReadOnly".equals(methodName)) {
      result = true;
    } else if ("setReadOnly".equals(methodName)) {
      setReadOnly(arguments);
      result = null;
    } else if ("getMetaData".equals(methodName)) {
      result = getMetaData((Connection) proxy);
    } else if ("setAutoCommit".equals(methodName)) {
      validateOpen();
      result = invokeDelegate(method, arguments);
    } else if ("commit".equals(methodName) || "rollback".equals(methodName) || "setSavepoint".equals(methodName) || "releaseSavepoint".equals(methodName)) {
      throw new SQLFeatureNotSupportedException("Mdbora does not support transactions");
    } else if ("unwrap".equals(methodName)) {
      result = unwrap(proxy, arguments);
    } else if ("isWrapperFor".equals(methodName)) {
      result = isWrapperFor(proxy, arguments);

    } else if ("equals".equals(methodName)) {
      result = proxy == arguments[0];
    } else if ("hashCode".equals(methodName)) {
      result = System.identityHashCode(proxy);
    } else if ("toString".equals(methodName)) {
      result = "MdboraConnection";
    } else {
      validateOpen();

      result = invokeDelegate(method, arguments);
    }

    return result;
  }

  private DatabaseMetaData getMetaData(Connection connection) throws SQLException {
    validateOpen();

    return MdboraDatabaseMetaDataProxy.wrap(context, connection, delegate.getMetaData());
  }

  private synchronized void close() throws SQLException {
    if (!closed) {
      closed = true;

      try {
        context.close();
      } catch (Exception exception) {
        throw new SQLException("Unable to close the Mdbora connection", exception);
      }
    }
  }

  private synchronized boolean isClosed() throws SQLException {
    return closed || context.isClosed() || delegate.isClosed();
  }

  private void setReadOnly(Object[] arguments) throws SQLFeatureNotSupportedException, SQLException {
    validateOpen();

    boolean readOnly = (Boolean) arguments[0];

    if (!readOnly) {
      throw new SQLFeatureNotSupportedException("Mdbora only supports read-only connections");
    }
  }

  private void validateOpen() throws SQLException {

    if (isClosed()) {
      throw new SQLException("The Mdbora connection is closed");
    }
  }

  private static Object unwrap(Object proxy, Object[] arguments) throws SQLException {
    Class<?> requestedType = (Class<?>) arguments[0];
    Object result = null;

    if (requestedType.isInstance(proxy)) {
      result = proxy;
    } else {
      throw new SQLException("The Mdbora connection cannot be unwrapped as " + requestedType.getName());
    }

    return result;
  }

  private static boolean isWrapperFor(Object proxy, Object[] arguments) {
    Class<?> requestedType = (Class<?>) arguments[0];

    return requestedType.isInstance(proxy);
  }

  private Object invokeDelegate(Method method, Object[] arguments) throws Throwable {
    Object result;

    try {
      result = method.invoke(delegate, arguments);
    } catch (InvocationTargetException exception) {
      throw exception.getCause();
    } catch (IllegalAccessException exception) {
      throw new SQLException("Unable to invoke JDBC connection method: " + method.getName(), exception);
    }

    return result;
  }
}
