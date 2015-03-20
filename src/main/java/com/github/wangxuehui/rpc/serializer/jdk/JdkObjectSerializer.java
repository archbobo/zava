package com.github.wangxuehui.rpc.serializer.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.github.wangxuehui.rpc.serializer.Deserializer;
import com.github.wangxuehui.rpc.serializer.Serializer;

public class JdkObjectSerializer implements Serializer, Deserializer {

    @Override
    public <T> T deserialize( final byte[] bytes , final Class<T> clazz ) {
        final ByteArrayInputStream in = new ByteArrayInputStream( bytes );
        try {
            final ObjectInputStream ois = new ObjectInputStream( in );
            Object obj = ois.readObject();
            return clazz.cast( obj );
        }
        catch ( final ClassNotFoundException e ) {
            throw new IllegalStateException( e.getMessage() , e );
        }
        catch ( final IOException e ) {
            throw new IllegalStateException( e.getMessage() , e );
        }
        finally {
            try {
                in.close();
            }
            catch ( final IOException e ) {
                throw new IllegalStateException( e.getMessage() , e );
            }
        }
    }

    @Override
    public <T> byte[] serialize( final T source ) {

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            final ObjectOutputStream oos = new ObjectOutputStream( out );
            oos.writeObject( source );
            oos.flush();
        }
        catch ( final IOException e ) {
            throw new IllegalStateException( e.getMessage() , e );
        }
        finally {
            try {
                out.close();
            }
            catch ( final IOException e ) {
                throw new IllegalStateException( e.getMessage() , e );
            }
        }

        return out.toByteArray();
    }
}
