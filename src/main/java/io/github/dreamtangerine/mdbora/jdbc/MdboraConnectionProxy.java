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
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

final class MdboraConnectionProxy implements InvocationHandler {

  private final MdboraConnectionContext context;
  private final Connection delegate;
  private boolean closed;

  private MdboraConnectionProxy(MdboraConnectionContext context, Connection delegate) {
    this.context = context;
    this.delegate = delegate;
  }

  static Connection wrap(MdboraConnectionContext context, Connection delegate) {
    return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, new MdboraConnectionProxy(context, delegate));
  }

  
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();

    if (name.equals("close")) {
      close();
      return null;
    }
    
    if (name.equals("isClosed")) {
      return closed || delegate.isClosed();
    }
    
    if (name.equals("isReadOnly")) {
      return true;
    }
    
    if (name.equals("setReadOnly")) {
      if (args != null && Boolean.FALSE.equals(args[0])) {
        throw new SQLFeatureNotSupportedException("Mdbora is read only");
      }
      
      return null;
    }
    
    if (name.equals("unwrap") && ((Class<?>) args[0]).isInstance(proxy)) {
      return proxy;
    }
    
    if (name.equals("isWrapperFor") && ((Class<?>) args[0]).isInstance(proxy)) {
      return true;
    }

    if (closed) {
      throw new SQLException("Connection is closed");
    }

    try {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private void close() throws SQLException {
    if (!closed) {
      closed = true;
      
      try {
        context.close();
      } catch (Exception e) {
        throw new SQLException("Error closing Mdbora connection", e);
      }
    }
  }
}
