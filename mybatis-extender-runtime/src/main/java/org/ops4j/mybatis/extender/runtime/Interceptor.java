package org.ops4j.mybatis.extender.runtime;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nmw on 26-04-2017.
 */
public class Interceptor {
    private SqlSessionFactory sqlSessionFactory;
    private Class originalClass;


    public Interceptor(Class originalClass) {


        this.originalClass = originalClass;
    }



    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        SqlSession sqlSession = sqlSessionFactory.openSession();
        Object mapper = sqlSession.getMapper(originalClass);

        Method methodInstance = mapper.getClass().getDeclaredMethod(method.getName(),method.getParameterTypes());

        Object invoke = methodInstance.invoke(mapper, args);
        sqlSession.commit();
        sqlSession.close();

        return invoke;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}