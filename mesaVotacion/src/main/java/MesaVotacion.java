import javax.swing.*;
import mesaVotacion.*;

public class MesaVotacion {

    public static void main(String[] args)
    {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try(com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "mesa.cfg", extraArgs))
        {
            if(!extraArgs.isEmpty())
            {
                System.err.println("too many arguments");
                status = 1;
            }
            else
            {
                status = run(communicator);
            }
        }

        System.exit(status);
    }

    private static int run(com.zeroc.Ice.Communicator communicator)
    {
        //
        // First we try to connect to the object with the `hello'
        // identity. If it's not registered with the registry, we
        // search for an object with the ::Demo::Hello type.
        //
        IRegistrarVotoPrx registrarVoto = null;
        com.zeroc.IceGrid.QueryPrx query =
            com.zeroc.IceGrid.QueryPrx.checkedCast(communicator.stringToProxy("DemoIceGrid/Query"));
        try
        {
            registrarVoto = IRegistrarVotoPrx.checkedCast(communicator.stringToProxy("regionalAdapter"));
        }
        catch(com.zeroc.Ice.NotRegisteredException ex)
        {
            registrarVoto = IRegistrarVotoPrx.checkedCast(query.findObjectByType("::Demo::IRegistrarVoto"));
        }
        if(registrarVoto == null)
        {
            System.err.println("couldn't find a `::Demo::IRegistrarVoto' object");
            return 1;
        }

        menu();

        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

        String line = null;
        do
        {
            try
            {
                System.out.print("==> ");
                System.out.flush();
                line = in.readLine();
                if(line == null)
                {
                    break;
                }
                if(line.equals("t"))
                {
                    registrarVoto.enviarVoto();
                }
                else if(line.equals("x"))
                {
                    // Nothing to do
                }
                else if(line.equals("?"))
                {
                    menu();
                }
                else
                {
                    System.out.println("unknown command `" + line + "'");
                    menu();
                }
            }
            catch(java.io.IOException ex)
            {
                ex.printStackTrace();
            }
            catch(com.zeroc.Ice.LocalException ex)
            {
                ex.printStackTrace();
            }
             registrarVoto = IRegistrarVotoPrx.checkedCast(query.findObjectByType("::Demo::IRegistrarVoto"));
        }
        while(!line.equals("x"));

        return 0;
    }

    private static void menu()
    {
        System.out.println(
            "usage:\n" +
            "t: enviar voto\n" +
            "x: exit\n" +
            "?: help\n");
    }

}
