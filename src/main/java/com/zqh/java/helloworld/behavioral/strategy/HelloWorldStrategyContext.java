package com.zqh.java.helloworld.behavioral.strategy;

import com.zqh.java.helloworld.HelloWorld;

/**
 * @author yihua.huang@dianping.com
 */
public class HelloWorldStrategyContext implements HelloWorld{

    private HelloWorldStrategy helloWorldStrategy;

    public HelloWorldStrategyContext(HelloWorldStrategy helloWorldStrategy) {
        this.helloWorldStrategy = helloWorldStrategy;
    }

    @Override
    public String helloWorld() {
        return helloWorldStrategy.helloWorld();
    }
}
