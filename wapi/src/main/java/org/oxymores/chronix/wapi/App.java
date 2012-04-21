package org.oxymores.chronix.wapi;

public class App 
{
    public static void main( String[] args )
    {
        JettyServer server = new JettyServer();
        server.start();
        try {
			Thread.sleep(60000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
