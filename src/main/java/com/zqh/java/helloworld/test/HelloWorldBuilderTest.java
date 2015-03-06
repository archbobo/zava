package com.zqh.java.helloworld.test;

import com.zqh.java.helloworld.HelloWorld;
import com.zqh.java.helloworld.creational.builder.HelloWorldBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.hamcrest.Matchers;
/**
 * @author yihua.huang@dianping.com
 */
public class HelloWorldBuilderTest {

    @Test
    public void testHelloWorldBuilder(){
        HelloWorld builderHelloWorld = HelloWorldBuilder.builder()
                .interjection("Hello")
                .object("Builder").getHelloWorld();
        MatcherAssert.assertThat(builderHelloWorld.helloWorld(), Matchers.is("Hello Builder!"));

        HelloWorld helloWorld = HelloWorldBuilder.builder()
                .interjection("Hello")
                .object("World").getHelloWorld();
        MatcherAssert.assertThat(helloWorld.helloWorld(), Matchers.is("Hello World!"));
    }
}
