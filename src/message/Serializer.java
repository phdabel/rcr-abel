package message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class Serializer {
    
	public static byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.flush();
        oos.close();

        return baos.toByteArray();
    }

    
    public static Object deserialize(byte[] ba) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);

        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }


}