package com.github.oogasawa.benchmark.ngs;


import java.util.logging.Logger;
import com.github.oogasawa.benchmark.BenchmarkCommands;
import com.github.oogasawa.benchmark.cuda.GpuCommands;
import com.github.oogasawa.benchmark.ngs.parabricks.ParabricksCommands;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;



public class App {

    private static final Logger logger = Logger.getLogger(App.class.getName());
    
    /**
     * The command-line usage synopsis.
     */
    String synopsis = "java -jar benchmark-ngs-<VERSION>.jar <command> <options>";
    
    /**
     * The repository that holds command definitions and executes them.
     */
    CommandRepository cmds = new CommandRepository();

    /**
     * The main method initializes the application and processes command-line input.
     * 
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        App app = new App();

        // Load the command definitions.
        app.setupCommands();

        try {
            CommandLine cl = app.cmds.parse(args);
            String command = app.cmds.getGivenCommand();

            if (command == null) {
                app.cmds.printCommandList(app.synopsis);
            } else if (app.cmds.hasCommand(command)) {
                app.cmds.execute(command, cl);
            } else {
                System.err.println("Error: Unknown command: " + app.cmds.getGivenCommand());
                System.err.println("Use one of the available commands listed below:");
                app.cmds.printCommandList(app.synopsis);
            }
        } catch (ParseException e) {
            System.err.println("Error: Failed to parse the command. Reason: " + e.getMessage());
            System.err.println("See the help below for correct usage:");
            app.cmds.printCommandHelp(app.cmds.getGivenCommand());
        }
    }



    /**
     * Registers all available commands by invoking their respective setup methods.
     */
    public void setupCommands() {

        var benchmarkCommands = new BenchmarkCommands();
        benchmarkCommands.setupCommands(this.cmds);
        
        var gpuCommands = new GpuCommands();
        gpuCommands.setupCommands(this.cmds);

        var parabricksCommands = new ParabricksCommands();
        parabricksCommands.setupCommands(this.cmds); 
    }



}
