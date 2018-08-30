/*
 * Copyright 2018 mnn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.netdesign.mybatis.extender.runtime.migrations;

import dk.netdesign.mybatis.extender.api.MigrationCommands;
import dk.netdesign.mybatis.extender.api.MigrationConfiguration;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.migration.Change;
import org.apache.ibatis.migration.ConnectionProvider;
import org.apache.ibatis.migration.JavaMigrationLoader;
import org.apache.ibatis.migration.MigrationLoader;
import org.apache.ibatis.migration.operations.BootstrapOperation;
import org.apache.ibatis.migration.operations.DownOperation;
import org.apache.ibatis.migration.operations.PendingOperation;
import org.apache.ibatis.migration.operations.StatusOperation;
import org.apache.ibatis.migration.operations.UpOperation;
import org.apache.ibatis.migration.operations.VersionOperation;
import org.apache.ibatis.migration.options.DatabaseOperationOption;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 */
public class MigrationCommandsLayer implements MigrationCommands, ConnectionProvider{

    private final SqlSessionFactory sqlFactory;
    private final MigrationLoader loader;
    private final DatabaseOperationOption options;
    private static final Logger LOGGER = LogManager.getLogger();
    private final PrintStream loggerStream;
    
    public MigrationCommandsLayer(SqlSessionFactory sqlFactory, MigrationConfiguration config) throws UnsupportedEncodingException {
        this.sqlFactory = sqlFactory;
        this.loader = new OSGiMigrationLoader(config);
        this.options = config.getDatabaseOptions();
        
        Logger migrationsLogger = LogManager.getLogger(config.getClass());
        loggerStream = new PrintStream(new LoggingOutputStream(migrationsLogger, Level.INFO), true, "UTF-16");
    }
    
    
    
    
    @Override
    public void bootstrap() {
        BootstrapOperation bootstrap = new BootstrapOperation();
        bootstrap.operate(this, loader, options, loggerStream);
    }

    @Override
    public void bootstrap(boolean force) {
        BootstrapOperation bootstrap = new BootstrapOperation(force);
        bootstrap.operate(this, loader, options, loggerStream);
    }
    
    @Override
    public void up() {
        UpOperation up = new UpOperation();
        up.operate(this, loader, options, loggerStream);
    }

    @Override
    public void down() {
        DownOperation down = new DownOperation();
        down.operate(this, loader, options, loggerStream);
    }

    @Override
    public void up(int number) {
        UpOperation up = new UpOperation(number);
        up.operate(this, loader, options, loggerStream);
    }

    @Override
    public void down(int number) {
        DownOperation down = new DownOperation(number);
        down.operate(this, loader, options, loggerStream);
    }

    @Override
    public void version(BigDecimal versionTarget) {
        VersionOperation version = new VersionOperation(versionTarget);
        version.operate(this, loader, options, loggerStream);
    }

    @Override
    public void pending() {
        PendingOperation pending = new PendingOperation();
        pending.operate(this, loader, options, loggerStream);
    }

    @Override
    public List<Change> status() {
        StatusOperation status = new StatusOperation();
        status.operate(this, loader, options, loggerStream);
        return status.getCurrentStatus();
    }

    @Override
    public Connection getConnection() throws SQLException {
         SessionClosingConnection connection = new SessionClosingConnection(sqlFactory);
         return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class}, connection);
    }

/**
 * This class provides a way for the (@link SqlSessionFactory) to have it's sessions closed after they have been used.
 * The Migrations commands only takes SQLConnections, and does not contain logic to handle sessions. The sessions are closed
 * by interceptiong the close method of the connection, and automatically closing the session as well.
 */    
    private static class SessionClosingConnection implements InvocationHandler{
        private final SqlSession session;
        private final Connection connection;

        public SessionClosingConnection(SqlSessionFactory factory) throws SQLException {
            this.session = factory.openSession();
            this.connection = session.getConnection();
            this.connection.setAutoCommit(true);
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try{
                Object result = method.invoke(connection, args);
                if(method.getName().equals("close") && method.getParameters().length == 0){
                    session.close();
                }
            return result;
            }catch(InvocationTargetException ex){
                throw ex.getCause();
            }
            
            
        }
        
    }
    
    private static class LoggingOutputStream extends OutputStream{
        private static final int BUFFER_LENGTH = 2048;
        
        private final Logger Logger;
        private final Level logLevel;
        
        private boolean closed = false;
        private byte[] buffer = new byte[BUFFER_LENGTH];
        private int bytesInBuffer = 0;

        public LoggingOutputStream(Logger Logger, Level logLevel) {
            this.Logger = Logger;
            this.logLevel = logLevel;
        }
        
        @Override
        public void write(int b) throws IOException {
            if(closed){
                throw new IOException("This stream has been closed");
            }
            if(b == 0){
                return;
            }
            int currentLength = buffer.length;
            if(bytesInBuffer == currentLength){
                byte[] newBuffer = new byte[currentLength+BUFFER_LENGTH];
                System.arraycopy(buffer, 0, newBuffer, 0, currentLength);
                buffer = newBuffer;
            }
            buffer[bytesInBuffer] = (byte)b;
            bytesInBuffer++;
        }

        @Override
        public void close() throws IOException {
            flush();
            closed = true;
        }

        @Override
        public void flush() throws IOException {
            if(bytesInBuffer == 0 || closed){
                return;
            }
            byte[] bytesToWrite = new byte[bytesInBuffer];
            System.arraycopy(buffer, 0, bytesToWrite, 0, bytesInBuffer);
            String logMessage = new String(bytesToWrite);
            Logger.log(logLevel, logMessage);
            bytesInBuffer = 0;
        }
        
        
        
    }
    
}
    