package com.github.oogasawa.benchmark;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class BenchmarkCommands {

    private static final Logger logger = Logger.getLogger(BenchmarkCommands.class.getName());
    
    /**
     * The command repository used to register commands.
     */
    CommandRepository cmdRepos = null;
    

    public void setupCommands(CommandRepository cmds) {
        this.cmdRepos = cmds;
        
        benchmarkRunCommand();
        processWatchCommand();
    }


    public void benchmarkRunCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                .longOpt("interval")
                .hasArg(true)
                .argName("SECONDS") // to make the CLI help output clearer (e.g., -i <SECONDS>)
                .desc("Interval in seconds between each statistics output.")
                .required(false)
                .build());

        opts.addOption(Option.builder("n")
                .longOpt("basename")
                .hasArg(true)
                .argName("FILENAME") 
                .desc("Base name for statistics output files")
                .required(false)
                .build());

        opts.addOption(Option.builder("g")
                .longOpt("gpu")
                .hasArg(false)
                //.argName("true or false") 
                .desc("Enable GPU monitoring (default: disabled)")
                .required(false)
                .build());

        
        
        this.cmdRepos.addCommand("benchmark commands", "benchmark:run", opts,
                "Execute an arbitrary command and collect statistics while it is running.",
                (CommandLine cl) -> {
                    int interval = Integer.valueOf(cl.getOptionValue("interval", "10"));
                    String basename = cl.getOptionValue("basename", "stats");
                    List<String> commands = cl.getArgList();
                    commands = commands.subList(1, commands.size()); // remove the 0th element (which is always "stats:run") 
                    boolean gpuFlg = cl.hasOption("gpu");

                    SimpleMonitor stats = new SimpleMonitor();
                    stats.executeWithMonitoring(commands, interval, basename, gpuFlg);
                });
    }


    public void processWatchCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                .longOpt("interval")
                .hasArg(true)
                .argName("SECONDS") // to make the CLI help output clearer (e.g., -i <SECONDS>)
                .desc("Interval in seconds between each statistics output.")
                .required(false)
                .build());

        opts.addOption(Option.builder("u")
                .longOpt("username")
                .hasArg(true)
                .argName("USERNAME") 
                .desc("The user whose processes are being monitored.")
                .required(true)
                .build());

        
        this.cmdRepos.addCommand("benchmark commands", "benchmark:processWatch", opts,
                "Continuously monitors process creation and termination events.",
                (CommandLine cl) -> {
                    int interval = Integer.valueOf(cl.getOptionValue("interval", "10"));
                    String username = cl.getOptionValue("username");
                    List<String> commands = cl.getArgList();
                    commands = commands.subList(1, commands.size()); // remove the 0th element (which is always "stats:run") 
                    
                    ProcessWatcher watcher = new ProcessWatcher(username, interval);
                    try {
                        watcher.watchAndRun(commands);
                    } catch (IOException | InterruptedException e) {
                        logger.log(Level.SEVERE, "Error: ", e);
                    }
                });
    }


    
}
