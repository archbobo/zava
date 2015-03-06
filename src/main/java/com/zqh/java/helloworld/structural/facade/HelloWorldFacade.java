package com.zqh.java.helloworld.structural.facade;

import com.zqh.java.helloworld.HelloWorld;
import com.zqh.java.helloworld.creational.builder.HelloWorldBuilder;

/**
 * @author yihua.huang@dianping.com
 */
public class HelloWorldFacade {

    private HelloWorldFacade(){
    }

    /**
     * facade is usually used with singleton
     */
    public static HelloWorldFacade instance(){
        return HelloWorldFacadeInstanceHolder.INSTANCE;
    }

    private static class HelloWorldFacadeInstanceHolder {
        private final static HelloWorldFacade INSTANCE = new HelloWorldFacade();
    }

    public HelloWorld facadeHelloWorld(){
        return HelloWorldBuilder.builder()
                .interjection("Hello")
                .object("Facade").getHelloWorld();
    }

}
