package org.ops4j.api;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
    public Object intercept(@Origin String method, @AllArguments Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        SqlSession sqlSession = sqlSessionFactory.openSession();
        Object mapper = sqlSession.getMapper(originalClass);

        List<Class> argumentTypes=new ArrayList<>();
        for (Object o:args)
        {
            argumentTypes.add(o.getClass());
        }

        Class[] classArray= argumentTypes.toArray(new Class[0]);
        Method methodInstance = mapper.getClass().getDeclaredMethod(method,classArray);

        Object invoke = methodInstance.invoke(mapper, args);
        sqlSession.close();

        System.out.println("I have intercepted a call");

        return invoke;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}