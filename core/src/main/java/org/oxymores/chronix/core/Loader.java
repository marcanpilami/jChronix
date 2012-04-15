package org.oxymores.chronix.core;

import java.io.FileOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Loader {
	public static void ser(Application a) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Application.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		m.marshal(a, System.out);
	}

	public static void ser2(Application a, String filePath) {
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(a);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public static Application deSerialize(String filePath) throws FileNotFoundException, IOException, ClassNotFoundException {

		FileInputStream fis = new FileInputStream(filePath);
		ObjectInputStream ois = new ObjectInputStream(fis);

		return (Application)ois.readObject();
	}

}
