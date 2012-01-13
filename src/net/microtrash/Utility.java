package net.microtrash;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Utility {
	
	/** Read the object from Base64 string. */
    public static Object fromString( String s ) throws IOException, ClassNotFoundException {
		byte [] data = Base64.decodeFast(s.getBytes());
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(  data ));
		Object o  = ois.readObject();
		ois.close();
		return o;
	}

    /** Write the object to a Base64 string. */
    public static String toString( Serializable o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return Base64.encodeToString(baos.toByteArray(), false);
    }

}
