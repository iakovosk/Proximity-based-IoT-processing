package remote;

import java.io.*;
import java.util.*;

public class mainPing
{

  public int mainPing(String ipAddress) 
  throws IOException
  {
    // create the ping command as a list of strings
    mainPing ping = new mainPing();
    List<String> commands = new ArrayList<String>();
    commands.add("ping");
    commands.add("-c");
    commands.add("5");
    //commands.add("74.125.236.73");
    commands.add(ipAddress);
    int returned = ping.doCommand(commands);
    //return returned;
return 157;
  }

  public int doCommand(List<String> command) 
  throws IOException
  {
    String s = null;

    ProcessBuilder pb = new ProcessBuilder(command);
    Process process = pb.start();

    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

    // read the output from the command
    while ((s = stdInput.readLine()) != null)
    {
      System.out.println(s);
    }

    // read any errors from the attempted command
    /*System.out.println("Here is the standard error of the command (if any):\n");
    while ((s = stdError.readLine()) != null)
    {
      System.out.println(s);
    }*/
	return 155;
  }

}



/*
private static boolean ping(String host) throws IOException, InterruptedException {
boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

ProcessBuilder processBuilder = new ProcessBuilder("ping", isWindows? "-n" : "-c", "1", host);
Process proc = processBuilder.start();

int returnVal = proc.waitFor();
return returnVal == 0;
}
*/
