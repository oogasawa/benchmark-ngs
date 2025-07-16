package com.github.oogasawa.benchmark.cuda;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;


public class GpuCommands {

    private static final Logger logger = LoggerFactory.getLogger(GpuCommands.class);
    /**
     * The command repository used to register commands.
     */
    CommandRepository cmdRepos = null;
    
    /**
     * Registers all JAR-related commands in the given command repository.
     * 
     * @param cmds The command repository to register commands with.
     */
    public void setupCommands(CommandRepository cmds) {
        this.cmdRepos = cmds;

        formatGpuCommand();
        formatGpuMemoryCommand();
        visGpuCommand();
        visGpuMemoryCommand();
    }

        
    public void formatGpuCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                       .longOpt("infile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the nvidia-smi log file.")
                       .required(true)
                       .build());


        opts.addOption(Option.builder("o")
                       .longOpt("outfile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the output file with csv format.")
                       .required(false)
                       .build());


        
        this.cmdRepos.addCommand("format commands", "format:gpu", opts,
                             "Generate a CSV file for a stacked area chart of GPU utilization.",
                             (CommandLine cl) -> {
                                 Path infile = Path.of(cl.getOptionValue("infile"));


                                 Path outfile;
                                 if (cl.hasOption("outfile")) {
                                     outfile = Path.of(cl.getOptionValue("outfile"));
                                 } else {
                                     // Replace extension with .csv
                                     String baseName = infile.getFileName().toString();
                                     int dotIndex = baseName.lastIndexOf('.');
                                     String csvName = (dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName) + ".csv";
                                     outfile = infile.resolveSibling(csvName);
                                 }

                    
                                 GpuUsageFormatter.gpuUsage(infile, outfile);                    
                             });
    }



        
    public void formatGpuMemoryCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                       .longOpt("infile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the nvidia-smi log file.")
                       .required(true)
                       .build());


        opts.addOption(Option.builder("o")
                       .longOpt("outfile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the output file with csv format.")
                       .required(false)
                       .build());


        this.cmdRepos.addCommand("format commands", "format:gpuMemory", opts,
                             "Generate a CSV file for a stacked area chart of GPU memory utilization.",                             
                             (CommandLine cl) -> {
                                 Path infile = Path.of(cl.getOptionValue("infile"));

                                 Path outfile;
                                 if (cl.hasOption("outfile")) {
                                     outfile = Path.of(cl.getOptionValue("outfile"));
                                 } else {
                                     // Replace extension with .csv
                                     String baseName = infile.getFileName().toString();
                                     int dotIndex = baseName.lastIndexOf('.');
                                     String csvName = (dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName) + ".csv";
                                     outfile = infile.resolveSibling(csvName);
                                 }

                
                                 GpuUsageFormatter.gpuMemoryUsage(infile, outfile);
                             });
    }


    public void visGpuCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                       .longOpt("infile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the nvidia-smi log file.")
                       .required(true)
                       .build());


        opts.addOption(Option.builder("o")
                       .longOpt("outfile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the output file with png format.")
                       .required(false)
                       .build());

    
    
        this.cmdRepos.addCommand("Visualization commands", "vis:gpu", opts,
                             "Draw a graph for a stacked area chart of GPU utilization.",
                             (CommandLine cl) -> {
                                 Path infile = Path.of(cl.getOptionValue("infile"));


                                 Path outfile;
                                 if (cl.hasOption("outfile")) {
                                     outfile = Path.of(cl.getOptionValue("outfile"));
                                 } else {
                                     // Replace extension with .csv
                                     String baseName = infile.getFileName().toString();
                                     int dotIndex = baseName.lastIndexOf('.');
                                     String csvName = (dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName) + ".png";
                                     outfile = infile.resolveSibling(csvName);
                                 }

                                 
                                 try {
                                     Table gpuUsageTable = GpuUsageFormatter.pivotGpuMetric(infile, "utilization.gpu");
                                     GpuUsageChart.draw(gpuUsageTable, outfile, "GPU Utilization");
                                     
                                } catch (IOException e) {
                                    logger.error("Can not draw Gpu Usage Chart", e);
                                }                    
                             });
    }



        
    public void visGpuMemoryCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                       .longOpt("infile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the nvidia-smi log file.")
                       .required(true)
                       .build());


        opts.addOption(Option.builder("o")
                       .longOpt("outfile")
                       .hasArg(true)
                       .argName("FILE")
                       .desc("The path to the output file with csv format.")
                       .required(false)
                       .build());


        this.cmdRepos.addCommand("Visualization commands", "vis:gpuMemory", opts,
                             "Generate a CSV file for a stacked area chart of GPU memory utilization.",                             
                             (CommandLine cl) -> {
                                 Path infile = Path.of(cl.getOptionValue("infile"));

                                 Path outfile;
                                 if (cl.hasOption("outfile")) {
                                     outfile = Path.of(cl.getOptionValue("outfile"));
                                 } else {
                                     // Replace extension with .csv
                                     String baseName = infile.getFileName().toString();
                                     int dotIndex = baseName.lastIndexOf('.');
                                     String csvName = (dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName) + ".png";
                                     outfile = infile.resolveSibling(csvName);
                                 }

                                 try {
                                     Table gpuUsageTable = GpuUsageFormatter.pivotGpuMetric(infile, "utilization.memory");
                                     GpuUsageChart.draw(gpuUsageTable, outfile, "GPU Memory Utilization");
                                     
                                } catch (IOException e) {
                                    logger.error("Can not draw Gpu Usage Chart", e);
                                }                    
                
                             });
    }

    

    

}
