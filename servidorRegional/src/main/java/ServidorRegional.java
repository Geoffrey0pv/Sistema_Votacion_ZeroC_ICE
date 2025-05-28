import Demo.IRegistrarVotoPrx;
import servidorRegional.*;
import com.zeroc.Ice.*;
import java.lang.Exception;

public class ServidorRegional {
    public static void main(String[] args)
    {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<String>();

        try(com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, extraArgs))
        {
            communicator.getProperties().setProperty("Ice.Default.Package", "com.zeroc.demos.IceGrid.simple");
       
            Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));

            if(!extraArgs.isEmpty())
            {
                System.err.println("too many arguments");
                status = 1;
            }
            else
            {
                com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("RegionalAdapter");
                com.zeroc.Ice.Properties properties = communicator.getProperties();
                com.zeroc.Ice.Identity id = com.zeroc.Ice.Util.stringToIdentity(properties.getProperty("Identity"));
                adapter.add(new ReceptorVotos(properties.getProperty("Ice.ProgramName")), id);
                adapter.activate();
                communicator.waitForShutdown();
            }
        }

        System.exit(status);
    }
}